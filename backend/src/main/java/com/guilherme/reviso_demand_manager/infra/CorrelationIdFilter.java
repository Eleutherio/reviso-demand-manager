package com.guilherme.reviso_demand_manager.infra;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter que adiciona um Correlation ID (X-Request-Id) em cada requisição
 * para rastreabilidade de logs. Se o header já existir, reutiliza; caso contrário, gera um novo.
 */
@Component
public class CorrelationIdFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Request-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Obtém ou gera correlation ID
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Adiciona ao MDC para logs
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Adiciona ao response header
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        logger.info("Request: {} {} - Correlation ID: {}", 
            httpRequest.getMethod(), 
            httpRequest.getRequestURI(), 
            correlationId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            // Limpa MDC após requisição
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
