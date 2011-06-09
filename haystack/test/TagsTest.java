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
 * TagsTest tests the HTags class
 */
public class TagsTest extends Test
{
  public void testEmpty()
  {
    HTags tags = new HTagsBuilder().toTags();
    verify(tags == HTags.EMPTY);
    verifyEq(tags, HTags.EMPTY);

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
    HTags tags = new HTagsBuilder()
       .add("id", HRef.make("aaaa-bbbb"))
       .add("site")
       .add("geoAddr", "Richmond, Va")
       .add("area", 1200, "ft")
       .add("date", HDate.make(2000, 12, 3))
       .toTags();

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
    HTags a = new HTagsBuilder().add("x").toTags();
    verifyEq(a, new HTagsBuilder().add("x").toTags());
    verifyNotEq(a, new HTagsBuilder().add("x", 3).toTags());
    verifyNotEq(a, new HTagsBuilder().add("y").toTags());
    verifyNotEq(a, new HTagsBuilder().add("x").add("y").toTags());

    a = new HTagsBuilder().add("x").add("y", "str").toTags();
    verifyEq(a, new HTagsBuilder().add("x").add("y", "str").toTags());
    verifyEq(a, new HTagsBuilder().add("y", "str").add("x").toTags());
    verifyNotEq(a, new HTagsBuilder().add("x", "str").add("y", "str").toTags());
    verifyNotEq(a, new HTagsBuilder().add("x").add("y", "strx").toTags());
    verifyNotEq(a, new HTagsBuilder().add("y", "str").toTags());
    verifyNotEq(a, new HTagsBuilder().add("x").toTags());
    verifyNotEq(a, new HTagsBuilder().add("x").add("yy", "str").toTags());
  }

  public void testIO()
  {
    verifyIO("",
      HTags.EMPTY);
    verifyIO("foo_12",
      new HTagsBuilder().add("foo_12").toTags());
    verifyIO("fooBar:123.0ft",
      new HTagsBuilder().add("fooBar", 123, "ft").toTags());
    verifyIO("dis:\"Bob\",bday:1970-06-03,marker",
      new HTagsBuilder().add("dis", "Bob").add("bday", HDate.make(1970,6,3)).add("marker").toTags());
    verifyIO("dis  :  \"Bob\" , bday : 1970-06-03 , marker",
      new HTagsBuilder().add("dis", "Bob").add("bday", HDate.make(1970,6,3)).add("marker").toTags());
  }

  void verifyIO(String s, HTags tags)
  {
    // println("  :: " +  HTags.read(s));
    if (tags.size() <= 1) verifyEq(tags.write(), s);
    verifyEq(HTags.read(s), tags);
  }
}