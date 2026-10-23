package com.sipm.admin;

import com.sipm.admin.dto.TimesheetResponse;
import com.sipm.common.util.Times;
import com.sipm.metrics.Basis;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/admin/timesheet")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTimesheetController {
    private final TimesheetService timesheetService;

    public AdminTimesheetController(TimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    @GetMapping
    public TimesheetResponse timesheet(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth to,
            @RequestParam(required = false) Basis basis,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return timesheetService.timesheet(from, to, basis, projectId, Times.orToday(asOf));
    }
}
