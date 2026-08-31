package com.sosehl.curtis_backend.domain.v1.session;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MatchingSubmissionPair {

    @Column(name = "left_index")
    private Integer leftIndex;

    @Column(name = "right_index")
    private Integer rightIndex;
}
