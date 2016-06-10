//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 June 2016  Matthew Giannini   Creation
//

package org.projecthaystack.test;

import org.projecthaystack.HList;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;
import org.projecthaystack.io.HZincReader;

import java.util.ArrayList;
import java.util.List;

/**
 * ListTest
 */
public class ListTest extends Test
{
  public void testEmpty()
  {
    verifyEq(HList.EMPTY, HList.make(new ArrayList()));
    verifyEq(HList.EMPTY, HList.make(new HVal[0]));
    verifyEq(0, HList.EMPTY.size());
    try { HList.EMPTY.get(0); fail(); } catch (Exception e) { verify(true); }
  }

  public void testBasics()
  {
    HRef ref = HRef.make("a");
    HStr str = HStr.make("string");
    List items = new ArrayList();
    items.add(ref);
    items.add(str);

    HList list = HList.make(items);
    verifyEq(2, list.size());
    verifyEq(ref, list.get(0));
    verifyEq(str, list.get(1));
  }

  public void testZinc()
  {
    verifyZinc("[]",
      HList.EMPTY);
  }

  private void verifyZinc(String zinc, HList list)
  {
//    HList fromZinc = new HZincReader(zinc).readVal()
  }
}
