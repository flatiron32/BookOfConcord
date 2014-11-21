package com.tomaw.BoC;

import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

public class View {

  static void header(String name) {
    System.out.println();
    System.out.println("============== " + name + " ==============");
    System.out.println();
  }
  
  static void display(CalendarList feed) {
    if (feed.getItems() != null) {
      for (CalendarListEntry entry : feed.getItems()) {
        System.out.println();
        System.out.println("-----------------------------------------------");
        display(entry);
      }
    }
  }

  static void display(Events feed) {
    if (feed.getItems() != null) {
      for (Event entry : feed.getItems()) {
        System.out.println();
        System.out.println("-----------------------------------------------");
        display(entry);
      }
    }
  }

  static void display(CalendarListEntry entry) {
    System.out.println("ID: " + entry.getId());
    System.out.println("Summary: " + entry.getSummary());
    System.out.println("Summary: " + entry.getDescription());
    if (entry.getDescription() != null) {
      System.out.println("Description: " + entry.getDescription());
    }
  }
  
  static void display(Event event) {
    System.out.println(event.getSummary());
    System.out.println(event.getDescription());
    if (event.getStart() != null) {
      System.out.println("Start Time: " + event.getStart());
    }
    if (event.getEnd() != null) {
      System.out.println("End Time: " + event.getEnd());
    }
  }
}
