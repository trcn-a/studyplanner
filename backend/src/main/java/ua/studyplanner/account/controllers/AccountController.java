package ua.studyplanner.account.controllers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import ua.studyplanner.account.service.AccountService;
import ua.studyplanner.account.dto.AccountView;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AccountController {
 public record Registration(@Email @NotBlank @Size(max=254) String email,@NotBlank @Size(max=100) String name,@NotBlank @Size(min=10,max=72) String password,@Size(max=200) String claimCode){}
 public record Login(@Email @NotBlank @Size(max=254) String email,@NotBlank @Size(max=72) String password){}
 public record Profile(@NotBlank @Size(max=100) String name,boolean remindersEnabled,@Min(5) @Max(1440) int reminderMinutes,@Min(1) @Max(127) Integer studyDays){}
 private final AccountService service;private final SecurityContextRepository contexts;
 public AccountController(AccountService service,SecurityContextRepository contexts){this.service=service;this.contexts=contexts;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/me") public AccountView me(){return service.me();}
 @PostMapping("/register") public AccountView register(@Valid @RequestBody Registration input,HttpServletRequest request,HttpServletResponse response){var account=service.register(input.email(),input.name(),input.password(),input.claimCode());signIn(account.id(),request,response);return account;}
 @PostMapping("/login") public AccountView login(@Valid @RequestBody Login input,HttpServletRequest request,HttpServletResponse response){signIn(service.authenticate(input.email(),input.password()),request,response);return service.me();}
 @PutMapping("/profile") public AccountView profile(@Valid @RequestBody Profile input){return service.update(input.name(),input.remindersEnabled(),input.reminderMinutes(),input.studyDays());}
 private void signIn(UUID id,HttpServletRequest request,HttpServletResponse response){
  var auth=UsernamePasswordAuthenticationToken.authenticated(id,null,List.of(new SimpleGrantedAuthority("ROLE_USER")));
  new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth,request,response);
  // Rotate CSRF and remove any pending Google OAuth attempt when account changes.
  new HttpSessionCsrfTokenRepository().saveToken(null,request,response);request.getSession().removeAttribute("googleAttempt");
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);
 }
}
