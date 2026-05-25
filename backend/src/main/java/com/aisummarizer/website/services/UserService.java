package com.aisummarizer.website.services;


import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.UserRepository;
import com.aisummarizer.website.dto.Tiers;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.AudioFileEntity;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final AudioService audioService;
    private final AudioFileRepository audioFileRepository;
    UserRepository userRepository;
    public UserService(UserRepository userRepository, AudioService audioService, AudioFileRepository audioFileRepository) {
        this.userRepository = userRepository;
        this.audioService = audioService;
        this.audioFileRepository = audioFileRepository;
    }

    public String getUserEmail(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }

         UserDetails user  = (UserDetails) auth.getPrincipal();


        return auth.getName();
    }

    public UserDetails getUserFromPrincipal(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return (UserDetails) auth.getPrincipal();
    }

    public AppUser getCurrentUser(){
        UserDetails details = getUserFromPrincipal();
        String username = details.getUsername();
        if(username == null){
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    public AppUser getUserFromRepo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null ||
                !auth.isAuthenticated() ||
                auth instanceof AnonymousAuthenticationToken) {

            throw new IllegalStateException("No authenticated user in security context");
        }

        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found in database: " + auth.getName())
                );
    }

    public boolean userIsSubscribed(){
        AppUser user = getCurrentUser();
        Boolean subscribed = user.getSubscribed();
        if(subscribed == null){
            return false;
        }
        return user != null && subscribed;
    }

    public boolean canUsePaidFeature(){
        AppUser user = getCurrentUser();
        if(user == null){
            return false;
        }

        //can use paid feature if they are subscribed or they have actions remaining
        return userIsSubscribed()
                ||
                user.getFreeActionsRemaining() > 0; //this will be decremented later
    }


    public boolean addToFileSize(long fileSizeInBytes, AudioFileEntity afEntity, AudioFileRepository afRepo){
        if(fileSizeInBytes <= 0){
            return false;
        }

        long fileSizeBytes = afEntity.getFileSizeBytes();

        afEntity.setFileSizeBytes(fileSizeBytes);
        afRepo.save(afEntity);
        return true;
    }



    @Transactional
    public void consumeFreeActionIfNeeded(){
        AppUser user = getCurrentUser();
        if(user == null) return;
        if(!user.getSubscribed() && user.getFreeActionsRemaining() >0){
            user.setFreeActionsRemaining(user.getFreeActionsRemaining() - 1);
            userRepository.save(user);
        }
    }

//    public boolean setInitialNumHoursWithTier(Tiers tier){
//        //
//        int starterHours = 5;
//        int proHours = 15;
//        int businessHours = 40;
//        AppUser user = getCurrentUser();
//        if(user == null) {
//            System.out.println("setInitialNumHoursWithTier() 1. User is null ");
//            return false;
//        };
//        if(tier == Tiers.STARTER){
//            //5 hours included
//            user.setNumHoursRemaining(starterHours);// this plan costs $19
//            return true;
//        }
//        else if(tier == Tiers.PRO){
//            user.setNumHoursRemaining(proHours);//this plan costs $29
//            return true;
//        }
//        else{
//            user.setNumHoursRemaining(businessHours);//this plan costs $50
//            return true;
//        }
//    }
//
//    public boolean takeAwayFromNumHoursRemaining(int hours){
//        AppUser user = getCurrentUser();
//        if(user == null){
//            return false;
//        }
//        int current = user.getNumHoursRemaining();
//        user.setNumHoursRemaining(current - hours);
//        return true;
//    }
//
//    public boolean addNumHoursRemaining(int hours){
//        AppUser user = getCurrentUser();
//        if(user == null){
//            return false;
//        }
//        int current = user.getNumHoursRemaining();
//        user.setNumHoursRemaining(current + hours);
//        return true;
//    }


}
