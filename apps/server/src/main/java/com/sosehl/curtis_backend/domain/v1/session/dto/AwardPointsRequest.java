package com.sosehl.curtis_backend.domain.v1.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AwardPointsRequest {

    @NotNull
    @Min(0)
    private Integer awardedPoints;
}
