package ua.studyplanner.task.controllers;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ua.studyplanner.task.dto.ManualTimeInput;
import ua.studyplanner.task.dto.TaskView;
import ua.studyplanner.task.dto.TimeEntryView;
import ua.studyplanner.task.service.TimeTrackingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TimeTrackingController {
    private final TimeTrackingService service;

    public TimeTrackingController(TimeTrackingService service) {
        this.service = service;
    }

    @GetMapping("/tasks/{id}/time-entries")
    public List<TimeEntryView> entries(@PathVariable UUID id) {
        return service.entries(id);
    }

    @PostMapping("/tasks/{id}/timer/start")
    public TaskView startTimer(@PathVariable UUID id) {
        return service.startTimer(id);
    }

    @PostMapping("/tasks/{id}/timer/stop")
    public TaskView stopTimer(@PathVariable UUID id) {
        return service.stopTimer(id);
    }

    @PostMapping("/tasks/{id}/time-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskView logManual(@PathVariable UUID id, @Valid @RequestBody ManualTimeInput input) {
        return service.logManual(id, input);
    }

    @DeleteMapping("/time-entries/{id}")
    public TaskView deleteEntry(@PathVariable UUID id) {
        return service.deleteEntry(id);
    }
}
