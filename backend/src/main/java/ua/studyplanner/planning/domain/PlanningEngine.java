package ua.studyplanner.planning.domain;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import ua.studyplanner.planning.dto.*;

/** Deadlines and occupancy are hard constraints; observed habits only rank feasible slots. */
public final class PlanningEngine {
    public record Work(UUID id, UUID subjectId, String title, LocalDate due, int priority, int minutes) {
        public Work(UUID id,String title,LocalDate due,int priority,int minutes) {
            this(id,null,title,due,priority,minutes);
        }
    }
    public record Result(List<Block> sessions, List<Issue> issues) {}
    private record Candidate(int index,LocalDateTime start,int minutes,double score) {}

    public Result plan(LocalDateTime now,Settings settings,List<Work> work,List<Block> occupied) {
        return plan(now,settings,work,occupied,ActivityPreferences.empty());
    }

    public Result plan(LocalDateTime now,Settings settings,List<Work> work,List<Block> occupied,ActivityPreferences habits) {
        var free = freeIntervals(now,settings,occupied);
        var ordered = new ArrayList<>(work);
        ordered.sort(Comparator.comparing((Work w) -> w.due()==null?LocalDate.MAX:w.due())
                .thenComparing(Comparator.comparingInt(Work::priority).reversed()).thenComparing(w -> w.id().toString()));
        var sessions=new ArrayList<Block>();
        var issues=new ArrayList<Issue>();
        for(var task:ordered) {
            int remaining=task.minutes();
            if(task.due()!=null && task.due().isBefore(now.toLocalDate())) {
                issues.add(new Issue(task.id(),task.title(),"Дедлайн минув. Оновіть дату виконання",remaining));
                continue;
            }
            while(remaining>0) {
                Candidate best=choose(free,task,Math.min(remaining,settings.sessionMinutes()),settings.habitAware()?habits:ActivityPreferences.empty());
                if(best==null) break;
                var end=best.start().plusMinutes(best.minutes());
                sessions.add(new Block(UUID.randomUUID(),task.id(),task.title(),best.start(),end,"AUTO"));
                free = subtract(free, best.start().minusMinutes(settings.breakMinutes()), end.plusMinutes(settings.breakMinutes()));
                free.sort(Comparator.comparing(Block::start));
                remaining-=best.minutes();
            }
            if(remaining>0) issues.add(new Issue(task.id(),task.title(),"Недостатньо доступного часу до дедлайну або в межах плану",remaining));
        }
        sessions.sort(Comparator.comparing(Block::start));
        return new Result(sessions,issues);
    }

    private Candidate choose(List<Block> free,Work task,int wanted,ActivityPreferences habits) {
        Candidate best=null;
        LocalDate earliest=null;
        for(int i=0;i<free.size();i++) {
            var slot=free.get(i);
            if(task.due()!=null && slot.start().toLocalDate().isAfter(task.due())) break;
            int available=(int)Duration.between(slot.start(),slot.end()).toMinutes();
            if(available<=0) continue;
            if(earliest==null) earliest=slot.start().toLocalDate();
            int use=Math.min(wanted,available);
            if(!habits.ready()) return new Candidate(i,slot.start(),use,0);
            // Do not defer study indefinitely in pursuit of a habitual weekday.
            long delay=ChronoUnit.DAYS.between(earliest,slot.start().toLocalDate());
            if(delay>2) break;
            for(var start=slot.start();!start.plusMinutes(use).isAfter(slot.end());start=start.plusMinutes(15)) {
                double score=habits.score(task.id(),task.subjectId(),start,use)-0.04*delay;
                if(best==null || score>best.score()+1e-9) best=new Candidate(i,start,use,score);
            }
        }
        return best;
    }

    private ArrayList<Block> freeIntervals(LocalDateTime now,Settings settings,List<Block> occupied) {
        var free=new ArrayList<Block>();
        for(int d=0;d<settings.horizonDays();d++) {
            LocalDate date=now.toLocalDate().plusDays(d);
            for(var a:settings.availability()) if(a.dayOfWeek()==date.getDayOfWeek().getValue()) {
                LocalDateTime start=date.atTime(a.start()),end=date.atTime(a.end());
                if(start.isBefore(now)) start=now;
                if(start.isBefore(end)) addStableIntervals(free,start,end,ZoneId.of(settings.zoneId()));
            }
        }
        free.sort(Comparator.comparing(Block::start));
        for(var busy:occupied) {
            int gap = busy.taskId() == null ? 0 : settings.breakMinutes();
            free = subtract(free, busy.start().minusMinutes(gap), busy.end().plusMinutes(gap));
        }
        return free;
    }

    private ArrayList<Block> subtract(List<Block> free, LocalDateTime start, LocalDateTime end) {
        var next = new ArrayList<Block>();
        for (var slot : free) {
            if (!start.isBefore(slot.end()) || !end.isAfter(slot.start())) { next.add(slot); continue; }
            if (slot.start().isBefore(start)) next.add(new Block(null,null,"",slot.start(),start,"FREE"));
            if (end.isBefore(slot.end())) next.add(new Block(null,null,"",end,slot.end(),"FREE"));
        }
        return next;
    }

    // Never allocate nonexistent or repeated wall-clock minutes at DST transitions.
    // Split at offset changes so local and elapsed session durations agree.
    private void addStableIntervals(List<Block> result,LocalDateTime start,LocalDateTime end,ZoneId zone) {
        LocalDateTime opened=null;
        for(var cursor=start;cursor.isBefore(end);) {
            var next=cursor.plusMinutes(1);
            if(next.isAfter(end)) next=end;
            var a=zone.getRules().getValidOffsets(cursor);
            var b=zone.getRules().getValidOffsets(next);
            boolean valid=a.size()==1 && b.size()==1 && a.getFirst().equals(b.getFirst());
            if(valid && opened==null) opened=cursor;
            if(!valid && opened!=null) { result.add(new Block(null,null,"",opened,cursor,"FREE"));opened=null; }
            cursor=next;
        }
        if(opened!=null) result.add(new Block(null,null,"",opened,end,"FREE"));
    }
}
