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

public class HRefTest extends HValTest
{
  @Test
  public void testEquality()
  {
    assertEquals(HRef.make("foo"), HRef.make("foo"));
    assertEquals(HRef.make("foo"), HRef.make("foo", "Foo"));
    assertNotEquals(HRef.make("foo"), HRef.make("Foo"));
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HRef.make("1234-5678.foo:bar"), "@1234-5678.foo:bar");
    verifyZinc(HRef.make("1234-5678", "Foo Bar"), "@1234-5678 \"Foo Bar\"");
    verifyZinc(HRef.make("1234-5678", "Foo \"Bar\""), "@1234-5678 \"Foo \\\"Bar\\\"\"");
  }

  @Test
  public void testIsId()
  {
    assertFalse(HRef.isId(""));
    assertFalse(HRef.isId("%"));
    assertTrue(HRef.isId("a"));
    assertTrue(HRef.isId("a-b:c"));
    assertFalse(HRef.isId("a b"));
    assertFalse(HRef.isId("a\u0129b"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
        dataProvider = "BadRefProvider")
  public void testBadRefConstruction(String id)
  {
    HRef.make(id);
  }

  @DataProvider
  public Object[][] BadRefProvider()
  {
    return new Object[][] {
      {"@a"},
      {"a b"},
      {"a\n"},
      {"@"},
    };
  }
}
