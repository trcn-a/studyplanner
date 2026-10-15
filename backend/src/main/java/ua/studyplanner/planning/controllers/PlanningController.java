package ua.studyplanner.planning.controllers;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.planning.service.PlanningService;
import java.util.UUID;
@RestController @RequestMapping("/api/planning")
public class PlanningController {
 private final PlanningService service;
 public PlanningController(PlanningService service){this.service=service;}
 @GetMapping public State read(){return service.read();}
 @PutMapping("/settings") public State settings(@Valid @RequestBody Settings input){return service.saveSettings(input);}
 @PostMapping("/generate") public State generate(){return service.generate();}
 @PostMapping("/refresh") public State refresh(){service.refreshAutomatically();return service.read();}
 @PatchMapping("/sessions/{id}") public State move(@PathVariable UUID id,@Valid @RequestBody MoveSessionInput input){return service.moveSession(id,input.start(),false);}
 @PatchMapping("/tasks/{id}/session") public State moveManual(@PathVariable UUID id,@Valid @RequestBody MoveSessionInput input){return service.moveSession(id,input.start(),true);}
 @DeleteMapping("/events/{id}") public State remove(@PathVariable UUID id){return service.removeEvent(id);}
}
