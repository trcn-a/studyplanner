package ua.studyplanner.googlecalendar;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import ua.studyplanner.googlecalendar.controllers.GoogleCalendarController;
import ua.studyplanner.googlecalendar.config.GoogleCalendarConfig;
import ua.studyplanner.googlecalendar.domain.GoogleBusyEvent;
import ua.studyplanner.googlecalendar.integration.*;
import ua.studyplanner.googlecalendar.service.GoogleCalendarDataService;
import ua.studyplanner.googlecalendar.service.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleCalendarTest extends ua.studyplanner.support.LegacyUserTest {
 private final GoogleCalendarConfig config=new GoogleCalendarConfig("client","secret",Base64.getEncoder().encodeToString(new byte[32]),"http://localhost/callback","http://localhost/");
 @Test void wholeDayUsesCalendarZoneAndExclusiveEnd() {
  var event=GoogleBusyEvent.from("calendar",Map.of("id","a","start",Map.of("date","2026-03-29"),"end",Map.of("date","2026-03-30")),ZoneId.of("Europe/Kyiv")).orElseThrow();
  assertEquals(23,Duration.between(event.start(),event.end()).toHours());
 }
 @Test void freeCancelledAndDeclinedEventsDoNotBlockTime() {
  assertTrue(GoogleBusyEvent.from("c",Map.of("status","cancelled"),ZoneOffset.UTC).isEmpty());
  assertTrue(GoogleBusyEvent.from("c",Map.of("transparency","transparent"),ZoneOffset.UTC).isEmpty());
  assertTrue(GoogleBusyEvent.from("c",Map.of("attendees",List.of(Map.of("self",true,"responseStatus","declined"))),ZoneOffset.UTC).isEmpty());
 }
 @Test void sameEventKeepsIdentityAfterReschedule() {
  var data=new HashMap<String,Object>(Map.of("id","a","start",Map.of("dateTime","2026-10-04T10:00:00+03:00"),"end",Map.of("dateTime","2026-10-04T11:00:00+03:00")));
  var before=GoogleBusyEvent.from("c",data,ZoneOffset.UTC).orElseThrow();
  data.put("start",Map.of("dateTime","2026-10-04T09:00:00+03:00"));
  assertEquals(before.id(),GoogleBusyEvent.from("c",data,ZoneOffset.UTC).orElseThrow().id());
  assertNotEquals(before.id(),GoogleBusyEvent.from("other",data,ZoneOffset.UTC).orElseThrow().id());
 }
 @Test void encryptedTokensSurviveRestartAndRejectTampering() {
  var cipher=new TokenCipher(config);String encrypted=cipher.encrypt("refresh-secret");
  assertFalse(encrypted.contains("refresh-secret"));
  assertEquals("refresh-secret",new TokenCipher(config).decrypt(encrypted));
  byte[] bytes=Base64.getDecoder().decode(encrypted);bytes[15]^=1;
  assertThrows(IllegalStateException.class,()->cipher.decrypt(Base64.getEncoder().encodeToString(bytes)));
 }
 @Test void callbackRequiresBrowserBoundStateAndCannotBeReplayed() throws Exception {
  var service=mock(GoogleCalendarService.class);
  var controller=new GoogleCalendarController(service,config,new GoogleCalendarClient(config));
  var session=new MockHttpSession();String url=controller.connect(session).get("url");
  String state=url.split("&state=")[1].split("&")[0];
  var other=new MockHttpServletRequest();other.setSession(new MockHttpSession());
  controller.callback(state,"code",other);verifyNoInteractions(service);
  var request=new MockHttpServletRequest();request.setSession(session);
  controller.callback(state,"code",request);verify(service).connect(eq("code"),anyString());
  controller.callback(state,"code",request);verifyNoMoreInteractions(service);
 }
 @Test void failedRefreshPreservesStoredEvents() {
  var client=mock(GoogleCalendarClient.class);var repository=mock(GoogleCalendarDataService.class);var store=mock(GoogleCalendarStore.class);
  var cipher=new TokenCipher(config);UUID revision=UUID.randomUUID();
  when(repository.connection()).thenReturn(Optional.of(new GoogleCalendarDataService.Connection(cipher.encrypt("refresh"),revision,null,null)));
  when(client.accessToken("refresh")).thenThrow(new IllegalStateException("private-provider-response"));
  new GoogleCalendarService(config,client,cipher,repository,store).sync();
  verifyNoInteractions(store);verify(repository).error(eq(revision),argThat(s->!s.contains("private-provider-response")));
 }
}
