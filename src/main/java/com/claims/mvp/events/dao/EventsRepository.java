package com.claims.mvp.events.dao;

import com.claims.mvp.events.model.ClaimEvents;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventsRepository extends JpaRepository<ClaimEvents, Long> {
    List<ClaimEvents> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
