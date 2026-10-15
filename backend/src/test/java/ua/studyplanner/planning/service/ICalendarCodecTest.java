package ua.studyplanner.planning.service;
import ua.studyplanner.planning.integration.ICalendarCodec;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import ua.studyplanner.planning.dto.Block;
import ua.studyplanner.common.exception.BusinessRuleException;
import static org.junit.jupiter.api.Assertions.*;
class ICalendarCodecTest {
 private final ICalendarCodec codec=new ICalendarCodec();
 private final ZoneId zone=ZoneId.of("Europe/Kyiv");
 @Test void utf8ExportRoundTripsWithFoldingAndEscaping(){
  var block=new Block(UUID.randomUUID(),UUID.randomUUID(),"Завдання, тест; ".repeat(15),LocalDateTime.of(2026,10,5,9,0),LocalDateTime.of(2026,10,5,10,0),"AUTO");
  String file=codec.export(List.of(block),zone);
  for(String line:file.split("\r\n"))assertTrue(line.getBytes(StandardCharsets.UTF_8).length<=75);
  var parsed=codec.parse(file,zone).getFirst();assertEquals(block.title(),parsed.title());assertEquals(block.start(),parsed.start());assertEquals(block.end(),parsed.end());
 }
 @Test void importsUtcTimeZonesAndAllDayExclusiveEnd(){
  var parsed=codec.parse("BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:a\r\nDTSTART:20261005T060000Z\r\nDTEND:20261005T070000Z\r\nEND:VEVENT\r\nBEGIN:VEVENT\r\nUID:b\r\nDTSTART;VALUE=DATE:20261006\r\nEND:VEVENT\r\nEND:VCALENDAR",zone);
  assertEquals(9,parsed.getFirst().start().getHour());assertEquals(LocalDateTime.of(2026,10,7,0,0),parsed.getLast().end());
 }
 @Test void rejectsRecurrenceAndInvalidDatesWithoutPartialImport(){
  assertThrows(BusinessRuleException.class,()->codec.parse("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:a\nRRULE:FREQ=WEEKLY\nEND:VEVENT\nEND:VCALENDAR",zone));
  assertThrows(BusinessRuleException.class,()->codec.parse("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:a\nDTSTART:invalid\nDTEND:invalid\nEND:VEVENT\nEND:VCALENDAR",zone));
 }
}
