package com.dtech.admin.filter;

import com.dtech.admin.dto.response.ApiResponse;
import com.dtech.admin.enums.Status;
import com.dtech.admin.model.WebUser;
import com.dtech.admin.repository.WebUserRepository;
import com.dtech.admin.util.JwtUtil;
import com.dtech.admin.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Log4j2
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ResponseUtil responseUtil;

    private final WebUserRepository webUserRepository;

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(ResponseUtil responseUtil, WebUserRepository webUserRepository, JwtUtil jwtUtil) {
        this.responseUtil = responseUtil;
        this.webUserRepository = webUserRepository;
        this.jwtUtil = jwtUtil;
    }

    private final List<AntPathRequestMatcher> excludedPaths = List.of(
            new AntPathRequestMatcher("/api/v1/login/login"),
            new AntPathRequestMatcher("/api/v1/password/reset"),
            new AntPathRequestMatcher("/api/v1/password/sent-otp"),
            new AntPathRequestMatcher("/api/v1/login/logout")
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        log.info(request.getRequestURI());
        return excludedPaths.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("JWT Authentication Filter Started {}", request.getRequestURI());

        try {
            String authorization = request.getHeader(AUTHORIZATION_HEADER);
            log.info("JWT Authentication Filter Authorized {}", authorization);
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                log.info("JWT Authentication Filter Authorized");
                String token = authorization.substring(7);
                String username = jwtUtil.extractUsername(token);

                Optional<WebUser> optionalUser = webUserRepository.findByUsernameAndStatus(username,Status.ACTIVE);
                if (optionalUser.isEmpty()) {
                    log.info("User Not Found or inactive{}", username);
                    sendUnauthorizedResponse(response, "Sorry,Can't find username or is inactive.");
                    return;
                }

                optionalUser =  webUserRepository.findByUsernameAndLoginStatus(username,Status.ACTIVE);

                if (optionalUser.isEmpty()) {
                    log.info("User found but login status inactive {}", username);
                    sendUnauthorizedResponse(response, "You are login status is inactive.");
                    return;
                }


                if (!optionalUser.get().getUserRole().getStatus().equals(Status.ACTIVE)) {
                    log.info("User role inactive or delete {} {}", username,optionalUser.get().getUserRole().getStatus());
                    sendUnauthorizedResponse(response, "You are user role is inactive or delete .");
                    return;
                }

                WebUser user = optionalUser.get();

                if (user.getWebToken() == null) {
                    sendUnauthorizedResponse(response, "User already logout.Please log in again.");
                    return;
                }

                if (!token.equals(user.getWebToken().getAccessToken())) {
                    sendUnauthorizedResponse(response, "Another user logged into your account.");
                    return;
                }

                if (!jwtUtil.validateToken(token, username, user.getUsername())) {
                    sendUnauthorizedResponse(response, "Token expired or invalid.");
                    return;
                }

                Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);

            }else{
                log.info("JWT Authentication Filter Authorization header missing");
                sendUnauthorizedResponse(response, "Authorization header is missing.");
                return;
            }

            log.info("Get JWT Authentication Filter Completed");
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in JWT Authentication Filter", e);
            sendUnauthorizedResponse(response, "An error occurred while processing the token");
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ApiResponse<Object> apiResponse = responseUtil.error(null, 1111, message);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
    }

}