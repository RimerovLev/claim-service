package com.claims.mvp.claim.service.letter;

import com.claims.mvp.claim.dto.response.LetterResponse;
import com.claims.mvp.claim.model.Claim;

public interface ClaimLetterService {
    LetterResponse generateLetter(Claim claim);
}
