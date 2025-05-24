package org.clematis.logging.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.clematis.logging.service.TransactionIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that populates the Transaction ID in Response and MDC.
 * MDC is populated before each request is processed and cleaned in the end.
 * Transaction ID is populated based on the request header.
 * New transaction id is generated if header is not provided.
 */
@Component(value = "transactionIdFilter")
public class TransactionIdFilter extends OncePerRequestFilter {

    @Value("${clematis.transactionId.httpHeader}")
    private String transactionIdHeader;

    @Autowired
    private TransactionIdService transactionIdService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {

        final String transactionId = request.getHeader(transactionIdHeader);
        try (
            TransactionIdService.Transaction transaction = transactionIdService.startTransaction(transactionId)) {
            response.addHeader(transactionIdHeader,
                URLEncoder.encode(transaction.id(), StandardCharsets.UTF_8.displayName()));
            filterChain.doFilter(request, response);
        }
    }
}
