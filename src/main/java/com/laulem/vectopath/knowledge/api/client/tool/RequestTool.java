package com.laulem.vectopath.knowledge.api.client.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laulem.vectopath.knowledge.api.shared.util.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RequestTool {

    private RequestTool() {
    }

    public static String getPath(final WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    public static String getHeaders(HttpServletRequest httpRequest) throws JsonProcessingException {
        Map<String, List<String>> headersMap = Collections.list(httpRequest.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        h -> Collections.list(httpRequest.getHeaders(h))
                ));
        return new ObjectMapper().writeValueAsString(headersMap);
    }

    public static String getInfoUrl(HttpServletRequest httpRequest) throws IOException {
        Map<String, String> result = new HashMap<>();
        result.put("URL", httpRequest.getRequestURL().toString());
        result.put("URI", httpRequest.getRequestURI());
        result.put("IP", UserUtils.getIpAddr(httpRequest));
        result.put("QUERY", httpRequest.getQueryString());
        result.put("METHOD", httpRequest.getMethod());

        BufferedReader reader = httpRequest.getReader();
        StringBuilder buffer = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
            buffer.append(System.lineSeparator());
        }
        String data = buffer.toString();
        result.put("BODY", data);

        return new ObjectMapper().writeValueAsString(result);
    }
}
