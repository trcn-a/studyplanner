package ua.studyplanner.planning.domain;

import java.time.*;
import java.util.*;

/** Frequency of observed activity, not a claim about productivity or learning outcomes. */
public final class ActivityPreferences {
    public static final int MIN_DAYS = 3;
    public record Observation(UUID taskId, UUID subjectId, Instant start, Instant end) {}
    private final Profile global = new Profile();
    private final Map<UUID,Profile> subjects = new HashMap<>();
    private final Map<UUID,Profile> tasks = new HashMap<>();
    private int samples;

    public static ActivityPreferences empty() { return new ActivityPreferences(); }

    public static ActivityPreferences from(List<Observation> history, ZoneId zone, Instant now) {
        var result = new ActivityPreferences();
        for (var session : history) {
            if (session.start() == null || session.end() == null || session.end().isAfter(now)) continue;
            long minutes = Duration.between(session.start(),session.end()).toMinutes();
            if (minutes < 5 || minutes > 480 || session.start().isBefore(now.minus(Duration.ofDays(180)))) continue;
            result.samples++;
            // Iterate on the instant timeline so midnight and DST boundaries use the actual local hour.
            for (Instant at=session.start(); at.isBefore(session.end()); at=at.plusSeconds(60)) {
                var local = at.atZone(zone);
                double amount = Math.min(60,Duration.between(at,session.end()).getSeconds()) / 60.0;
                result.global.add(local.toLocalDate(),local.getHour(),amount);
                result.subjects.computeIfAbsent(session.subjectId(),key -> new Profile()).add(local.toLocalDate(),local.getHour(),amount);
                result.tasks.computeIfAbsent(session.taskId(),key -> new Profile()).add(local.toLocalDate(),local.getHour(),amount);
            }
        }
        LocalDate today = now.atZone(zone).toLocalDate();
        result.global.finish(today);
        result.subjects.values().forEach(p -> p.finish(today));
        result.tasks.values().forEach(p -> p.finish(today));
        return result;
    }

    public boolean ready() { return global.ready(); }
    public int sessions() { return samples; }
    public int activeDays() { return global.days.size(); }
    public int subjectProfiles() { return (int) subjects.values().stream().filter(Profile::ready).count(); }

    public double score(UUID taskId, UUID subjectId, LocalDateTime start, int minutes) {
        if (!ready()) return 0;
        double sum = 0;
        for (int m=0; m<minutes; m+=15) {
            LocalDateTime time=start.plusMinutes(m);
            double score=global.score(time);
            Profile subject=subjects.get(subjectId), task=tasks.get(taskId);
            if(subject!=null && subject.ready()) score=0.7*subject.score(time)+0.3*score;
            if(task!=null && task.ready()) score=0.6*task.score(time)+0.4*score;
            sum+=score*Math.min(15,minutes-m);
        }
        return sum/minutes;
    }

    private static final class Profile {
        final Map<LocalDate,double[]> days=new HashMap<>();
        final double[] hours=new double[24];
        final double[] weekdays=new double[7];
        void add(LocalDate date,int hour,double minutes) {
            days.computeIfAbsent(date,key -> new double[24])[hour]+=minutes;
        }
        boolean ready() { return days.size()>=MIN_DAYS; }
        void finish(LocalDate today) {
            double weightSum=0;
            for(var item:days.entrySet()) {
                double total=Arrays.stream(item.getValue()).sum();
                double weight=Math.pow(0.5,java.time.temporal.ChronoUnit.DAYS.between(item.getKey(),today)/45.0);
                weightSum+=weight;
                // One day has at most one vote, even if a timer was restarted many times.
                for(int h=0;h<24;h++) hours[h]+=weight*item.getValue()[h]/total;
                weekdays[item.getKey().getDayOfWeek().getValue()-1]+=weight;
            }
            if(weightSum>0) {
                for(int h=0;h<24;h++) hours[h]/=weightSum;
                for(int d=0;d<7;d++) weekdays[d]/=weightSum;
            }
        }
        double score(LocalDateTime time) {
            return 0.85*hours[time.getHour()]+0.15*weekdays[time.getDayOfWeek().getValue()-1];
        }
    }
}
