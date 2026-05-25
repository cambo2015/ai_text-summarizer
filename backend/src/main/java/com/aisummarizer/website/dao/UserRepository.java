package com.aisummarizer.website.dao;

import com.aisummarizer.website.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByStripeCustomerId(String customerId);

    Optional<AppUser> findByStripeSubscriptionId(String id);
}

