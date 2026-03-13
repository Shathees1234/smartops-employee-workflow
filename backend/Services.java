package com.smartops;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

// ─── AuthService ──────────────────────────────────────────────────────────────

@Service @RequiredArgsConstructor
class AuthService {
    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public Controllers.JwtResponse login(Controllers.LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = jwtUtils.generateJwtToken(auth);
        UserDetailsImpl u = (UserDetailsImpl) auth.getPrincipal();
        List<String> roles = u.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return new Controllers.JwtResponse(jwt, u.getId(), u.getEmployeeId(),
            u.getEmail(), u.getFirstName(), u.getLastName(), roles);
    }

    @Transactional
    public String register(Controllers.RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already in use");
        if (userRepo.existsByEmployeeId(req.getEmployeeId()))
            throw new RuntimeException("Employee ID already in use");

        Role.ERole eRole = switch (req.getRole() != null ? req.getRole().toUpperCase() : "EMPLOYEE") {
            case "ADMIN"   -> Role.ERole.ROLE_ADMIN;
            case "MANAGER" -> Role.ERole.ROLE_MANAGER;
            default        -> Role.ERole.ROLE_EMPLOYEE;
        };
        Role role = roleRepo.findByName(eRole).orElseThrow();
        User user = User.builder()
            .employeeId(req.getEmployeeId()).firstName(req.getFirstName())
            .lastName(req.getLastName()).email(req.getEmail())
            .password(encoder.encode(req.getPassword())).phone(req.getPhone())
            .department(req.getDepartment()).designation(req.getDesignation())
            .status(UserStatus.ACTIVE).roles(new HashSet<>(Set.of(role))).build();
        if (req.getManagerId() != null)
            user.setManager(userRepo.findById(req.getManagerId()).orElseThrow());
        userRepo.save(user);
        return "Employee registered successfully";
    }
}

// ─── EmployeeService ──────────────────────────────────────────────────────────

@Service @RequiredArgsConstructor
class EmployeeService {
    private final UserRepository userRepo;

