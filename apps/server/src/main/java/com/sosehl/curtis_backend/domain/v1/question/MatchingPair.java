package com.sosehl.curtis_backend.domain.v1.question;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MatchingPair {

    @NotBlank(message = "Levá část páru musí být vyplněna")
    @Size(max = 500, message = "Levá část páru je příliš dlouhá")
    @Column(name = "left_value")
    private String left;

    @NotBlank(message = "Pravá část páru musí být vyplněna")
    @Size(max = 500, message = "Pravá část páru je příliš dlouhá")
    @Column(name = "right_value")
    private String right;
}
