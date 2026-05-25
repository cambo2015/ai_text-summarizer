package com.aisummarizer.website.services;


import com.aisummarizer.website.dao.AudioFileRepository;
import com.aisummarizer.website.dao.JobArtifactRepository;
import org.springframework.stereotype.Service;

@Service
public class StorageService {
    public final AudioFileRepository audioFileRepository;
    public final JobArtifactRepository jobArtifactRepository;

    public StorageService(AudioFileRepository audioFileRepository,JobArtifactRepository jobArtifactRepository){
        this.audioFileRepository = audioFileRepository;
        this.jobArtifactRepository = jobArtifactRepository;
    }

    /// <p> Gets the total storage bytes based on the user (or owner) id.</p>
    public long getTotalStorageBytes(Long ownerId){
        long audioBytes = audioFileRepository.getTotalBytes(ownerId);
        long artifactBytes = jobArtifactRepository.sumBytesByOwnerId(ownerId);
        return audioBytes + artifactBytes;
    }
}
