/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onebusaway.api.actions.siri;

import org.onebusaway.siri.model.DistanceExtensions;
import org.onebusaway.siri.model.Distances;
import org.onebusaway.siri.model.FramedVehicleJourneyRef;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.OnwardCall;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SiriUtils {

  public static String getIdWithoutAgency(String id) {
    int startIndex = id.indexOf('_') + 1;
    id = id.substring(startIndex);
    return id;
  }

  public static List<OnwardCall> getOnwardCalls(
      List<TripStopTimeBean> stopTimes, long serviceDate,
      double distance, StopBean currentStop) {

    ArrayList<OnwardCall> onwardCalls = new ArrayList<OnwardCall>();

    HashMap<String, Integer> visitNumberForStop = new HashMap<String, Integer>();
    boolean afterStart = false;
    boolean afterStop = currentStop == null;
    int i = 0;
    for (TripStopTimeBean stopTime : stopTimes) {
      StopBean stop = stopTime.getStop();
      int visitNumber = getVisitNumber(visitNumberForStop, stop);
      if (afterStart) {
        i += 1;
        if (afterStop) {
          OnwardCall onwardCall = new OnwardCall();
          onwardCall.StopPointRef = SiriUtils.getIdWithoutAgency(stop.getId());
          onwardCall.StopPointName = stop.getName();
          onwardCall.VisitNumber = visitNumber;
          onwardCall.Extensions = new DistanceExtensions();
          
          onwardCall.Extensions.Distances = new Distances();
          onwardCall.Extensions.Distances.DistanceFromCall = stopTime.getDistanceAlongTrip()
              - distance;
          onwardCall.Extensions.Distances.StopsFromCall = i;

          /*
           * This is not really that useful without being more certain about what
           * trip we're on, so it's commented out 
           * Calendar arrivalTime = new GregorianCalendar(); 
           * long millis = serviceDate + stopTime.getArrivalTime() * 1000;
           * arrivalTime.setTimeInMillis(millis); 
           * onwardCall.AimedArrivalTime = arrivalTime;
           * 
           * Calendar departureTime = new GregorianCalendar(); 
           * millis = serviceDate + stopTime.getDepartureTime() * 1000;
           * departureTime.setTimeInMillis(millis); 
           * onwardCall.AimedDepartureTime = departureTime;
           */
        
           onwardCalls.add(onwardCall);
          }
          if (stop == currentStop) {
            afterStop = true;
          }
      }
      if (stopTime.getDistanceAlongTrip() >= distance) {
        afterStart = true;
      }
    }
    if (onwardCalls.size() == 0) {
      return null;
    }
    return onwardCalls;
  }

  public static int getVisitNumber(HashMap<String, Integer> visitNumberForStop,
      StopBean stop) {
    int visitNumber;
    if (visitNumberForStop.containsKey(stop.getId())) {
      visitNumber = visitNumberForStop.get(stop.getId()) + 1;
    } else {
      visitNumber = 1;
    }
    visitNumberForStop.put(stop.getId(), visitNumber);
    return visitNumber;
  }

  public static MonitoredVehicleJourney getMonitoredVehicleJourney(
      TripDetailsBean trip) {
    TripBean tripBean = trip.getTrip();
    TripStatusBean status = trip.getStatus();

    MonitoredVehicleJourney monitoredVehicleJourney = new MonitoredVehicleJourney();

    monitoredVehicleJourney.CourseOfJourneyRef = getIdWithoutAgency(trip.getTripId());
    RouteBean route = tripBean.getRoute();
    monitoredVehicleJourney.LineRef = getIdWithoutAgency(route.getId());
    monitoredVehicleJourney.DirectionRef = tripBean.getDirectionId();
    monitoredVehicleJourney.PublishedLineName = tripBean.getTripHeadsign();

    monitoredVehicleJourney.FramedVehicleJourneyRef = new FramedVehicleJourneyRef();
    monitoredVehicleJourney.VehicleRef = status.getVehicleId();

    Date serviceDate = new Date(status.getServiceDate());

    monitoredVehicleJourney.FramedVehicleJourneyRef.DataFrameRef = String.format(
        "%1$tY-%1$tm-%1$td", serviceDate);
    monitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef = trip.getTripId();

    List<TripStopTimeBean> stops = trip.getSchedule().getStopTimes();
    monitoredVehicleJourney.OriginRef = getIdWithoutAgency(stops.get(0).getStop().getId());
    StopBean lastStop = stops.get(stops.size() - 1).getStop();
    monitoredVehicleJourney.DestinationRef = getIdWithoutAgency(lastStop.getId());

    return monitoredVehicleJourney;
  }
}
