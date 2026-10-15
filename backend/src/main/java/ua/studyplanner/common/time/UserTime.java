package ua.studyplanner.common.time;

import java.time.*;

import org.springframework.stereotype.Component;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.common.exception.BusinessRuleException;

/** Converts calendar wall time at the persistence boundary; the API stays local. */
@Component
public class UserTime {
    private final UserZoneProvider zones;
    public UserTime(UserZoneProvider zones) { this.zones=zones; }
    public ZoneId zone() {
        return zones.zone();
    }
    public OffsetDateTime store(LocalDateTime local) {
        if (local == null) return null;
        return resolve(local, zone());
    }
    public static OffsetDateTime resolve(LocalDateTime local, ZoneId zone) {
        var offsets = zone.getRules().getValidOffsets(local);
        if (offsets.isEmpty()) throw new BusinessRuleException("Цей час не існує через перехід на літній час. Оберіть інший час.");
        // On the repeated autumn hour, consistently choose its first occurrence.
        return local.atOffset(offsets.getFirst());
    }
    public LocalDateTime display(OffsetDateTime instant) {
        return instant == null ? null : instant.atZoneSameInstant(zone()).toLocalDateTime();
    }
}
