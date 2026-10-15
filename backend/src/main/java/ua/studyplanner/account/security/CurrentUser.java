package ua.studyplanner.account.security;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

public final class CurrentUser {
 private static final ThreadLocal<UUID> jobUser=new ThreadLocal<>();
 public static final UUID LEGACY=UUID.fromString("00000000-0000-0000-0000-000000000001");
 private CurrentUser() {}
 public static UUID id() {
  if(jobUser.get()!=null)return jobUser.get();
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth==null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UUID)) throw new AuthenticationCredentialsNotFoundException("Потрібно увійти");
  return (UUID)auth.getPrincipal();
 }
 public static void runAs(UUID user,Runnable work) {UUID before=jobUser.get();try{jobUser.set(user);work.run();}finally{if(before==null)jobUser.remove();else jobUser.set(before);}}
}
