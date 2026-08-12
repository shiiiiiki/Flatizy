package org.flatizy.flatizy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.flatizy.flatizy.entity.enums.NotificationTypeCode;

@Entity
@Getter
@Setter
public class NotificationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationTypeCode code;

    private String title;
    private String description;
}
