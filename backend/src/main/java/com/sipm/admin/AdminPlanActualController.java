package com.sipm.admin;

import com.sipm.admin.dto.PlanActualResponse;
import com.sipm.common.util.Times;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/admin/plan-vs-actual")
public class AdminPlanActualController {
    private final PlanActualService planActualService;

    public AdminPlanActualController(PlanActualService planActualService) {
        this.planActualService = planActualService;
    }

    @GetMapping
    public PlanActualResponse planVsActual(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return planActualService.planVsActual(projectId, from, to, Times.orToday(asOf));
    }
}
