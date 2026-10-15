package ua.studyplanner.gamification.dto;
import java.time.LocalDate;
import java.util.List;

public record GamificationView(long points,long level,long levelProgress,long nextLevelPoints,
        int completedTasks,int currentStreak,int longestStreak,boolean activeToday,
        List<Day> week,List<Achievement> achievements) {
 public record Day(LocalDate date,boolean active) {}
 public record Achievement(String id,String title,String description,int progress,int target,boolean unlocked) {}
}