    public List<Controllers.EmployeeDto> getAll() {
        return userRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Controllers.EmployeeDto getById(Long id) {
        return toDto(userRepo.findById(id).orElseThrow(() -> new RuntimeException("Employee not found")));
    }

    public List<Controllers.EmployeeDto> getTeam(Long managerId) {
        return userRepo.findByManagerId(managerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<String> getDepartments() { return userRepo.findAllDepartments(); }

    @Transactional
    public Controllers.EmployeeDto update(Long id, Controllers.UpdateRequest req) {
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
        if (req.getFirstName()   != null) u.setFirstName(req.getFirstName());
        if (req.getLastName()    != null) u.setLastName(req.getLastName());
        if (req.getPhone()       != null) u.setPhone(req.getPhone());
        if (req.getDepartment()  != null) u.setDepartment(req.getDepartment());
        if (req.getDesignation() != null) u.setDesignation(req.getDesignation());
        if (req.getStatus()      != null) u.setStatus(req.getStatus());
        if (req.getManagerId()   != null)
            u.setManager(userRepo.findById(req.getManagerId()).orElseThrow());
        return toDto(userRepo.save(u));
    }

    @Transactional
    public void delete(Long id) { userRepo.deleteById(id); }

    Controllers.EmployeeDto toDto(User u) {
        return Controllers.EmployeeDto.builder()
            .id(u.getId()).employeeId(u.getEmployeeId()).fullName(u.getFullName())
            .email(u.getEmail()).phone(u.getPhone()).department(u.getDepartment())
            .designation(u.getDesignation()).status(u.getStatus())
            .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
            .managerId(u.getManager() != null ? u.getManager().getId() : null)
            .managerName(u.getManager() != null ? u.getManager().getFullName() : null)
            .createdAt(u.getCreatedAt()).build();
    }
}

// ─── LeaveService ─────────────────────────────────────────────────────────────

@Service @RequiredArgsConstructor
class LeaveService {
    private final LeaveRequestRepository leaveRepo;
    private final UserRepository userRepo;

    @Transactional
    public Controllers.LeaveResponse apply(Controllers.LeaveCreateRequest req) {
        User emp = currentUser();
        if (req.getEndDate().isBefore(req.getStartDate()))
            throw new RuntimeException("End date cannot be before start date");
        if (!leaveRepo.findOverlappingLeaves(emp.getId(), req.getStartDate(), req.getEndDate()).isEmpty())
            throw new RuntimeException("Overlapping approved leave exists");
        return toDto(leaveRepo.save(LeaveRequest.builder()
            .employee(emp).leaveType(req.getLeaveType())
            .startDate(req.getStartDate()).endDate(req.getEndDate())
            .reason(req.getReason()).status(LeaveStatus.PENDING).build()));
    }

    public List<Controllers.LeaveResponse> myLeaves() {
        return leaveRepo.findByEmployeeId(currentPrincipal().getId())
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<Controllers.LeaveResponse> teamLeaves() {
        return leaveRepo.findByManagerId(currentPrincipal().getId())
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<Controllers.LeaveResponse> pendingForManager() {
        return leaveRepo.findByManagerIdAndStatus(currentPrincipal().getId(), LeaveStatus.PENDING)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<Controllers.LeaveResponse> all() {
        return leaveRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public Controllers.LeaveResponse review(Long id, Controllers.ReviewRequest req) {
        LeaveRequest lr = leaveRepo.findById(id).orElseThrow(() -> new RuntimeException("Leave not found"));
        if (lr.getStatus() != LeaveStatus.PENDING) throw new RuntimeException("Already " + lr.getStatus());
        lr.setStatus(req.getStatus()); lr.setReviewedBy(currentUser());
        lr.setReviewComments(req.getReviewComments()); lr.setReviewedAt(LocalDateTime.now());
        return toDto(leaveRepo.save(lr));
    }

    @Transactional
    public Controllers.LeaveResponse cancel(Long id) {
        LeaveRequest lr = leaveRepo.findById(id).orElseThrow(() -> new RuntimeException("Leave not found"));
        if (!lr.getEmployee().getId().equals(currentPrincipal().getId()))
            throw new RuntimeException("Cannot cancel another employee's leave");
        if (lr.getStatus() != LeaveStatus.PENDING) throw new RuntimeException("Only PENDING leaves can be cancelled");
        lr.setStatus(LeaveStatus.CANCELLED);
        return toDto(leaveRepo.save(lr));
    }

    private User currentUser() {
        return userRepo.findById(currentPrincipal().getId()).orElseThrow();
    }

    private UserDetailsImpl currentPrincipal() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    Controllers.LeaveResponse toDto(LeaveRequest lr) {
        return Controllers.LeaveResponse.builder()
            .id(lr.getId()).employeeId(lr.getEmployee().getId())
            .employeeName(lr.getEmployee().getFullName()).employeeCode(lr.getEmployee().getEmployeeId())
            .department(lr.getEmployee().getDepartment()).leaveType(lr.getLeaveType())
            .startDate(lr.getStartDate()).endDate(lr.getEndDate()).totalDays(lr.getTotalDays())
            .reason(lr.getReason()).status(lr.getStatus())
            .reviewedByName(lr.getReviewedBy() != null ? lr.getReviewedBy().getFullName() : null)
            .reviewComments(lr.getReviewComments()).reviewedAt(lr.getReviewedAt())
            .createdAt(lr.getCreatedAt()).build();
    }
}

// ─── DashboardService ─────────────────────────────────────────────────────────

@Service @RequiredArgsConstructor
class DashboardService {
    private final UserRepository userRepo;
    private final LeaveRequestRepository leaveRepo;

    public Controllers.StatsDto stats() {
        UserDetailsImpl me = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Controllers.StatsDto.builder()
            .totalEmployees(userRepo.count())
            .activeEmployees((long) userRepo.findByStatus(UserStatus.ACTIVE).size())
            .totalManagers((long) userRepo.findByRoleName(Role.ERole.ROLE_MANAGER).size())
            .pendingLeaves(leaveRepo.countByStatus(LeaveStatus.PENDING))
            .approvedLeaves(leaveRepo.countByStatus(LeaveStatus.APPROVED))
            .rejectedLeaves(leaveRepo.countByStatus(LeaveStatus.REJECTED))
            .myPendingLeaves(leaveRepo.findByEmployeeIdAndStatus(me.getId(), LeaveStatus.PENDING).size())
            .myApprovedDays(Optional.ofNullable(
                leaveRepo.getTotalApprovedDaysByEmployeeAndYear(me.getId(), LocalDate.now().getYear())).orElse(0))
            .build();
    }
}
