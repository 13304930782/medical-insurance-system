package com.medical.insurance.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final SessionAuthenticationFilter sessionAuthenticationFilter;

    SecurityConfig(SessionAuthenticationFilter sessionAuthenticationFilter){this.sessionAuthenticationFilter=sessionAuthenticationFilter;}

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(authorize->authorize
                .requestMatchers("/","/index.html","/css/**","/js/**","/images/**","/favicon.ico").permitAll()
                .requestMatchers("/api/health","/api/auth/encryption-challenge","/api/auth/login","/api/auth/register","/api/auth/password-reset/**").permitAll()
                .requestMatchers("/api/admin/**","/api/audit-logs/**","/api/treatment-parameters/**","/api/bulk/**").hasRole("ADMIN")
                .requestMatchers("/api/ai/knowledge/**").hasRole("ADMIN")
                .requestMatchers("/api/approvals/**").hasAnyRole("ADMIN","APPROVER")
                .requestMatchers("/api/reimbursements/**").hasAnyRole("ADMIN","REIMBURSEMENT")
                .requestMatchers(HttpMethod.GET,"/api/medicines/**","/api/diagnoses/**","/api/facilities/**","/api/diseases/**","/api/institutions/**","/api/companies/**","/api/people/**","/api/dictionaries/**").authenticated()
                .requestMatchers("/api/medicines/**","/api/diagnoses/**","/api/facilities/**","/api/diseases/**","/api/institutions/**","/api/companies/**","/api/people/**").hasRole("ADMIN")
                .requestMatchers("/api/dictionaries/**","/api/auth/me","/api/auth/logout","/api/auth/change-password").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(errors->errors
                .authenticationEntryPoint((request,response,exception)->json(response,HttpServletResponse.SC_UNAUTHORIZED,"请先登录"))
                .accessDeniedHandler((request,response,exception)->json(response,HttpServletResponse.SC_FORBIDDEN,"当前角色无权执行该操作")))
            .addFilterBefore(sessionAuthenticationFilter,AnonymousAuthenticationFilter.class);
        return http.build();
    }

    private static void json(HttpServletResponse response,int status,String message) throws IOException {response.setStatus(status);response.setCharacterEncoding(StandardCharsets.UTF_8.name());response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"success\":false,\"message\":\""+message+"\"}");}
}
