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
 * ValTest tests the scalar value HVal types
 */
public class ValTest extends Test
{
  public void testMarker()
  {
    // equality
    verifyEq(HMarker.VAL, HMarker.VAL);

    // encoding
    verifyEq(HMarker.VAL.write(), "marker");
  }

  public void testBool()
  {
    // equality
    verifyEq(HBool.TRUE, HBool.TRUE);
    verifyNotEq(HBool.TRUE, HBool.FALSE);
    verify(HBool.make(true) == HBool.TRUE);
    verify(HBool.make(false) == HBool.FALSE);

    // compare
    verify(HBool.FALSE.compareTo(HBool.TRUE) < 0);
    verify(HBool.TRUE.compareTo(HBool.TRUE) == 0);

    // encoding
    verifyIO(HBool.TRUE, "true");
    verifyIO(HBool.FALSE, "false");
  }

  public void testNum()
  {
    // equality
    verifyEq(HNum.make(2), HNum.make(2.0, null));
    verifyNotEq(HNum.make(2), HNum.make(2, "%"));
    verifyNotEq(HNum.make(2, "%"), HNum.make(2));
    verify(HNum.make(0) == HNum.make(0.0));

    // compare
    verify(HNum.make(9).compareTo(HNum.make(11)) < 0);
    verify(HNum.make(-3).compareTo(HNum.make(-4)) > 0);
    verify(HNum.make(-23).compareTo(HNum.make(-23)) == 0);

    // encoding
    verifyIO(HNum.make(123), "123");
    verifyIO(HNum.make(123.4, "m/s"), "123.4m/s");
    verifyIO(HNum.make(9.6, "m/s"), "9.6m/s");
    verifyIO(HNum.make(-5.2, "\u00b0F"), "-5.2\u00b0F");
    verifyIO(HNum.make(23, "%"), "23%");
    verifyIO(HNum.make(2.4e-3, "fl_oz"), "0.0024fl_oz");
    verifyIO(HNum.make(2.4e5, "$"), "240000$");
    verifyEq(HVal.read("1234.56fl_oz"), HNum.make(1234.56, "fl_oz"));
    verifyEq(HVal.read("0.000028fl_oz"), HNum.make(0.000028, "fl_oz"));

    // specials
    verifyIO(HNum.make(Double.NEGATIVE_INFINITY), "-INF");
    verifyIO(HNum.make(Double.POSITIVE_INFINITY), "INF");
    verifyIO(HNum.make(Double.NaN), "NaN");

    // verify units never serialized for special values
    verifyEq(HNum.make(Double.NaN, "ignore").write(), "NaN");
    verifyEq(HNum.make(Double.POSITIVE_INFINITY, "%").write(), "INF");
    verifyEq(HNum.make(Double.NEGATIVE_INFINITY, "%").write(), "-INF");

    // verify bad unit names are caught on encoding
    try { HNum.make(123.4, "foo bar").write(); fail(); } catch (IllegalArgumentException e) { verifyException(e); }
    try { HNum.make(123.4, "foo,bar").write(); fail(); } catch (IllegalArgumentException e) { verifyException(e); }
  }

  public void testStr()
  {
    // equality
    verifyEq(HStr.make("a"), HStr.make("a"));
    verifyNotEq(HStr.make("a"), HStr.make("b"));
    verify(HStr.make("") == HStr.make(""));

    // compare
    verify(HStr.make("abc").compareTo(HStr.make("z")) < 0);
    verify(HStr.make("Foo").compareTo(HStr.make("Foo")) == 0);

    // encoding
    verifyIO(HStr.make("hello"), "\"hello\"");
    verifyIO(HStr.make("_ \\ \" \n \r \t \u0011 _"), "\"_ \\\\ \\\" \\n \\r \\t \\u0011 _\"");
    verifyIO(HStr.make("\u0abc"), "\"\u0abc\"");

    // hex upper and lower case
    verifyEq(HVal.read("\"[\\uabcd \\u1234]\""), HStr.make("[\uabcd \u1234]"));
    verifyEq(HVal.read("\"[\\uABCD \\u1234]\""), HStr.make("[\uABCD \u1234]"));
    try {HVal.read("\"end..."); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("\"end...\n\""); fail(); } catch (ParseException e) { verifyException(e); }
    try {HVal.read("\"\\u1x34\""); fail(); } catch (ParseException e) { verifyException(e); }
    try {HVal.read("\"hi\" "); fail(); } catch (ParseException e) { verifyException(e); }
  }

