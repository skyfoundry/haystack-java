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

import java.util.HashMap;

public class HFilterTest extends HaystackTest
{
  @Test
  public void testIdentity()
  {
    assertEquals(HFilter.has("a"), HFilter.has("a"));
    assertNotEquals(HFilter.has("a"), HFilter.has("b"));
  }

  @Test
  public void testBasics()
  {
    verifyParse("x", HFilter.has("x"));
    verifyParse("foo", HFilter.has("foo"));
    verifyParse("fooBar", HFilter.has("fooBar"));
    verifyParse("foo7Bar", HFilter.has("foo7Bar"));
    verifyParse("foo_bar->a", HFilter.has("foo_bar->a"));
    verifyParse("a->b->c", HFilter.has("a->b->c"));
    verifyParse("not foo", HFilter.missing("foo"));
  }

  @Test
  public void testZincOnlyLiteralsDontWork()
  {
    assertNull(HFilter.make("x==T", false));
    assertNull(HFilter.make("x==F", false));
    assertNull(HFilter.make("x==F", false));
  }

  @Test
  public void testBool()
  {
    verifyParse("x->y==true", HFilter.eq("x->y", HBool.TRUE));
    verifyParse("x->y!=false", HFilter.ne("x->y", HBool.FALSE));
  }

  @Test
  public void testStr()
  {
    verifyParse("x==\"hi\"", HFilter.eq("x", HStr.make("hi")));
    verifyParse("x!=\"\\\"hi\\\"\"",  HFilter.ne("x", HStr.make("\"hi\"")));
    verifyParse("x==\"_\\uabcd_\\n_\"", HFilter.eq("x", HStr.make("_\uabcd_\n_")));
  }

  @Test
  public void testUri()
  {
    verifyParse("ref==`http://foo/?bar`", HFilter.eq("ref", HUri.make("http://foo/?bar")));
    verifyParse("ref->x==`file name`", HFilter.eq("ref->x", HUri.make("file name")));
    verifyParse("ref == `foo bar`", HFilter.eq("ref", HUri.make("foo bar")));
  }

  @Test
  public void testInt()
  {
    verifyParse("num < 4", HFilter.lt("num", n(4)));
    verifyParse("num <= -99", HFilter.le("num", n(-99)));
  }

  @Test
  public void testFloat()
  {
    verifyParse("num < 4.0", HFilter.lt("num", n(4f)));
    verifyParse("num <= -9.6", HFilter.le("num", n(-9.6f)));
    verifyParse("num > 400000", HFilter.gt("num", n(4e5f)));
    verifyParse("num >= 16000", HFilter.ge("num", n(1.6e+4f)));
    verifyParse("num >= 2.16", HFilter.ge("num", n(2.16)));
  }

  @Test
  public void testUnit()
  {
    verifyParse("dur < 5ns", HFilter.lt("dur", n(5,"ns")));
    verifyParse("dur < 10kg", HFilter.lt("dur", n(10, "kg")));
    verifyParse("dur < -9sec", HFilter.lt("dur", n(-9, "sec")));
    verifyParse("dur < 2.5hr", HFilter.lt("dur", n(2.5, "hr")));
  }

  @Test
  public void testDateTime()
  {
    verifyParse("foo < 2009-10-30", HFilter.lt("foo", HDate.make("2009-10-30")));
    verifyParse("foo < 08:30:00", HFilter.lt("foo", HTime.make("08:30:00")));
    verifyParse("foo < 13:00:00", HFilter.lt("foo", HTime.make("13:00:00")));
  }

  @Test
  public void testRef()
  {
    verifyParse("author == @xyz", HFilter.eq("author", HRef.make("xyz")));
    verifyParse("author==@xyz:foo.bar", HFilter.eq("author", HRef.make("xyz:foo.bar")));
  }

