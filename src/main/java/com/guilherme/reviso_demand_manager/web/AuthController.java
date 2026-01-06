package com.guilherme.reviso_demand_manager.web;

import com.guilherme.reviso_demand_manager.domain.User;
import com.guilherme.reviso_demand_manager.infra.JwtService;
import com.guilherme.reviso_demand_manager.infra.UserRepository;
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

    public AuthController(UserRepository userRepository, 
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!user.getActive()) {
            throw new UnauthorizedException("Usuário inativo");
        }

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

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
}