  public void testUri()
  {
    // equality
    verifyEq(HUri.make("a"), HUri.make("a"));
    verifyNotEq(HUri.make("a"), HUri.make("b"));
    verify(HUri.make("") == HUri.make(""));

    // compare
    verify(HUri.make("abc").compareTo(HUri.make("z")) < 0);
    verify(HUri.make("Foo").compareTo(HUri.make("Foo")) == 0);

    // encoding
    verifyIO(HUri.make("http://foo.com/f?q"), "`http://foo.com/f?q`");

    // errors
    try {HUri.make("`bad`").write(); fail(); } catch (IllegalArgumentException e) { verifyException(e); }
    try {HVal.read("`no end"); fail(); } catch (ParseException e) { verifyException(e); }
    try {HVal.read("`new\nline`"); fail(); } catch (ParseException e) { verifyException(e); }
  }

  public void testRef()
  {
    // equality (ignore dis)
    verifyEq(HRef.make("foo"), HRef.make("foo"));
    verifyEq(HRef.make("foo"), HRef.make("foo", "Foo"));
    verifyNotEq(HRef.make("foo"), HRef.make("Foo"));

    // encoding
    verifyIO(HRef.make("1234-5678.foo:bar"), "@1234-5678.foo:bar");
    verifyIO(HRef.make("1234-5678", "Foo Bar"), "@1234-5678 \"Foo Bar\"");
    verifyIO(HRef.make("1234-5678", "Foo \"Bar\""), "@1234-5678 \"Foo \\\"Bar\\\"\"");

    // verify bad refs are caught on encoding
    try { HRef.make("@a").write(); fail(); } catch (Exception e) { verify(true); }
    try { HRef.make("a b").write(); fail(); } catch (Exception e) { verify(true); }
    try { HRef.make("a\n").write(); fail(); } catch (Exception e) { verify(true); }
    try {HVal.read("@"); fail(); } catch (Exception e) { verifyException(e); }
  }

