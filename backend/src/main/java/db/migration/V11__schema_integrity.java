package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Upgrade existing records before adding ownership constraints. No data is discarded. */
public class V11__schema_integrity extends BaseJavaMigration {
    @Override public Integer getChecksum() { return 11001; }
    @Override public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        boolean postgres = c.getMetaData().getDatabaseProductName().equals("PostgreSQL");
        sql(c, "SET TIME ZONE 'UTC'");
        sql(c, "ALTER TABLE tasks ADD COLUMN user_id UUID");
        sql(c, "UPDATE tasks SET user_id=(SELECT user_id FROM subjects WHERE subjects.id=tasks.subject_id)");
        sql(c, "ALTER TABLE tasks ALTER COLUMN user_id SET NOT NULL");
        sql(c, "ALTER TABLE time_entries ADD COLUMN user_id UUID");
        sql(c, "UPDATE time_entries SET user_id=(SELECT user_id FROM tasks WHERE tasks.id=time_entries.task_id)");
        sql(c, "ALTER TABLE time_entries ALTER COLUMN user_id SET NOT NULL");
        // Existing inconsistent owners fail validation instead of silently reassigning private data.
        sql(c, "ALTER TABLE subjects ADD CONSTRAINT uq_subject_owner UNIQUE(id,user_id)");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT uq_task_owner UNIQUE(id,user_id)");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT uq_task_subject_owner UNIQUE(id,subject_id,user_id)");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT fk_task_subject_owner FOREIGN KEY(subject_id,user_id) REFERENCES subjects(id,user_id) ON UPDATE CASCADE");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT fk_task_parent_scope FOREIGN KEY(parent_task_id,subject_id,user_id) REFERENCES tasks(id,subject_id,user_id) ON DELETE CASCADE ON UPDATE CASCADE");
        for (String table : List.of("time_entries","study_sessions","planning_issues")) {
            sql(c, "ALTER TABLE " + table + " ADD CONSTRAINT fk_" + table + "_owner FOREIGN KEY(task_id,user_id) REFERENCES tasks(id,user_id) ON DELETE CASCADE ON UPDATE CASCADE");
        }
        sql(c, "ALTER TABLE subjects DROP COLUMN archived");
        sql(c, "ALTER TABLE planning_settings DROP COLUMN id");
        sql(c, "ALTER TABLE google_calendar_connection DROP COLUMN id");
        sql(c, "DROP TABLE timer_guard");
        sql(c, "DROP TABLE gamification_lock");
        sql(c, "DROP TABLE account_setup_lock");
        sql(c, "ALTER TABLE app_users ALTER COLUMN password_hash TYPE VARCHAR(255)");
        sql(c, "ALTER TABLE subjects ADD CONSTRAINT chk_subject_color CHECK (REGEXP_LIKE(color,'^#[0-9A-Fa-f]{6}$'))");
        sql(c, "ALTER TABLE notifications ADD CONSTRAINT chk_notification_kind CHECK(kind IN ('SESSION','DEADLINE','PLAN','ACHIEVEMENT'))");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT chk_initial_priority CHECK(initial_priority IN ('LOW','MEDIUM','HIGH'))");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT chk_initial_estimate CHECK(initial_estimated_minutes > 0)");
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT chk_initial_description CHECK(initial_description_length >= 0)");
        sql(c, "ALTER TABLE planning_issues ADD CONSTRAINT chk_remaining_minutes CHECK(remaining_minutes >= 0)");
        sql(c, "ALTER TABLE time_entries ADD CONSTRAINT chk_time_entry_order CHECK(ended_at IS NULL OR ended_at >= started_at)");
        sql(c, "ALTER TABLE time_entries ADD CONSTRAINT chk_running_source CHECK(ended_at IS NOT NULL OR source='TIMER')");
        if (postgres) {
            sql(c, "CREATE UNIQUE INDEX uq_user_email_case ON app_users(LOWER(email))");
            sql(c, "CREATE UNIQUE INDEX uq_running_timer_user ON time_entries(user_id) WHERE ended_at IS NULL");
        } else {
            // H2 test equivalent: nullable generated keys obey the same uniqueness rules.
            sql(c, "ALTER TABLE app_users ADD COLUMN email_key VARCHAR(254) GENERATED ALWAYS AS (LOWER(email))");
            sql(c, "ALTER TABLE app_users ADD CONSTRAINT uq_user_email_case UNIQUE(email_key)");
            sql(c, "ALTER TABLE time_entries ADD COLUMN running_user UUID GENERATED ALWAYS AS (CASE WHEN ended_at IS NULL THEN user_id ELSE NULL END)");
            sql(c, "ALTER TABLE time_entries ADD CONSTRAINT uq_running_timer_user UNIQUE(running_user)");
        }
        sql(c, "ALTER TABLE tasks DROP CONSTRAINT chk_task_schedule");
        convert(c, postgres, "tasks", "scheduled_start", "scheduled_end");
        convert(c, postgres, "study_sessions", "starts_at", "ends_at");
        convert(c, postgres, "calendar_events", "starts_at", "ends_at");
        // Same-local-day validation stays in TaskInput: a UTC day is not the user's local day.
        sql(c, "ALTER TABLE tasks ADD CONSTRAINT chk_task_schedule CHECK((scheduled_start IS NULL AND scheduled_end IS NULL) OR (scheduled_start IS NOT NULL AND scheduled_end IS NOT NULL AND scheduled_end>scheduled_start))");
    }

    private record Schedule(UUID id, OffsetDateTime start, OffsetDateTime end) {}
    private void convert(Connection c, boolean postgres, String table, String start, String end) throws SQLException {
        List<Schedule> saved = new ArrayList<>();
        try (Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT t.id,t."+start+",t."+end+",p.zone_id FROM "+table+" t JOIN planning_settings p ON p.user_id=t.user_id WHERE t."+start+" IS NOT NULL")) {
            while(rs.next()) {
                ZoneId zone=ZoneId.of(rs.getString(4));
                // Existing ambiguous wall time has no offset. Java's earlier-offset policy is deterministic.
                var from=rs.getObject(2,LocalDateTime.class);var to=rs.getObject(3,LocalDateTime.class);
                if(zone.getRules().getValidOffsets(from).isEmpty() || zone.getRules().getValidOffsets(to).isEmpty())
                    throw new SQLException("Existing schedule falls inside a DST gap: " + table + " / " + rs.getObject(1));
                saved.add(new Schedule(rs.getObject(1,UUID.class),from.atZone(zone).toOffsetDateTime(),to.atZone(zone).toOffsetDateTime()));
            }
        }
        for(String column:List.of(start,end)) sql(c,"ALTER TABLE "+table+" ALTER COLUMN "+column+" TYPE TIMESTAMP WITH TIME ZONE"+(postgres?" USING "+column+" AT TIME ZONE 'UTC'":""));
        try(PreparedStatement st=c.prepareStatement("UPDATE "+table+" SET "+start+"=?,"+end+"=? WHERE id=?")) {
            for(var row:saved) {st.setObject(1,row.start());st.setObject(2,row.end());st.setObject(3,row.id());st.addBatch();} st.executeBatch();
        }
    }
    private void sql(Connection c,String query) throws SQLException {try(Statement st=c.createStatement()){st.execute(query);}}
}
