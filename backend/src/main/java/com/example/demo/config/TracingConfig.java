package com.example.demo.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class TracingConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnBean(Tracer.class)
    public Filter traceIdFilter(Tracer tracer) {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                String incomingTraceId = httpRequest.getHeader("X-Trace-ID");
                String traceId = incomingTraceId;

                if (traceId == null && tracer.currentSpan() != null) {
                    traceId = tracer.currentSpan().context().traceId();
                }

                if (traceId == null) {
                    traceId = java.util.UUID.randomUUID().toString().replace("-", "");
                }

                MDC.put("traceId", traceId);
                httpResponse.setHeader("X-Trace-ID", traceId);

                try {
                    chain.doFilter(request, response);
                } finally {
                    MDC.remove("traceId");
                }
            }
        };
    }
}
