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

public class HTimeTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HTime.make(1, 2, 3, 4), HTime.make(1, 2, 3, 4));
    assertNotEquals(HTime.make(1, 2, 3, 4), HTime.make(9, 2, 3, 4));
    assertNotEquals(HTime.make(1, 2, 3, 4), HTime.make(1, 9, 3, 4));
    assertNotEquals(HTime.make(1, 2, 3, 4), HTime.make(1, 2, 9, 9));
  }

  @Test
  public void testCompare()
  {
    assertTrue(HTime.make(0, 0, 0, 0).compareTo(HTime.make(0, 0, 0, 9)) < 0);
    assertTrue(HTime.make(0, 0, 0, 0).compareTo(HTime.make(0, 0, 1, 0)) < 0);
    assertTrue(HTime.make(0, 1, 0, 0).compareTo(HTime.make(0, 0, 0, 0)) > 0);
    assertTrue(HTime.make(0, 0, 0, 0).compareTo(HTime.make(2, 0, 0, 0)) < 0);
    assertEquals(HTime.make(2, 0, 0, 0).compareTo(HTime.make(2, 0, 0, 0)), 0);
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HTime.make(2, 3), "02:03:00");
    verifyZinc(HTime.make(2, 3, 4), "02:03:04");
    verifyZinc(HTime.make(2, 3, 4, 5), "02:03:04.005");
    verifyZinc(HTime.make(2, 3, 4, 56), "02:03:04.056");
    verifyZinc(HTime.make(2, 3, 4, 109), "02:03:04.109");
    verifyZinc(HTime.make(2, 3, 10, 109), "02:03:10.109");
    verifyZinc(HTime.make(2, 10, 59), "02:10:59");
    verifyZinc(HTime.make(10, 59, 30), "10:59:30");
    verifyZinc(HTime.make(23, 59, 59, 999), "23:59:59.999");
    verifyZinc(HTime.make(3, 20, 0), "03:20:00");
  }

  @Test (expectedExceptions = ParseException.class,
         dataProvider = "BadZincProvider")
  public void testBadZinc(String zinc)
  {
    read(zinc);
  }

  @DataProvider
  public Object[][] BadZincProvider()
  {
    return new Object[][] {
      {"13:xx:00"},
      {"13:45:0x"},
      {"13:45:00.4561"},
    };
  }
}
