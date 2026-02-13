package com.virtualoffice.service.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
public class VerificationRequests {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "otp", nullable = false)
    private long OTP;
}
