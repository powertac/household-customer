/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.householdcustomer.persons;

import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.powertac.householdcustomer.configurations.VillageConstants;
import org.powertac.householdcustomer.enumerations.Status;

/**
 * This is the instance of the person type that works. In addition to the simple
 * persons they are working certain hours a day and they have less time for
 * leisure activities.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 **/
public class WorkingPerson extends Person
{

  /** This variable describes the duration of the work procedure. **/
  int workingDuration = 0;

  /** This variables shows how many days are working vacation for this person. **/
  int vacationDuration = 0;

  /** The time of the day that the person begins to work. **/
  int workingStartHour = 0;

  /**
   * This function fills out the working days' vector of the person by choosing
   * randomly days of the week, while the amount of days is different for each
   * person type.
   * 
   * @param days
   * @param gen
   * @return
   */
  Vector<Integer> createWorkingDaysVector (int days, Random gen)
  {
    // Creating an auxiliary variables
    Vector<Integer> v = new Vector<Integer>(days);

    if (days < VillageConstants.WEEKDAYS) {
      for (int i = 0; i < days; i++) {
        int x = (gen.nextInt(1) * (VillageConstants.WEEKDAYS - 1)) + 1;
        ListIterator<Integer> iter = v.listIterator();
        while (iter.hasNext()) {
          int temp = (int) iter.next();
          if (x == temp) {
            x = x + 1;
            iter = v.listIterator();
          }
        }
        v.add(x);
      }
      java.util.Collections.sort(v);
      return v;
    } else {
      v.add(VillageConstants.MONDAY);
      v.add(VillageConstants.TUESDAY);
      v.add(VillageConstants.WEDNESDAY);
      v.add(VillageConstants.THURSDAY);
      v.add(VillageConstants.FRIDAY);
      if (days == VillageConstants.WEEKDAYS) {
      } else {
        if (days == VillageConstants.DAYS_OF_WEEK) {
          v.add(VillageConstants.SATURDAY);
          v.add(VillageConstants.SUNDAY);
        } else {
          if (gen.nextFloat() > 0.5) {
            v.add(VillageConstants.SATURDAY);
          } else {
            v.add(VillageConstants.SUNDAY);
          }
        }
      }
      java.util.Collections.sort(v);
      return v;
    }
  }

  /**
   * This function fills out the work vacation days' vector of the person by
   * choosing randomly days of the year that the person chooses as vacations. He
   * may choose to go on vacation for short periods, but the summary of the days
   * must be in bounds.
   * 
   * @param duration
   * @param gen
   * @return
   */
  Vector<Integer> createVacationVector (int duration, Random gen)
  {

    // Create auxiliary variables
    Vector<Integer> v = new Vector<Integer>(duration);
    int counter = duration;
    int counter2 = 0;
    while (counter > 0) {
      int x = (int) gen.nextInt(VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP - 1) + 1;
      counter2 = 1 + (int) (gen.nextInt(counter));
      while (counter2 > 0) {
        v.add(x);
        counter = counter - 1;
        counter2 = counter2 - 1;
        x = x + 1;
      }
    }
    java.util.Collections.sort(v);
    return v;
  }

  /**
   * This function chooses randomly the number of the working days of a person
   * The percentages used where taken from a thesis on the subject, based on
   * demographic data.
   * 
   * @param conf
   * @param gen
   * @return
   */
  int workingDaysRandomizer (Properties conf, Random gen)
  {
    int returnValue;
    int twoDays = Integer.parseInt(conf.getProperty("TwoDays"));
    int threeDays = Integer.parseInt(conf.getProperty("ThreeDays"));
    int fourDays = Integer.parseInt(conf.getProperty("FourDays"));
    int fiveDays = Integer.parseInt(conf.getProperty("FiveDays"));
    int sixDays = Integer.parseInt(conf.getProperty("SixDays"));
    int sevenDays = Integer.parseInt(conf.getProperty("SevenDays"));

    int x = (int) gen.nextInt(VillageConstants.PERCENTAGE);
    if (x < fiveDays) {
      returnValue = VillageConstants.FIVE_WORKING_DAYS;
    } else {
      if (x >= fiveDays & x < (fiveDays + sixDays)) {
        returnValue = VillageConstants.SIX_WORKING_DAYS;
      } else {
        if (x >= (fiveDays + sixDays) & x < (fiveDays + sixDays + fourDays)) {
          returnValue = VillageConstants.FOUR_WORKING_DAYS;
        } else {
          if (x >= (fiveDays + sixDays + fourDays) & x < (fiveDays + sixDays + fourDays + threeDays)) {
            returnValue = VillageConstants.THREE_WORKING_DAYS;
          } else {
            if (x >= (fiveDays + sixDays + fourDays + threeDays) & x < (fiveDays + sixDays + fourDays + threeDays + twoDays)) {
              returnValue = VillageConstants.TWO_WORKING_DAYS;
            } else {
              if (x >= (fiveDays + sixDays + fourDays + threeDays + twoDays) & x < (fiveDays + sixDays + fourDays + threeDays + twoDays + sevenDays)) {
                returnValue = VillageConstants.SEVEN_WORKING_DAYS;
              } else {
                returnValue = VillageConstants.ONE_WORKING_DAY;
              }
            }
          }
        }
      }
    }
    return returnValue;

  }

  @Override
  public void showInfo ()
  {
    // Printing the base variable
    log.debug("Name = " + name);
    log.debug("Member Of = " + memberOf.toString());

    // Printing Sickness variables
    log.debug("Sickness Days = ");
    ListIterator<Integer> iter = sicknessVector.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());

    // Printing Leisure variables
    log.debug("Leisure Days of Week = ");
    iter = leisureVector.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());
    log.debug("Leisure Duration = " + leisureDuration);

    // Printing working variables
    log.debug("Working Days = ");
    iter = workingDays.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());
    log.debug("Working Duration = " + workingDuration);
    log.debug("Working Starting Hour = " + workingStartHour);

    // Printing vacation variables
    log.debug("Vacation Duration = " + vacationDuration);
    log.debug("Vacation Days = ");
    iter = vacationVector.listIterator();
    while (iter.hasNext())
      log.debug(iter.next());
    log.debug("Public Vacation of Year = ");
    iter = publicVacationVector.listIterator();

    while (iter.hasNext())
      log.debug(iter.next());

    // Printing Weekly Schedule
    log.debug("Weekly Routine : ");
    ListIterator<Status> iter2 = weeklyRoutine.get(0).listIterator();

    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION + VillageConstants.DAYS_OF_BOOTSTRAP; i++) {
      log.debug("Day " + i);
      iter2 = weeklyRoutine.get(i).listIterator();
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter : " + (j + 1) + " Status : " + iter2.next());
    }
  }

}
