package ua.studyplanner.account.dto;
import java.util.UUID;
public record AccountView(UUID id,String email,String name,boolean remindersEnabled,int reminderMinutes,int studyDays) {}
