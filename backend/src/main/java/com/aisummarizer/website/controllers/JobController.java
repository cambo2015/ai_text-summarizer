//package com.aisummarizer.website.controllers;
//
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//@RestController
//@RequestMapping("/jobs")
//public class JobController {
//
//    @GetMapping("/{jobId}/stream")
//    public SseEmitter stream(@PathVariable String jobId) {
//        return sseEmitterStore.create(jobId);
//    }
//
//}
