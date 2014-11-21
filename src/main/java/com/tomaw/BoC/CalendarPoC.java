package com.tomaw.BoC;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

public class CalendarPoC {

  private static final File DATA_STORE_DIR = 
      new java.io.File(System.getProperty("user.home"), ".store/calendar_sample");
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String APPLICATION_NAME = "";

  private static NetHttpTransport httpTransport;
  private static FileDataStoreFactory dataStoreFactory;
  private static Calendar client;
  private static final String calId = "hoc1649lbh3vdvufkumjqatg6s@group.calendar.google.com";

  /** Authorizes the installed application to access user's protected data. */
  private static Credential authorize() throws Exception {
    // load client secrets
    InputStreamReader reader = new InputStreamReader(CalendarPoC.class
        .getResourceAsStream("/client_secrets.json"));
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
    
    // set up authorization code flow
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, clientSecrets,
        Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory).
        build();
    
    // authorize
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  public static void main(String[] args) {
    try (Scanner reader = new Scanner(System.in)) {
      // initialize the transport
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();

      // initialize the data store factory
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

      // authorization
      Credential credential = authorize();

      // set up global Calendar instance
      client = new com.google.api.services.calendar.Calendar.Builder(
          httpTransport, JSON_FACTORY, credential).setApplicationName(
          APPLICATION_NAME).build();
      
      while (true) {
        Event latest = getLatestEvent();
        View.header("Foremost");
        View.display(latest);

        View.header("Next");
        DateTime nextDateTime = nextDateTime(latest);
        String nextSummary = nextSummary(latest, nextDateTime);

        System.out.println("Enter previous summary:");
        System.out.println(latest.getDescription());
        putDescInClipboard(latest);

        System.out.println("Enter readings for " + nextSummary);
        String nextDescription = reader.nextLine();

        if (nextDescription.length() == 0) {
          View.header("Ending");
          System.exit(1);
        }

        Event nextEvent = addNextEvent(nextSummary, nextDescription, nextDateTime);
        View.header("Added");
        View.display(nextEvent);

      }
    } catch (Throwable t) {
      System.err.println(t);
    } 
    System.exit(1);
  }

  private static void putDescInClipboard(Event latest) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(latest.getDescription()), null);
  }

  private static String nextSummary(Event latest, DateTime nextDateTime) {
    String[] summSplit = latest.getSummary().split(" ");
    int nextWeek = Integer.valueOf(summSplit[summSplit.length - 1]);
    if (nextDateTime.dayOfWeek().get() == DateTimeConstants.MONDAY) {
      nextWeek += 1;
    }
    String nextSummary = "Book of Concord Readings for " + 
        getDayOfWeek(nextDateTime.getDayOfWeek()) + " of Week " + nextWeek;
    return nextSummary;
  }

  private static DateTime nextDateTime(Event latest) {
    com.google.api.client.util.DateTime date = latest.getStart().getDate();
    DateTimeZone timeZone = DateTimeZone.forOffsetHours(date.getTimeZoneShift());
    DateTime lastDateTime = new DateTime(date.getValue(), timeZone);

    DateTime nextDateTime;
    if (lastDateTime.dayOfWeek().get() == DateTimeConstants.FRIDAY) {
      nextDateTime = lastDateTime.plusDays(3);
    } else {
      nextDateTime = lastDateTime.plusDays(1);
    }
    return nextDateTime;
  }

  private static Event addNextEvent(String summary, String description, DateTime startTime) 
      throws IOException {
    EventDateTime nextEventStart = new EventDateTime();
    nextEventStart.setDate(new com.google.api.client.util.DateTime(startTime.toString("yyyy-MM-dd")));
    EventDateTime nextEventEnd = new EventDateTime();
    nextEventEnd.setDate(new com.google.api.client.util.DateTime(startTime.plusDays(1).toString("yyyy-MM-dd")));

    Event next = new Event();
    next.setSummary(summary);
    next.setDescription(description);
    next.setStart(nextEventStart);
    next.setEnd(nextEventEnd);
    client.events().insert(calId, next).execute();
    
    return next;
  }

  private static Event getLatestEvent() throws IOException {
    List<Event> events = getAllEvents();
    if (!events.isEmpty()) {
      System.out.println("Number of events is " + events.size());

      Collections.sort(events, new Comparator<Event>() {

        @Override
        public int compare(Event o1, Event o2) {
          Long start1 = o1.getStart().getDate().getValue();
          Long start2 = o2.getStart().getDate().getValue();
          return start2.compareTo(start1);
        }
      });
      
      return events.get(0);
    }
    return null;
  }
  
  private static List<Event> getAllEvents() throws IOException {
    List<Event> events = new ArrayList<>();
    String nextPageToken = null;
    do {
      System.out.println("Loading page " + nextPageToken);
      Events feed = client.events().list(calId).setPageToken(nextPageToken).execute();
      events.addAll(feed.getItems());
      nextPageToken = feed.getNextPageToken();
    } while (nextPageToken != null);
    
    return events;
  }

  private static String getDayOfWeek(int dayOfWeek) {
    switch (dayOfWeek) {
    case 0:
      return "Sunday";
    case 1:
      return "Monday";
    case 2:
      return "Tuesday";
    case 3:
      return "Wednesday";
    case 4:
      return "Thursday";
    case 5:
      return "Friday";
    case 6:
      return "Saturday";
    default:
      return "Firtaterdy";
    }
  }
}
