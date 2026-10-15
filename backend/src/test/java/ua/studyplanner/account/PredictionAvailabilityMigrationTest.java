package ua.studyplanner.account;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PredictionAvailabilityMigrationTest {
    @Test void upgradePreservesOldTasksWithoutInventingAvailability() {
        var source = new SingleConnectionDataSource(
                "jdbc:h2:mem:predictionavailability;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "", true);
        try {
            Flyway.configure().dataSource(source).target("22").load().migrate();
            var db = new JdbcTemplate(source);
            var subject = UUID.randomUUID();
            var task = UUID.randomUUID();
            var user = UUID.fromString("00000000-0000-0000-0000-000000000001");
            db.update("INSERT INTO subjects(id,user_id,name,description,color,created_at,updated_at) VALUES(?,?,'Study','','#aabbcc',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", subject, user);
            db.update("INSERT INTO tasks(id,subject_id,title,description,status,priority,estimated_minutes,created_at,updated_at) VALUES(?,?,'Old task','','DONE','MEDIUM',60,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", task, subject);
            Flyway.configure().dataSource(source).load().migrate();
            assertNull(db.queryForMap("SELECT prediction_data_at FROM tasks WHERE id=?", task).get("prediction_data_at"));
            assertEquals(60, db.queryForObject("SELECT estimated_minutes FROM tasks WHERE id=?", Integer.class, task));
        } finally { source.destroy(); }
    }
}
