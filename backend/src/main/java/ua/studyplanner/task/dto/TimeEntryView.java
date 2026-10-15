package ua.studyplanner.task.dto;

import ua.studyplanner.session.dto.SessionRecord;

import ua.studyplanner.session.domain.SessionSource;

import java.time.Instant;
import java.util.UUID;

public record TimeEntryView(
        UUID id, UUID taskId, Instant startedAt, Instant endedAt, Integer minutes, SessionSource source) {
    public static TimeEntryView of(SessionRecord e) {
        return new TimeEntryView(
                e.id(), e.taskId(), e.startedAt(), e.endedAt(), e.minutes(), e.source());
    }
}
