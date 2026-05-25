package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUser(AppUser user);
}
