package com.nectar.workflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID tokenTenant = null;
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p) {
            tokenTenant = p.tenantId();
        }

        String header = request.getHeader("X-Tenant-Id");

        if(header != null && !header.isBlank()){
            try{
                UUID tenantId = UUID.fromString(header);
                if(tokenTenant != null && !tenantId.equals(tokenTenant)){
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "X-Tenant-Id doesn't match");
                    return false;
                }
                TenantContext.set(tenantId);
            } catch (IllegalArgumentException | IOException ex){
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-Id");
                return false;
            }
        } else if(tokenTenant != null){
            TenantContext.set(tokenTenant);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object Handler, Exception ex){
        TenantContext.clear();
    }
}
