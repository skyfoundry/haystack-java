//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack;

import static org.testng.Assert.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class HDateTimeTest extends HValTest
{
  public static HTimeZone utc = HTimeZone.UTC;
  public static HTimeZone london = HTimeZone.make("London");

  @Test
  public void testEquality()
  {
    assertEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2009, 1, 2, 3, 4, 5, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 9, 2, 3, 4, 5, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 9, 3, 4, 5, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 9, 4, 5, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 9, 5, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 9, utc, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 5, london, 0));
    assertNotEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 5, london, 3600));
  }

  @Test
  public void testCompare()
  {
    assertEquals(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0)), 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 4, 6, utc, 0)) < 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 5, 5, utc, 0)) < 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 4, 4, 5, utc, 0)) < 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 3, 3, 4, 5, utc, 0)) < 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 2, 2, 3, 4, 5, utc, 0)) < 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2012, 1, 2, 3, 4, 5, utc, 0)) < 0);
    assertTrue(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 4, 0, utc, 0)) > 0);
  }

  @Test
  public void testZinc()
  {
    HDateTime ts = HDateTime.make(1307377618069L, HTimeZone.make("New_York"));
    verifyZinc(ts, "2011-06-06T12:26:58.069-04:00 New_York");
    assertEquals(ts.date.toString(), "2011-06-06");
    assertEquals(ts.time.toString(), "12:26:58.069");
    assertEquals(ts.tzOffset, -4*60*60);
    assertEquals(ts.tz.name, "New_York");
    assertEquals(ts.tz.java.getID(), "America/New_York");
    assertEquals(ts.millis(), 1307377618069L);

    // convert back to millis
    ts = HDateTime.make(ts.date, ts.time, ts.tz, ts.tzOffset);
    assertEquals(ts.millis(), 1307377618069L);

    // different timezones
    ts = HDateTime.make(949478640000L, HTimeZone.make("New_York"));
    verifyZinc(ts, "2000-02-02T03:04:00-05:00 New_York");
    ts = HDateTime.make(949478640000L, HTimeZone.make("UTC"));
    verifyZinc(ts, "2000-02-02T08:04:00Z UTC");
    ts = HDateTime.make(949478640000L, HTimeZone.make("Taipei"));
    verifyZinc(ts, "2000-02-02T16:04:00+08:00 Taipei");
    verifyZinc(HDateTime.make(2011, 6, 7, 11, 3, 43, HTimeZone.make("GMT+10"), -36000),
      "2011-06-07T11:03:43-10:00 GMT+10");
    verifyZinc(HDateTime.make(HDate.make(2011, 6, 8), HTime.make(4, 7, 33, 771), HTimeZone.make("GMT-7"), 25200),
      "2011-06-08T04:07:33.771+07:00 GMT-7");
  }

  @Test
  public void testMillis()
  {
    HDate date = HDate.make(2014, 12, 24);
    HTime time = HTime.make(11, 12, 13, 456);
    HTimeZone newYork = HTimeZone.make("New_York");
    long utcMillis = 1419437533456L;

    HDateTime a = HDateTime.make(date, time, newYork);
    HDateTime b = HDateTime.make(date, time, newYork, a.tzOffset);
    HDateTime c = HDateTime.make(utcMillis,  newYork);
    HDateTime d = HDateTime.make("2014-12-24T11:12:13.456-05:00 New_York");

    assertEquals(a.millis(), utcMillis);
    assertEquals(b.millis(), utcMillis);
    assertEquals(c.millis(), utcMillis);
    assertEquals(d.millis(), utcMillis);
  }

  @Test(expectedExceptions = { ParseException.class, NumberFormatException.class},
        dataProvider = "BadZincProvider")
  public void testBadZinc(String zinc)
  {
    read(zinc);
  }

  @DataProvider
  public Object[][] BadZincProvider()
  {
    return new Object[][] {
      {"2000-02-02T03:04:00-0x:00 New_York"},
      {"2000-02-02T03:04:00-05 New_York"},
      {"2000-02-02T03:04:00-05:!0 New_York"},
      {"2000-02-02T03:04:00-05:00"},
      {"2000-02-02T03:04:00-05:00 @"},
    };
  }
}
