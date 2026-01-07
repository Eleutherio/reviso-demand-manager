package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.infra.JwtService;
import com.guilherme.reviso_demand_manager.infra.RateLimitService;
import com.guilherme.reviso_demand_manager.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitService rateLimitService;

    public AuthController(UserRepository userRepository, 
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto,
                                                    HttpServletRequest request) {
        // Rate limiting por IP
        String clientIp = getClientIp(request);
        if (!rateLimitService.isAllowed("login:" + clientIp)) {
            throw new TooManyRequestsException("Muitas tentativas de login. Aguarde 1 minuto.");
        }
        
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!user.getActive()) {
            throw new UnauthorizedException("Usuário inativo");
        }

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        
        // Login bem-sucedido: reseta rate limit
        rateLimitService.reset("login:" + clientIp);

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId()
        );

        LoginResponseDTO response = new LoginResponseDTO(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId()
        );

        return ResponseEntity.ok(response);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
