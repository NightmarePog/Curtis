package com.sosehl.curtis_backend.controllers;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public class QuizSessionController {

    @GetMapping("/session/{uuid}/start")
    public ResponseEntity<?> start(@PathVariable UUID uuid) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{uuid}/next")
    public ResponseEntity<?> nextAction(@PathVariable UUID uuid) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{uuid}/results")
    public ResponseEntity<?> getResults(@PathVariable UUID uuid) {
        return ResponseEntity.ok().build();
    }
}
