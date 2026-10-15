package ua.studyplanner.account;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaMigrationTest {
    @Test void upgradePreservesDataAndEnforcesOwnership() throws Exception {
        String url=System.getProperty("schema.test.url","jdbc:h2:mem:upgrade;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        String user=System.getProperty("schema.test.user","sa"), password=System.getProperty("schema.test.password","");
        var source=new org.springframework.jdbc.datasource.SingleConnectionDataSource(url,user,password,true);
        Flyway.configure().dataSource(source).target("10").load().migrate();
        UUID legacy=UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID owner=UUID.randomUUID(),subject=UUID.randomUUID(),task=UUID.randomUUID();
        try(Connection c=source.getConnection()) {
            exec(c,"INSERT INTO app_users(id,email,display_name,password_hash) VALUES(?,'Owner@Example.com','Owner','hash')",owner);
            exec(c,"INSERT INTO subjects(id,name,description,color,created_at,updated_at) VALUES(?,'Subject','','#aabbcc',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",subject);
            exec(c,"INSERT INTO tasks(id,subject_id,title,description,status,priority,scheduled_start,scheduled_end,created_at,updated_at) VALUES(?,?,'Task','','TODO','HIGH',TIMESTAMP '2026-10-08 10:00:00',TIMESTAMP '2026-10-08 11:00:00',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",task,subject);
            exec(c,"INSERT INTO study_sessions(id,task_id,starts_at,ends_at) VALUES(?,?,TIMESTAMP '2026-10-08 10:00:00',TIMESTAMP '2026-10-08 11:00:00')",UUID.randomUUID(),task);
            exec(c,"INSERT INTO time_entries(id,task_id,started_at,source,created_at) VALUES(?,?,CURRENT_TIMESTAMP,'TIMER',CURRENT_TIMESTAMP)",UUID.randomUUID(),task);
            exec(c,"INSERT INTO completion_rewards(task_id,user_id,points) VALUES(?,?,10)",task,legacy);
        }
        Flyway.configure().dataSource(source).target("11").load().migrate();
        try(Connection c=source.getConnection()) {
            try(var rs=c.createStatement().executeQuery("SELECT scheduled_start FROM tasks")) {assertTrue(rs.next());assertEquals(Instant.parse("2026-10-08T07:00:00Z"),rs.getObject(1,OffsetDateTime.class).toInstant());}
            UUID otherSubject=UUID.randomUUID(),otherTask=UUID.randomUUID();
            exec(c,"INSERT INTO subjects(id,user_id,name,description,color,created_at,updated_at) VALUES(?,?,'Other','','#aabbcc',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",otherSubject,legacy);
            assertThrows(SQLException.class,()->exec(c,"INSERT INTO tasks(id,subject_id,user_id,parent_task_id,title,description,status,priority,created_at,updated_at) VALUES(?,?,?,?,'Child','','TODO','LOW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",UUID.randomUUID(),otherSubject,legacy,task));
            exec(c,"INSERT INTO tasks(id,subject_id,user_id,title,description,status,priority,created_at,updated_at) VALUES(?,?,?,'Second','','TODO','LOW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",otherTask,subject,legacy);
            assertThrows(SQLException.class,()->exec(c,"INSERT INTO study_sessions(id,task_id,user_id,starts_at,ends_at) VALUES(?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1' HOUR)",UUID.randomUUID(),task,owner));
            assertThrows(SQLException.class,()->exec(c,"INSERT INTO time_entries(id,task_id,user_id,started_at,source,created_at) VALUES(?,?,?,CURRENT_TIMESTAMP,'TIMER',CURRENT_TIMESTAMP)",UUID.randomUUID(),otherTask,legacy));
            assertThrows(SQLException.class,()->exec(c,"UPDATE subjects SET color='invalid' WHERE id=?",subject));
            assertThrows(SQLException.class,()->exec(c,"INSERT INTO app_users(id,email,display_name,password_hash) VALUES(?,'owner@example.com','Other','hash')",UUID.randomUUID()));
            // Ownership cascades during the one-time legacy claim, including active timers.
            exec(c,"UPDATE subjects SET user_id=? WHERE id=?",owner,subject);
            for(String table:List.of("tasks","time_entries","study_sessions")) {
                try(var rs=c.createStatement().executeQuery("SELECT user_id FROM "+table)) {assertTrue(rs.next());assertEquals(owner,rs.getObject(1,UUID.class));}
            }
            // Awards deliberately survive deletion; dependent learning records do not.
            exec(c,"DELETE FROM tasks WHERE id=?",task);
            exec(c,"DELETE FROM tasks WHERE id=?",otherTask);
            for(String table:List.of("tasks","time_entries","study_sessions"))try(var rs=c.createStatement().executeQuery("SELECT COUNT(*) FROM "+table)){rs.next();assertEquals(0,rs.getInt(1));}
            try(var rs=c.createStatement().executeQuery("SELECT COUNT(*) FROM completion_rewards")){rs.next();assertEquals(1,rs.getInt(1));}
        }
    }
    private static void exec(Connection c,String sql,Object... args) throws SQLException {
        try(var st=c.prepareStatement(sql)){for(int i=0;i<args.length;i++)st.setObject(i+1,args[i]);st.executeUpdate();}
    }
}

