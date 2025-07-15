package com.swp.adnV2.AdnV2.controller;

import com.swp.adnV2.AdnV2.dto.FeedbackRequest;
import com.swp.adnV2.AdnV2.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/search/by-service-name/{serviceName}")
    @PreAuthorize("hasAnyRole('GUEST', 'CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> searchFeedbacksByServiceName(@PathVariable String serviceName,
            @RequestParam(required = false) String keyword) {
        return feedbackService.searchFeedback(serviceName, keyword);
    }

    @PostMapping("/create/{serviceId}")
    public ResponseEntity<?> createFeedback(@PathVariable Long serviceId,
            @Valid @RequestBody FeedbackRequest feedbackRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String username = authentication.getName();
        if ("guestuser".equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return feedbackService.createFeedback(username, serviceId, feedbackRequest);
    }

    @PutMapping("/update/{feedbackId}")
    public ResponseEntity<?> updateFeedback(@PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackRequest feedbackRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String username = authentication.getName();
        if ("guestuser".equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return feedbackService.updateFeedback(username, feedbackId, feedbackRequest);
    }

    @DeleteMapping("/delete/{feedbackId}")
    public ResponseEntity<?> deleteFeedback(@PathVariable Long feedbackId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String username = authentication.getName();
        if ("customeruser".equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return feedbackService.deleteFeedback(feedbackId);
    }
}
