package org.flatizy.flatizy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Long id;
    private String firstName;
    private String lastName;
    private Integer apartmentNumber;
    private Integer houseNumber;
    private String message;
    private LocalDateTime createdAt;
}
