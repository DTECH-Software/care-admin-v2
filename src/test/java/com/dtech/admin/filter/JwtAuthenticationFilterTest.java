package com.dtech.admin.filter;

import com.dtech.admin.repository.WebUserRepository;
import com.dtech.admin.util.JwtUtil;
import com.dtech.admin.util.ResponseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    @Test
    void allowsSplashWithoutJwtAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                mock(ResponseUtil.class),
                mock(WebUserRepository.class),
                mock(JwtUtil.class)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/login/splash");
        request.setServletPath("/api/v1/login/splash");

        assertTrue(filter.shouldNotFilter(request));
    }
}
