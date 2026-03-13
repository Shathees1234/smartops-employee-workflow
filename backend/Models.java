package com.smartops;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.*;
import java.util.*;

// ─── Enums ────────────────────────────────────────────────────────────────────

enum UserStatus  { ACTIVE, INACTIVE, SUSPENDED }
enum LeaveType   { ANNUAL, SICK, CASUAL, MATERNITY, PATERNITY, UNPAID, COMPENSATORY }
enum LeaveStatus { PENDING, APPROVED, REJECTED, CANCELLED }

// ─── Role ─────────────────────────────────────────────────────────────────────

@Entity @Table(name = "roles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private ERole name;

    public enum ERole { ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_ADMIN }
}

// ─── User ─────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "employee_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", unique = true, nullable = false)
    @NotBlank private String employeeId;

    @NotBlank private String firstName;
    @NotBlank private String lastName;

    @Email @NotBlank @Column(unique = true)
    private String email;

    @NotBlank private String password;
    private String phone;
    private String department;
    private String designation;

    @Enumerated(EnumType.STRING)
    @Builder.Default private UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default private Set<Role> roles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    public String getFullName() { return firstName + " " + lastName; }
}

// ─── LeaveRequest ─────────────────────────────────────────────────────────────

@Entity @Table(name = "leave_requests")
@EntityListeners(AuditingEntityListener.class)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    private Integer totalDays;

    @NotBlank @Column(length = 1000) private String reason;

    @Enumerated(EnumType.STRING)
    @Builder.Default private LeaveStatus status = LeaveStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by") private User reviewedBy;

    @Column(length = 500) private String reviewComments;
    private LocalDateTime reviewedAt;

    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    public void calculateTotalDays() {
        if (startDate != null && endDate != null)
            this.totalDays = (int)(endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
