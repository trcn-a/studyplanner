package ua.studyplanner.prediction.controllers;

import org.springframework.web.bind.annotation.*;

import ua.studyplanner.prediction.dto.PredictionReport;
import ua.studyplanner.prediction.service.DurationPredictionService;

@RestController
@RequestMapping("/api")
public class PredictionController {
    private final DurationPredictionService service;

    public PredictionController(DurationPredictionService service) {
        this.service = service;
    }

    @PostMapping("/predictions/preview")
    public org.springframework.http.ResponseEntity<ua.studyplanner.prediction.dto.PredictionView> preview(
            @jakarta.validation.Valid @RequestBody ua.studyplanner.prediction.dto.PredictionPreviewInput input) {
        var result = service.preview(input);
        return result == null ? org.springframework.http.ResponseEntity.noContent().build()
                : org.springframework.http.ResponseEntity.ok(result);
    }

    /** Прогнози для всіх невиконаних задач і стан моделі. */
    @GetMapping("/predictions")
    public PredictionReport predictions() {
        return service.report();
    }
}
