package ua.studyplanner.account.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

/** A bounded per-address throttle for password checks and account creation. */
public final class AuthThrottle extends OncePerRequestFilter {
 private record Bucket(long until,int attempts) {}
 private final Map<String,Bucket> attempts=new HashMap<>();
 private synchronized boolean allowed(String address) {
  long now=System.currentTimeMillis();attempts.values().removeIf(b->b.until()<now);
  var previous=attempts.get(address);
  if(previous==null && attempts.size()>=10000)return false;
  if(previous!=null && previous.attempts()>=30)return false;
  attempts.put(address,new Bucket(previous==null?now+900000:previous.until(),previous==null?1:previous.attempts()+1));return true;
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String path=request.getServletPath();
  if("POST".equals(request.getMethod()) && (path.equals("/api/auth/login")||path.equals("/api/auth/register")) && !allowed(request.getRemoteAddr())) {
   response.setStatus(429);response.setHeader("Retry-After","900");response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"Забагато спроб. Спробуйте пізніше.\"}");return;
  }
  chain.doFilter(request,response);
 }
}
