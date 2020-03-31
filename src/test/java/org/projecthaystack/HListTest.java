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

import java.util.ArrayList;
import java.util.List;

public class HListTest extends HValTest
{
  @Test
  public void testEmpty()
  {
    assertEquals(HList.EMPTY, HList.make(new ArrayList<HVal>()));
    assertEquals(HList.EMPTY, HList.make(new HVal[0]));
    assertEquals(HList.EMPTY.size(), 0);
    try { HList.EMPTY.get(0); fail(); } catch (Exception e) { assertTrue(true); }
  }

  @Test
  public void testBasics()
  {
    HRef ref = HRef.make("a");
    HStr str = HStr.make("string");
    List<HVal> items = new ArrayList<HVal>();
    items.add(ref);
    items.add(str);

    HList list = HList.make(items);
    assertEquals(list.size(), 2);
    assertEquals(list.get(0), ref);
    assertEquals(list.get(1), str);
  }

  @Test
  public void testZinc()
  {
    verifyZinc(HList.EMPTY, "[]");
    // TODO: more tests
  }
}
