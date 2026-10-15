package ua.studyplanner.googlecalendar;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import ua.studyplanner.googlecalendar.config.GoogleCalendarConfig;
import ua.studyplanner.googlecalendar.integration.GoogleCalendarClient;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleCalendarClientTest {
 @Test void allPagesAreLoadedAndRecurringInstancesAreRequested() {
  var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
  var client=new GoogleCalendarClient(new GoogleCalendarConfig("id","secret","","",""),builder.build());
  server.expect(requestTo("https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250"))
   .andExpect(header("Authorization","Bearer access"))
   .andRespond(withSuccess("{\"items\":[{\"id\":\"a@google.com\",\"primary\":true,\"timeZone\":\"UTC\"}],\"nextPageToken\":\"cal2\"}",MediaType.APPLICATION_JSON));
  String events="https://www.googleapis.com/calendar/v3/calendars/a%40google.com/events?singleEvents=true&showDeleted=false&maxResults=2500&timeMin=2026-10-01T00%3A00%3A00Z&timeMax=2026-10-03T00%3A00%3A00Z";
  server.expect(requestTo(events)).andRespond(withSuccess("{\"items\":[],\"nextPageToken\":\"next\"}",MediaType.APPLICATION_JSON));
  server.expect(requestTo(events+"&pageToken=next")).andRespond(withSuccess("{\"items\":[{\"id\":\"recurring-instance\",\"start\":{\"date\":\"2026-10-02\"},\"end\":{\"date\":\"2026-10-03\"}}]}",MediaType.APPLICATION_JSON));
  server.expect(requestTo("https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250&pageToken=cal2"))
   .andRespond(withSuccess("{\"items\":[{\"id\":\"hidden\",\"selected\":false}]}",MediaType.APPLICATION_JSON));
  var result=client.events("access",Instant.parse("2026-10-01T00:00:00Z"),Instant.parse("2026-10-03T00:00:00Z"));
  assertEquals(1,result.size());server.verify();
 }
}
