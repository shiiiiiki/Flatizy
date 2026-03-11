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
public class RequestDto {
    private Integer id;
    private String type;
    private String description;
    private LocalDateTime createdAt;
    private Integer apartmentNumber;
    private Integer houseNumber;
    private String status;
    private String feedback;
}
