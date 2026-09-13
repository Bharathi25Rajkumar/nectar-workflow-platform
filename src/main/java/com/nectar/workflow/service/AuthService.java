package com.nectar.workflow.service;

import com.nectar.workflow.dtos.LoginRequestDto;
import com.nectar.workflow.dtos.LoginResponseDto;
import com.nectar.workflow.entity.Tenant;
import com.nectar.workflow.entity.User;
import com.nectar.workflow.exception.ResourceNotFoundException;
import com.nectar.workflow.exception.UnauthorizedException;
import com.nectar.workflow.repository.TenantRepository;
import com.nectar.workflow.repository.UserRepository;
import com.nectar.workflow.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponseDto login(LoginRequestDto requestDto){
        Tenant tenant = tenantRepository.findBySlug(requestDto.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + requestDto.tenantSlug()));

        User user = userRepository.findWithTenantByUsernameAndTenantId(requestDto.username(), tenant.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid Credentials"));

        if(!passwordEncoder.matches(requestDto.password(), user.getPasswordHash())){
            throw new UnauthorizedException("Invalid Credentials");
        }

        String token = tokenProvider.generateToken(tenant.getId(),
                user.getId(), user.getUsername(), user.getRole().name());

        return new LoginResponseDto(token, user.getUsername(), user.getRole().name(), tenant.getId());
    }
}
