package ua.studyplanner.planning.domain;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.task.dto.PlanningSnapshot;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class PlanningSignature {
    private PlanningSignature() {}
    public static String of(Settings settings,PlanningSnapshot snapshot,List<Block> external,LocalDate date) {
        StringBuilder s=new StringBuilder("habits-v1").append(settings).append(date);
        snapshot.tasks().stream().sorted(Comparator.comparing(t -> t.id().toString()))
                .forEach(t -> s.append(t.id()).append(t.updatedAt()));
        snapshot.timeEntries().stream().sorted(Comparator.comparing(e -> e.id().toString()))
                .forEach(e -> s.append(e.id()).append(e.startedAt()).append(e.endedAt()).append(e.minutes()).append(e.source()));
        external.forEach(s::append);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.toString().getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
