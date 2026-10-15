package ua.studyplanner.gamification;
import org.junit.jupiter.api.Test;
import ua.studyplanner.gamification.domain.GamificationRules;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GamificationRulesTest {
 private final LocalDate today=LocalDate.of(2026,10,7);
 @Test void noActivityStartsAtLevelOne() {
  var result=GamificationRules.summarize(0,0,List.of(),today);
  assertEquals(1,result.level());assertEquals(0,result.currentStreak());assertEquals(7,result.week().size());
  assertTrue(result.achievements().stream().noneMatch(a->a.unlocked()));
 }
 @Test void yesterdayPreservesStreakUntilTodayEndsAndDuplicateDaysDoNotInflateIt() {
  var result=GamificationRules.summarize(30,3,List.of(today.minusDays(1),today.minusDays(2),today.minusDays(2),today.minusDays(3)),today);
  assertEquals(3,result.currentStreak());assertEquals(3,result.longestStreak());assertFalse(result.activeToday());
  assertTrue(result.achievements().stream().anyMatch(a->a.id().equals("streak3")&&a.unlocked()));
 }
 @Test void missedDayResetsCurrentButKeepsRecordAndAchievements() {
  var dates=List.of(today.minusDays(2),today.minusDays(3),today.minusDays(4));
  var result=GamificationRules.summarize(100,10,dates,today);
  assertEquals(0,result.currentStreak());assertEquals(3,result.longestStreak());assertEquals(2,result.level());assertEquals(0,result.levelProgress());
  assertTrue(result.achievements().stream().anyMatch(a->a.id().equals("streak3")&&a.unlocked()));
 }
 @Test void unknownHistoricalDatesAndFutureDatesDoNotCreateActivity() {
  var dates=Arrays.asList(null,today.plusDays(1),today);
  var result=GamificationRules.summarize(105,11,dates,today);
  assertEquals(1,result.currentStreak());assertTrue(result.activeToday());assertEquals(5,result.levelProgress());
 }
 @Test void streakCrossesYearBoundary() {
  var result=GamificationRules.summarize(20,2,List.of(LocalDate.of(2026,12,31),LocalDate.of(2027,1,1)),LocalDate.of(2027,1,1));
  assertEquals(2,result.currentStreak());assertEquals(10,GamificationRules.points(false));assertEquals(5,GamificationRules.points(true));
 }

 @Test void weekendsPreserveStreakThroughMondayAndDoNotAddDays() {
  var friday=LocalDate.of(2026,10,9);
  var days=List.of(friday.minusDays(1),friday,friday.plusDays(1));
  for(int offset=0;offset<=3;offset++)assertEquals(2,GamificationRules.summarize(30,3,days,friday.plusDays(offset),31).currentStreak());
  assertEquals(3,GamificationRules.summarize(40,4,List.of(friday.minusDays(1),friday,friday.plusDays(1),friday.plusDays(3)),friday.plusDays(3),31).currentStreak());
  assertEquals(0,GamificationRules.summarize(30,3,days,friday.plusDays(4),31).currentStreak());
 }
 @Test void missedStudyDayBreaksLongestEvenWithWeekendActivity() {
  var friday=LocalDate.of(2026,10,9);
  var result=GamificationRules.summarize(40,4,List.of(friday.minusDays(2),friday.minusDays(1),friday.plusDays(1),friday.plusDays(3)),friday.plusDays(3),31);
  assertEquals(1,result.currentStreak());assertEquals(2,result.longestStreak());assertEquals(40,result.points());
 }
 @Test void customSingleStudyDayCrossesWeeksAndYearBoundary() {
  var friday=LocalDate.of(2027,1,1);
  var result=GamificationRules.summarize(20,2,List.of(friday.minusWeeks(1),friday),friday.plusDays(6),16);
  assertEquals(2,result.currentStreak());assertEquals(2,result.longestStreak());
  assertEquals(0,GamificationRules.summarize(20,2,List.of(friday.minusWeeks(1),friday),friday.plusDays(8),16).currentStreak());
 }
 @Test void restDayOnlyActivityDoesNotCreateStreak() {
  var sunday=LocalDate.of(2026,10,11);
  var result=GamificationRules.summarize(10,1,List.of(sunday),sunday,31);
  assertEquals(0,result.currentStreak());assertEquals(0,result.longestStreak());assertTrue(result.activeToday());
 }
 @Test void invalidStudyDaysAreRejected() {
  for(int mask:List.of(0,128,-1))assertThrows(IllegalArgumentException.class,()->GamificationRules.summarize(0,0,List.of(),today,mask));
 }
}
