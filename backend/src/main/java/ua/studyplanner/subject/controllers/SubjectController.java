package ua.studyplanner.subject.controllers;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ua.studyplanner.subject.dto.*;
import ua.studyplanner.subject.service.SubjectService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SubjectController {
    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping("/subjects")
    public List<SubjectView> subjects() {
        return service.subjects();
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectView createSubject(@Valid @RequestBody SubjectInput input) {
        return service.createSubject(input);
    }

    @PutMapping("/subjects/{id}")
    public SubjectView updateSubject(
            @PathVariable UUID id, @Valid @RequestBody SubjectInput input) {
        return service.updateSubject(id, input);
    }
}
