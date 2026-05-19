package com.claims.mvp.scheduler.controller;

import com.claims.mvp.scheduler.FollowUpSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("dev")
@RequestMapping("/api/admin")
public class SchedulerAdminController {

    private final FollowUpSchedulerService followUpSchedulerService;

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping("/run-follow-up")
    public String runFollowUpScheduler() {
        followUpSchedulerService.checkForFollowUps();
        System.out.println("Follow-up scheduler executed");
        return ResponseEntity.ok("Follow-up scheduler executed").toString();
    }
}
