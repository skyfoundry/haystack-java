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

public class HDictTest extends HValTest
{
  @Test
  public void testEmpty()
  {
    HDict tags = new HDictBuilder().toDict();
    assertTrue(tags == HDict.EMPTY);
    assertEquals(tags, HDict.EMPTY);

    // size
    assertEquals(tags.size(), 0);
    assertTrue(tags.isEmpty());

    // missing tag
    assertFalse(tags.has("foo"));
    assertTrue(tags.missing("foo"));
    assertNull(tags.get("foo", false));
  }

  @Test(expectedExceptions = UnknownNameException.class)
  public void testCheckedImplicitMissing()
  {
    HDict tags = new HDictBuilder().toDict();
    tags.get("foo");
  }

  @Test(expectedExceptions = UnknownNameException.class)
  public void testCheckedExplicitMissing()
  {
    HDict tags = new HDictBuilder().toDict();
    tags.get("foo", true);
  }

  @Test
  public void testIsTagName()
  {
    assertFalse(HDict.isTagName(""));
    assertFalse(HDict.isTagName("A"));
    assertFalse(HDict.isTagName(" "));
    assertTrue(HDict.isTagName("a"));
    assertTrue(HDict.isTagName("a_B_19"));
    assertFalse(HDict.isTagName("a b"));
    assertFalse(HDict.isTagName("a\u0128"));
    assertFalse(HDict.isTagName("a\u0129x"));
    assertFalse(HDict.isTagName("a\uabcdx"));
  }

  @Test
  public void testBasics()
  {
    HDict tags = new HDictBuilder()
      .add("id", HRef.make("aaaa-bbbb"))
      .add("site")
      .add("geoAddr", "Richmond, Va")
      .add("area", 1200, "ft")
      .add("date", HDate.make(2000, 12, 3))
      .toDict();

    // size
    assertEquals(tags.size(), 5);
    assertFalse(tags.isEmpty());

    // configured tags
    assertEquals(tags.get("id"),      HRef.make("aaaa-bbbb"));
    assertEquals(tags.get("site"),    HMarker.VAL);
    assertEquals(tags.get("geoAddr"), HStr.make("Richmond, Va"));
    assertEquals(tags.get("area"),    HNum.make(1200, "ft"));
    assertEquals(tags.get("date"),    HDate.make(2000, 12, 3));

    // missing tag
    assertFalse(tags.has("foo"));
    assertTrue(tags.missing("foo"));
    assertNull(tags.get("foo", false));
    try { tags.get("foo"); fail(); } catch (UnknownNameException e) { assertTrue(true); }
    try { tags.get("foo", true); fail(); } catch (UnknownNameException e) {assertTrue(true); }
  }

  @Test
  public void testEquality()
  {
    HDict a = new HDictBuilder().add("x").toDict();
    assertEquals(a, new HDictBuilder().add("x").toDict());
    assertNotEquals(a, new HDictBuilder().add("x", 3).toDict());
    assertNotEquals(a, new HDictBuilder().add("y").toDict());
    assertNotEquals(a, new HDictBuilder().add("x").add("y").toDict());

    a = new HDictBuilder().add("x").add("y", "str").toDict();
    assertEquals(a, new HDictBuilder().add("x").add("y", "str").toDict());
    assertEquals(a, new HDictBuilder().add("y", "str").add("x").toDict());
    assertNotEquals(a, new HDictBuilder().add("x", "str").add("y", "str").toDict());
    assertNotEquals(a, new HDictBuilder().add("x").add("y", "strx").toDict());
    assertNotEquals(a, new HDictBuilder().add("y", "str").toDict());
    assertNotEquals(a, new HDictBuilder().add("x").toDict());
    assertNotEquals(a, new HDictBuilder().add("x").add("yy", "str").toDict());
  }

  @Test
  public void testZinc()
  {
    verifyZinc(
      HDict.EMPTY,
      "{}");
    verifyZinc(
      new HDictBuilder().add("foo_12").toDict(),
      "{foo_12}");
    verifyZinc(
      new HDictBuilder().add("fooBar", 123, "ft").toDict(),
      "{fooBar:123ft}");
    verifyZinc(
      new HDictBuilder().add("dis", "Bob").add("bday", HDate.make(1970,6,3)).add("marker").toDict(),
      "{dis:\"Bob\" bday:1970-06-03 marker}");

    // nested dict
    verifyZinc(
      new HDictBuilder().add("auth", HDict.EMPTY).toDict(),
      "{auth:{}}");
    verifyZinc(
      new HDictBuilder().add("auth",
        new HDictBuilder().add("alg", "scram").add("c", 10000).add("marker").toDict()
      ).toDict(),
      "{auth:{alg:\"scram\" c:10000 marker}}");

    // nested list
    verifyZinc(
      new HDictBuilder().add("arr", HList.make(new HVal[] {HNum.make(1.0), HNum.make(2), HNum.make(3)}))
        .add("x").toDict(),
      "{arr:[1.0,2,3] x}");
  }

  @Test
  public void testDis()
  {
    assertEquals(new HDictBuilder().add("id", HRef.make("a")).toDict().dis(), "a");
    assertEquals(new HDictBuilder().add("id", HRef.make("a", "b")).toDict().dis(), "b");
    assertEquals(new HDictBuilder().add("id", HRef.make("a")).add("dis", "d").toDict().dis(), "d");
  }
}
