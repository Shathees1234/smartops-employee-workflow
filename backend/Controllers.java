package com.smartops;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

// ═══════════════════════════════════════════════════════════════════════════════
// DTOs
// ═══════════════════════════════════════════════════════════════════════════════

public class Controllers {

    // ── Auth DTOs ──────────────────────────────────────────────────────────────

    @Data public static class LoginRequest {
        @NotBlank @Email private String email;
        @NotBlank        private String password;
    }

    @Data public static class RegisterRequest {
        @NotBlank private String employeeId;
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @NotBlank @Email private String email;
        @NotBlank private String password;
        private String phone, department, designation, role;
        private Long managerId;
    }

    @Data @AllArgsConstructor
    public static class JwtResponse {
        private String token;
        private final String type = "Bearer";
        private Long id;
        private String employeeId, email, firstName, lastName;
        private List<String> roles;
    }

    // ── Employee DTOs ──────────────────────────────────────────────────────────

    @Data @Builder
    public static class EmployeeDto {
        private Long id;
        private String employeeId, fullName, email, phone, department, designation;
        private UserStatus status;
        private List<String> roles;
        private Long managerId;
        private String managerName;
        private LocalDateTime createdAt;
    }

    @Data public static class UpdateRequest {
        private String firstName, lastName, phone, department, designation;
        private UserStatus status;
        private Long managerId;
    }

    // ── Leave DTOs ─────────────────────────────────────────────────────────────

    @Data public static class LeaveCreateRequest {
        @NotNull private LeaveType leaveType;
        @NotNull private LocalDate startDate;
        @NotNull private LocalDate endDate;
        @NotBlank private String reason;
    }

    @Data public static class ReviewRequest {
        @NotNull private LeaveStatus status;
        private String reviewComments;
    }

    @Data @Builder
    public static class LeaveResponse {
        private Long id, employeeId;
        private String employeeName, employeeCode, department;
        private LeaveType leaveType;
        private LocalDate startDate, endDate;
        private Integer totalDays;
        private String reason;
        private LeaveStatus status;
        private String reviewedByName, reviewComments;
        private LocalDateTime reviewedAt, createdAt;
    }

    // ── Dashboard DTO ──────────────────────────────────────────────────────────

    @Data @Builder
    public static class StatsDto {
        private Long totalEmployees, activeEmployees, totalManagers;
        private Long pendingLeaves, approvedLeaves, rejectedLeaves;
        private int myPendingLeaves, myApprovedDays;
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Controllers
// ═══════════════════════════════════════════════════════════════════════════════

// ── Auth ──────────────────────────────────────────────────────────────────────

@RestController @RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Controllers.JwtResponse> login(@Valid @RequestBody Controllers.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> register(@Valid @RequestBody Controllers.RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }
}

// ── Employee ──────────────────────────────────────────────────────────────────

@RestController @RequestMapping("/employees")
@RequiredArgsConstructor
class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Controllers.EmployeeDto>> getAll() {
        return ResponseEntity.ok(employeeService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ResponseEntity<Controllers.EmployeeDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping("/team/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<Controllers.EmployeeDto>> getTeam(@PathVariable Long managerId) {
        return ResponseEntity.ok(employeeService.getTeam(managerId));
    }

    @GetMapping("/departments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getDepts() {
        return ResponseEntity.ok(employeeService.getDepartments());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Controllers.EmployeeDto> update(@PathVariable Long id,
            @RequestBody Controllers.UpdateRequest req) {
        return ResponseEntity.ok(employeeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

// ── Leave ─────────────────────────────────────────────────────────────────────

@RestController @RequestMapping("/leaves")
@RequiredArgsConstructor
class LeaveController {
    private final LeaveService leaveService;

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Controllers.LeaveResponse> apply(@Valid @RequestBody Controllers.LeaveCreateRequest req) {
        return ResponseEntity.ok(leaveService.apply(req));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Controllers.LeaveResponse>> myLeaves() {
        return ResponseEntity.ok(leaveService.myLeaves());
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<Controllers.LeaveResponse>> teamLeaves() {
        return ResponseEntity.ok(leaveService.teamLeaves());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<Controllers.LeaveResponse>> pending() {
        return ResponseEntity.ok(leaveService.pendingForManager());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Controllers.LeaveResponse>> all() {
        return ResponseEntity.ok(leaveService.all());
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Controllers.LeaveResponse> review(@PathVariable Long id,
            @RequestBody Controllers.ReviewRequest req) {
        return ResponseEntity.ok(leaveService.review(id, req));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Controllers.LeaveResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.cancel(id));
    }
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

@RestController @RequestMapping("/dashboard")
@RequiredArgsConstructor
class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Controllers.StatsDto> stats() {
        return ResponseEntity.ok(dashboardService.stats());
    }
}
