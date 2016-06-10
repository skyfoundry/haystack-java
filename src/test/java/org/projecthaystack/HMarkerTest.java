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

public class HMarkerTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HMarker.VAL, HMarker.VAL);
  }

  @Test
  public void testToString()
  {
    assertEquals(HMarker.VAL.toString(), "marker");
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HMarker.VAL, "M");
  }
}
