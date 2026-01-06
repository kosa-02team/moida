package com.back.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", unique = true, nullable = false, length = 50)
    private String loginId;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "system_role", length = 20)
    private String systemRole = "USER";

    @Column(name = "simple_password", length = 255)
    private String simplePassword;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    // 생성자
    public Users(String loginId, String password, String realName) {
        this.loginId = loginId;
        this.password = password;
        this.realName = realName;
    }

    // 도메인 메서드
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateSimplePassword(String simplePassword) {
        this.simplePassword = simplePassword;
    }

    public void updateProfile(String realName) {
        this.realName = realName;
    }

    public void ban() {
        this.status = "BANNED";
        this.bannedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = "DELETED";
        this.deletedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = "ACTIVE";
        this.bannedAt = null;
        this.deletedAt = null;
    }

    public void changeSystemRole(String role) {
        this.systemRole = role;
    }
}

