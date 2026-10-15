package ua.studyplanner.account.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ua.studyplanner.account.service.AccountDataService;
import ua.studyplanner.account.security.CurrentUser;
@Service
public class AccountAccess {
 private final AccountDataService accounts;
 public AccountAccess(AccountDataService accounts){this.accounts=accounts;}
 @Transactional(propagation=Propagation.MANDATORY) public void lockCurrent(){accounts.lockUser(CurrentUser.id());}
 @Transactional(readOnly=true) public List<UUID> activeUsers(){return accounts.users();}
}
