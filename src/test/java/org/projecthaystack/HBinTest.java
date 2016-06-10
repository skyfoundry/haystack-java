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

public class HBinTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HBin.make("text/plain"), HBin.make("text/plain"));
    assertNotEquals(HBin.make("text/plain"), HBin.make("text/xml"));
  }
  // TODO:FIXIT
//    // encoding
//    verifyZinc(HBin.make("text/plain"), "Bin(\"text/plain\")");
//    verifyZinc(HBin.make("text/plain; charset=utf-8"), "Bin(\"text/plain; charset=utf-8\")");
//
//    // verify bad bins are caught on encoding
//    try { HBin.make("text/plain; f()").toZinc(); fail(); } catch (Exception e) { verifyException(e); }
//    try { read("Bin()"); fail(); } catch (Exception e) { verifyException(e); }
//    try { read("Bin(\"text\")"); fail(); } catch (Exception e) { verifyException(e); }
}
