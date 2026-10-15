package ua.studyplanner.planning.domain;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.common.exception.BusinessRuleException;
import java.time.*;
import java.util.Comparator;

public final class PlanningSettingsValidator {
    private PlanningSettingsValidator() {}
    public static void validate(Settings input) {
        if (input.breakMinutes() < 0 || input.breakMinutes() > 120)
            throw new BusinessRuleException("Перерва має бути від 0 до 120 хвилин");
        try { ZoneId.of(input.zoneId()); }
        catch(DateTimeException e) { throw new BusinessRuleException("Невідомий часовий пояс"); }
        for(var a:input.availability()) {
            if(!a.end().isAfter(a.start()) || a.start().getSecond()!=0 || a.end().getSecond()!=0
                    || a.start().getNano()!=0 || a.end().getNano()!=0) {
                throw new BusinessRuleException("Задайте початок і кінець у межах одного дня з точністю до хвилини");
            }
        }
        var sorted=input.availability().stream().sorted(Comparator.comparingInt(Availability::dayOfWeek).thenComparing(Availability::start)).toList();
        for(int i=1;i<sorted.size();i++) {
            if(sorted.get(i).dayOfWeek()==sorted.get(i-1).dayOfWeek() && sorted.get(i).start().isBefore(sorted.get(i-1).end())) {
                throw new BusinessRuleException("Доступні проміжки не повинні перетинатися");
            }
        }
    }
}
