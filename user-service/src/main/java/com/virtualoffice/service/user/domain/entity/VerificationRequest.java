package com.virtualoffice.service.user.domain.entity;

import com.virtualoffice.service.user.domain.enumuration.VerificationRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "otp", nullable = false, length = 10)
    private String otp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VerificationRequestStatus status = VerificationRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}