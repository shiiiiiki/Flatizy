package org.flatizy.flatizy.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationDto {

    private Long id;

    private String text;

    @JsonProperty("typeNotification")
    private Long notificationTypeId;

    @JsonProperty("sendTime")
    private LocalDateTime sendTime;

    @JsonProperty("isSendNow")
    private Boolean sendNow;
}
