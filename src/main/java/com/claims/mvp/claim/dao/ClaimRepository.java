package com.claims.mvp.claim.dao;

import com.claims.mvp.claim.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
}
