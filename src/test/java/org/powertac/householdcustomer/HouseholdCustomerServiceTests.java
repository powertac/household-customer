/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.householdcustomer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.householdcustomer.customers.Village;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Antonios Chrysopoulos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-config.xml" })
@DirtiesContext
public class HouseholdCustomerServiceTests
{

  @Autowired
  private TimeService timeService;

  @Autowired
  private Accounting mockAccounting;

  @Autowired
  private TariffMarket mockTariffMarket;

  @Autowired
  private HouseholdCustomerService householdCustomerService;

  @Autowired
  private HouseholdCustomerInitializationService householdCustomerInitializationService;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private PluginConfigRepo pluginConfigRepo;

  private Instant exp;
  private Broker broker1;
  private Broker broker2;
  private Instant now;
  private TariffSpecification defaultTariffSpec;
  private Tariff defaultTariff;
  private Competition comp;
  private List<Object[]> accountingArgs;

  @Before
  public void setUp ()
  {
    customerRepo.recycle();
    brokerRepo.recycle();
    tariffRepo.recycle();
    tariffSubscriptionRepo.recycle();
    pluginConfigRepo.recycle();
    randomSeedRepo.recycle();
    timeslotRepo.recycle();
    weatherReportRepo.recycle();
    reset(mockTariffMarket);
    reset(mockAccounting);

    // create a Competition, needed for initialization
    comp = Competition.newInstance("household-customer-test");

    broker1 = new Broker("Joe");
    broker2 = new Broker("Anna");

    now = new DateTime(2011, 1, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(now);
    timeService.setBase(now.getMillis());
    exp = now.plus(TimeService.WEEK * 10);

    defaultTariffSpec = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(exp).withMinDuration(TimeService.WEEK * 8).addRate(new Rate().withValue(-0.222));

    defaultTariff = new Tariff(defaultTariffSpec);
    defaultTariff.init();

    when(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION)).thenReturn(defaultTariff);

    accountingArgs = new ArrayList<Object[]>();

    // mock the AccountingService, capture args
    doAnswer(new Answer()
    {
      public Object answer (InvocationOnMock invocation)
      {
        Object[] args = invocation.getArguments();
        accountingArgs.add(args);
        return null;
      }
    }).when(mockAccounting).addTariffTransaction(isA(TariffTransaction.Type.class), isA(Tariff.class), isA(CustomerInfo.class), anyInt(), anyDouble(), anyDouble());

  }

  public void initializeService ()
  {
    householdCustomerInitializationService.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("HouseholdCustomer");
    config.getConfiguration().put("configFile", "Household.properties");
    List<String> inits = new ArrayList<String>();
    inits.add("DefaultBroker");
    householdCustomerInitializationService.initialize(comp, inits);
  }

  @Test
  public void testServiceInitialization ()
  {
    initializeService();
    assertEquals("Two Consumers Created", 2, householdCustomerService.getVillageList().size());
    for (Village customer : householdCustomerService.getVillageList()) {

      // capture subscription method args
      ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
      ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
      ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

      // System.out.println(tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff().toString());
      assertEquals("one subscription for our customer", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());
      assertEquals("customer on DefaultTariff", mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff());
    }
  }

