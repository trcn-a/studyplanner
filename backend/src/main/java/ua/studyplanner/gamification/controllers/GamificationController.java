package ua.studyplanner.gamification.controllers;
import org.springframework.web.bind.annotation.*;
import ua.studyplanner.gamification.service.GamificationService;
import ua.studyplanner.gamification.dto.GamificationView;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {
 private final GamificationService service;
 public GamificationController(GamificationService service) {this.service=service;}
 @GetMapping public GamificationView summary() {return service.summary();}
}
