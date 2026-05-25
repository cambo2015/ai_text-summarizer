package com.aisummarizer.website.controllers;

import com.aisummarizer.website.aspects.RequiresStorageQuota;
import com.aisummarizer.website.aspects.RequiresSubscription;
import com.aisummarizer.website.dao.JobArtifactRepository;
import com.aisummarizer.website.dao.JobRepository;
import com.aisummarizer.website.dao.LLMInstructionRepository;
import com.aisummarizer.website.dao.TranscriptionJobRepository;
import com.aisummarizer.website.entities.AppUser;
import com.aisummarizer.website.entities.JobEntity;
import com.aisummarizer.website.services.*;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final JobRepository jobRepo;
    private final SummaryJobService service;
    private final JobSseEmitterStore sse;

    private final FileService fileService;
    private final LLMInstructionRepository  llmInstructionRepo;
    private final TranscriptionJobRepository transcriptionJobRepository;
    private final FileEncrypterDecrypterService fileEncrypterDecrypterService;
    private final UserService userService;

    public SummaryController(
            JobRepository jobRepo,
            JobArtifactRepository artifactRepo,
            SummaryJobService service,
            JobSseEmitterStore sse,
            FileService fileService,
            LLMInstructionRepository  llmInstructionRepo, TranscriptionJobRepository transcriptionJobRepository, FileEncrypterDecrypterService fileEncrypterDecrypterService, UserService userService) {
        this.jobRepo = jobRepo;
        this.service = service;
        this.sse = sse;
        this.fileService = fileService;
        this.llmInstructionRepo = llmInstructionRepo;
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.fileEncrypterDecrypterService = fileEncrypterDecrypterService;
        this.userService = userService;
    }

    /** 1️⃣ CREATE JOB */
    @PostMapping("/create")
    @RequiresSubscription
    @RequiresStorageQuota
    public ResponseEntity<Map<String, String>> create(@RequestBody Map<String, String> body) {

        String transcriptionId = body.get("transcriptionId");
        if(transcriptionId == null || transcriptionId.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error", "transcriptionId required."));
        }

        String transcriptFile = body.get("transcriptFile");
        if (transcriptFile == null || transcriptFile.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "transcriptFile required"));
        }

        //dont use this:
//        String model = body.get("model");
//        if(model == null || model.isEmpty()){
//            return ResponseEntity.badRequest().body(Map.of("error", "model required."));
//        }

        String model = "mistral-small";//model that will be used

        AppUser user = userService.getCurrentUser();
        if(user == null){
            return ResponseEntity.badRequest().body(Map.of("error","User must be signed in"));
        }

        System.out.println("model being used: " + model);

        JobEntity job = new JobEntity();
        job.setType("SUMMARY");
        jobRepo.save(job);

        System.out.println("Starting to Summarize");
        service.summarize(job.getId(), transcriptFile,transcriptionId, model,user.getId());
        System.out.println("Finished Summarizing");


        return ResponseEntity.accepted()
                .body(Map.of("jobId", job.getId().toString()));
    }

    /** 2️⃣ STREAM UPDATES */
    @GetMapping("/jobs/{jobId}/stream")
    public SseEmitter stream(@PathVariable String jobId) {
        return sse.create(jobId);
    }


    @PostMapping("get-based-on-name")
    public ResponseEntity<String> getBasedOnName(@RequestBody Map<String, String> body) {
        String summaryFileName = body.get("fileName");
        if (summaryFileName == null || summaryFileName.isBlank()) {
            return ResponseEntity.badRequest().body("fileName required.");
        }
        System.out.println("getBasedOnName() 1. summary file being used: " + summaryFileName);
        if(!fileService.isOwnerOfSummaryFile(summaryFileName,transcriptionJobRepository)){
            return  ResponseEntity.badRequest().body("You are not the owner of this summary file.");
        }
        try{
            String encryptedText    = fileService.readSummary(summaryFileName);
            String decryptedText = fileEncrypterDecrypterService.decrypt(encryptedText);
            return ResponseEntity
                    .ok()
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .body(decryptedText);
        }
        catch(Exception e){
            System.out.println("Error reading summary file: "+e.getMessage());
            return ResponseEntity.badRequest().body("Error reading the file: "+e.getMessage());
        }
    }

