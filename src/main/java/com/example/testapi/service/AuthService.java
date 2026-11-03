package com.example.testapi.service;

import com.example.testapi.config.JwtService;
import com.example.testapi.model.*;
import com.example.testapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        Users u = new Users();
        u.setEmail(req.email());
        u.setName(req.name());
        u.setPassword(encoder.encode(req.password())); // hash it!
        users.save(u);
        return issueTokens(u);
    }

    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException if the password is wrong
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        Users u = users.findByEmail(req.email()).orElseThrow();
        return issueTokens(u);
    }

    public AuthResponse refresh(String rawRefreshToken) {
        var rotated = refreshTokens.rotate(rawRefreshToken);
        return new AuthResponse(
                jwt.generateAccessToken(rotated.email()),
                rotated.newRefreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
        log.info("works");
    }

    public MeResponse me(String email) {
        Users u = users.findByEmail(email).orElseThrow();
        return new MeResponse(u.getId(), u.getEmail(), u.getName());
    }

    private AuthResponse issueTokens(Users u) {
        return new AuthResponse(
                jwt.generateAccessToken(u.getEmail()),
                refreshTokens.create(u));
    }
}
