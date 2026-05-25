package com.aisummarizer.website.aspects;


import com.aisummarizer.website.services.StorageService;
import com.aisummarizer.website.services.UserService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class StorageQuotaAspect {

    private final UserService userService;
    private final StorageService storageService;

    public StorageQuotaAspect(UserService userService,
                              StorageService storageService) {
        this.userService = userService;
        this.storageService = storageService;
    }

    @Before("@annotation(com.aisummarizer.website.aspects.RequiresStorageQuota) || " +
            "@within(com.aisummarizer.website.aspects.RequiresStorageQuota)")
    public void checkQuota() {

        var user = userService.getCurrentUser();

        if (user == null) return; // optional safety

        long used = storageService.getTotalStorageBytes(user.getId());
        long quota = user.getQuotaFileSizeBytes();

        if (used >= quota) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Storage quota exceeded."
            );
        }
    }
}