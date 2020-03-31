//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class HBoolTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HBool.TRUE, HBool.TRUE);
    assertNotEquals(HBool.TRUE, HBool.FALSE);
    assertTrue(HBool.make(true) == HBool.TRUE);
    assertTrue(HBool.make(false) == HBool.FALSE);
  }

  @Test
  public void testCompare()
  {
    assertTrue(HBool.FALSE.compareTo(HBool.TRUE) < 0);
    assertEquals(HBool.TRUE.compareTo(HBool.TRUE), 0);
  }

  @Test
  public void testToString()
  {
    assertEquals(HBool.TRUE.toString(), "true");
    assertEquals(HBool.FALSE.toString(), "false");
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HBool.TRUE, "T");
    verifyZinc(HBool.FALSE, "F");
  }
}