  public void testDate()
  {
    // equality
    verifyEq(HDate.make(2011, 6, 7), HDate.make(2011, 6, 7));
    verifyNotEq(HDate.make(2011, 6, 7), HDate.make(2011, 6, 8));
    verifyNotEq(HDate.make(2011, 6, 7), HDate.make(2011, 2, 7));
    verifyNotEq(HDate.make(2011, 6, 7), HDate.make(2009, 6, 7));

    // compare
    verify(HDate.make(2011, 6, 9).compareTo(HDate.make(2011, 6, 21)) < 0);
    verify(HDate.make(2011, 10, 9).compareTo(HDate.make(2011, 3, 21)) > 0);
    verify(HDate.make(2010, 6, 9).compareTo(HDate.make(2000, 9, 30)) > 0);
    verify(HDate.make(2010, 6, 9).compareTo(HDate.make(2010, 6, 9))  == 0);

    // plus/minus
    verifyEq(HDate.make(2011, 12, 1).minusDays(0), HDate.make(2011, 12, 1));
    verifyEq(HDate.make(2011, 12, 1).minusDays(1), HDate.make(2011, 11, 30));
    verifyEq(HDate.make(2011, 12, 1).minusDays(-2), HDate.make(2011, 12, 3));
    verifyEq(HDate.make(2011, 12, 1).plusDays(2), HDate.make(2011, 12, 3));
    verifyEq(HDate.make(2011, 12, 1).plusDays(31), HDate.make(2012, 1, 1));
    verifyEq(HDate.make(2008, 3, 3).minusDays(3), HDate.make(2008, 2, 29));
    verifyEq(HDate.make(2008, 3, 3).minusDays(4), HDate.make(2008, 2, 28));

    // encoding
    verifyIO(HDate.make(2011, 6, 7), "2011-06-07");
    verifyIO(HDate.make(2011,10,10), "2011-10-10");
    verifyIO(HDate.make(2011,12,31), "2011-12-31");
    try {HVal.read("2003-xx-02"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("2003-02"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("2003-02-xx"); fail(); } catch (Exception e) { verifyException(e); }
  }

  public void testTime()
  {
    // equality
    verifyEq(HTime.make(1, 2, 3, 4), HTime.make(1, 2, 3, 4));
    verifyNotEq(HTime.make(1, 2, 3, 4), HTime.make(9, 2, 3, 4));
    verifyNotEq(HTime.make(1, 2, 3, 4), HTime.make(1, 9, 3, 4));
    verifyNotEq(HTime.make(1, 2, 3, 4), HTime.make(1, 2, 9, 9));

    // compare
    verify(HTime.make(0, 0, 0, 0).compareTo(HTime.make(0, 0, 0, 9)) < 0);
    verify(HTime.make(0, 0, 0, 0).compareTo(HTime.make(0, 0, 1, 0)) < 0);
    verify(HTime.make(0, 1, 0, 0).compareTo(HTime.make(0, 0, 0, 0)) > 0);
    verify(HTime.make(0, 0, 0, 0).compareTo(HTime.make(2, 0, 0, 0)) < 0);
    verify(HTime.make(2, 0, 0, 0).compareTo(HTime.make(2, 0, 0, 0)) == 0);

    // encoding
    verifyIO(HTime.make(2, 3), "02:03:00");
    verifyIO(HTime.make(2, 3, 4), "02:03:04");
    verifyIO(HTime.make(2, 3, 4, 5), "02:03:04.005");
    verifyIO(HTime.make(2, 3, 4, 56), "02:03:04.056");
    verifyIO(HTime.make(2, 3, 4, 109), "02:03:04.109");
    verifyIO(HTime.make(2, 3, 10, 109), "02:03:10.109");
    verifyIO(HTime.make(2, 10, 59), "02:10:59");
    verifyIO(HTime.make(10, 59, 30), "10:59:30");
    verifyIO(HTime.make(23, 59, 59, 999), "23:59:59.999");

    try {HVal.read("3:20:00"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("13:xx:00"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("13:45:0x"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("13:45:00.4561"); fail(); } catch (Exception e) { verifyException(e); }
  }

  public void testTz()
  {
    verifyTz("New_York", "America/New_York");
    verifyTz("Chicago",  "America/Chicago");
    verifyTz("Phoenix",  "America/Phoenix");
    verifyTz("London",   "Europe/London");
    verifyTz("UTC",      "Etc/UTC");
  }

  private void verifyTz(String name, String javaId)
  {
    HTimeZone tz = HTimeZone.make(name);
    TimeZone java = TimeZone.getTimeZone(javaId);
    verifyEq(tz.name, name);
    verifyEq(tz.java, java);
    verifyEq(tz, HTimeZone.make(java));
  }

  public void testDateTime()
  {
    // equality
    HTimeZone utc = HTimeZone.UTC;
    HTimeZone london = HTimeZone.make("London");

    verifyEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2009, 1, 2, 3, 4, 5, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 9, 2, 3, 4, 5, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 9, 3, 4, 5, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 9, 4, 5, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 9, 5, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 9, utc, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 5, london, 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0), HDateTime.make(2011, 1, 2, 3, 4, 5, london, 3600));

    // compare
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0)) == 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 4, 6, utc, 0)) < 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 5, 5, utc, 0)) < 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 4, 4, 5, utc, 0)) < 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 3, 3, 4, 5, utc, 0)) < 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 2, 2, 3, 4, 5, utc, 0)) < 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2012, 1, 2, 3, 4, 5, utc, 0)) < 0);
    verify(HDateTime.make(2011, 1, 2, 3, 4, 5, utc, 0).compareTo(HDateTime.make(2011, 1, 2, 3, 4, 0, utc, 0)) > 0);

    // encoding
    HDateTime ts = HDateTime.make(1307377618069L, HTimeZone.make("New_York"));
    verifyIO(ts, "2011-06-06T12:26:58.069-04:00 New_York");
    verifyEq(ts.date.write(), "2011-06-06");
    verifyEq(ts.time.write(), "12:26:58.069");
    verifyEq(ts.tzOffset, -4*60*60);
    verifyEq(ts.tz.name, "New_York");
    verifyEq(ts.tz.java.getID(), "America/New_York");
    verifyEq(ts.millis(), 1307377618069L);

    // convert back to millis
    ts = HDateTime.make(ts.date, ts.time, ts.tz, ts.tzOffset);
    verifyEq(ts.millis(), 1307377618069L);

    // different timezones
    ts = HDateTime.make(949478640000L, HTimeZone.make("New_York"));
    verifyIO(ts, "2000-02-02T03:04:00-05:00 New_York");
    ts = HDateTime.make(949478640000L, HTimeZone.make("UTC"));
    verifyIO(ts, "2000-02-02T08:04:00Z UTC");
    ts = HDateTime.make(949478640000L, HTimeZone.make("Taipei"));
    verifyIO(ts, "2000-02-02T16:04:00+08:00 Taipei");
    verifyIO(HDateTime.make(2011, 6, 7, 11, 3, 43, HTimeZone.make("GMT+10"), -36000),
             "2011-06-07T11:03:43-10:00 GMT+10");
    verifyIO(HDateTime.make(HDate.make(2011, 6, 8), HTime.make(4, 7, 33, 771), HTimeZone.make("GMT-7"), 25200),
             "2011-06-08T04:07:33.771+07:00 GMT-7");

    // errors
    try {HVal.read("2000-02-02T03:04:00-0x:00 New_York"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("2000-02-02T03:04:00-05 New_York"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("2000-02-02T03:04:00-05:!0 New_York"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("2000-02-02T03:04:00-05:00"); fail(); } catch (Exception e) { verifyException(e); }
    try {HVal.read("2000-02-02T03:04:00-05:00 @"); fail(); } catch (Exception e) { verifyException(e); }
  }

  public void testMidnight()
  {
    verifyMidnight(HDate.make(2011, 11, 3),  "UTC",      "2011-11-03T00:00:00Z UTC");
    verifyMidnight(HDate.make(2011, 11, 3),  "New_York", "2011-11-03T00:00:00-04:00 New_York");
    verifyMidnight(HDate.make(2011, 12, 15), "Chicago",  "2011-12-15T00:00:00-06:00 Chicago");
    verifyMidnight(HDate.make(2008, 2, 29),  "Phoenix",  "2008-02-29T00:00:00-07:00 Phoenix");
  }

  private void verifyMidnight(HDate date, String tzName, String str)
  {
    HDateTime ts = date.midnight(HTimeZone.make(tzName));
    verifyEq(ts.date, date);
    verifyEq(ts.time.hour, 0);
    verifyEq(ts.time.min,  0);
    verifyEq(ts.time.sec,  0);
    verifyEq(ts.write(), str);
    verifyEq(ts, HVal.read(ts.write()));
    verifyEq(ts.millis(), ((HDateTime)HVal.read(str)).millis());
  }

  public void testRange()
  {
    HTimeZone ny = HTimeZone.make("New_York");
    HDate today = HDate.today();
    HDate yesterday = today.minusDays(1);
    HDate x = HDate.make(2011, 7, 4);
    HDate y = HDate.make(2011, 11, 4);
    HDateTime xa = HDateTime.make(x, HTime.make(2, 30), ny);
    HDateTime xb = HDateTime.make(x, HTime.make(22, 5), ny);

    verifyRange(HDateTimeRange.read("today", ny), today, today);
    verifyRange(HDateTimeRange.read("yesterday", ny), yesterday, yesterday);
    verifyRange(HDateTimeRange.read("2011-07-04", ny), x, x);
    verifyRange(HDateTimeRange.read("2011-07-04,2011-11-04", ny), x, y);
    verifyRange(HDateTimeRange.read(""+xa+","+xb, ny), xa, xb);

    HDateTimeRange r = HDateTimeRange.read(xb.write(), ny);
    verifyEq(r.start, xb);
    verifyEq(r.end.date, today);
    verifyEq(r.end.tz, ny);
  }

  private void verifyRange(HDateTimeRange r, HDate start, HDate end)
  {
    verifyEq(r.start.date,    start);
    verifyEq(r.start.time,    HTime.MIDNIGHT);
    verifyEq(r.start.tz.name, "New_York");
    verifyEq(r.end.date,      end.plusDays(1));
    verifyEq(r.end.time,      HTime.MIDNIGHT);
    verifyEq(r.end.tz.name,   "New_York");
  }

  private void verifyRange(HDateTimeRange r, HDateTime start, HDateTime end)
  {
    verifyEq(r.start, start);
    verifyEq(r.end, end);
  }

  public void verifyIO(HVal val, String s)
  {
    // println("  :: " + s);
    // println("     " + HVal.read(s));
    verifyEq(val.write(), s);
    verifyEq(HVal.read(s), val);
  }
}