package com.aisummarizer.website.boostrap;


import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.dao.UserRepository;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.Role;
import com.aisummarizer.website.services.CleanupService;
import com.aisummarizer.website.services.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.Set;

@Component
public class RunOnStart implements CommandLineRunner {


    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;
    private UserRepository userRepository;
    private PasswordEncoder encoder;
    private StripeService stripeService;
    private final CleanupService cleanupService;
    private static final Logger logger = LoggerFactory.getLogger(RunOnStart.class);
//    TranscriptionJobRepository transcriptionJobRepository;

    public RunOnStart(UserRepository userRepository, PasswordEncoder encoder, StripeService stripeService,CleanupService cleanupService) {
        this.userRepository=userRepository;
        this.encoder = encoder;
        this.stripeService = stripeService;
//        this.transcriptionJobRepository = transcriptionJobRepository;
        this.cleanupService = cleanupService;
    }

    @Override
    public void run(String... args) throws Exception {
        //create admin users
        createAdminUsers();

        cleanupService.cleanupExpiredFiles();
//        seeAllTranscriptionJobs();
    }

    public void createAdminUsers(){
        //check if admin exists
        Optional<AppUser> a = userRepository.findByUsername(adminEmail);
        if(a.isPresent()){
            return; //do not create a new admin user
        }
        AppUser user = new AppUser();
        user.setUsername(adminEmail);
        user.setPassword(encoder.encode(adminPassword));
        Role role = Role.valueOf("ADMIN"); // convert string to enum
        Role userRole = Role.valueOf("USER");
        user.setRoles(Set.of(role,userRole));
        try{
            Customer c = stripeService.createCustomer("Cam Hansen", adminEmail);
            String cId = c.getId();
            user.setStripeCustomerId(cId);

            userRepository.save(user);
            logger.info("Created Customer with Id: {}", cId);
        }
        catch(StripeException e){
            logger.error("Error while creating Customer: {}", e.getMessage());
        }
    }
}