  @Test
  public void testPowerConsumption ()
  {
    initializeService();

    for (Village customer : householdCustomerService.getVillageList()) {

      // capture subscription method args
      ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
      ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
      ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

    }

    timeService.setCurrentTime(now.plus(TimeService.HOUR));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);
    for (Village customer : householdCustomerService.getVillageList()) {
      assertFalse("Household consumed power", tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()) == null
          || tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTotalUsage() == 0);
    }

    assertEquals("Tariff Transactions Created", 5 * householdCustomerService.getVillageList().size(), accountingArgs.size());

  }

  @Test
  public void changeSubscription ()
  {
    initializeService();

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();

    assertEquals("Two Tariffs", 2, tariffRepo.findAllTariffs().size());
    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (Village customer : householdCustomerService.getVillageList()) {
      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

      // Changing from default to another tariff
      TariffSubscription sub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
      sub.subscribe(customer.getCustomerInfo().getPopulation());

      assertEquals("Two Subscriptions Active", 2, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);

      // Changing from default to another tariff
      TariffSubscription sub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
      sub.subscribe(customer.getCustomerInfo().getPopulation());

      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
      when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tariffRepo.findAllTariffs());

      // System.out.println("Subscriptions: " +
      // tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).toString());

      customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));
      assertFalse("Changed from default tariff", tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      // System.out.println("Subscriptions: " +
      // tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).toString());

      // Changing back from the new tariff to the default one in order to check
      // every changeSubscription Method
      Tariff lastTariff = sub.getTariff();

      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      customer.changeSubscription(lastTariff, mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());

      assertTrue("Changed to default tariff", tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).get(0).getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      sub.subscribe(customer.getHouses("SS").size());

      // Single type changeSubscription Method checked
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
      customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), lastTariff, "SS");

      assertFalse("Changed SS from default tariff", customer.getSubscriptionMap().get("SS").getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

      sub.subscribe(customer.getHouses("NS").size());

      // Changing back from the new tariff to the default one in order to check
      // every changeSubscription Method
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(sub);
      when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tariffRepo.findAllTariffs());

      customer.changeSubscription(mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)), "NS");

      assertFalse("Changed NS from default tariff", customer.getSubscriptionMap().get("NS").getTariff() == mockTariffMarket.getDefaultTariff(customer.getCustomerInfo().getPowerTypes().get(0)));

    }
  }

  @Test
  public void revokeSubscription ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<TariffRevoke> tariffRevokeArg = ArgumentCaptor.forClass(TariffRevoke.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals("Four consumption tariffs", 4, tariffRepo.findAllTariffs().size());

    TariffStatus st = new TariffStatus(broker1, tariff2.getId(), tariff2.getId(), TariffStatus.Status.success);
    when(mockTariffMarket.processTariff(tariffRevokeArg.capture())).thenReturn(st);

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription tsd = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(mockTariffMarket.getDefaultTariff(PowerType.CONSUMPTION), customer.getCustomerInfo());
      customer.unsubscribe(tsd, 3);
      TariffSubscription sub1 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff1);
      sub1.subscribe(3);
      TariffSubscription sub2 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff2);
      sub2.subscribe(3);
      TariffSubscription sub3 = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), tariff3);
      sub3.subscribe(4);

      TariffSubscription ts1 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff1, customer.getCustomerInfo());
      customer.unsubscribe(ts1, 2);
      TariffSubscription ts2 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff2, customer.getCustomerInfo());
      customer.unsubscribe(ts2, 1);
      TariffSubscription ts3 = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(tariff3, customer.getCustomerInfo());
      customer.unsubscribe(ts3, 2);
      assertEquals("4 Subscriptions for customer", 4, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR));

    }

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tsc2.getBroker(), tsc2);
    TariffStatus status = mockTariffMarket.processTariff(tex);
    tariff2.setState(Tariff.State.KILLED);
    assertNotNull("non-null status", status);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tariff2.isRevoked());

    householdCustomerService.activate(timeService.getCurrentTime(), 1);
    for (Village customer : householdCustomerService.getVillageList()) {
      assertEquals("3 Subscriptions for customer", 3, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

    TariffStatus st2 = new TariffStatus(broker1, tariff3.getId(), tariff3.getId(), TariffStatus.Status.success);
    when(mockTariffMarket.processTariff(tariffRevokeArg.capture())).thenReturn(st2);

    TariffRevoke tex2 = new TariffRevoke(tariff3.getBroker(), tariff3.getTariffSpec());
    TariffStatus status2 = mockTariffMarket.processTariff(tex2);
    tariff3.setState(Tariff.State.KILLED);
    assertNotNull("non-null status", status2);
    assertEquals("success", TariffStatus.Status.success, status2.getStatus());
    assertTrue("tariff revoked", tariff3.isRevoked());

    householdCustomerService.activate(timeService.getCurrentTime(), 1);
    for (Village customer : householdCustomerService.getVillageList()) {
      assertEquals("2 Subscriptions for customer", 2, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());
    }

  }

  @Test
  public void testPublishAndEvaluatingTariffs ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    Rate r2 = new Rate().withValue(-0.222);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();

    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals("Four consumption tariffs", 4, tariffRepo.findAllTariffs().size());

    List<Tariff> tclist = tariffRepo.findAllTariffs();
    assertEquals("4 consumption tariffs", 4, tclist.size());

    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tclist);

    // Test the function with different inputs, in order to get the same result.
    householdCustomerService.publishNewTariffs(tclist);
    List<Tariff> tclist2 = new ArrayList<Tariff>();
    householdCustomerService.publishNewTariffs(tclist2);

    householdCustomerService.publishNewTariffs(tclist);
    householdCustomerService.publishNewTariffs(tclist2);
  }

  @Test
  public void testSupersedingTariffs ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<TariffRevoke> tariffRevokeArg = ArgumentCaptor.forClass(TariffRevoke.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();

    }

    Rate r2 = new Rate().withValue(-0.222);
    Rate r3 = new Rate().withValue(-0.111);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc2 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(2 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc3 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r2);
    TariffSpecification tsc4 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(3 * TimeService.DAY)).withMinDuration(TimeService.WEEK * 8).addRate(r3);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();
    Tariff tariff2 = new Tariff(tsc2);
    tariff2.init();
    Tariff tariff3 = new Tariff(tsc3);
    tariff3.init();
    Tariff tariff4 = new Tariff(tsc4);
    tariff4.init();

    tsc4.addSupersedes(tsc3.getId());
    assertEquals("correct length", 1, tsc4.getSupersedes().size());
    assertEquals("correct first element", tsc3.getId(), (long) tsc4.getSupersedes().get(0));

    assertNotNull("first tariff found", tariff1);
    assertNotNull("second tariff found", tariff2);
    assertNotNull("third tariff found", tariff3);
    assertEquals("Five consumption tariffs", 5, tariffRepo.findAllTariffs().size());

    List<Tariff> tclist = tariffRepo.findAllTariffs();
    assertEquals("5 consumption tariffs", 5, tclist.size());

    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tclist);

    // Test the function with different inputs, in order to get the same result.
    householdCustomerService.publishNewTariffs(tclist);

    TariffStatus st = new TariffStatus(broker1, tariff3.getId(), tariff3.getId(), TariffStatus.Status.success);
    when(mockTariffMarket.processTariff(tariffRevokeArg.capture())).thenReturn(st);

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));
    TariffRevoke tex = new TariffRevoke(tsc3.getBroker(), tsc3);
    TariffStatus status = mockTariffMarket.processTariff(tex);
    tariff3.setState(Tariff.State.KILLED);
    assertNotNull("non-null status", status);
    assertEquals("success", TariffStatus.Status.success, status.getStatus());
    assertTrue("tariff revoked", tariff3.isRevoked());

    timeService.setCurrentTime(new Instant(timeService.getCurrentTime().getMillis() + TimeService.HOUR));

    tclist = tariffRepo.findAllTariffs();
    assertEquals("5 consumption tariffs", 5, tclist.size());
    List<Tariff> tcactivelist = new ArrayList<Tariff>();
    for (Tariff tariff : tclist) {
      if (tariff.isRevoked() == false)
        tcactivelist.add(tariff);
    }

    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tcactivelist);

    householdCustomerService.publishNewTariffs(tcactivelist);
  }

  @Test
  public void testDailyShifting ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    // for (int i = 0; i < 10; i++) {

    Rate r0 = new Rate().withValue(-Math.random()).withDailyBegin(0).withDailyEnd(0);
    Rate r1 = new Rate().withValue(-Math.random()).withDailyBegin(1).withDailyEnd(1);
    Rate r2 = new Rate().withValue(-Math.random()).withDailyBegin(2).withDailyEnd(2);
    Rate r3 = new Rate().withValue(-Math.random()).withDailyBegin(3).withDailyEnd(3);
    Rate r4 = new Rate().withValue(-Math.random()).withDailyBegin(4).withDailyEnd(4);
    Rate r5 = new Rate().withValue(-Math.random()).withDailyBegin(5).withDailyEnd(5);
    Rate r6 = new Rate().withValue(-Math.random()).withDailyBegin(6).withDailyEnd(6);
    Rate r7 = new Rate().withValue(-Math.random()).withDailyBegin(7).withDailyEnd(7);
    Rate r8 = new Rate().withValue(-Math.random()).withDailyBegin(8).withDailyEnd(8);
    Rate r9 = new Rate().withValue(-Math.random()).withDailyBegin(9).withDailyEnd(9);
    Rate r10 = new Rate().withValue(-Math.random()).withDailyBegin(10).withDailyEnd(10);
    Rate r11 = new Rate().withValue(-Math.random()).withDailyBegin(11).withDailyEnd(11);
    Rate r12 = new Rate().withValue(-Math.random()).withDailyBegin(12).withDailyEnd(12);
    Rate r13 = new Rate().withValue(-Math.random()).withDailyBegin(13).withDailyEnd(13);
    Rate r14 = new Rate().withValue(-Math.random()).withDailyBegin(14).withDailyEnd(14);
    Rate r15 = new Rate().withValue(-Math.random()).withDailyBegin(15).withDailyEnd(15);
    Rate r16 = new Rate().withValue(-Math.random()).withDailyBegin(16).withDailyEnd(16);
    Rate r17 = new Rate().withValue(-Math.random()).withDailyBegin(17).withDailyEnd(17);
    Rate r18 = new Rate().withValue(-Math.random()).withDailyBegin(18).withDailyEnd(18);
    Rate r19 = new Rate().withValue(-Math.random()).withDailyBegin(19).withDailyEnd(19);
    Rate r20 = new Rate().withValue(-Math.random()).withDailyBegin(20).withDailyEnd(20);
    Rate r21 = new Rate().withValue(-Math.random()).withDailyBegin(21).withDailyEnd(21);
    Rate r22 = new Rate().withValue(-Math.random()).withDailyBegin(22).withDailyEnd(22);
    Rate r23 = new Rate().withValue(-Math.random()).withDailyBegin(23).withDailyEnd(23);

    TariffSpecification tsc1 = new TariffSpecification(broker1, PowerType.CONSUMPTION).withExpiration(now.plus(TimeService.DAY)).withMinDuration(TimeService.WEEK * 8);
    tsc1.addRate(r0);
    tsc1.addRate(r1);
    tsc1.addRate(r2);
    tsc1.addRate(r3);
    tsc1.addRate(r4);
    tsc1.addRate(r5);
    tsc1.addRate(r6);
    tsc1.addRate(r7);
    tsc1.addRate(r8);
    tsc1.addRate(r9);
    tsc1.addRate(r10);
    tsc1.addRate(r11);
    tsc1.addRate(r12);
    tsc1.addRate(r13);
    tsc1.addRate(r14);
    tsc1.addRate(r15);
    tsc1.addRate(r16);
    tsc1.addRate(r17);
    tsc1.addRate(r18);
    tsc1.addRate(r19);
    tsc1.addRate(r20);
    tsc1.addRate(r21);
    tsc1.addRate(r22);
    tsc1.addRate(r23);

    Tariff tariff1 = new Tariff(tsc1);
    tariff1.init();

    assertNotNull("first tariff found", tariff1);

    List<Tariff> tclist = tariffRepo.findAllTariffs();
    when(mockTariffMarket.getActiveTariffList(powerArg.capture())).thenReturn(tclist);

    householdCustomerService.publishNewTariffs(tclist);

    // }
    timeService.setBase(now.getMillis());
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 23));
    householdCustomerService.activate(timeService.getCurrentTime(), 1);
  }

  @Test
  public void testWeather ()
  {
    initializeService();

    // capture subscription method args
    ArgumentCaptor<Tariff> tariffArg = ArgumentCaptor.forClass(Tariff.class);
    ArgumentCaptor<CustomerInfo> customerArg = ArgumentCaptor.forClass(CustomerInfo.class);
    ArgumentCaptor<Integer> countArg = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<PowerType> powerArg = ArgumentCaptor.forClass(PowerType.class);

    for (Village customer : householdCustomerService.getVillageList()) {

      TariffSubscription defaultSub = tariffSubscriptionRepo.getSubscription(customer.getCustomerInfo(), defaultTariff);
      defaultSub.subscribe(customer.getCustomerInfo().getPopulation());
      when(mockTariffMarket.subscribeToTariff(tariffArg.capture(), customerArg.capture(), countArg.capture())).thenReturn(defaultSub);
      assertEquals("one subscription", 1, tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer.getCustomerInfo()).size());

      // Doing it again in order to check the correct configuration of the
      // SubscriptionMapping //
      customer.subscribeDefault();
    }

    // for (int i = 0; i < 10; i++) {
    timeService.setBase(now.getMillis());
    timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 5));

    Timeslot ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
    // log.debug(ts1.toString());
    double temperature = 40 * Math.random();
    WeatherReport wr = new WeatherReport(ts1, temperature, 2, 3, 4);
    weatherReportRepo.add(wr);
    householdCustomerService.activate(timeService.getCurrentTime(), 1);

    for (int i = 0; i < 1000; i++) {
      timeService.setBase(now.getMillis());
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 1));
      ts1 = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
      // log.debug(ts1.toString());
      temperature = 40 * Math.random();
      wr = new WeatherReport(ts1, temperature, 2, 3, 4);
      weatherReportRepo.add(wr);
      householdCustomerService.activate(timeService.getCurrentTime(), 1);
    }

    for (Village customer : householdCustomerService.getVillageList()) {
      customer.showAggDailyLoad("SS", 0);
    }
    /*
    // for (int i = 0; i < 10; i++) {
      timeService.setBase(now.getMillis());
      timeService.setCurrentTime(timeService.getCurrentTime().plus(TimeService.HOUR * 12));
      householdCustomerService.activate(timeService.getCurrentTime(), 1);
    */
    // }

  }
}
