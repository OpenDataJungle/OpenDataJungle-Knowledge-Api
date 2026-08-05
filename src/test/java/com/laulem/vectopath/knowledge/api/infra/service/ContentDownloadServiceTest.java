package com.laulem.vectopath.knowledge.api.infra.service;

import com.laulem.vectopath.knowledge.api.business.exception.ContentDownloadException;
import com.laulem.vectopath.knowledge.api.business.exception.HttpDownloadException;
import com.laulem.vectopath.knowledge.api.business.exception.ParamException;
import com.laulem.vectopath.knowledge.api.infra.properties.ContentDownloadProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class ContentDownloadServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec uriSpec;

    @Mock
    private RestClient.RequestHeadersSpec headersSpec;

    @Mock
    private RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response;

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    private void stubExchange() {
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.exchange(any())).thenAnswer(invocation -> {
            RestClient.RequestHeadersSpec.ExchangeFunction<?> exchangeFunction = invocation.getArgument(0);
            return exchangeFunction.exchange(null, response);
        });
    }

    private void stubStatus(int statusCode) throws Exception {
        when(response.getStatusCode()).thenReturn(HttpStatus.valueOf(statusCode));
    }

    private void stubBody(String body) throws Exception {
        when(response.getBody()).thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    private void stubContentType(String contentTypeHeader) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (contentTypeHeader != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentTypeHeader);
        }
        when(response.getHeaders()).thenReturn(headers);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenUrlIsBlank() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent(" "))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("INVALID_URL"));
    }

    @Test
    void downloadContent_shouldThrowParamException_whenUrlIsMalformed() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://[invalid"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenSchemeIsNotHttpOrHttps() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("ftp://laulem.com/file.txt"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenHostIsMissing() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http:///path"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenUrlContainsUserInfo() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://user:pass@laulem.com"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenHostIsNotInAllowedList() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of("allowed.laulem.com")), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://not-allowed.laulem.com"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsLoopbackAddress() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://127.0.0.1/"))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("INVALID_URL"));
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsSiteLocalAddress() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://10.0.0.1/"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsLinkLocalAddress() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://169.254.1.1/"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsMulticastAddress() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://224.0.0.1/"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsInCarrierGradeNatRange() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://100.64.0.1/"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsUniqueLocalIpv6Address() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://[fd00::1]/"))
                .isInstanceOf(ParamException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenTargetIsLocalhost() {
        // Given
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://localhost/"))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("INVALID_URL"));
    }

    @Test
    void downloadContent_shouldThrowParamException_whenHostNameCannotBeResolved() {
        // Given: a single-label host name (no TLD, e.g. an internal-only DNS entry) that does not resolve.
        ContentDownloadService service = new ContentDownloadService(properties(true, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://this-host-does-not-exist-xyz123/"))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("INVALID_URL"));
    }

    @Test
    void downloadContent_shouldReturnParsedText_whenRequestSucceeds() throws Exception {
        // Given
        stubExchange();
        stubStatus(200);
        stubBody("<html><body><p>Hello World</p></body></html>");
        stubContentType("text/html; charset=UTF-8");
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When
        String result = service.downloadContent("http://laulem.com/page");

        // Then
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void downloadContent_shouldFallBackToUtf8_whenContentTypeHeaderIsMissing() throws Exception {
        // Given
        stubExchange();
        stubStatus(200);
        stubBody("<p>café</p>");
        stubContentType(null);
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When
        String result = service.downloadContent("http://laulem.com/page");

        // Then
        assertThat(result).isEqualTo("café");
    }

    @Test
    void downloadContent_shouldFallBackToUtf8_whenContentTypeHeaderIsMalformed() throws Exception {
        // Given
        stubExchange();
        stubStatus(200);
        stubBody("<p>hello</p>");
        stubContentType("not-a-valid-media-type");
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When
        String result = service.downloadContent("http://laulem.com/page");

        // Then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void downloadContent_shouldThrowHttpDownloadException_whenStatusIsNotOk() throws Exception {
        // Given
        stubExchange();
        stubStatus(404);
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://laulem.com/missing"))
                .isInstanceOf(HttpDownloadException.class);
    }

    @Test
    void downloadContent_shouldThrowParamException_whenContentExceedsMaxSize() throws Exception {
        // Given
        stubExchange();
        stubStatus(200);
        stubBody("0123456789");
        ContentDownloadProperties properties = properties(false, List.of());
        properties.setMaxSizeBytes(5L);
        ContentDownloadService service = new ContentDownloadService(properties, restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://laulem.com/big"))
                .isInstanceOf(ParamException.class)
                .satisfies(ex -> assertThat(((ParamException) ex).getCode()).isEqualTo("CONTENT_TOO_LARGE"));
    }

    @Test
    void downloadContent_shouldThrowContentDownloadException_whenThreadIsInterruptedDuringExchange() {
        // Given
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.exchange(any())).thenAnswer(_ -> {
            Thread.currentThread().interrupt();
            throw new ResourceAccessException("I/O error", new InterruptedIOException("interrupted"));
        });
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://laulem.com/page"))
                .isInstanceOf(ContentDownloadException.class);
    }

    @Test
    void downloadContent_shouldThrowContentDownloadException_whenConnectivityFails() {
        // Given
        ResourceAccessException connectException = new ResourceAccessException("I/O error", new ConnectException("refused"));
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.exchange(any())).thenThrow(connectException);
        ContentDownloadService service = new ContentDownloadService(properties(false, List.of()), restClient);

        // When & Then
        assertThatThrownBy(() -> service.downloadContent("http://laulem.com/page"))
                .isInstanceOf(ContentDownloadException.class)
                .hasCause(connectException);
    }

    private static ContentDownloadProperties properties(boolean blockInternalNetworks, List<String> allowedHosts) {
        ContentDownloadProperties properties = new ContentDownloadProperties();
        properties.setTimeoutSeconds(5);
        properties.setConnectTimeoutSeconds(5);
        properties.setMaxSizeBytes(1_000_000L);
        properties.setBlockInternalNetworks(blockInternalNetworks);
        properties.setAllowedHosts(allowedHosts);
        return properties;
    }
}
