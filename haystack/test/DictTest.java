//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack.test;

import haystack.*;
import java.util.*;

/**
 * DictTest tests the HDict class
 */
public class DictTest extends Test
{
  public void testEmpty()
  {
    HDict tags = new HDictBuilder().toDict();
    verify(tags == HDict.EMPTY);
    verifyEq(tags, HDict.EMPTY);

    // size
    verifyEq(tags.size(), 0);
    verifyEq(tags.isEmpty(), true);

    // missing tag
    verifyEq(tags.has("foo"), false);
    verifyEq(tags.missing("foo"), true);
    verifyEq(tags.get("foo", false), null);
    try { tags.get("foo"); fail(); } catch (MissingTagException e) { verify(true); }
    try { tags.get("foo", true); fail(); } catch (MissingTagException e) { verify(true); }
  }

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
    verifyEq(tags.size(), 5);
    verifyEq(tags.isEmpty(), false);

    // configured tags
    verifyEq(tags.get("id"),      HRef.make("aaaa-bbbb"));
    verifyEq(tags.get("site"),    HMarker.VAL);
    verifyEq(tags.get("geoAddr"), HStr.make("Richmond, Va"));
    verifyEq(tags.get("area"),    HNum.make(1200, "ft"));
    verifyEq(tags.get("date"),    HDate.make(2000, 12, 3));

    // missing tag
    verifyEq(tags.has("foo"), false);
    verifyEq(tags.missing("foo"), true);
    verifyEq(tags.get("foo", false), null);
    try { tags.get("foo"); fail(); } catch (MissingTagException e) { verify(true); }
    try { tags.get("foo", true); fail(); } catch (MissingTagException e) { verify(true); }
  }

  public void testEquality()
  {
    HDict a = new HDictBuilder().add("x").toDict();
    verifyEq(a, new HDictBuilder().add("x").toDict());
    verifyNotEq(a, new HDictBuilder().add("x", 3).toDict());
    verifyNotEq(a, new HDictBuilder().add("y").toDict());
    verifyNotEq(a, new HDictBuilder().add("x").add("y").toDict());

    a = new HDictBuilder().add("x").add("y", "str").toDict();
    verifyEq(a, new HDictBuilder().add("x").add("y", "str").toDict());
    verifyEq(a, new HDictBuilder().add("y", "str").add("x").toDict());
    verifyNotEq(a, new HDictBuilder().add("x", "str").add("y", "str").toDict());
    verifyNotEq(a, new HDictBuilder().add("x").add("y", "strx").toDict());
    verifyNotEq(a, new HDictBuilder().add("y", "str").toDict());
    verifyNotEq(a, new HDictBuilder().add("x").toDict());
    verifyNotEq(a, new HDictBuilder().add("x").add("yy", "str").toDict());
  }

  public void testIO()
  {
    verifyIO("",
      HDict.EMPTY);
    verifyIO("foo_12",
      new HDictBuilder().add("foo_12").toDict());
    verifyIO("fooBar:123ft",
      new HDictBuilder().add("fooBar", 123, "ft").toDict());
    verifyIO("dis:\"Bob\",bday:1970-06-03,marker",
      new HDictBuilder().add("dis", "Bob").add("bday", HDate.make(1970,6,3)).add("marker").toDict());
    verifyIO("dis  :  \"Bob\" , bday : 1970-06-03 , marker",
      new HDictBuilder().add("dis", "Bob").add("bday", HDate.make(1970,6,3)).add("marker").toDict());
  }

  void verifyIO(String s, HDict tags)
  {
    // println("  :: " +  HDict.read(s));
    if (tags.size() <= 1) verifyEq(tags.write(), s);
    verifyEq(HDict.read(s), tags);
  }
}