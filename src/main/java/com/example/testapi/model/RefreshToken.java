package com.example.testapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String tokenHash; // we store a hash, not the raw token

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Users user;

    private Instant expiresAt;
    private boolean revoked;
}
