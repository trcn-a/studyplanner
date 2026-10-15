package ua.studyplanner.gamification.domain;

import ua.studyplanner.gamification.dto.GamificationView;
import ua.studyplanner.gamification.dto.GamificationView.*;
import java.time.LocalDate;
import java.util.*;

public final class GamificationRules {
 private GamificationRules() {}
 public static int points(boolean subtask) {return subtask?5:10;}
 public static GamificationView summarize(long points,int completed,Collection<LocalDate> activity,LocalDate today) {
  return summarize(points,completed,activity,today,127);
 }
 public static GamificationView summarize(long points,int completed,Collection<LocalDate> activity,LocalDate today,int studyDays) {
  if(studyDays<1||studyDays>127)throw new IllegalArgumentException("At least one study day is required");
  var days=new TreeSet<LocalDate>();
  for(var day:activity) if(day!=null && !day.isAfter(today)) days.add(day);
  int longest=0,run=0;LocalDate previous=null;
  for(var day:days) {if(!isStudyDay(day,studyDays))continue;run=previous!=null && nextStudyDay(previous,studyDays).equals(day)?run+1:1;longest=Math.max(longest,run);previous=day;}
  int current=0;LocalDate cursor=today;
  if(!isStudyDay(cursor,studyDays)||!days.contains(cursor))cursor=previousStudyDay(cursor,studyDays);
  while(days.contains(cursor)) {current++;cursor=previousStudyDay(cursor,studyDays);}
  var week=new ArrayList<Day>();
  for(int i=6;i>=0;i--) {var day=today.minusDays(i);week.add(new Day(day,days.contains(day)));}
  var achievements=List.of(
   badge("first","Перший крок","Заверши перше завдання",completed,1),
   badge("ten","Набираєш темп","Заверши 10 завдань",completed,10),
   badge("fifty","Впевнено вперед","Заверши 50 завдань",completed,50),
   badge("streak3","Три навчальні дні","Завершуй завдання 3 навчальні дні поспіль",longest,3),
   badge("streak7","Навчальний тиждень","Завершуй завдання 7 навчальних днів поспіль",longest,7),
   badge("streak30","Сила звички","Завершуй завдання 30 навчальних днів поспіль",longest,30));
  return new GamificationView(points,points/100+1,points%100,100,completed,current,longest,days.contains(today),week,achievements);
 }
 private static boolean isStudyDay(LocalDate day,int mask){return (mask & (1 << (day.getDayOfWeek().getValue()-1)))!=0;}
 private static LocalDate previousStudyDay(LocalDate day,int mask){do{day=day.minusDays(1);}while(!isStudyDay(day,mask));return day;}
 private static LocalDate nextStudyDay(LocalDate day,int mask){do{day=day.plusDays(1);}while(!isStudyDay(day,mask));return day;}
 private static Achievement badge(String id,String title,String description,int value,int target) {
  return new Achievement(id,title,description,Math.min(value,target),target,value>=target);
 }
}
