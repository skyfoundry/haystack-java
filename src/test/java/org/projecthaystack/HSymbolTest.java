//
// Copyright (c) 2020, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Nov 2020  Matthew Giannini   Creation
//
package org.projecthaystack;

import static org.testng.Assert.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class HSymbolTest extends HValTest
{
  @Test
  public void testBasics()
  {
    // tag
    verifySymbol("a", "a");
    verifySymbol("foo", "foo");
    verifySymbol("foo_bar", "foo_bar");

    // conjunct
    verifySymbol("a-b", "a-b");
    verifySymbol("a-b-c-d", "a-b-c-d");
    verifySymbol("foo-bar-baz", "foo-bar-baz");
    verifySymbol("foo-x-baz", "foo-x-baz");
    verifySymbol("foo1-bar2", "foo1-bar2");
    verifySymbol("foo_1-bar_2", "foo_1-bar_2");

    // key
    verifySymbol("a:b", "b");
    verifySymbol("lib:ph", "ph");
  }

  private void verifySymbol(final String str, final String name)
  {
    HSymbol x = HSymbol.make(str);
    assertEquals(x, HSymbol.make(str));
    assertNotEquals(x, HSymbol.make(str+"foo"));
    assertEquals(x.name(), name);
  }

  @Test(expectedExceptions = ParseException.class,
    dataProvider = "BadSymbolProvider")
  public void testBadZinc(String str) { HSymbol.make(str); }

  @DataProvider
  public Object[][] BadSymbolProvider()
  {
    return new Object[][] {
      {""},
      {"a.b"},
      {"a-b.c"},
      {"lib:foo.bar_4"},
      {"a.Foo"},
      {"B"},
      {"Quick"},
      {"2a"},
      {"a b"},
      {"a/b"},
      {"foo "},
      {"a.b.c."},
      {"a:b:c."},
      {"a-b#"},
      // TODO: tests for when we have symbol sub-types (e.g. conjunct)
//      {"a--b"},
//      {"a-7b"},
//      {"a-Boo"},
//      {"a:-b"},
//      {"a-:b"},
//      {"a:3b"},
//      {"a:Foo"},
    };
  }

}
