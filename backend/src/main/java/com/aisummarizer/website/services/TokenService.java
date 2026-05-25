package com.aisummarizer.website.services;


import com.aisummarizer.website.dao.VerificationTokenRepository;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.VerificationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@Service
public class TokenService {


    private final VerificationTokenRepository tokenRepo;
    private final EmailService emailService;
    public TokenService(VerificationTokenRepository verificationTokenrepository,EmailService emailService) {
        tokenRepo = verificationTokenrepository;
        this.emailService = emailService;
    }

    public VerificationToken createEmailToken(AppUser user) {

//        add rate limiting
        tokenRepo.findByUser(user).ifPresent(existing -> {
            if(existing.getLastSendAt() != null && existing.getLastSendAt().isAfter(Instant.now().minus(5, ChronoUnit.MINUTES))) {
                tokenRepo.delete(existing);
                throw new IllegalArgumentException("Please wait 5 minutes before requesting another verification email.");
            }
            tokenRepo.delete(existing);
        });

        //create token
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepo.save(verificationToken);
        return verificationToken;
    }

    public void deleteEmailToken(AppUser user) {
        tokenRepo.findByUser(user).ifPresent(tokenRepo::delete);
    }

    public String createVerificationLink(VerificationToken token){
        return "https://localhost:8443/api/auth/verify?token=" + token.getToken();
    }

    public void sendTokenToEmail(AppUser user,String verificationLink){
        emailService.sendVerificationEmail(user.getUsername(),verificationLink);
    }
}
