package com.minthive.audit;

import com.minthive.common.Result;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 审计响应结果采集
 *
 * <p>功能描述：在响应体被媒体地址改写前捕获 Result 的 code/msg，写入当前审计上下文</p>
 * <p>注意事项：必须高于 MediaBodyAdvice 执行，否则拿到的已是被改写的 JsonNode</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.minthive.controller")
public class AuditResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?> result) {
            AuditScope scope = AuditScope.current();
            if (scope != null) {
                scope.setResultCode(result.getCode());
                scope.setResultMsg(truncate(result.getMsg()));
            }
        }
        return body;
    }

    /**
     * 截断响应消息，避免超长文本写入审计表
     *
     * @param msg 响应消息
     * @return 不超过 255 字符的消息
     */
    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() <= 255 ? msg : msg.substring(0, 255);
    }
}
