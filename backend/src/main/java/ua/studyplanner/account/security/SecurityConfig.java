package ua.studyplanner.account.security;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwords(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityContextRepository contexts(){return new HttpSessionSecurityContextRepository();}
 @Bean org.springframework.security.core.userdetails.UserDetailsService noDefaultUser(){return name->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use account login");};}
 @Bean SecurityFilterChain security(HttpSecurity http,SecurityContextRepository contexts) throws Exception {
  return http.addFilterBefore(new AuthThrottle(),org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/api/auth/register","/error").permitAll().anyRequest().authenticated())
   .securityContext(c->c.securityContextRepository(contexts))
   .requestCache(c->c.requestCache(new NullRequestCache()))
   .exceptionHandling(e->e.authenticationEntryPoint((q,s,x)->{s.setStatus(401);s.setContentType("application/json;charset=UTF-8");s.getWriter().write("{\"message\":\"Потрібно увійти в обліковий запис\"}");})
    .accessDeniedHandler((q,s,x)->{s.setStatus(403);s.setContentType("application/json;charset=UTF-8");s.getWriter().write("{\"message\":\"Оновіть сторінку та повторіть дію\"}");}))
   .logout(l->l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID").logoutSuccessHandler((q,s,a)->s.setStatus(204)))
   .build();
 }
}
