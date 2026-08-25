package com.medical.insurance.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.medical.insurance.service.impl.AuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService authService;

    SessionAuthenticationFilter(AuthService authService){this.authService=authService;}

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if(SecurityContextHolder.getContext().getAuthentication()==null)authService.restoreSecurityContext(request);
        chain.doFilter(request,response);
    }
}
