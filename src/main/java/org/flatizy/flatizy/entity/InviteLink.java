package org.flatizy.flatizy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"apartments", "creator"})
@Table(name = "invite_links")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class InviteLink {

    public static final int CODE_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = CODE_LENGTH)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false)
    private UserRole targetRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "invite_link_apartments",
            joinColumns = @JoinColumn(name = "invite_link_id"),
            inverseJoinColumns = @JoinColumn(name = "apartment_id")
    )
    private Set<Apartment> apartments = new HashSet<>();


    public boolean isValid() {
        return active
                && usedCount < maxUses
                && LocalDateTime.now().isBefore(expiresAt);
    }


    public void incrementUsage() {
        this.usedCount++;
        if (this.usedCount >= this.maxUses) {
            this.active = false;
        }
    }

    public int getRemainingUses() {
        return Math.max(0, maxUses - usedCount);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
