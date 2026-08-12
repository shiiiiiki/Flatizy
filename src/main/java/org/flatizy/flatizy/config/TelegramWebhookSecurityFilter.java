package org.flatizy.flatizy.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.security.SecurityEventLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookSecurityFilter extends OncePerRequestFilter {

    @Value("${telegram.webhook.secret-token}")
    private String expectedSecretToken;
    private final SecurityEventLogger securityEventLogger;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().equals("/telegram/webhook") &&
            request.getMethod().equals("POST")) {

            String headerToken = request.getHeader("X-Telegram-Bot-Api-Secret-Token");

            if (headerToken == null || !headerToken.equals(expectedSecretToken)) {
                securityEventLogger.logWebhookUnauthorized(getClientIp(request));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Unauthorized\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
