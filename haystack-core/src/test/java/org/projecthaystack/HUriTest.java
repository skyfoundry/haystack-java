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

public class HUriTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HUri.make("a"), HUri.make("a"));
    assertNotEquals(HUri.make("a"), HUri.make("b"));
    assertTrue(HUri.make("") == HUri.make(""));
  }

  @Test
  public void testCompare()
  {
    assertTrue(HUri.make("abc").compareTo(HUri.make("z")) < 0);
    assertEquals(HUri.make("Foo").compareTo(HUri.make("Foo")), 0);
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HUri.make("http://foo.com/f?q"), "`http://foo.com/f?q`");
    verifyZinc(HUri.make("a$b"), "`a$b`");
    verifyZinc(HUri.make("a`b"), "`a\\`b`");
    verifyZinc(HUri.make("http\\:a\\?b"), "`http\\:a\\?b`");
    verifyZinc(HUri.make("\u01ab.txt"), "`\u01ab.txt`");
  }

  @Test(expectedExceptions = ParseException.class,
        dataProvider = "BadZincProvider")
  public void testBadZinc(String zinc)
  {
    read(zinc);
  }

  @DataProvider
  public Object[][] BadZincProvider()
  {
    return new Object[][] {
      {"`no end"},
      {"`new\nline`"},
    };
  }
}
