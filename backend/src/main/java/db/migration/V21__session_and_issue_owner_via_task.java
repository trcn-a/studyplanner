package db.migration;
import org.flywaydb.core.api.migration.*;
public class V21__session_and_issue_owner_via_task extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 21001;}
 @Override public void migrate(Context context)throws Exception {
  var c=context.getConnection();var postgres=c.getMetaData().getDatabaseProductName().equals("PostgreSQL");
  try(var st=c.createStatement()){
   if(postgres)st.execute("DROP INDEX uq_session_running_user");
   else {st.execute("ALTER TABLE study_sessions DROP CONSTRAINT uq_session_running_user");st.execute("ALTER TABLE study_sessions DROP COLUMN running_user");}
   st.execute("DROP INDEX idx_session_user_status");
   st.execute("ALTER TABLE study_sessions DROP COLUMN user_id");
   st.execute("DROP INDEX idx_planning_issues_user");
   st.execute("ALTER TABLE planning_issues DROP COLUMN user_id");
  }
 }
}
