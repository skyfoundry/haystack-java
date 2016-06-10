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

public class HStrTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HStr.make("a"), HStr.make("a"));
    assertNotEquals(HStr.make("a"), HStr.make("b"));
    assertTrue(HStr.make("") == HStr.make(""));
  }

  @Test
  public void testCompare()
  {
    assertTrue(HStr.make("abc").compareTo(HStr.make("z")) < 0);
    assertEquals(HStr.make("Foo").compareTo(HStr.make("Foo")), 0);
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HStr.make("hello"), "\"hello\"");
    verifyZinc(HStr.make("_ \\ \" \n \r \t \u0011 _"), "\"_ \\\\ \\\" \\n \\r \\t \\u0011 _\"");
    verifyZinc(HStr.make("\u0abc"), "\"\u0abc\"");
  }

  @Test
  public void testHex()
  {
    assertEquals(read("\"[\\uabcd \\u1234]\""), HStr.make("[\uabcd \u1234]"));
    assertEquals(read("\"[\\uABCD \\u1234]\""), HStr.make("[\uABCD \u1234]"));
  }

  @Test(expectedExceptions = ParseException.class)
  public void testNoEndQuote()
  {
    read("\"end...");
  }

  @Test(expectedExceptions = ParseException.class)
  public void testBadUnicodeEsc()
  {
    read("\"\\u1x34\"");
  }
}
