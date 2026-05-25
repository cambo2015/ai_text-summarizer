package com.aisummarizer.website.entities;

import com.aisummarizer.website.dto.Tiers;
import com.aisummarizer.website.helpers.StorageQuota;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // Enum roles (stored as strings in DB)
    @ElementCollection(fetch = FetchType.EAGER, targetClass = Role.class)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles;

    @Column
    private String stripeCustomerId;

    @Column
    private boolean enabled = false;

    @Column(nullable = false)
    private Boolean subscribed = false;

    @Column
    private String stripeSubscriptionId;

    @Column(nullable = false)
    private int freeActionsRemaining = 1;

//    @Column
//    private Integer numHoursRemaining = 0;

    @Column(nullable = false)
    long quotaFileSizeBytes = StorageQuota.STARTER;

    @Column
    private Tiers tiers = Tiers.STARTER;

}




