package ua.studyplanner.task.controllers;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.service.TaskService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/subjects/{id}/tasks")
    public List<TaskView> tasks(@PathVariable UUID id) {
        return service.tasks(id);
    }

    @PostMapping("/subjects/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskView createTask(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID parentId,
            @Valid @RequestBody TaskInput input) {
        return service.createTask(id, parentId, input);
    }

    @PutMapping("/tasks/{id}")
    public TaskView updateTask(@PathVariable UUID id, @Valid @RequestBody TaskInput input) {
        return service.updateTask(id, input);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskView updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusInput input) {
        return service.updateStatus(id, input.status());
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable UUID id) {
        service.deleteTask(id);
    }
}
