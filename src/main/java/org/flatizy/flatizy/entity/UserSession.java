package org.flatizy.flatizy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.flatizy.flatizy.entity.enums.RequestType;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.entity.enums.UserSessionState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private UserSessionState state;

    @Column(name = "temp_invite_code")
    private String tempInviteCode;

    @Column(name = "invite_max_uses")
    private Integer inviteMaxUses;

    @Column(name = "invite_expiration_days")
    private Integer inviteExpirationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_target_role")
    private UserRole inviteTargetRole;

    @Column(name = "registration_phone")
    private String registrationPhone;

    @Column(name = "registration_invite_code")
    private String registrationInviteCode;

    // ===== НОВЫЕ поля для создания заявки =====
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private RequestType requestType;

    @Column(name = "request_apartment_id")
    private Integer requestApartmentId;

    @Column(name = "request_description", columnDefinition = "TEXT")
    private String requestDescription;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_session_apartment_ids", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "apartment_id")
    private List<Integer> selectedApartmentIds = new ArrayList<>();

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        lastActivity = LocalDateTime.now();
    }

    public void clearRequestData() {
        this.requestType = null;
        this.requestApartmentId = null;
        this.requestDescription = null;
    }
}
