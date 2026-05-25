package com.aisummarizer.website.controllers;


import com.aisummarizer.website.dao.UserRepository;
import com.aisummarizer.website.dao.VerificationTokenRepository;
import com.aisummarizer.website.dto.LogoutResponse;
import com.aisummarizer.website.dto.SigninRequest;
import com.aisummarizer.website.dto.SignupRequest;
import com.aisummarizer.website.entities.Role;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.VerificationToken;
import com.aisummarizer.website.services.StripeService;
import com.aisummarizer.website.services.StripeServiceImpl;
import com.aisummarizer.website.services.TokenService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ResponseStatusException;

//@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    private final VerificationTokenRepository tokenRepo;

    private final SecurityContextRepository securityContextRepository;
    private final TokenService tokenService;
    private final StripeServiceImpl stripeService;

    @Value("${website.clientUrl}")
    private String clientUrl;

    public AuthController(
            AuthenticationManager authManager,
            UserRepository repo,
            PasswordEncoder encoder,
            SecurityContextRepository securityContextRepository,
            VerificationTokenRepository verificationTokenRepository,TokenService tokenService,
            StripeServiceImpl stripeService) {
        this.authManager = authManager;
        this.repo = repo;
        this.encoder = encoder;
        this.securityContextRepository = securityContextRepository;
        this.tokenRepo = verificationTokenRepository;
        this.tokenService = tokenService;
        this.stripeService = stripeService;
    }

    @PostMapping("/signup")
    public  ResponseEntity<?> signup(@RequestBody SignupRequest request) {

//        get user
        AppUser user = repo.findByUsername(request.getUsername()).orElse(null);

//      check if user already exists
        if (user != null && user.isEnabled()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Account already exists. Please log in.");
        }

        if (user != null && !user.isEnabled()) {
            VerificationToken token = tokenService.createEmailToken(user);
            String link = tokenService.createVerificationLink(token);
            tokenService.sendTokenToEmail(user, link);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Account already exists but is not verified. Verification email resent.");
        }

        try {


            user = new AppUser();
            user.setUsername(request.getUsername());
            user.setPassword(encoder.encode(request.getPassword()));
            Role role = Role.valueOf("USER"); // convert string to enum
            user.setRoles(Set.of(role));

            // Create Stripe customer if needed
            Customer c = stripeService.createCustomer(request.getUsername());
            user.setStripeCustomerId(c.getId());

            user.setEnabled(false);
            repo.save(user);

//        ____VERIFICATION____
//        Create and save token
            VerificationToken verificationToken = tokenService.createEmailToken(user);
            String link = tokenService.createVerificationLink(verificationToken);
            tokenService.sendTokenToEmail(user, link);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Signup successful. Please check your email to verify your account.");
        }
        catch (StripeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestParam String token) {
        VerificationToken vt = tokenRepo.findByToken(token).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid token."));
        if(vt.getExpiresAt().isBefore(Instant.now())){
            tokenRepo.delete(vt);
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI.create(clientUrl+"/verify-expired"))
                    .build();

        }
        AppUser user = vt.getUser();
        user.setEnabled(true);
        repo.save(user);
        tokenRepo.delete(vt);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(clientUrl+"/verify-success"))
                .build();
    }

    @PostMapping("/resend-token")
    public ResponseEntity<?> resendToken(@AuthenticationPrincipal UserDetails userDetails) {
//        user logged in?
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You must be logged in.");
        }

//        user not found
        AppUser user = repo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

//        user is already verified email
        if (user.isEnabled()) {
            return ResponseEntity.badRequest().body("Account is already verified.");
        }

//       send email verification
        VerificationToken newToken = tokenService.createEmailToken(user);
        String link = tokenService.createVerificationLink(newToken);
        tokenService.sendTokenToEmail(user, link);

        return ResponseEntity.ok("Token has been sent.");
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // Create & save SecurityContext to session
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            // (optional) create session now to force Set-Cookie
            httpRequest.getSession(true);

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "sessionCreated", true
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public LogoutResponse logout(HttpServletRequest request) {
        request.getSession().invalidate();
        LogoutResponse response = new LogoutResponse();
        response.setMessage("Logout Successful");
        return response;
    }

    @PostMapping("/status")
    public ResponseEntity<?> status(HttpSession session) {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();

       if(auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please log in.");
       }

       return ResponseEntity.ok(Map.of(
               "username", auth.getName(),
               "roles", auth.getAuthorities()
       ));
    }
}




