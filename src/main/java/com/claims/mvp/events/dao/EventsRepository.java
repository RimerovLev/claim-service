package com.claims.mvp.events.dao;

import com.claims.mvp.events.model.ClaimEvents;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventsRepository extends JpaRepository<ClaimEvents, Long> {
}
