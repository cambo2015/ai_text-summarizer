package com.aisummarizer.website.helpers;

import com.aisummarizer.website.dto.Tiers;

public class StorageQuota {

    public static final long ONE_GB = 1024L * 1024 * 1024;

    public static final long STARTER = 5L * ONE_GB;
    public static final long PRO = 20L * ONE_GB;
    public static final long BUSINESS = 100L * ONE_GB;


    private StorageQuota(){}

    public static long getBytesFromGB(long gb){
        return gb * ONE_GB;
    }

    /// <p>Gets the quota from the Tiers </p>
    public static long getQuotaFromTier(Tiers tier){
        if(tier == null) return 0;

        return switch(tier){
            case STARTER -> StorageQuota.STARTER;
            case PRO -> StorageQuota.PRO;
            case BUSINESS -> StorageQuota.BUSINESS;
        };
    }
}
