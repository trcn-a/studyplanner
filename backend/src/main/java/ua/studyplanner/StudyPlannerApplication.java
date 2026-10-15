package ua.studyplanner;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudyPlannerApplication {

    public static void main(String[] args) {
        // Keep JDBC connections and server timestamps independent of the OS timezone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(StudyPlannerApplication.class, args);
    }
}
