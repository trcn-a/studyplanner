package ua.studyplanner.analytics.controllers;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ua.studyplanner.analytics.dto.AnalyticsView;
import ua.studyplanner.analytics.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    /** Агреговані показники; обидві дати необов’язкові (за замовчуванням — останні 30 днів). */
    @GetMapping
    public AnalyticsView analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.report(from, to);
    }
}
