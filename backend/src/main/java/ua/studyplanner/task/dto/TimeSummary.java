package ua.studyplanner.task.dto;

import ua.studyplanner.session.dto.SessionRecord;


import java.time.Instant;
import java.util.Collection;

/** Підсумок обліку часу задачі: сума завершених записів і момент запуску активного таймера. */
public record TimeSummary(int minutes, Instant runningSince) {
    public static final TimeSummary EMPTY = new TimeSummary(0, null);

    public static TimeSummary of(Collection<SessionRecord> entries) {
        int minutes = 0;
        Instant running = null;
        for (SessionRecord entry : entries) {
            if (entry.isRunning()) {
                running = entry.startedAt();
            } else if (entry.isCompleted()) {
                minutes += entry.minutes();
            }
        }
        return new TimeSummary(minutes, running);
    }
}
