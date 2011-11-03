//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   04 Oct 2011  Brian Frank  Creation
//
package haystack.test;

import haystack.*;
import java.util.*;

/**
 * QueryTest tests parsing and filtering of HQuery
 */
public class QueryTest extends Test
{

//////////////////////////////////////////////////////////////////////////
// Path
//////////////////////////////////////////////////////////////////////////

  /* Path isn't public, so we can't run these tests all the time
  public void testPath()
  {
    // single name
    HQuery.Path path = HQuery.Path.make("foo");
    verifyEq(path.size(), 1);
    verifyEq(path.get(0), "foo");
    verifyEq(path.toString(), "foo");
    verifyEq(path, HQuery.Path.make("foo"));

    // two names
    path = HQuery.Path.make("foo->bar");
    verifyEq(path.size(), 2);
    verifyEq(path.get(0), "foo");
    verifyEq(path.get(1), "bar");
    verifyEq(path.toString(), "foo->bar");
    verifyEq(path, HQuery.Path.make("foo->bar"));

    // three names
    path = HQuery.Path.make("x->y->z");
    verifyEq(path.size(), 3);
    verifyEq(path.get(0), "x");
    verifyEq(path.get(1), "y");
    verifyEq(path.get(2), "z");
    verifyEq(path.toString(), "x->y->z");
    verifyEq(path, HQuery.Path.make("x->y->z"));
  }
  */

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  public void testIdentity()
  {
    verifyEq(HQuery.has("a"), HQuery.has("a"));
    verifyNotEq(HQuery.has("a"), HQuery.has("b"));
  }

//////////////////////////////////////////////////////////////////////////
// Parse
//////////////////////////////////////////////////////////////////////////

  public void testParse()
  {
    // basics
    verifyParse("x", HQuery.has("x"));
    verifyParse("foo", HQuery.has("foo"));
    verifyParse("fooBar", HQuery.has("fooBar"));
    verifyParse("foo7Bar", HQuery.has("foo7Bar"));
    verifyParse("foo_bar->a", HQuery.has("foo_bar->a"));
    verifyParse("a->b->c", HQuery.has("a->b->c"));
    verifyParse("not foo", HQuery.missing("foo"));

    // str literals
    verifyParse("x==\"hi\"", HQuery.eq("x", HStr.make("hi")));
    verifyParse("x!=\"\\\"hi\\\"\"",  HQuery.ne("x", HStr.make("\"hi\"")));
    verifyParse("x==\"_\\uabcd_\\n_\"", HQuery.eq("x", HStr.make("_\uabcd_\n_")));

    // uri literals
    verifyParse("ref==`http://foo/?bar`", HQuery.eq("ref", HUri.make("http://foo/?bar")));
    verifyParse("ref->x==`file name`", HQuery.eq("ref->x", HUri.make("file name")));
    verifyParse("ref == `foo bar`", HQuery.eq("ref", HUri.make("foo bar")));

    // int literals
    verifyParse("num < 4", HQuery.lt("num", n(4)));
    verifyParse("num <= -99", HQuery.le("num", n(-99)));

    // float literals
    verifyParse("num < 4.0", HQuery.lt("num", n(4f)));
    verifyParse("num <= -9.6", HQuery.le("num", n(-9.6f)));
    verifyParse("num > 400000", HQuery.gt("num", n(4e5f)));
    verifyParse("num >= 16000", HQuery.ge("num", n(1.6e+4f)));
    verifyParse("num >= 0.000000016", HQuery.ge("num", n(1.6e-8f)));

    // unit literals
    verifyParse("dur < 5ns", HQuery.lt("dur", n(5,"ns")));
    verifyParse("dur < 10kg", HQuery.lt("dur", n(10, "kg")));
    verifyParse("dur < -9sec", HQuery.lt("dur", n(-9, "sec")));
    verifyParse("dur < 2.5hr", HQuery.lt("dur", n(2.5, "hr")));

    // date, time, datetime
    verifyParse("foo < 2009-10-30", HQuery.lt("foo", HDate.read("2009-10-30")));
    verifyParse("foo < 08:30:00", HQuery.lt("foo", HTime.read("08:30:00")));
    verifyParse("foo < 13:00:00", HQuery.lt("foo", HTime.read("13:00:00")));

    // recId literals
    verifyParse("author == <xyz>", HQuery.eq("author", HRef.make("xyz")));

    // and
    verifyParse("a and b", HQuery.has("a").and(HQuery.has("b")));
    verifyParse("a and b and c == 3", HQuery.has("a").and( HQuery.has("b").and(HQuery.eq("c", n(3))) ));

    // or
    verifyParse("a or b", HQuery.has("a").or(HQuery.has("b")));
    verifyParse("a or b or c == 3", HQuery.has("a").or(HQuery.has("b").or(HQuery.eq("c", n(3)))));

    // parens
    verifyParse("(a)", HQuery.has("a"));
    verifyParse("(a) and (b)", HQuery.has("a").and(HQuery.has("b")));
    verifyParse("( a )  and  ( b ) ", HQuery.has("a").and(HQuery.has("b")));
    verifyParse("(a or b) or (c == 3)", HQuery.has("a").or(HQuery.has("b")).or(HQuery.eq("c", n(3))));

    // combo
    HQuery isA = HQuery.has("a");
    HQuery isB = HQuery.has("b");
    HQuery isC = HQuery.has("c");
    HQuery isD = HQuery.has("d");
    verifyParse("a and b or c", (isA.and(isB)).or(isC));
    verifyParse("a or b and c", isA.or(isB.and(isC)));
    verifyParse("a and b or c and d", (isA.and(isB)).or(isC.and(isD)));
    verifyParse("(a and (b or c)) and d", isA.and(isB.or(isC)).and(isD));
    verifyParse("(a or (b and c)) or d", isA.or(isB.and(isC)).or(isD));
  }

