package ua.studyplanner.account.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import ua.studyplanner.account.service.AccountDataService;
import ua.studyplanner.account.dto.AccountView;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.common.exception.BusinessRuleException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AccountService {
 private final AccountDataService accounts;private final PasswordEncoder passwords;private final String claimToken;
 @org.springframework.beans.factory.annotation.Autowired private ua.studyplanner.gamification.service.GamificationService gamification;
 private final String dummy;
 public AccountService(AccountDataService accounts,PasswordEncoder passwords,@Value("${app.legacy-claim-token:}") String claimToken){this.accounts=accounts;this.passwords=passwords;this.claimToken=claimToken;dummy=passwords.encode(UUID.randomUUID().toString());}
 private String email(String value){return value.strip().toLowerCase(Locale.ROOT);}
 @Transactional
 public AccountView register(String email,String name,String password,String claim){
  if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new BusinessRuleException("Пароль має займати не більше 72 байтів");
  String hash=passwords.encode(password);accounts.lockSetup();
  if(accounts.emailExists(email(email)))throw new BusinessRuleException("Не вдалося зареєструвати цю адресу. Спробуйте увійти або використайте іншу.");
  UUID id=accounts.create(email(email),name.strip(),hash);
  if(claim!=null && !claim.isBlank()){
   if(claimToken.length()<24 || !MessageDigest.isEqual(claimToken.getBytes(StandardCharsets.UTF_8),claim.getBytes(StandardCharsets.UTF_8)))throw new BusinessRuleException("Неправильний код перенесення даних");
   accounts.claimLegacy(id);
  }
  return accounts.profile(id);
 }
 public UUID authenticate(String email,String password){if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new org.springframework.security.authentication.BadCredentialsException("Неправильна пошта або пароль");var c=accounts.credentials(email(email));boolean matches=passwords.matches(password,c.map(AccountDataService.Credentials::hash).orElse(dummy));if(!matches||c.isEmpty())throw new org.springframework.security.authentication.BadCredentialsException("Неправильна пошта або пароль");return c.get().id();}
 public AccountView me(){return accounts.profile(CurrentUser.id());}
 @Transactional public AccountView update(String name,boolean reminders,int minutes,Integer studyDays){accounts.lockUser(CurrentUser.id());int days=studyDays==null?me().studyDays():studyDays;if(days<1||days>127)throw new BusinessRuleException("Оберіть хоча б один навчальний день");gamification.captureAchievements(java.time.Instant.now());accounts.update(CurrentUser.id(),name.strip(),reminders,minutes,days);gamification.captureAchievements(java.time.Instant.now());return me();}
}
