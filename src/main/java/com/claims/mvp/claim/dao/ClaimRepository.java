package com.claims.mvp.claim.dao;

import com.claims.mvp.claim.model.Claim;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    @EntityGraph(attributePaths = {"user", "flight", "euContext", "documents", "issue"})
    Optional<Claim> findWithDetailsById(Long id);
}
