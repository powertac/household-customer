/*
 * Copyright 2009-2012 the original author or authors.
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

import org.powertac.householdcustomer.configurations.Gaussian;
import org.powertac.householdcustomer.configurations.VillageConstants;

/**
 * Lights are utilized when the persons inhabiting the house have need for them
 * to light the rooms they are in. So it's a not shifting appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */
public class Lights extends NotShiftingAppliance
{

  /**
   * This variable is used to simulated the luminance levels in the household
   * during the day
   **/
  double luminance;// = new Gaussian(VillageConstants.MID_DAY_QUARTER,
                   // VillageConstants.LUMINANCE_VARIANCE)

  @Override
  public void initialize (String household, Properties conf, Random gen)
  {
    // Filling the base variables
    name = household + " Lights";
    saturation = 1;
    power = (int) (VillageConstants.LIGHTS_POWER_VARIANCE * gen.nextGaussian() + VillageConstants.LIGHTS_POWER_MEAN);
    cycleDuration = VillageConstants.LIGHTS_DURATION_CYCLE;

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

      if (applianceOf.isEmpty(weekday, i) == false) {

        luminance = VillageConstants.LUMINANCE_FACTOR * Gaussian.phi(i, VillageConstants.MID_DAY_QUARTER, VillageConstants.LUMINANCE_VARIANCE);

        // System.out.println("Quarter:" + i + " Luminance: " + luminance);
        if (luminance < gen.nextDouble()) {
          dailyOperation.set(i, true);
          loadVector.set(i, power * applianceOf.tenantsNumber(weekday, i));
        }
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