  @Test
  public void testAnd()
  {
    verifyParse("a and b", HFilter.has("a").and(HFilter.has("b")));
    verifyParse("a and b and c == 3", HFilter.has("a").and( HFilter.has("b").and(HFilter.eq("c", n(3))) ));
  }

  @Test
  public void testOr()
  {
    verifyParse("a or b", HFilter.has("a").or(HFilter.has("b")));
    verifyParse("a or b or c == 3", HFilter.has("a").or(HFilter.has("b").or(HFilter.eq("c", n(3)))));
  }

  @Test
  public void testParens()
  {
    verifyParse("(a)", HFilter.has("a"));
    verifyParse("(a) and (b)", HFilter.has("a").and(HFilter.has("b")));
    verifyParse("( a )  and  ( b ) ", HFilter.has("a").and(HFilter.has("b")));
    verifyParse("(a or b) or (c == 3)", HFilter.has("a").or(HFilter.has("b")).or(HFilter.eq("c", n(3))));
  }

  @Test
  public void testCombo()
  {
    HFilter isA = HFilter.has("a");
    HFilter isB = HFilter.has("b");
    HFilter isC = HFilter.has("c");
    HFilter isD = HFilter.has("d");
    verifyParse("a and b or c", (isA.and(isB)).or(isC));
    verifyParse("a or b and c", isA.or(isB.and(isC)));
    verifyParse("a and b or c and d", (isA.and(isB)).or(isC.and(isD)));
    verifyParse("(a and (b or c)) and d", isA.and(isB.or(isC)).and(isD));
    verifyParse("(a or (b and c)) or d", isA.or(isB.and(isC)).or(isD));
  }

  void verifyParse(String s, HFilter expected)
  {
    HFilter actual = HFilter.make(s);
    assertEquals(actual, expected);
  }

