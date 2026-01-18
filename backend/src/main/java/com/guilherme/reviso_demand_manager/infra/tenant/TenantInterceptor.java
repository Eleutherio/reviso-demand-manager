package com.guilherme.reviso_demand_manager.infra.tenant;

import com.guilherme.reviso_demand_manager.infra.AgencyRepository;
import com.guilherme.reviso_demand_manager.infra.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final AgencyRepository agencyRepository;

    public TenantInterceptor(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof JwtAuthFilter.AuthenticatedUser user) {
            UUID agencyId = user.agencyId();
            if (agencyId != null) {
                agencyRepository.findById(agencyId).ifPresent(agency -> {
                    if (agency.getDatabaseName() != null) {
                        TenantContext.setCurrentTenant(agency.getDatabaseName());
                    }
                });
            }
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
