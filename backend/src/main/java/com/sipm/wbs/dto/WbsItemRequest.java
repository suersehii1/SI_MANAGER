package com.sipm.wbs.dto;

import com.sipm.common.validation.DateRange;
import com.sipm.wbs.WbsStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** WBS 생성(#31)/수정(#32) 요청. parentId/insertAfterId는 생성에서만 사용. */
@DateRange(start = "plannedStart", end = "plannedEnd")
@DateRange(start = "actualStart", end = "actualEnd", requireStartWhenEnd = true)
public class WbsItemRequest {
    private Long parentId;
    private Long insertAfterId;
    @NotBlank
    @Size(max = 200)
    private String name;
    @Size(max = 4000)
    private String description;
    @Size(max = 200)
    private String deliverable;
    private Long assigneeId;
    private WbsStatus status;
    @DecimalMin("0.01")
    @DecimalMax("999.99")
    private BigDecimal weight;
    private LocalDate plannedStart;
    private LocalDate plannedEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    @Min(0)
    @Max(100)
    private Integer progress;
    /** 수정 시 화면이 불러온 버전 (덮어쓰기 방지). 생성 시에는 쓰지 않음 */
    private Long version;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getInsertAfterId() { return insertAfterId; }
    public void setInsertAfterId(Long insertAfterId) { this.insertAfterId = insertAfterId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDeliverable() { return deliverable; }
    public void setDeliverable(String deliverable) { this.deliverable = deliverable; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public WbsStatus getStatus() { return status; }
    public void setStatus(WbsStatus status) { this.status = status; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate v) { this.plannedStart = v; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate v) { this.plannedEnd = v; }
    public LocalDate getActualStart() { return actualStart; }
    public void setActualStart(LocalDate v) { this.actualStart = v; }
    public LocalDate getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDate v) { this.actualEnd = v; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
}
