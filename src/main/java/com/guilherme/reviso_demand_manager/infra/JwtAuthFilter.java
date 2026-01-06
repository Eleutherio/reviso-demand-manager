package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.validateAndExtractClaims(token);
            
            UUID userId = jwtService.extractUserId(claims);
            String email = jwtService.extractEmail(claims);
            UserRole role = jwtService.extractRole(claims);
            UUID companyId = jwtService.extractCompanyId(claims);
            
            // Create authentication token with role as authority
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            var authToken = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, email, role, companyId),
                null,
                authorities
            );
            
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
        } catch (Exception e) {
            // Invalid token, continue without authentication
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    public record AuthenticatedUser(UUID userId, String email, UserRole role, UUID companyId) {}
}
