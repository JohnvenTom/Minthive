package com.minthive.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/** HTTP boundary only: canonical references in storage, short-lived viewer-bound URLs on the wire. */
@RestControllerAdvice(basePackages = "com.minthive.controller")
@RequiredArgsConstructor
public class MediaBodyAdvice extends RequestBodyAdviceAdapter implements ResponseBodyAdvice<Object> {
    private final ObjectMapper json;
    private final MediaLocator locator;
    private final MediaAccessService access;
    private final MediaTicketService tickets;

    @Override
    public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converter);
    }

    @Override
    public boolean supports(MethodParameter parameter, Class<? extends HttpMessageConverter<?>> converter) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converter);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type type,
                                           Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        // Rewrite raw JSON BEFORE DTO binding: write-only fields (e.g. passwords) must not be lost by reserialization of a DTO.
        JsonNode body = json.readTree(input.getBody());
        byte[] bytes = json.writeValueAsBytes(rewrite(body, currentViewer(), false, new HashMap<>(), 0));
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(input.getHeaders());
        headers.setContentLength(bytes.length);
        return new HttpInputMessage() {
            @Override public InputStream getBody() { return new ByteArrayInputStream(bytes); }
            @Override public HttpHeaders getHeaders() { return headers; }
        };
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter parameter, MediaType type,
                                  Class<? extends HttpMessageConverter<?>> converter,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) return null;
        Map<String, String> memo = new HashMap<>();
        // Copy the serialized shape, never mutate entities that may be persisted/cached elsewhere.
        JsonNode result = rewrite(json.valueToTree(body), currentViewer(), true, memo, 0);
        if (!memo.isEmpty()) {
            response.getHeaders().setCacheControl("private, no-store");
            response.getHeaders().set("Referrer-Policy", "no-referrer");
        }
        return result;
    }

    private Long currentViewer() { return UserContext.get() == null ? null : UserContext.get().getUserId(); }

    JsonNode rewrite(JsonNode node, Long viewer, boolean outgoing, Map<String, String> memo, int depth) {
        if (node == null) return null;
        if (depth > 64) throw new BusinessException(ResultCode.PARAM_ERROR, "媒体数据嵌套过深");
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.properties().forEach(entry -> object.set(entry.getKey(), rewrite(entry.getValue(), viewer, outgoing, memo, depth + 1)));
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) array.set(i, rewrite(array.get(i), viewer, outgoing, memo, depth + 1));
        } else if (node.isTextual()) {
            String value = node.textValue();
            MediaLocator.Ref ref;
            try {
                ref = locator.parse(value);
            } catch (IllegalArgumentException e) {
                if (outgoing) return TextNode.valueOf("");
                throw new BusinessException(ResultCode.PARAM_ERROR, "无效媒体路径");
            }
            if (ref != null) {
                String mapped = memo.computeIfAbsent(ref.canonical(), ignored -> {
                    try {
                        if (outgoing) access.requireReadable(ref, viewer);
                        else access.requireReference(ref, viewer);
                        return outgoing ? ref.canonical() + "?ticket=" + tickets.issue(ref, viewer) : ref.canonical();
                    } catch (BusinessException e) {
                        if (outgoing && (e.getCode().equals(ResultCode.FORBIDDEN.getCode())
                                || e.getCode().equals(ResultCode.NOT_FOUND.getCode()))) return "";
                        throw e;
                    }
                });
                return TextNode.valueOf(mapped);
            }
            // images are persisted as JSON strings; also support arrays/maps returned directly by DTOs.
            if (value.stripLeading().startsWith("[") || value.stripLeading().startsWith("{")) {
                JsonNode embedded;
                try { embedded = json.readTree(value); }
                catch (IOException ignored) { return node; }
                if (embedded != null && embedded.isContainerNode()) {
                    JsonNode original = embedded.deepCopy();
                    JsonNode transformed = rewrite(embedded, viewer, outgoing, memo, depth + 1);
                    if (original.equals(transformed)) return node;
                    try { return TextNode.valueOf(json.writeValueAsString(transformed)); }
                    catch (IOException e) { throw new IllegalStateException(e); }
                }
            }
        }
        return node;
    }
}
