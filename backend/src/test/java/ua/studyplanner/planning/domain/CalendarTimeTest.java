package ua.studyplanner.planning.domain;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import ua.studyplanner.common.time.UserTime;
import ua.studyplanner.common.exception.BusinessRuleException;
import ua.studyplanner.planning.dto.*;
import static org.junit.jupiter.api.Assertions.*;

class CalendarTimeTest {
    @Test void daylightSavingGapIsRejectedAndOverlapHasExplicitPolicy() {
        var zone=ZoneId.of("Europe/Kyiv");
        assertThrows(BusinessRuleException.class,()->UserTime.resolve(LocalDateTime.parse("2026-03-29T03:30"),zone));
        assertEquals(ZoneOffset.ofHours(3),UserTime.resolve(LocalDateTime.parse("2026-10-25T03:30"),zone).getOffset());
    }
    @Test void automaticSessionsAvoidDstDiscontinuities() {
        for(var day:List.of(LocalDate.of(2026,3,29),LocalDate.of(2026,10,25))) {
            var settings=new Settings("Europe/Kyiv",true,1,60,List.of(new Availability(7,LocalTime.of(1,0),LocalTime.of(6,0))),false);
            var result=new PlanningEngine().plan(day.atStartOfDay(),settings,List.of(new PlanningEngine.Work(UUID.randomUUID(),"Study",day,1,240)),List.of());
            assertFalse(result.sessions().isEmpty());
            for(var block:result.sessions()) {
                var a=UserTime.resolve(block.start(),ZoneId.of(settings.zoneId()));var b=UserTime.resolve(block.end(),ZoneId.of(settings.zoneId()));
                assertEquals(Duration.between(block.start(),block.end()),Duration.between(a,b));
            }
        }
    }
}
