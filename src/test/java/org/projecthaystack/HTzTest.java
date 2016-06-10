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

import java.util.TimeZone;


public class HTzTest
{
  @Test
  public void testTz()
  {
    verifyTz("New_York", "America/New_York");
    verifyTz("Chicago",  "America/Chicago");
    verifyTz("Phoenix",  "America/Phoenix");
    verifyTz("London",   "Europe/London");
    verifyTz("UTC",      "Etc/UTC");
  }

  private void verifyTz(String name, String javaId)
  {
    HTimeZone tz = HTimeZone.make(name);
    TimeZone java = TimeZone.getTimeZone(javaId);
    assertEquals(tz.name, name);
    assertEquals(tz.java, java);
    assertEquals(tz, HTimeZone.make(java));
  }
}
