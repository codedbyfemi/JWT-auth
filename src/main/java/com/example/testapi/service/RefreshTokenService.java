package com.example.testapi.service;

import com.example.testapi.model.RefreshToken;
import com.example.testapi.model.Users;
import com.example.testapi.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private final RefreshTokenRepository repo;
    private final long ttlDays;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${app.jwt.refresh-ttl-days}") long ttlDays) {
        this.repo = repo;
        this.ttlDays = ttlDays;
    }

    public record Rotated(Users user, String newRefreshToken) {}

    @Transactional
    public String create(Users user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hash(raw));
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS));
        repo.save(rt);
        return raw; // the only time the raw value exists on the server
    }

    // noRollbackFor so the "revoke everything" below is saved even though we throw
    @Transactional(dontRollbackOn = BadCredentialsException.class)
    public Rotated rotate(String raw) {
        RefreshToken old = repo.findByTokenHash(hash(raw))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (old.isRevoked()) {
            // A revoked token came back: it was probably stolen. Kill every session for this user.
            repo.revokeAllForUser(old.getUser());
            throw new BadCredentialsException("Refresh token reuse detected");
        }
        if (old.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        old.setRevoked(true);                       // one-time use
        String fresh = create(old.getUser());       // issue a replacement
        return new Rotated(old.getUser(), fresh);
    }

    @Transactional
    public void revoke(String raw) {
        repo.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevoked(true));
        log.info("works");
    }

    private String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
