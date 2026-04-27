package com.claims.mvp.claim.dao;

import com.claims.mvp.claim.model.BoardingDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardingDocumentsRepository extends JpaRepository<BoardingDocuments, String> {
    List<BoardingDocuments> findAllByClaimId(Long claimId);
}