  void verifyParse(String s, HQuery expected)
  {
    HQuery actual = HQuery.read(s);
    verifyEq(actual, expected);
  }

//////////////////////////////////////////////////////////////////////////
// Include
//////////////////////////////////////////////////////////////////////////

  public void testInclude()
  {
    HTags a = new HTagsBuilder()
      .add("dis", "a")
      .add("num", 100)
      .add("foo", 99)
      .add("date", HDate.make(2011,10,5))
      .toTags();

   HTags b = new HTagsBuilder()
      .add("dis", "b")
      .add("num", 200)
      .add("foo", 88)
      .add("date", HDate.make(2011,10,20))
      .add("bar")
      .add("ref", HRef.make("a"))
      .toTags();

   HTags c = new HTagsBuilder()
      .add("dis", "c")
      .add("num", 300)
      .add("ref", HRef.make("b"))
      .add("bar")
      .toTags();

    final HashMap db = new HashMap();
    db.put("a", a);
    db.put("b", b);
    db.put("c", c);

    verifyInclude(db, "dis",                "a,b,c");
    verifyInclude(db, "dis == \"b\"",       "b");
    verifyInclude(db, "dis != \"b\"",       "a,c");
    verifyInclude(db, "dis <= \"b\"",       "a,b");
    verifyInclude(db, "dis >  \"b\"",       "c");
    verifyInclude(db, "num < 200",          "a");
    verifyInclude(db, "num <= 200",         "a,b");
    verifyInclude(db, "num > 200",          "c");
    verifyInclude(db, "num >= 200",         "b,c");
    verifyInclude(db, "date",               "a,b");
    verifyInclude(db, "date == 2011-10-20", "b");
    verifyInclude(db, "date < 2011-10-10",  "a");
    verifyInclude(db, "foo",                "a,b");
    verifyInclude(db, "not foo",            "c");
    verifyInclude(db, "foo == 88",          "b");
    verifyInclude(db, "foo != 88",          "a");
    verifyInclude(db, "foo == \"x\"",       "");
    verifyInclude(db, "ref",                "b,c");
    verifyInclude(db, "ref->dis",           "b,c");
    verifyInclude(db, "ref->dis == \"a\"",  "b");
    verifyInclude(db, "ref->bar",           "c");
    verifyInclude(db, "not ref->bar",       "a,b");
    verifyInclude(db, "foo and bar",        "b");
    verifyInclude(db, "foo or bar",         "a,b,c");
    verifyInclude(db, "(foo and bar) or num==300",  "b,c");
    verifyInclude(db, "foo and bar and num==300",   "");
  }

  void verifyInclude(final HashMap map, String query, String expected)
  {
    HQuery.Pather db = new HQuery.Pather()
    {
      public HTags find(String id) { return (HTags)map.get(id); }
    };

    HQuery q = HQuery.read(query);

    String actual = "";
    for (int c='a'; c<='c'; ++c)
    {
      String id = "" + (char)c;
      if (q.include(db.find(id), db))
        actual += actual.length() > 0 ? ","+id : id;
    }
    verifyEq(expected, actual);
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  HNum n(double v) { return HNum.make(v); }
  HNum n(double v, String u) { return HNum.make(v, u); }

}