package com.aisummarizer.website.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class VerificationToken {

    @Id
    @GeneratedValue
    private Long id;

    private String token;

    @OneToOne
    private AppUser user;

    private Instant expiresAt;

    private Instant lastSendAt;

    // getters/setters
}

