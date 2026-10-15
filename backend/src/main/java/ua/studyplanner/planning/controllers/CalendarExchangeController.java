package ua.studyplanner.planning.controllers;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.planning.service.CalendarExchangeService;
@RestController @RequestMapping("/api/planning/calendar")
public class CalendarExchangeController {
 private final CalendarExchangeService service;
 public CalendarExchangeController(CalendarExchangeService service){this.service=service;}
 @PostMapping("/import") public State importCalendar(@Valid @RequestBody ImportInput input){return service.importCalendar(input.content());}
 @GetMapping(value="/export",produces="text/calendar;charset=UTF-8") public ResponseEntity<String> exportCalendar(){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=studyplanner.ics").body(service.exportCalendar());}
}
