//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack;

import org.projecthaystack.io.HZincReader;

import static org.testng.Assert.*;

public abstract class HValTest extends HaystackTest
{
  protected void verifyZinc(HVal val, String s)
  {
    assertEquals(val.toZinc(), s);
    assertEquals(read(s), val);
  }

  protected HVal read(String s)
  {
    return new HZincReader(s).readVal();
  }
}
