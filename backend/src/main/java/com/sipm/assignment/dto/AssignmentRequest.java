package com.sipm.assignment.dto;

import com.sipm.common.validation.DateRange;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@DateRange(start = "plannedStart", end = "plannedEnd")
@DateRange(start = "actualStart", end = "actualEnd", requireStartWhenEnd = true)
public class AssignmentRequest {
    @NotNull
    private Long userId;
    @Size(max = 50)
    private String roleInProject;
    @DecimalMin("0.01")
    @DecimalMax("1.00")
    @Digits(integer = 1, fraction = 2)
    private BigDecimal allocationRate;
    @NotNull
    private LocalDate plannedStart;
    @NotNull
    private LocalDate plannedEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    @Size(max = 500)
    private String note;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleInProject() { return roleInProject; }
    public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
    public BigDecimal getAllocationRate() { return allocationRate; }
    public void setAllocationRate(BigDecimal allocationRate) { this.allocationRate = allocationRate; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public LocalDate getActualStart() { return actualStart; }
    public void setActualStart(LocalDate actualStart) { this.actualStart = actualStart; }
    public LocalDate getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDate actualEnd) { this.actualEnd = actualEnd; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