  @Test
  public void testInclude()
  {
    HDict a = new HDictBuilder()
      .add("dis", "a")
      .add("num", 10)
      .add("date", HDate.make(2016,1,1))
      .add("foo", "baz")
      .toDict();

    HDict b = new HDictBuilder()
      .add("dis", "b")
      .add("num", 20)
      .add("date", HDate.make(2016,1,2))
      .add("foo", 12)
      .add("ref", HRef.make("a"))
      .toDict();

    HDict c = new HDictBuilder()
      .add("dis", "c")
      .add("num", 30)
      .add("date", HDate.make(2016,1,3))
      .add("foo", 13)
      .add("ref", HRef.make("b"))
      .add("thru", "c")
      .toDict();

    HDict d = new HDictBuilder()
      .add("dis", "d")
      .add("num", 30)
      .add("date", HDate.make(2016,1,3))
      .add("ref", HRef.make("c"))
      .toDict();

    HDict e = new HDictBuilder()
      .add("dis", "e")
      .add("num", 40)
      .add("date", HDate.make(2016,1,6))
      .add("ref", new HDictBuilder().add("thru", "e").toDict())
      .toDict();

    final HashMap<String, HDict> db = new HashMap<String, HDict>();
    db.put("a", a);
    db.put("b", b);
    db.put("c", c);
    db.put("d", d);
    db.put("e", e);

    verifyInclude(db, "ref->thru", "d,e");

    verifyInclude(db, "dis",            "a,b,c,d,e");
    verifyInclude(db, "foo",            "a,b,c");

    verifyInclude(db, "not dis",        "");
    verifyInclude(db, "not foo",        "d,e");

    verifyInclude(db, "dis == \"c\"",     "c");
    verifyInclude(db, "num == 30",        "c,d");
    verifyInclude(db, "date==2016-01-02", "b");
    verifyInclude(db, "foo==12",          "b");

    verifyInclude(db, "dis != \"c\"",       "a,b,d,e");
    verifyInclude(db, "num != 30",          "a,b,e");
    verifyInclude(db, "date != 2016-01-02", "a,c,d,e");
    verifyInclude(db, "foo != 13",          "a,b");

    verifyInclude(db, "dis < \"c\"",        "a,b");
    verifyInclude(db, "num < 20",           "a");
    verifyInclude(db, "date < 2016-01-04",  "a,b,c,d");
    verifyInclude(db, "foo < 13",           "b");
    verifyInclude(db, "foo < \"c\"",        "a");

    verifyInclude(db, "dis <= \"c\"",       "a,b,c");
    verifyInclude(db, "num <= 20",          "a,b");
    verifyInclude(db, "date <= 2016-01-02", "a,b");
    verifyInclude(db, "foo <= 13",          "b,c");
    verifyInclude(db, "foo <= \"baz\"",     "a");

    verifyInclude(db, "dis > \"c\"",       "d,e");
    verifyInclude(db, "num > 20",          "c,d,e");
    verifyInclude(db, "date > 2016-01-02", "c,d,e");
    verifyInclude(db, "foo > 12",          "c");
    verifyInclude(db, "foo > \"a\"",       "a");

    verifyInclude(db, "dis >= \"c\"",       "c,d,e");
    verifyInclude(db, "num >= 20",          "b,c,d,e");
    verifyInclude(db, "date >= 2016-01-02", "b,c,d,e");
    verifyInclude(db, "foo >= 12",          "b,c");
    verifyInclude(db, "foo >= \"baz\"",     "a");

    verifyInclude(db, "dis==\"c\" or num == 30",  "c,d");
    verifyInclude(db, "dis==\"c\" and num == 30", "c");
    verifyInclude(db, "dis==\"c\" or num == 30 or dis==\"b\"", "b,c,d");
    verifyInclude(db, "dis==\"c\" and num == 30 and foo==13",  "c");
    verifyInclude(db, "dis==\"c\" and num == 30 and foo==12",  "");
    verifyInclude(db, "dis==\"c\" and num == 30 or foo==12",   "b,c");
    verifyInclude(db, "(dis==\"c\" or num == 30) and not foo", "d");
    verifyInclude(db, "(num == 30 and foo) or (num <= 10)",    "a,c");

    verifyInclude(db, "ref->dis == \"a\"", "b");
    verifyInclude(db, "ref->ref->dis == \"a\"", "c");
    verifyInclude(db, "ref->ref->ref->dis == \"a\"", "d");
    verifyInclude(db, "ref->num <= 20", "b,c");
    verifyInclude(db, "ref->thru", "d,e");
    verifyInclude(db, "ref->thru == \"e\"", "e");
  }

  void verifyInclude(final HashMap<String, HDict> map, String query, String expected)
  {
    HFilter.Pather db = new HFilter.Pather()
    {
      public HDict find(String id) { return map.get(id); }
    };

    HFilter q = HFilter.make(query);

    String actual = "";
    for (int c='a'; c<='e'; ++c)
    {
      String id = "" + (char)c;
      if (q.include(db.find(id), db))
        actual += actual.length() > 0 ? ","+id : id;
    }
    assertEquals(actual, expected);
  }

  @Test
  public void testPath()
  {
    // single name
    HFilter.Path path = HFilter.Path.make("foo");
    assertEquals(path.size(), 1);
    assertEquals(path.get(0), "foo");
    assertEquals(path.toString(), "foo");
    assertEquals(path, HFilter.Path.make("foo"));

    // two names
    path = HFilter.Path.make("foo->bar");
    assertEquals(path.size(), 2);
    assertEquals(path.get(0), "foo");
    assertEquals(path.get(1), "bar");
    assertEquals(path.toString(), "foo->bar");
    assertEquals(path, HFilter.Path.make("foo->bar"));

    // three names
    path = HFilter.Path.make("x->y->z");
    assertEquals(path.size(), 3);
    assertEquals(path.get(0), "x");
    assertEquals(path.get(1), "y");
    assertEquals(path.get(2), "z");
    assertEquals(path.toString(), "x->y->z");
    assertEquals(path, HFilter.Path.make("x->y->z"));
  }
}
