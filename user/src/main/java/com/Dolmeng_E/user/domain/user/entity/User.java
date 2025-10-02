package com.Dolmeng_E.user.domain.user.entity;

import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length=50)
    private String name;

    @Column(unique = true, nullable = false, length=100)
    private String email;

    @Column(nullable = false, length=100)
    private String password;

    @Column(length=30)
    private String phoneNumber;

    private String profileImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDelete = false;
}
