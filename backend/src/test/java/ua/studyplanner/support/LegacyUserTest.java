package ua.studyplanner.support;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import ua.studyplanner.account.security.CurrentUser;
import java.util.List;
public abstract class LegacyUserTest {
 @BeforeEach public void setLegacy(){authenticate();}
 @AfterEach public void clearLegacy(){SecurityContextHolder.clearContext();}
 protected static void authenticate(){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(CurrentUser.LEGACY,null,List.of()));}
}
