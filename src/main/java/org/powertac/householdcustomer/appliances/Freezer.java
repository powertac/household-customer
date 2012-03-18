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

import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.configurations.VillageConstants;

/**
 * Freezer is the utilized in combination with the refrigerator in the
 * household. This appliance can automatically change the freezing cycles in
 * order to save energy, without tenants manipulation. So this is a fully
 * shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Freezer extends FullyShiftingAppliance
{

  public void fillWeeklyFunction (Random gen)
  {
    for (int i = 0; i < VillageConstants.DAYS_OF_WEEK; i++)
      fillDailyOperation(i, gen);
  }

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {

    // Filling the base variables
    name = household + " Freezer";
    saturation = Double.parseDouble(conf.getProperty("FreezerSaturation"));
    power = (int) (VillageConstants.FREEZER_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.FREEZER_POWER_MEAN);
    cycleDuration = VillageConstants.FREEZER_DURATION_CYCLE;

  }

  @Override
  Vector<Boolean> createDailyPossibilityOperationVector (int day)
  {

    Vector<Boolean> possibilityDailyOperation = new Vector<Boolean>();

    // Freezer can work anytime
    for (int j = 0; j < VillageConstants.QUARTERS_OF_DAY; j++) {
      possibilityDailyOperation.add(true);
    }

    return possibilityDailyOperation;
  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {
    // Initializing Variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();
    int k = gen.nextInt(cycleDuration);

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {
      if (i % cycleDuration == k) {
        loadVector.add(power);
        dailyOperation.add(true);
      } else {
        loadVector.add(0);
        dailyOperation.add(false);
      }
    }
    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public long[] dailyShifting (Tariff tariff, Instant now, int day, Random gen)
  {

    long[] newControllableLoad = new long[VillageConstants.HOURS_OF_DAY];

    Instant now2 = now;

    // Daily operation is seperated in shifting periods
    for (int i = 0; i < VillageConstants.REFRIGERATOR_SHIFTING_PERIODS; i++) {
      double minvalue = Double.NEGATIVE_INFINITY;
      int minindex = 0;

      // For each shifting period we search the best value
      for (int j = 0; j < VillageConstants.REFRIGERATOR_SHIFTING_INTERVAL; j++) {
        if ((minvalue < tariff.getUsageCharge(now2, 1, 0)) || (minvalue == tariff.getUsageCharge(now2, 1, 0) && gen.nextFloat() > VillageConstants.SAME)) {
          minvalue = tariff.getUsageCharge(now2, 1, 0);
          minindex = j;
        }
        now2 = new Instant(now2.getMillis() + TimeService.HOUR);
      }
      newControllableLoad[VillageConstants.REFRIGERATOR_SHIFTING_INTERVAL * i + minindex] = VillageConstants.QUARTERS_OF_HOUR * power;
    }
    return newControllableLoad;
  }

  @Override
  public void refresh (Random gen)
  {
    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
