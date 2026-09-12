package com.dtech.admin.maintenance;

import com.dtech.admin.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.core.MethodParameter;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
@Log4j2
public class SystemMaintenanceFilter extends OncePerRequestFilter implements ResponseBodyAdvice<Object> {
    private static final String APPLICATION = "CARE_ADMIN";
    private static final String STATE_ATTRIBUTE = "wecare.maintenance.state";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private final JdbcTemplate jdbcTemplate;
    private volatile CacheEntry cache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MaintenanceState state = currentState();
        request.setAttribute(STATE_ATTRIBUTE, state);
        setHeaders(response, state);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.contains("/swagger")
                || path.contains("/v3/api-docs")
                || path.endsWith("/error");
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        MaintenanceState state = stateFromRequest();
        if (body instanceof ApiResponse<?> apiResponse) {
            apiResponse.setUnderMaintenance(state.underMaintenance());
            apiResponse.setMaintenance(state.response());
        }
        response.getHeaders().set("X-Under-Maintenance", Boolean.toString(state.underMaintenance()));
        return body;
    }

    private MaintenanceState currentState() {
        CacheEntry current = cache;
        Instant now = Instant.now();
        if (current != null && current.loadedAt().plus(CACHE_TTL).isAfter(now)) return evaluate(current.config());

        try {
            List<MaintenanceConfig> rows = jdbcTemplate.query("""
                    SELECT maintenance_enabled, title, message, start_at, end_at
                    FROM system_maintenance_config
                    WHERE application = ?
                    LIMIT 1
                    """, (resultSet, rowNum) -> new MaintenanceConfig(
                    resultSet.getBoolean("maintenance_enabled"),
                    resultSet.getString("title"),
                    resultSet.getString("message"),
                    toLocalDateTime(resultSet.getTimestamp("start_at")),
                    toLocalDateTime(resultSet.getTimestamp("end_at"))), APPLICATION);
            MaintenanceConfig config = rows.isEmpty() ? null : rows.get(0);
            cache = new CacheEntry(config, now);
            return evaluate(config);
        } catch (Exception exception) {
            cache = new CacheEntry(null, now);
            log.warn("Unable to load maintenance configuration for {}; requests will remain available",
                    APPLICATION, exception);
            return MaintenanceState.available();
        }
    }

    private MaintenanceState evaluate(MaintenanceConfig config) {
        if (config == null) return MaintenanceState.available();
        boolean active = isActive(config.enabled(), config.startAt(), config.endAt(), LocalDateTime.now());
        MaintenanceResponse details = config.enabled() ? MaintenanceResponse.builder()
                .title(config.title()).message(config.message())
                .startAt(config.startAt()).endAt(config.endAt()).build() : null;
        return new MaintenanceState(active, details);
    }

    static boolean isActive(boolean enabled, LocalDateTime startAt, LocalDateTime endAt,
                            LocalDateTime now) {
        boolean started = startAt == null || !now.isBefore(startAt);
        boolean notEnded = endAt == null || !now.isAfter(endAt);
        return enabled && started && notEnded;
    }

    private MaintenanceState stateFromRequest() {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes) {
            Object state = servletAttributes.getRequest().getAttribute(STATE_ATTRIBUTE);
            if (state instanceof MaintenanceState maintenanceState) return maintenanceState;
        }
        return MaintenanceState.available();
    }

    private void setHeaders(HttpServletResponse response, MaintenanceState state) {
        response.setHeader("X-Under-Maintenance", Boolean.toString(state.underMaintenance()));
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record MaintenanceConfig(boolean enabled, String title, String message,
                                     LocalDateTime startAt, LocalDateTime endAt) {
    }

    private record CacheEntry(MaintenanceConfig config, Instant loadedAt) {
    }

    private record MaintenanceState(boolean underMaintenance, MaintenanceResponse response) {
        private static MaintenanceState available() {
            return new MaintenanceState(false, null);
        }
    }
}
