package org.flatizy.flatizy.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTypeDto {

    private Long id;

    private String code;

    private String title;

    private String description;
}
