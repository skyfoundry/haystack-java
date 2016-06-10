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

import java.util.Iterator;
import java.util.Map;

public class HGridTest extends HValTest
{
  @Test
  public void testEmpty()
  {
    HGrid g = new HGridBuilder().toGrid();
    assertEquals(g.meta(), HDict.EMPTY);
    assertEquals(g.numRows(), 0);
    assertTrue(g.isEmpty());
    assertNull(g.col("foo", false));
    try { g.col("foo"); fail(); } catch (UnknownNameException e) { assertTrue(true); }
  }
  
  @Test
  public void testNoRows()
  {
    HGridBuilder b = new HGridBuilder();
    b.meta().add("dis", "Title");
    b.addCol("a").add("dis", "Alpha");
    b.addCol("b");
    HGrid g = b.toGrid();

    // meta
    assertEquals(g.meta().size(), 1);
    assertEquals(g.meta().get("dis"), HStr.make("Title"));

    // cols
    HCol c;
    assertEquals(g.numCols(), 2);
    c = verifyCol(g, 0, "a");
    assertEquals(c.dis(), "Alpha");
    assertEquals(c.meta().size(), 1);
    assertEquals(c.meta().get("dis"), HStr.make("Alpha"));

    // rows
    assertEquals(g.numRows(), 0);
    assertEquals(g.isEmpty(), true);

    // iterator
    verifyGridIterator(g);
  }
  
  @Test
  public void testSimple()
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addCol("dis");
    b.addCol("area");
    b.addRow(new HVal[] { HRef.make("a"), HStr.make("Alpha"), HNum.make(1200) });
    b.addRow(new HVal[] { HRef.make("b"), HStr.make("Beta"), null });

    // meta
    HGrid g = b.toGrid();
    assertEquals(g.meta().size(), 0);

    // cols
    HCol c;
    assertEquals(g.numCols(), 3);
    verifyCol(g, 0, "id");
    verifyCol(g, 1, "dis");
    verifyCol(g, 2, "area");

    // rows
    assertEquals(g.numRows(), 2);
    assertFalse(g.isEmpty());
    HRow r;
    r = g.row(0);
    assertEquals(r.get("id"), HRef.make("a"));
    assertEquals(r.get("dis"), HStr.make("Alpha"));
    assertEquals(r.get("area"), HNum.make(1200));
    r = g.row(1);
    assertEquals(r.get("id"), HRef.make("b"));
    assertEquals(r.get("dis"), HStr.make("Beta"));
    assertNull(r.get("area", false));
    try { r.get("area"); fail(); } catch (UnknownNameException e) { assertTrue(true); }
    assertNull(r.get("fooBar", false));
    try { r.get("fooBar"); fail(); } catch (UnknownNameException e) { assertTrue(true); }

    // HRow.iterator no-nulls
    Iterator it = g.row(0).iterator();
    verifyRowIterator(it, "id",   HRef.make("a"));
    verifyRowIterator(it, "dis",  HStr.make("Alpha"));
    verifyRowIterator(it, "area", HNum.make(1200));
    assertFalse(it.hasNext());

    // HRow.iterator with nulls
    it = g.row(1).iterator();
    verifyRowIterator(it, "id",  HRef.make("b"));
    verifyRowIterator(it, "dis", HStr.make("Beta"));
    assertFalse(it.hasNext());

    // iterator
    verifyGridIterator(g);
  }

  HCol verifyCol(HGrid g, int i, String n)
  {
    HCol col = g.col(i);
    assertTrue(g.col(i) == g.col(n));
    assertEquals(col.name(), n);
    return col;
  }

  void verifyRowIterator(Iterator it, String name, HVal val)
  {
    assertTrue(it.hasNext());
    Map.Entry entry = (Map.Entry)it.next();
    assertEquals(entry.getKey(), name);
    assertEquals(entry.getValue(), val);
  }

  void verifyGridIterator(HGrid g)
  {
    Iterator it = g.iterator();
    int c = 0;
    while (c < g.numRows())
    {
      assertTrue(it.hasNext());
      assertTrue(it.next() == g.row(c++));
    }
    assertFalse(it.hasNext());
    assertEquals(g.numRows(), c);
  }
}
