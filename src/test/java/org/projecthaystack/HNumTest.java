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

import java.util.Locale;

public class HNumTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HNum.make(2), HNum.make(2.0, null));
    assertNotEquals(HNum.make(2), HNum.make(2, "%"));
    assertNotEquals(HNum.make(2, "%"), HNum.make(2));
    assertTrue(HNum.make(0) == HNum.make(0.0));
  }

  @Test
  public void testCompare()
  {
    assertTrue(HNum.make(9).compareTo(HNum.make(11)) < 0);
    assertTrue(HNum.make(-3).compareTo(HNum.make(-4)) > 0);
    assertEquals(HNum.make(-23).compareTo(HNum.make(-23)), 0);
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HNum.make(123), "123");
    verifyZinc(HNum.make(123.4, "m/s"), "123.4m/s");
    verifyZinc(HNum.make(9.6, "m/s"), "9.6m/s");
    verifyZinc(HNum.make(-5.2, "\u00b0F"), "-5.2\u00b0F");
    verifyZinc(HNum.make(23, "%"), "23%");
    verifyZinc(HNum.make(2.4e-3, "fl_oz"), "0.0024fl_oz");
    verifyZinc(HNum.make(2.4e5, "$"), "240000$");
    assertEquals(read("1234.56fl_oz"), HNum.make(1234.56, "fl_oz"));
    assertEquals(read("0.000028fl_oz"), HNum.make(0.000028, "fl_oz"));

    // specials
    verifyZinc(HNum.make(Double.NEGATIVE_INFINITY), "-INF");
    verifyZinc(HNum.make(Double.POSITIVE_INFINITY), "INF");
    verifyZinc(HNum.make(Double.NaN), "NaN");

    // verify units never serialized for special values
    assertEquals(HNum.make(Double.NaN, "ignore").toZinc(), "NaN");
    assertEquals(HNum.make(Double.POSITIVE_INFINITY, "%").toZinc(), "INF");
    assertEquals(HNum.make(Double.NEGATIVE_INFINITY, "%").toZinc(), "-INF");
  }

  @Test
  public void verifyUnitNames()
  {
    assertTrue(HNum.isUnitName(null));
    assertFalse(HNum.isUnitName(""));
    assertTrue(HNum.isUnitName("x_z"));
    assertFalse(HNum.isUnitName("x z"));
  }

  @Test
  public void testFormatDecimalWithDot()
  {
    Locale locale = Locale.getDefault();
    Locale.setDefault(new Locale("fr"));
    verifyZinc(HNum.make(2.4), "2.4");
    Locale.setDefault(locale);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
        dataProvider = "BadUnitNames")
  public void testBadUnitConstruction(String unit)
  {
    HNum.make(123.4, unit);
  }

  @DataProvider
  public Object[][] BadUnitNames() {
    return new Object[][] {
      {"foo bar"},
      {"foo,bar"},
    };
  }
}
