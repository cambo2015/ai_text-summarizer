package com.aisummarizer.website.helpers;

public class StorageUtilities {

    public static final long ONE_GB = 1024L * 1024L * 1024L;


    /// converts bytes to KB
    public static double bytesToKB(long bytes){
        return bytes / 1024.0;
    }

    /// convert bytes to MB
    public static double bytesToMB(long bytes){
        return bytes / (1024.0 * 1024.0);
    }

    /// converts bytes to GB
    public static double bytesToGB(long bytes){
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    /// calculates the percentage used in bytes
    public static double getPercentFromBytes(long usedBytes,long quotaBytes){
        return (usedBytes * 100.0) / quotaBytes;
    }
}
