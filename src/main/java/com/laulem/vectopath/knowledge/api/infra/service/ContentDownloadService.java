package com.laulem.vectopath.knowledge.api.infra.service;

import com.laulem.vectopath.knowledge.api.business.exception.ContentDownloadException;
import com.laulem.vectopath.knowledge.api.business.exception.HttpDownloadException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.business.service.ContentDownloaderUseCase;
import com.laulem.vectopath.knowledge.api.infra.properties.ContentDownloadProperties;
import com.laulem.vectopath.knowledge.api.shared.util.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class ContentDownloadService implements ContentDownloaderUseCase {
    private static final Logger logger = LoggerFactory.getLogger(ContentDownloadService.class);
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final String INVALID_URL_CODE = "INVALID_URL";
    private static final String URL_FIELD = "url";
    private static final int READ_BUFFER_SIZE = 8192;

    private final RestClient restClient;
    private final ContentDownloadProperties contentDownloadProperties;

    public ContentDownloadService(ContentDownloadProperties contentDownloadProperties, RestClient contentDownloadRestClient) {
        this.contentDownloadProperties = contentDownloadProperties;
        this.restClient = contentDownloadRestClient;
    }

    @Override
    public String downloadContent(String url) throws IOException {
        URI uri = validateUrl(url);
        logger.info("Downloading content from URL: {}", StringUtils.sanitizeForLog(uri.toString()));

        try {
            return restClient.get()
                    .uri(uri)
                    .exchange((_, response) -> {
                        if (response.getStatusCode().value() != HttpStatus.OK.value()) {
                            throw new HttpDownloadException(response.getStatusCode().value(), url);
                        }

                        try (InputStream responseBody = response.getBody()) {
                            byte[] body = readWithSizeLimit(responseBody, contentDownloadProperties.getMaxSizeBytes());
                            Charset charset = resolveCharset(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));

                            logger.info("Content successfully downloaded from: {}", StringUtils.sanitizeForLog(uri.toString()));
                            return Jsoup.parse(new String(body, charset)).text();
                        }
                    });
        } catch (ResourceAccessException e) {
            throw new ContentDownloadException(url, e);
        }
    }

    /**
     * Reads the response body incrementally, failing fast once the configured
     * size limit is exceeded instead of buffering an unbounded payload in memory.
     */
    private byte[] readWithSizeLimit(InputStream inputStream, long maxSizeBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[READ_BUFFER_SIZE];
        long totalRead = 0;
        int bytesRead;

        while ((bytesRead = inputStream.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxSizeBytes) {
                throw new ParamException(
                        "CONTENT_TOO_LARGE",
                        "Downloaded content exceeds the maximum allowed size of " + maxSizeBytes + " bytes",
                        URL_FIELD);
            }
            buffer.write(chunk, 0, bytesRead);
        }

        return buffer.toByteArray();
    }

    /**
     * Resolves the response charset from the Content-Type header, falling back to UTF-8
     * when it is missing, malformed, or does not specify a charset.
     */
    private Charset resolveCharset(String contentTypeHeader) {
        if (contentTypeHeader == null) {
            return StandardCharsets.UTF_8;
        }

        try {
            Charset charset = MediaType.parseMediaType(contentTypeHeader).getCharset();
            return charset != null ? charset : StandardCharsets.UTF_8;
        } catch (InvalidMediaTypeException _) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * Rejects any URL that could be used to reach internal infrastructure (SSRF).
     */
    private URI validateUrl(String url) {
        if (StringUtils.isNullOrBlank(url)) {
            throw new ParamException(INVALID_URL_CODE, "URL must not be blank", URL_FIELD);
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException _) {
            throw new ParamException(INVALID_URL_CODE, "URL is malformed", URL_FIELD);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new ParamException(INVALID_URL_CODE, "Only http and https URLs are supported", URL_FIELD);
        }

        String host = uri.getHost();
        if (StringUtils.isNullOrBlank(host)) {
            throw new ParamException(INVALID_URL_CODE, "URL must contain a valid host", URL_FIELD);
        }

        if (uri.getUserInfo() != null) {
            throw new ParamException(INVALID_URL_CODE, "URL must not contain user information", URL_FIELD);
        }

        if (!contentDownloadProperties.getAllowedHosts().isEmpty()
                && contentDownloadProperties.getAllowedHosts().stream().noneMatch(allowed -> allowed.equalsIgnoreCase(host))) {
            throw new ParamException(INVALID_URL_CODE, "This host is not allowed", URL_FIELD);
        }

        if (contentDownloadProperties.isBlockInternalNetworks()) {
            ensureHostIsPublic(host);
        }

        return uri;
    }

    private void ensureHostIsPublic(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException _) {
            throw new ParamException(INVALID_URL_CODE, "URL host could not be resolved", URL_FIELD);
        }

        for (InetAddress address : addresses) {
            if (isInternal(address)) {
                logger.warn("Blocked download targeting an internal address: host={}", StringUtils.sanitizeForLog(host));
                throw new ParamException(INVALID_URL_CODE, "URL targets a non-public address", URL_FIELD);
            }
        }
    }

    private boolean isInternal(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address)
                || isSharedAddressSpace(address);
    }

    /**
     * IPv6 unique local addresses (fc00::/7), not covered by isSiteLocalAddress.
     */
    private boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    /**
     * Carrier-grade NAT range 100.64.0.0/10, not covered by isSiteLocalAddress.
     */
    private boolean isSharedAddressSpace(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4 && (bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 0x40;
    }
}
