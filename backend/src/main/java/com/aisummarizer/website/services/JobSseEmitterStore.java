package com.aisummarizer.website.services;


import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Server-Sent Event (SSE) emitters for long-running background jobs.
 *
 * <p>
 * Each job is identified by a {@code jobId}. When a client subscribes to
 * {@code /jobs/{jobId}/stream}, an {@link SseEmitter} is created and stored here.
 * Background services (e.g., Whisper transcription) can then push progress
 * messages to the client in real time.
 * </p>
 *
 * <p>
 * This class acts as a bridge between asynchronous background workers and
 * HTTP streaming connections.
 * </p>
 */
@Service
public class JobSseEmitterStore {

    /**
     * Active SSE emitters keyed by jobId.
     *
     * <p>
     * A {@link ConcurrentHashMap} is used because emitters may be accessed
     * concurrently by HTTP request threads and async worker threads.
     * </p>
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();


    /**
     * Creates and registers a new {@link SseEmitter} for the given job ID.
     *
     * <p>
     * This method is typically called by the controller when a client connects
     * to {@code /jobs/{jobId}/stream}. The returned emitter keeps the HTTP
     * connection open so that the server can push progress updates.
     * </p>
     *
     * <p>
     * The emitter is automatically removed from the internal store when:
     * <ul>
     *   <li>The client disconnects</li>
     *   <li>The connection times out</li>
     *   <li>An error occurs</li>
     * </ul>
     * </p>
     *
     * @param jobId the unique identifier of the background job
     * @return a new {@link SseEmitter} bound to the given job ID
     */
    public SseEmitter create(String jobId){
        System.out.println("Creating emitter for "+jobId);
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));
        return emitter;
    }


    /**
     * Sends a string message to the client subscribed to the given job.
     *
     * <p>
     * This method is typically called from an asynchronous service (e.g.,
     * Whisper transcription) to report progress such as:
     * <ul>
     *   <li>"Downloading audio..."</li>
     *   <li>"Transcribing with Whisper..."</li>
     *   <li>"Completed"</li>
     * </ul>
     * </p>
     *
     * <p>
     * If no client is currently subscribed for the job, the message is ignored.
     * This allows background jobs to run independently of client connections.
     * </p>
     *
     * @param jobId   the job identifier
     * @param message the message to send to the client
     */
    public void send(String jobId, String message) {
        System.out.println("Sending message to emitter for "+jobId);
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) {
            return; // no client connected yet — this is OK
        }
        try {
            emitter.send(SseEmitter.event().data(message));
        } catch (Exception e) {
            emitters.remove(jobId);
        }
    }

    /**
     * Completes and removes the SSE emitter associated with the given job.
     *
     * <p>
     * This should be called when the background job has finished successfully
     * or failed. Completing the emitter signals to the client that no more
     * events will be sent.
     * </p>
     *
     * @param jobId the job identifier
     */
    public void complete(String jobId){
        SseEmitter emitter = emitters.remove(jobId);
        if(emitter != null){
            emitter.complete();
        }
    }
}
