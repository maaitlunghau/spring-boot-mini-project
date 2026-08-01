package com.maaitlunghau.spring_boot_mini_project.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RequestUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void clientIpIgnoresClientSuppliedForwardedForHeader() {
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(request.getRemoteAddr()).thenReturn("10.0.0.7");

        assertThat(RequestUtils.clientIp(request))
            .as("this deployment has no reverse proxy, so a client-supplied X-Forwarded-For must not override the real peer address")
            .isEqualTo("10.0.0.7");
    }
}
