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

public class HDateTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HDate.make(2011, 6, 7), HDate.make(2011, 6, 7));
    assertNotEquals(HDate.make(2011, 6, 7), HDate.make(2011, 6, 8));
    assertNotEquals(HDate.make(2011, 6, 7), HDate.make(2011, 2, 7));
    assertNotEquals(HDate.make(2011, 6, 7), HDate.make(2009, 6, 7));
  }

  @Test
  public void testCompare()
  {
    assertTrue(HDate.make(2011, 6, 9).compareTo(HDate.make(2011, 6, 21)) < 0);
    assertTrue(HDate.make(2011, 10, 9).compareTo(HDate.make(2011, 3, 21)) > 0);
    assertTrue(HDate.make(2010, 6, 9).compareTo(HDate.make(2000, 9, 30)) > 0);
    assertEquals(HDate.make(2010, 6, 9).compareTo(HDate.make(2010, 6, 9)), 0);
  }

  @Test
  public void testPlusMinus()
  {
    assertEquals(HDate.make(2011, 12, 1).minusDays(0), HDate.make(2011, 12, 1));
    assertEquals(HDate.make(2011, 12, 1).minusDays(1), HDate.make(2011, 11, 30));
    assertEquals(HDate.make(2011, 12, 1).minusDays(-2), HDate.make(2011, 12, 3));
    assertEquals(HDate.make(2011, 12, 1).plusDays(2), HDate.make(2011, 12, 3));
    assertEquals(HDate.make(2011, 12, 1).plusDays(31), HDate.make(2012, 1, 1));
    assertEquals(HDate.make(2008, 3, 3).minusDays(3), HDate.make(2008, 2, 29));
    assertEquals(HDate.make(2008, 3, 3).minusDays(4), HDate.make(2008, 2, 28));
  }

  @Test
  public void testLeapYear()
  {
    for (int y = 1900; y <= 2100; y++)
    {
      if (((y % 4) == 0) && (y != 1900) && (y != 2100))
        assertTrue(HDate.isLeapYear(y));
      else
        assertFalse(HDate.isLeapYear(y));
    }
  }

  @Test
  public void testMidnight()
  {
    verifyMidnight(HDate.make(2011, 11, 3),  "UTC",      "2011-11-03T00:00:00Z UTC");
    verifyMidnight(HDate.make(2011, 11, 3),  "New_York", "2011-11-03T00:00:00-04:00 New_York");
    verifyMidnight(HDate.make(2011, 12, 15), "Chicago",  "2011-12-15T00:00:00-06:00 Chicago");
    verifyMidnight(HDate.make(2008, 2, 29),  "Phoenix",  "2008-02-29T00:00:00-07:00 Phoenix");
  }

  private void verifyMidnight(HDate date, String tzName, String str)
  {
    HDateTime ts = date.midnight(HTimeZone.make(tzName));
    assertEquals(ts.date, date);
    assertEquals(ts.time.hour, 0);
    assertEquals(ts.time.min,  0);
    assertEquals(ts.time.sec,  0);
    assertEquals(ts.toString(), str);
    assertEquals(ts, read(ts.toZinc()));
    assertEquals(ts.millis(), ((HDateTime)read(str)).millis());
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HDate.make(2011, 6, 7), "2011-06-07");
    verifyZinc(HDate.make(2011,10,10), "2011-10-10");
    verifyZinc(HDate.make(2011,12,31), "2011-12-31");
  }

  @Test(expectedExceptions = ParseException.class,
        dataProvider = "BadDateProvider")
  public void testBadZinc(String zinc)
  {
    read(zinc);
  }

  @DataProvider
  public Object[][] BadDateProvider()
  {
    return new Object[][] {
      {"2003-xx-02"} ,
      {"2003-02"},
      {"2003-02-xx"},
    };
  }
}
