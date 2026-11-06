package com.petsalud.backendpetsalud.monitoreo;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class SuspiciousActivityFilter extends OncePerRequestFilter {

    private static final Pattern PATRON_SOSPECHOSO = Pattern.compile(
            "(<\\s*script|javascript:|onerror\\s*=|onload\\s*="   // XSS
            + "|'\\s*or\\s+'?\\d|union\\s+select|;\\s*drop\\s+table|--"   // Inyección SQL
            + "|\\.\\./)",                                      // Path traversal
            Pattern.CASE_INSENSITIVE);

    private final SuspiciousActivityService monitoreo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        request.getParameterMap().forEach((nombre, valores) -> {
            for (String valor : valores) {
                if (PATRON_SOSPECHOSO.matcher(valor).find()) {
                    monitoreo.registrarPatronSospechoso(ip, request.getMethod(), request.getRequestURI(), nombre + "=" + valor);
                }
            }
        });

        filterChain.doFilter(request, response);
    }
}