//    @PostMapping("/get")
//    public ResponseEntity<String> get(@RequestBody Map<String, String> body) {
//
//        String jobIdRaw = body.get("jobId");
//
//        if (jobIdRaw == null || jobIdRaw.isBlank()) {
//            return ResponseEntity.badRequest()
//                    .body("jobId is required");
//        }
//
//        UUID jobId;
//        try {
//            jobId = UUID.fromString(jobIdRaw);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest()
//                    .body("jobId must be a valid UUID");
//        }
//
//        //do not let the user see the file if he/she is not the owner
//        if(!fileService.isOwnerOfSummaryFile(jobId.toString()+".txt",transcriptionJobRepository)){
//            return ResponseEntity.badRequest().body("You are not the owner of this summary file.");
//        }
//
//        try {
//            // Reads the ENTIRE file into memory as a String
//            String summaryText = fileService.readSummary(jobId);
//
//            return ResponseEntity.ok()
//                    .header("Content-Type", "text/plain; charset=UTF-8")
//                    .body(summaryText);
//
//        } catch (java.nio.file.NoSuchFileException e) {
//            System.out.print("File not found:"+e.getMessage());
//            return ResponseEntity.status(404)
//                    .body("Summary not found for jobId " + jobId);
//
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body("Failed to read summary");
//        }
//    }

    @PostMapping("template/save")
    @RequiresSubscription
    public ResponseEntity<Map<String,String>> customize(@RequestBody Map<String, String> body) {
        if(body.get("instructions") == null || body.get("instructions").isBlank()){
            return ResponseEntity.badRequest().body(Map.of("error", "instructions is required."));
        }
//        get the instructions
        String instructions = body.get("instructions");

//        save instructions to a file
        boolean savedSuccessfully = fileService.saveLLMCustomInstructions(instructions,llmInstructionRepo);
        if(savedSuccessfully) {
            return ResponseEntity.ok().body(Map.of("success","saved successfully"));
        }
        return ResponseEntity.internalServerError().body(Map.of("error","save failed"));
    }

    @PostMapping("/template/get")
    public ResponseEntity<Map<String,String>> customizeGet() {

        try{
            if(!fileService.llmCustomFileExists()){
                return ResponseEntity.ok().body(Map.of("text","Use the pen icon to create your desired instructions for the summarizer."));
            }
            if(!fileService.isOwnerOfLLMInstructionFile(llmInstructionRepo)){
                return ResponseEntity.badRequest().body(Map.of("error", "You are not the owner of those instructions"));
            }
            if(fileService.llmCustomFileExists()){
                Map<String,String> m =  Map.of("text",fileService.getLLMCustomInstructions());
                return ResponseEntity.ok().body(m);
            }
            return ResponseEntity.notFound().build();
        }catch(Exception e){
            System.out.println(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/supported-models")
    public ResponseEntity<Map<String,Object>> getSupportedModels(@RequestBody Map<String, String> body) {
        if(body.get("modelType") == null || body.get("modelType").isBlank()){
            return ResponseEntity.badRequest().body(Map.of("error", "modelType is required."));
        }
        String modelType = body.get("modelType");
        List<String> supportedModels = service.getSupportedModels(modelType);
        return ResponseEntity.ok().body(Map.of("models", supportedModels));
    }
}

/*
* ___HOW TO USE__
* CREATE A SUMMARY JOB
* async function createSummaryJob(transcriptFile) {
  const response = await fetch("/api/summary/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      transcriptFile: transcriptFile
    })
  });

  if (!response.ok) {
    throw new Error("Failed to create summary job");
  }

  const data = await response.json();
  return data.jobId;
}
 ---use it:
 * const jobId = await createSummaryJob("meeting-01.txt");
console.log("Created job:", jobId);

* OPEN THE SSE STREAM
function streamJobUpdates(jobId, onUpdate, onDone) {
  const source = new EventSource(`/api/summary/jobs/${jobId}/stream`);

  source.onmessage = (event) => {
    console.log("Job update:", event.data);
    onUpdate?.(event.data);

    if (event.data === "Completed" || event.data === "Failed") {
      source.close();
      onDone?.(event.data);
    }
  };

  source.onerror = (err) => {
    console.error("SSE error", err);
    source.close();
  };

  return source;
}
--use it:
* streamJobUpdates(
  jobId,
  (msg) => console.log("Progress:", msg),
  (finalStatus) => console.log("Final:", finalStatus)
);
*
* FETCH THE SUMMARY RESULT
* async function getSummary(jobId) {
  const response = await fetch("/api/summary/get", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      jobId: jobId
    })
  });

  if (!response.ok) {
    throw new Error("Failed to fetch summary");
  }

  return response.text(); // or response.json() if you change it later
}
*
* PUT IT ALL TOGETHER
* async function summarizeTranscript(transcriptFile) {
  try {
    const jobId = await createSummaryJob(transcriptFile);

    streamJobUpdates(
      jobId,
      (msg) => {
        document.getElementById("status").innerText = msg;
      },
      async (finalStatus) => {
        if (finalStatus === "Completed") {
          const summary = await getSummary(jobId);
          document.getElementById("output").innerText = summary;
        } else {
          document.getElementById("output").innerText = "Job failed";
        }
      }
    );
  } catch (err) {
    console.error(err);
  }
}
* */
