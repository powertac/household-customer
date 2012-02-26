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

package org.powertac.householdcustomer.appliances;

import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.configurations.VillageConstants;
import org.powertac.householdcustomer.customers.Household;

/**
 * A appliance domain instance represents a single appliance inside a household.
 * There are different kinds of appliances utilized by the persons inhabiting
 * the premises. Some of them are functioning automatically, some are only used
 * when someone is present etc.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Appliance
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static protected Logger log = Logger.getLogger(Appliance.class.getName());

  /**
   * The appliance name. Appliances are named after the type of appliance and
   * the household that contains it.
   */
  protected String name;

  /** The household that the appliance is installed at. **/
  protected Household applianceOf;

  /**
   * This variable shows the possibility (%) that this appliance is contained in
   * a house.
   */
  protected double saturation;

  /**
   * This variable shows the power (in Watts) that are consumed when using this
   * appliance.
   */
  protected int power;

  /** This variable equals the duration of the operation cycle of the appliance. */
  protected int cycleDuration;

  /**
   * This is the occupancy dependence boolean variable. Shows if the appliance
   * needs the presence of an inhabitant in order to start operating.
   */
  protected boolean od;

  /**
   * This is a vector containing the quarters that the appliance should start
   * functioning (before shifting).
   */
  Vector<Vector<Boolean>> operationVector = new Vector<Vector<Boolean>>();

  /**
   * This is a vector containing the quarters that the appliance can start
   * functioning.
   */
  Vector<Vector<Boolean>> possibilityOperationVector = new Vector<Vector<Boolean>>();

  /**
   * This is a vector containing the consumption load of the appliance during
   * the day.
   */
  Vector<Integer> loadVector = new Vector<Integer>();

  /**
   * This is a vector that contains the operation days of each appliance for the
   * competition's duration.
   */
  Vector<Boolean> operationDaysVector = new Vector<Boolean>();

  /**
   * This is a vector containing the final daily operation of the appliance
   * (after shifting due to any cause).
   */
  Vector<Boolean> dailyOperation = new Vector<Boolean>();

  /**
   * This is a vector containing the final weekly operation of the appliance
   * (after shifting due to any cause).
   */
  Vector<Vector<Boolean>> weeklyOperation = new Vector<Vector<Boolean>>();

  /**
   * This is a vector containing the final weekly load of the appliance (after
   * shifting due to any cause).
   */
  Vector<Vector<Integer>> weeklyLoadVector = new Vector<Vector<Integer>>();

  /**
   * This variable contains the amount of times the appliance may work through
   * the week or day.
   */
  int times;

  /** This function returns the power variable of the appliance. */
  public int getPower ()
  {
    return power;
  }

  /** This function returns the household where the appliance is installed. */
  public Household getApplianceOf ()
  {
    return applianceOf;
  }

  /** This function returns the saturation variable of the appliance. */
  public double getSaturation ()
  {
    return saturation;
  }

  /** This function returns the duration variable of the appliance. */
  public int getDuration ()
  {
    return cycleDuration;
  }

  /** This function returns the weekly operation vector of the appliance. */
  public Vector<Vector<Boolean>> getWeeklyOperation ()
  {
    return weeklyOperation;
  }

  /** This function returns the weekly load vector of the appliance. */
  public Vector<Vector<Integer>> getWeeklyLoadVector ()
  {
    return weeklyLoadVector;
  }

  /** This function helps to set the operation vector of the appliance. */
  public void setOperationVector (Vector<Vector<Boolean>> v)
  {
    operationVector = v;
  }

  /** This function sets the household in which the appliance is installed in. */
  public void setApplianceOf (Household house)
  {
    applianceOf = house;
  }

  /**
   * This function is used to create the operation vector of the appliance for
   * the week taking into consideration the times that this appliance has to
   * function.
   * 
   * @param times
   * @param gen
   * @return
   */
  void createOperationVector (int times, Random gen)
  {

  }

  /**
   * This function is used to create the daily possibility operation vector of
   * each appliance for the week taking into consideration the days that this
   * appliance could be able to function.
   * 
   * @param day
   * @return
   */
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {
    return new Vector<Boolean>();
  }

  /**
   * This function is used to create the weekly possibility operation vector of
   * each appliance taking into consideration the times that this appliance
   * could be able to function.
   * 
   * @return
   */
  public void createWeeklyPossibilityOperationVector ()
  {
    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++)
      possibilityOperationVector.add(createDailyPossibilityOperationVector(i));
  }

  /**
   * This is the initialization function. It uses the variable values for the
   * configuration file to create the appliance as it should for this type.
   * 
   * @param household
   * @param conf
   * @param gen
   * @return
   */
  public void initialize (String household, Properties conf, Random gen)
  {

  }

  /**
   * This is a complex function that changes the appliance's function in order
   * to have the most cost effective operation load in a day schedule.
   * 
   * @param gen
   * @param tariff
   * @param now
   * @param day
   * @return
   */
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {
    return new long[VillageConstants.HOURS_OF_DAY];
  }

  /**
   * This is a simple function utilized for the creation of the function Vector
   * that will be used in the shifting procedure.
   * 
   * @param day
   * @return
   */
  boolean[] createShiftingOperationMatrix (int day)
  {

    boolean[] shiftingOperationMatrix = new boolean[VillageConstants.HOURS_OF_DAY];

    for (int i = 0; i < VillageConstants.HOURS_OF_DAY; i++) {
      boolean function = possibilityOperationVector.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR) || possibilityOperationVector.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 1)
          || possibilityOperationVector.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 2) || possibilityOperationVector.get(day).get(i * VillageConstants.QUARTERS_OF_HOUR + 3);
      shiftingOperationMatrix[i] = function;
    }
    return shiftingOperationMatrix;
  }

  /**
   * This function fills out all the quarters of the appliance functions for a
   * single day of the week.
   * 
   * @param gen
   * @return
   */
  public void fillDailyFunction (int times, Random gen)
  {

  }

  /**
   * This function fills out all the days of the appliance functions for each
   * day of the week.
   * 
   * @param gen
   * @return
   */
  public void fillWeeklyFunction (Random gen)
  {

  }

  /**
   * This function creates the weekly operation vector after the procedure of
   * shifting for each day of the week.
   * 
   * @param times
   * @param gen
   * @return
   */
  void createWeeklyOperationVector (int times, Random gen)
  {

  }

  /**
   * This function creates the daily operation vector after the shifting
   * procedure.
   * 
   * @param times
   * @param gen
   * @return
   */
  Vector<Boolean> createDailyOperationVector (int times, Random gen)
  {
    return new Vector<Boolean>();
  }

  /**
   * This is the function utilized to show the information regarding the
   * appliance in question, its variables values etc.
   * 
   * @return
   */
  public void showStatus ()
  {
    // Printing base variables
    log.debug("Name = " + name);
    log.debug("Member Of = " + applianceOf.toString());
    log.debug("Saturation = " + saturation);
    log.debug("Power = " + power);
    log.debug("Cycle Duration = " + cycleDuration);
    log.debug("Occupancy Dependence = " + od);

    // Printing Weekly Function Vector and Load
    log.debug("Weekly Operation Vector and Load = ");
    for (int i = 0; i < VillageConstants.DAYS_OF_COMPETITION; i++) {
      log.debug("Day " + i);
      ListIterator<Boolean> iter = weeklyOperation.get(i).listIterator();
      ListIterator<Integer> iter2 = weeklyLoadVector.get(i).listIterator();
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++)
        log.debug("Quarter " + j + " = " + iter.next() + "   Load = " + iter2.next());
    }
  }

  /** This function fills out the daily function of an appliance for the day. */
  public void weatherDailyFunction (int day, int hour, double temp)
  {
  }

  /**
   * At the end of each week the appliance models refresh their schedule. This
   * way we have a realistic and dynamic model, changing function hours,
   * consuming power and so on.
   * 
   * @param conf
   * @param gen
   * @return
   */
  public void refresh (Random gen)
  {
  }

  /**
   * This is an function to fill the maps utilized by Services in order to keep
   * the vectors of each appliance during the runtime.
   * 
   * @return
   */
  public void setOperationDays ()
  {

    // Add the data values for each day of competition and each quarter of each
    // day.
    for (int i = 0; i < weeklyOperation.size(); i++) {
      boolean function = false;
      for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
        function = function || weeklyOperation.get(i).get(j);
      }
      operationDaysVector.add(function);
    }
  }

  public String toString ()
  {
    return name;
  }
}
