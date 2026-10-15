package ua.studyplanner.planning.integration;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.charset.StandardCharsets;
import ua.studyplanner.common.exception.BusinessRuleException;
import ua.studyplanner.planning.dto.Block;

/** Deliberately bounded iCalendar exchange: standalone events, no silent recurrence loss. */
public final class ICalendarCodec {
 public record Event(String uid,String title,LocalDateTime start,LocalDateTime end,boolean remove) {}
 private static final DateTimeFormatter DATE=DateTimeFormatter.BASIC_ISO_DATE;
 private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
 private record Property(String key,String value){}
 public List<Event> parse(String content,ZoneId zone) {
  if(!content.contains("BEGIN:VCALENDAR") || !content.contains("END:VCALENDAR")) fail("Файл не є календарем .ics");
  String unfolded=content.replace("\r\n","\n").replaceAll("\n[ \\t]", "");
  var result=new ArrayList<Event>(); Map<String,Property> event=null;int nested=0;
  for(String line:unfolded.split("\n")) {
   if(line.equals("BEGIN:VEVENT")){if(event!=null)fail("Пошкоджена структура подій");event=new HashMap<>();nested=0;continue;}
   if(event==null)continue;
   if(line.equals("END:VEVENT")) {
    if(event.containsKey("RRULE") || event.containsKey("RDATE") || event.containsKey("EXDATE") || event.containsKey("RECURRENCE-ID")) fail("Повторювані події поки не підтримуються. Імпортуйте окремі події без повторення");
    String uid=value(event,"UID","");if(uid.isBlank() || uid.length()>500)fail("Подія має некоректний UID");
    boolean remove=value(event,"STATUS","").equals("CANCELLED") || value(event,"TRANSP","").equals("TRANSPARENT");
    if(remove)result.add(new Event(uid,"",null,null,true));
    else {
     var start=event.get("DTSTART");if(start==null)fail("Подія не має часу початку");
     LocalDateTime from=parseDate(start,zone),to;
     if(event.containsKey("DTEND"))to=parseDate(event.get("DTEND"),zone);
     else if(event.containsKey("DURATION")) {
      try{to=from.plus(Duration.parse(value(event,"DURATION","")));}catch(Exception e){throw new BusinessRuleException("Непідтримувана тривалість події");}
     } else if(start.value().length()==8)to=from.plusDays(1);
     else {fail("Подія не має часу завершення");return List.of();}
     if(!to.isAfter(from))fail("Кінець події має бути пізніше початку");
     String title=unescape(value(event,"SUMMARY","Зайнято"));
     if(title.length()>500)title=title.substring(0,500);
     result.add(new Event(uid,title,from,to,false));
    }
    if(result.size()>2000)fail("Імпортуйте не більше 2000 подій за один раз");event=null;continue;
   }
   if(line.startsWith("BEGIN:")){nested++;continue;}if(line.startsWith("END:")){nested--;continue;}if(nested>0)continue;
   int colon=line.indexOf(':');if(colon<0)continue;
   String key=line.substring(0,colon);String name=key.split(";",2)[0].toUpperCase(Locale.ROOT);
   event.put(name,new Property(key,line.substring(colon+1)));
  }
  if(event!=null)fail("Незавершена подія у файлі");
  if(result.isEmpty())fail("У файлі немає подій");return result;
 }
 private LocalDateTime parseDate(Property p,ZoneId target) {
  try {
   if(p.value().length()==8)return LocalDate.parse(p.value(),DATE).atStartOfDay();
   if(p.value().endsWith("Z"))return LocalDateTime.parse(p.value().substring(0,p.value().length()-1),TIME).atZone(ZoneOffset.UTC).withZoneSameInstant(target).toLocalDateTime();
   ZoneId source=target;
   for(String param:p.key().split(";"))if(param.startsWith("TZID="))source=ZoneId.of(param.substring(5).replace("\"",""));
   var local=LocalDateTime.parse(p.value(),TIME);
   if(source.getRules().getValidOffsets(local).isEmpty())fail("Подія потрапляє в пропущену годину переходу часу");
   return local.atZone(source).withZoneSameInstant(target).toLocalDateTime();
  }catch(DateTimeException e){throw new BusinessRuleException("Непідтримуваний формат дати або часовий пояс у .ics");}
 }
 private static String value(Map<String,Property> e,String k,String fallback){return e.containsKey(k)?e.get(k).value():fallback;}
 private static String unescape(String s){return s.replace("\\n","\n").replace("\\N","\n").replace("\\,",",").replace("\\;",";").replace("\\\\","\\");}
 private static String escape(String s){return s.replace("\\","\\\\").replace("\r","").replace("\n","\\n").replace(";","\\;").replace(",","\\,");}
 private static void fail(String message){throw new BusinessRuleException(message);}
 public String export(List<Block> blocks,ZoneId zone) {
  var lines=new ArrayList<String>(List.of("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//StudyPlanner//Study Schedule//UK","CALSCALE:GREGORIAN"));
  String stamp=TIME.format(LocalDateTime.now(ZoneOffset.UTC))+"Z";
  for(var b:blocks){
   lines.add("BEGIN:VEVENT");lines.add("UID:"+b.id()+"@studyplanner");lines.add("DTSTAMP:"+stamp);
   lines.add("DTSTART:"+TIME.format(b.start().atZone(zone).withZoneSameInstant(ZoneOffset.UTC))+"Z");
   lines.add("DTEND:"+TIME.format(b.end().atZone(zone).withZoneSameInstant(ZoneOffset.UTC))+"Z");
   lines.add("SUMMARY:"+escape(b.title()));lines.add("END:VEVENT");
  }
  lines.add("END:VCALENDAR");
  var out=new StringBuilder();
  for(String line:lines){int bytes=0;for(int c:line.codePoints().toArray()){String ch=new String(Character.toChars(c));int length=ch.getBytes(StandardCharsets.UTF_8).length;if(bytes+length>75){out.append("\r\n ");bytes=1;}out.append(ch);bytes+=length;}out.append("\r\n");}
  return out.toString();
 }
}
