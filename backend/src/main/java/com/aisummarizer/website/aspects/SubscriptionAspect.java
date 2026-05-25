package com.aisummarizer.website.aspects;


import com.aisummarizer.website.services.UserService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;


@Aspect
@Component
public class SubscriptionAspect {

    private final UserService userService;

    public SubscriptionAspect(UserService userService) {
        this.userService = userService;
    }

    @Before( "@annotation(com.aisummarizer.website.aspects.RequiresSubscription) || " +
            "@within(com.aisummarizer.website.aspects.RequiresSubscription)")
    public void checkSubscription(){
        System.out.println("Checking to see if the user is subscribed");

        if (!userService.canUsePaidFeature()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A subscription is required to use this feature."
            );
        }
    }
}
