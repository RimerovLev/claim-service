package com.claims.mvp.dao;

import com.claims.mvp.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
}
