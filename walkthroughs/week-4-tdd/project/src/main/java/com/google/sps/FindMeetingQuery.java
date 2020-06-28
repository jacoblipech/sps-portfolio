// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class FindMeetingQuery {

  Collection<TimeRange> possibleMeetingTime = new ArrayList<>();

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // find the list of attendees and required duration
    Collection<String> reqAttendees = request.getAttendees();
    int reqDuration = (int)request.getDuration();

    // corner case: no attendees in the request
    if (reqAttendees.isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // corner case: requested duration more than the day duration
    if (reqDuration > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    // check if each event's attendees clash with the request attendees sorted by starting time
    List<TimeRange> eventTimes = events.stream()
                                      .filter(event -> isAffectedEvent(event.getAttendees(), reqAttendees))
                                      .map(event -> event.getWhen())
                                      .sorted(TimeRange.ORDER_BY_START)
                                      .collect(Collectors.toList());

    // no event affecting required attendees
    if (eventTimes.isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    int possibleStartTime = TimeRange.START_OF_DAY;

    // check first event starting time
    int eventStartTime = eventTimes.get(0).start();
    addPossibleTime(possibleStartTime, eventStartTime, reqDuration);
    // sets the end time of first event to the start time of the possible slot
    possibleStartTime = eventTimes.get(0).end();

    for (int i = 1; i < eventTimes.size(); i++) {
      int nextEventStart = eventTimes.get(i).start();
      int prevEventEnd = eventTimes.get(i-1).end();
      int currEventEnd = eventTimes.get(i).end();

      addPossibleTime(possibleStartTime, nextEventStart, reqDuration);
      // ensures that the next free time is the based on the latest event ending time
      possibleStartTime = Math.max(prevEventEnd, currEventEnd);
    }

    addPossibleTime(possibleStartTime, TimeRange.END_OF_DAY + 1, reqDuration);
    return possibleMeetingTime;
  }

  /**
    * Adds a possible meeting time to the query
    */
  private void addPossibleTime(int start, int end, long reqDuration) {
    if (end - start >= reqDuration) {
      possibleMeetingTime.add(TimeRange.fromStartEnd(start, end, false));
    }
  }

  /**
   * Checks to ensure that events are added only when attendee overlaps.
   */
  private Boolean isAffectedEvent(Set<String> eventAttendees, Collection<String> reqAttendees) {
    for (String attendee : reqAttendees) {
      if (eventAttendees.contains(attendee)) {
        return true;
      }
    }
    return false;
  }
}
