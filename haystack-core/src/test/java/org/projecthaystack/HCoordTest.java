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

public class HCoordTest extends HValTest
{
  @Test
  public void testLatBoundaries()
  {
    verifyCoord(90, 123, "C(90.0,123.0)");
    verifyCoord(-90, 123, "C(-90.0,123.0)");
    verifyCoord(89.888999, 123, "C(89.888999,123.0)");
    verifyCoord(-89.888999, 123, "C(-89.888999,123.0)");
  }

  @Test
  public void testLonBoundaries()
  {
    verifyCoord(45, 180, "C(45.0,180.0)");
    verifyCoord(45, -180, "C(45.0,-180.0)");
    verifyCoord(45, 179.999129, "C(45.0,179.999129)");
    verifyCoord(45, -179.999129, "C(45.0,-179.999129)");
  }

  @Test
  public void testDecimalPlaces()
  {
    verifyCoord(9.1, -8.1, "C(9.1,-8.1)");
    verifyCoord(9.12, -8.13, "C(9.12,-8.13)");
    verifyCoord(9.123, -8.134, "C(9.123,-8.134)");
    verifyCoord(9.1234, -8.1346, "C(9.1234,-8.1346)");
    verifyCoord(9.12345,- 8.13456, "C(9.12345,-8.13456)");
    verifyCoord(9.123452, -8.134567, "C(9.123452,-8.134567)");
  }

  @Test
  public void testZeroBoundaries()
  {
    verifyCoord(0, 0, "C(0.0,0.0)");
    verifyCoord(0.3, -0.3, "C(0.3,-0.3)");
    verifyCoord(0.03, -0.03, "C(0.03,-0.03)");
    verifyCoord(0.003, -0.003, "C(0.003,-0.003)");
    verifyCoord(0.0003, -0.0003, "C(0.0003,-0.0003)");
    verifyCoord(0.02003, -0.02003, "C(0.02003,-0.02003)");
    verifyCoord(0.020003, -0.020003, "C(0.020003,-0.020003)");
    verifyCoord(0.000123, -0.000123, "C(0.000123,-0.000123)");
    verifyCoord(7.000123, -7.000123, "C(7.000123,-7.000123)");
  }

  @Test
  public void testIsLat()
  {
    assertFalse(HCoord.isLat(-91.0));
    assertTrue(HCoord.isLat(-90.0));
    assertTrue(HCoord.isLat(-89.0));
    assertTrue(HCoord.isLat(90.0));
    assertFalse(HCoord.isLat(91.0));
  }

  @Test
  public void testIsLng()
  {
    assertFalse(HCoord.isLng(-181.0));
    assertTrue(HCoord.isLng(-179.99));
    assertTrue(HCoord.isLng(180.0));
    assertFalse(HCoord.isLng(181.0));
  }

  @Test
  public void testMakeErrors()
  {
    try { HCoord.make(91, 12); fail(); } catch (IllegalArgumentException e) { assertTrue(true); }
    try { HCoord.make(-90.2, 12); fail(); } catch (IllegalArgumentException e) { assertTrue(true);  }
    try { HCoord.make(13, 180.009); fail(); } catch (IllegalArgumentException e) { assertTrue(true); }
    try { HCoord.make(13, -181); fail(); } catch (IllegalArgumentException e) { assertTrue(true); }
  }

  void verifyCoord(double lat, double lng, String s)
  {
    HCoord c = HCoord.make(lat, lng);
    assertEquals(c.lat(), lat);
    assertEquals(c.lng(), lng);
    assertEquals(c.toString(), s);
    assertEquals(HCoord.make(s), c);
  }

  @Test(expectedExceptions = ParseException.class,
        dataProvider = "BadZincProvider")
  public void testBadZinc(String zinc)
  {
    HCoord.make(zinc);
  }

  @DataProvider
  public Object[][] BadZincProvider()
  {
    return new Object[][] {
      {"C(0.123,-.789)"},
      {"1.0,2.0"},
      {"(1.0,2.0)"},
      {"C(1.0,2.0"},
      {"C1.0,2.0)"},
      {"C(x,9)"},
    };
  }
}
