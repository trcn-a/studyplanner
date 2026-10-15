package ua.studyplanner.googlecalendar.controllers;

import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import ua.studyplanner.googlecalendar.service.GoogleCalendarService;
import ua.studyplanner.googlecalendar.config.GoogleCalendarConfig;
import ua.studyplanner.googlecalendar.integration.GoogleCalendarClient;
import ua.studyplanner.common.exception.BusinessRuleException;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/google-calendar")
public class GoogleCalendarController {
 private record Attempt(String state,String verifier,long expires,UUID userId) {}
 private final GoogleCalendarService service;
 private final GoogleCalendarConfig config;
 private final GoogleCalendarClient client;
 public GoogleCalendarController(GoogleCalendarService service,GoogleCalendarConfig config,GoogleCalendarClient client) {this.service=service;this.config=config;this.client=client;}
 @GetMapping public GoogleCalendarService.Status status() {return service.status();}
 @PostMapping(value="/connect",headers="X-StudyPlanner=1")
 public Map<String,String> connect(HttpSession session) throws Exception {
  if(!config.configured()) throw new BusinessRuleException("Підключення Google Calendar ще не налаштоване на сервері.");
  String state=random(),verifier=random();session.setAttribute("googleAttempt",new Attempt(state,verifier,System.currentTimeMillis()+600000,ua.studyplanner.account.security.CurrentUser.id()));
  String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
  return Map.of("url",client.authorizationUrl(state,challenge));
 }
 @GetMapping("/callback")
 public ResponseEntity<Void> callback(@RequestParam(required=false) String state,@RequestParam(required=false) String code,HttpServletRequest request) {
  HttpSession session=request.getSession(false);Attempt attempt=null;
  if(session!=null) synchronized(session) {attempt=(Attempt)session.getAttribute("googleAttempt");session.removeAttribute("googleAttempt");}
  boolean valid=attempt!=null && attempt.userId().equals(ua.studyplanner.account.security.CurrentUser.id()) && attempt.expires()>System.currentTimeMillis() && state!=null && MessageDigest.isEqual(attempt.state().getBytes(StandardCharsets.UTF_8),state.getBytes(StandardCharsets.UTF_8));
  if(!valid || code==null) return redirect("error");
  try {service.connect(code,attempt.verifier());return redirect("connected");}
  catch(Exception e) {return redirect("error");}
 }
 @PostMapping(value="/sync",headers="X-StudyPlanner=1") public GoogleCalendarService.Status sync() {service.sync();return service.status();}
 @DeleteMapping(headers="X-StudyPlanner=1") public GoogleCalendarService.Status disconnect() {service.disconnect();return service.status();}
 private ResponseEntity<Void> redirect(String result) {return ResponseEntity.status(303).header("Location",config.frontendUrl()+"?google="+result).build();}
 private static String random() {byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
}
