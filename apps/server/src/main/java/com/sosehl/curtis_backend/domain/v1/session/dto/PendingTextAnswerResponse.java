package com.sosehl.curtis_backend.domain.v1.session.dto;

import lombok.Data;

@Data
public class PendingTextAnswerResponse {

    private Long resultId;
    private String studentId;
    private int questionIndex;
    private String question;
    private String text;
    private int points;
    private Integer awardedPoints;
    private String status;
}
