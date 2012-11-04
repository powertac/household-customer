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

import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * The Other appliances contain several type of appliances that cannot be in any
 * other category of appliances such as air condition or small heaters and so
 * on. They works only when someone is at home. So it's a not shifting
 * appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Others extends NotShiftingAppliance
{

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Others";
    saturation = 1;
    power = (int) (VillageConstants.OTHERS_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.OTHERS_POWER_MEAN);
    cycleDuration = VillageConstants.OTHERS_DURATION_CYCLE;
    times = Integer.parseInt(conf.getProperty("OthersDailyTimes")) + applianceOf.getMembers().size();

  }

  @Override
  public void fillDailyOperation (int weekday, Random gen)
  {
    // Initializing and Creating auxiliary variables
    loadVector = new Vector<Integer>();
    dailyOperation = new Vector<Boolean>();

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {

      dailyOperation.add(false);
      loadVector.add(0);

    }

    Vector<Integer> temp = new Vector<Integer>();

    for (int i = 0; i < VillageConstants.QUARTERS_OF_DAY; i++) {

      int count = applianceOf.tenantsNumber(weekday, i);
      for (int j = 0; j < count; j++) {
        temp.add(i);
      }

    }

    if (temp.size() > 0) {
      for (int i = 0; i < times; i++) {
        int rand = gen.nextInt(temp.size());
        int quarter = temp.get(rand);

        dailyOperation.set(quarter, true);
        loadVector.set(quarter, (loadVector.get(quarter) + power));
        temp.remove(rand);
        if (temp.size() == 0)
          break;
      }
    }

    weeklyLoadVector.add(loadVector);
    weeklyOperation.add(dailyOperation);
  }

  @Override
  public void refresh (Random gen)
  {

    fillWeeklyOperation(gen);
    createWeeklyPossibilityOperationVector();
  }

}
