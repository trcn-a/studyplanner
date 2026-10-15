package db.migration;
import org.flywaydb.core.api.migration.*;
import java.sql.*;
import java.util.*;
public class V18__unified_calendar_events extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 18001;}
 @Override public void migrate(Context context)throws Exception {
  var c=context.getConnection();
  try(var st=c.createStatement()){
   st.execute("CREATE TABLE unified_calendar_events(id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES app_users(id), source VARCHAR(12) NOT NULL DEFAULT 'ICALENDAR' CHECK(source IN ('ICALENDAR','GOOGLE')), external_uid VARCHAR(500) NOT NULL, title VARCHAR(500) NOT NULL, starts_at TIMESTAMP WITH TIME ZONE NOT NULL, ends_at TIMESTAMP WITH TIME ZONE NOT NULL, UNIQUE(user_id,source,external_uid), CHECK(ends_at>starts_at))");
   st.execute("INSERT INTO unified_calendar_events(id,user_id,source,external_uid,title,starts_at,ends_at) SELECT id,user_id,'ICALENDAR',external_uid,title,starts_at,ends_at FROM calendar_events");
  }
  // Google IDs can repeat across accounts or collide with existing ICS row IDs.
  try(var st=c.createStatement();var rs=st.executeQuery("SELECT id,user_id,title,starts_at,ends_at FROM google_calendar_events");var q=c.prepareStatement("INSERT INTO unified_calendar_events(id,user_id,source,external_uid,title,starts_at,ends_at) VALUES(?,?,'GOOGLE',?,?,?,?)")){
   while(rs.next()){q.setObject(1,UUID.randomUUID());q.setObject(2,rs.getObject(2,UUID.class));q.setString(3,rs.getObject(1,UUID.class).toString());q.setString(4,rs.getString(3));q.setObject(5,rs.getObject(4,java.time.OffsetDateTime.class));q.setObject(6,rs.getObject(5,java.time.OffsetDateTime.class));q.executeUpdate();}
  }
  try(var st=c.createStatement();var r=st.executeQuery("SELECT (SELECT COUNT(*) FROM unified_calendar_events), (SELECT COUNT(*) FROM calendar_events)+(SELECT COUNT(*) FROM google_calendar_events)")){r.next();if(r.getLong(1)!=r.getLong(2))throw new SQLException("Calendar migration count mismatch");}
  try(var st=c.createStatement()){st.execute("DROP TABLE google_calendar_events");st.execute("DROP TABLE calendar_events");st.execute("ALTER TABLE unified_calendar_events RENAME TO calendar_events");st.execute("CREATE INDEX idx_calendar_user_source ON calendar_events(user_id,source)");}
 }
}
