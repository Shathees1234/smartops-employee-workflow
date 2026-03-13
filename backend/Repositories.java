package com.smartops;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

// ─── UserRepository ───────────────────────────────────────────────────────────

@Repository
interface UserRepository extends org.springframework.data.jpa.repository.JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmployeeId(String employeeId);
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
    List<User> findByStatus(UserStatus status);
    List<User> findByDepartment(String department);

    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId")
    List<User> findByManagerId(Long managerId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(Role.ERole roleName);

    @Query("SELECT DISTINCT u.department FROM User u WHERE u.department IS NOT NULL")
    List<String> findAllDepartments();
}

// ─── RoleRepository ───────────────────────────────────────────────────────────

@Repository
interface RoleRepository extends org.springframework.data.jpa.repository.JpaRepository<Role, Long> {
    Optional<Role> findByName(Role.ERole name);
}

// ─── LeaveRequestRepository ───────────────────────────────────────────────────

@Repository
interface LeaveRequestRepository extends org.springframework.data.jpa.repository.JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.manager.id = :managerId")
    List<LeaveRequest> findByManagerId(Long managerId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.manager.id = :managerId AND lr.status = :status")
    List<LeaveRequest> findByManagerIdAndStatus(Long managerId, LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.status = 'APPROVED' AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingLeaves(Long employeeId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.status = :status")
    Long countByStatus(LeaveStatus status);

    @Query("SELECT SUM(lr.totalDays) FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.status = 'APPROVED' AND YEAR(lr.startDate) = :year")
    Integer getTotalApprovedDaysByEmployeeAndYear(Long employeeId, int year);
}
