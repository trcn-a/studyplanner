package ua.studyplanner.googlecalendar.domain;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public record GoogleBusyEvent(UUID id,String title,Instant start,Instant end) {
 @SuppressWarnings("unchecked")
 public static Optional<GoogleBusyEvent> from(String calendarId, Map<String,Object> event, ZoneId calendarZone) {
  if("cancelled".equals(event.get("status")) || "transparent".equals(event.get("transparency"))) return Optional.empty();
  for(var attendee:(List<Map<String,Object>>)event.getOrDefault("attendees",List.of()))
   if(Boolean.TRUE.equals(attendee.get("self")) && "declined".equals(attendee.get("responseStatus"))) return Optional.empty();
  Instant start=time((Map<String,Object>)event.get("start"),calendarZone);
  Instant end=time((Map<String,Object>)event.get("end"),calendarZone);
  if(!end.isAfter(start)) throw new IllegalArgumentException("Invalid Google event duration");
  String id=Objects.requireNonNull((String)event.get("id"));
  String title=(String)event.getOrDefault("summary","Зайнятий час · Google Calendar");
  return Optional.of(new GoogleBusyEvent(UUID.nameUUIDFromBytes((calendarId+"\n"+id).getBytes(StandardCharsets.UTF_8)),title.substring(0,Math.min(500,title.length())),start,end));
 }
 private static Instant time(Map<String,Object> value,ZoneId zone) {
  if(value.get("dateTime")!=null) return OffsetDateTime.parse((String)value.get("dateTime")).toInstant();
  return LocalDate.parse((String)value.get("date")).atStartOfDay(zone).toInstant();
 }
}
