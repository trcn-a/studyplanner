package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.*;
import java.util.*;

/** Plan estimates and recorded work share a lifecycle; never infer actual time from an old plan. */
public class V12__unified_study_sessions extends BaseJavaMigration {
    @Override public Integer getChecksum(){return 12001;}
    @Override public void migrate(Context context) throws Exception {
        Connection c=context.getConnection();
        sql(c,"""
            CREATE TABLE unified_sessions (
              id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES app_users(id), task_id UUID NOT NULL,
              starts_at TIMESTAMP WITH TIME ZONE NOT NULL, ends_at TIMESTAMP WITH TIME ZONE,
              minutes INTEGER, status VARCHAR(12) NOT NULL, source VARCHAR(10) NOT NULL,
              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
              CONSTRAINT fk_session_task_owner FOREIGN KEY(task_id,user_id) REFERENCES tasks(id,user_id) ON DELETE CASCADE ON UPDATE CASCADE,
              CONSTRAINT chk_session_source CHECK(source IN ('AUTO','TIMER','MANUAL')),
              CONSTRAINT chk_session_lifecycle CHECK(
                (status IN ('PLANNED','SKIPPED') AND ends_at IS NOT NULL AND ends_at>starts_at AND minutes IS NULL AND source IN ('AUTO','MANUAL')) OR
                (status='RUNNING' AND ends_at IS NULL AND minutes IS NULL AND source='TIMER') OR
                (status='COMPLETED' AND ends_at IS NOT NULL AND ends_at>=starts_at AND minutes IS NOT NULL AND minutes>0 AND source IN ('TIMER','MANUAL'))
              )
            )
            """);
        sql(c,"""
            INSERT INTO unified_sessions(id,user_id,task_id,starts_at,ends_at,minutes,status,source,created_at)
            SELECT s.id,s.user_id,s.task_id,s.starts_at,s.ends_at,NULL,
              CASE WHEN t.status='DONE' OR s.ends_at<=CURRENT_TIMESTAMP THEN 'SKIPPED' ELSE 'PLANNED' END,
              'AUTO',CURRENT_TIMESTAMP
            FROM study_sessions s JOIN tasks t ON t.id=s.task_id
            """);
        sql(c,"""
            INSERT INTO unified_sessions(id,user_id,task_id,starts_at,ends_at,minutes,status,source,created_at)
            SELECT id,user_id,task_id,started_at,ended_at,minutes,
              CASE WHEN ended_at IS NULL THEN 'RUNNING' ELSE 'COMPLETED' END,source,created_at FROM time_entries
            """);
        // Preserve task-form manual schedules too, before removing their duplicate storage.
        try(var select=c.createStatement();var rs=select.executeQuery("SELECT id,user_id,scheduled_start,scheduled_end,status,created_at FROM tasks WHERE scheduled_start IS NOT NULL");
            var insert=c.prepareStatement("INSERT INTO unified_sessions(id,user_id,task_id,starts_at,ends_at,minutes,status,source,created_at) VALUES(?,?,?,?,?,NULL,?,'MANUAL',?)")) {
            while(rs.next()) {
                var end=rs.getObject(4,java.time.OffsetDateTime.class);
                insert.setObject(1,UUID.randomUUID());insert.setObject(2,rs.getObject(2));insert.setObject(3,rs.getObject(1));
                insert.setObject(4,rs.getObject(3,java.time.OffsetDateTime.class));insert.setObject(5,end);
                insert.setString(6,rs.getString(5).equals("DONE")||!end.toInstant().isAfter(java.time.Instant.now())?"SKIPPED":"PLANNED");
                insert.setObject(7,rs.getObject(6,java.time.OffsetDateTime.class));insert.addBatch();
            }
            insert.executeBatch();
        }
        // Counts checked before removing the superseded tables. UUID collisions fail the insert.
        try(var st=c.createStatement();var rs=st.executeQuery("SELECT (SELECT COUNT(*) FROM unified_sessions), (SELECT COUNT(*) FROM study_sessions)+(SELECT COUNT(*) FROM time_entries)+(SELECT COUNT(*) FROM tasks WHERE scheduled_start IS NOT NULL)")) {
            rs.next();if(rs.getLong(1)!=rs.getLong(2))throw new SQLException("Session migration record count mismatch");
        }
        sql(c,"DROP TABLE time_entries");
        sql(c,"DROP TABLE study_sessions");
        sql(c,"ALTER TABLE unified_sessions RENAME TO study_sessions");
        sql(c,"ALTER TABLE tasks DROP CONSTRAINT chk_task_schedule");
        sql(c,"ALTER TABLE tasks DROP COLUMN scheduled_start");
        sql(c,"ALTER TABLE tasks DROP COLUMN scheduled_end");
        if(c.getMetaData().getDatabaseProductName().equals("PostgreSQL")) {
            sql(c,"CREATE UNIQUE INDEX uq_session_running_user ON study_sessions(user_id) WHERE status='RUNNING'");
            sql(c,"CREATE UNIQUE INDEX uq_session_manual_plan ON study_sessions(task_id) WHERE status='PLANNED' AND source='MANUAL'");
        } else {
            sql(c,"ALTER TABLE study_sessions ADD COLUMN running_user UUID GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN user_id ELSE NULL END)");
            sql(c,"ALTER TABLE study_sessions ADD CONSTRAINT uq_session_running_user UNIQUE(running_user)");
            sql(c,"ALTER TABLE study_sessions ADD COLUMN manual_task UUID GENERATED ALWAYS AS (CASE WHEN status='PLANNED' AND source='MANUAL' THEN task_id ELSE NULL END)");
            sql(c,"ALTER TABLE study_sessions ADD CONSTRAINT uq_session_manual_plan UNIQUE(manual_task)");
        }
        sql(c,"CREATE INDEX idx_session_task ON study_sessions(task_id)");
        sql(c,"CREATE INDEX idx_session_user_status ON study_sessions(user_id,status,starts_at)");
        sql(c,"UPDATE planning_settings SET fingerprint=NULL");
    }
    private void sql(Connection c,String query)throws SQLException{try(var st=c.createStatement()){st.execute(query);}}
}
