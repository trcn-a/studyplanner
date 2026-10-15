package ua.studyplanner.googlecalendar.integration;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import ua.studyplanner.googlecalendar.config.GoogleCalendarConfig;
import ua.studyplanner.googlecalendar.domain.GoogleBusyEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Component
public class GoogleCalendarClient {
 private final GoogleCalendarConfig config;
 private final RestClient http;
 @org.springframework.beans.factory.annotation.Autowired
 public GoogleCalendarClient(GoogleCalendarConfig config) {
  this.config=config;
  var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(10000);factory.setReadTimeout(20000);
  http=RestClient.builder().requestFactory(factory).build();
 }
 public GoogleCalendarClient(GoogleCalendarConfig config,RestClient http) {this.config=config;this.http=http;}
 public static String encode(String text) {return URLEncoder.encode(text,StandardCharsets.UTF_8);}
 public String authorizationUrl(String state,String challenge) {
  return "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&access_type=offline&prompt=consent&scope="+
   encode("https://www.googleapis.com/auth/calendar.readonly")+"&client_id="+encode(config.clientId())+
   "&redirect_uri="+encode(config.redirectUri())+"&state="+encode(state)+"&code_challenge_method=S256&code_challenge="+encode(challenge);
 }
 public Map<String,Object> exchange(String code,String verifier) {
  return token(Map.of("grant_type","authorization_code","code",code,"redirect_uri",config.redirectUri(),"code_verifier",verifier));
 }
 public String accessToken(String refresh) {
  return Objects.requireNonNull((String)token(Map.of("grant_type","refresh_token","refresh_token",refresh)).get("access_token"));
 }
 @SuppressWarnings("unchecked")
 private Map<String,Object> token(Map<String,String> values) {
  var form=new LinkedMultiValueMap<String,String>();values.forEach(form::add);
  form.add("client_id",config.clientId());form.add("client_secret",config.clientSecret());
  return http.post().uri("https://oauth2.googleapis.com/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
 }
 @SuppressWarnings("unchecked")
 private Map<String,Object> get(String url,String access) {
  return http.get().uri(java.net.URI.create(url)).headers(h->h.setBearerAuth(access)).retrieve().body(Map.class);
 }
 @SuppressWarnings("unchecked")
 public List<GoogleBusyEvent> events(String access,Instant from,Instant to) {
  var result=new ArrayList<GoogleBusyEvent>();String page=null;int pages=0;
  do {
   if(++pages>100) throw new IllegalStateException("Calendar limit");
   var list=get("https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250"+(page==null?"":"&pageToken="+encode(page)),access);
   for(var calendar:(List<Map<String,Object>>)list.getOrDefault("items",List.of())) {
    if(!Boolean.TRUE.equals(calendar.get("selected")) && !Boolean.TRUE.equals(calendar.get("primary"))) continue;
    String id=(String)calendar.get("id");ZoneId zone=ZoneId.of((String)calendar.getOrDefault("timeZone","UTC"));
    String eventPage=null;int eventPages=0;
    do {
     if(++eventPages>100) throw new IllegalStateException("Event limit");
     var response=get("https://www.googleapis.com/calendar/v3/calendars/"+encode(id)+"/events?singleEvents=true&showDeleted=false&maxResults=2500&timeMin="+encode(from.toString())+"&timeMax="+encode(to.toString())+(eventPage==null?"":"&pageToken="+encode(eventPage)),access);
     for(var event:(List<Map<String,Object>>)response.getOrDefault("items",List.of())) GoogleBusyEvent.from(id,event,zone).ifPresent(result::add);
     eventPage=(String)response.get("nextPageToken");
    } while(eventPage!=null);
   }
   page=(String)list.get("nextPageToken");
  } while(page!=null);
  return result;
 }
}
