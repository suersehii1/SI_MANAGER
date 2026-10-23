package com.sipm.project.dto;

import com.sipm.common.validation.DateRange;
import com.sipm.project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Plain class (not a record) so Spring's BindingResult can read rejected values via getters. */
@DateRange(start = "plannedStartDate", end = "plannedEndDate")
@DateRange(start = "actualStartDate", end = "actualEndDate", requireStartWhenEnd = true)
public class ProjectRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9-]{2,30}$", message = "코드는 영문 대문자/숫자/- 2~30자입니다.")
    private String code;
    @NotBlank
    @Size(max = 200)
    private String name;
    @Size(max = 200)
    private String clientName;
    @Size(max = 4000)
    private String description;
    private ProjectStatus status;
    @NotNull
    private LocalDate plannedStartDate;
    @NotNull
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(LocalDate v) { this.plannedStartDate = v; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public void setPlannedEndDate(LocalDate v) { this.plannedEndDate = v; }
    public LocalDate getActualStartDate() { return actualStartDate; }
    public void setActualStartDate(LocalDate v) { this.actualStartDate = v; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public void setActualEndDate(LocalDate v) { this.actualEndDate = v; }
}
