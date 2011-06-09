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

    // encoding
    verifyIO(HNum.make(123), "123.0");
    verifyIO(HNum.make(123.4, "m/s"), "123.4m/s");
    verifyIO(HNum.make(-5.2, "\u00b0F"), "-5.2\u00b0F");
    verifyIO(HNum.make(23, "%"), "23.0%");
    verifyIO(HNum.make(2.4e-8, "fl_oz"), "2.4E-8fl_oz");
    verifyIO(HNum.make(2.4e14, "$"), "2.4E14$");
    verifyEq(HVal.read("2.4E14fl_oz"), HNum.make(2.4e14, "fl_oz"));
    verifyEq(HVal.read("2.4e14fl_oz"), HNum.make(2.4e14, "fl_oz"));

    // specials
    verifyIO(HNum.make(Double.NEGATIVE_INFINITY), "-INF");
    verifyIO(HNum.make(Double.POSITIVE_INFINITY), "INF");
    verifyIO(HNum.make(Double.NaN), "NaN");

    // verify units never serialized for special values
    verifyEq(HNum.make(Double.NaN, "ignore").write(), "NaN");
    verifyEq(HNum.make(Double.POSITIVE_INFINITY, "%").write(), "INF");
    verifyEq(HNum.make(Double.NEGATIVE_INFINITY, "%").write(), "-INF");

    // verify bad unit names are caught on encoding
    try { HNum.make(123.4, "foo bar").write(); fail(); } catch (IllegalArgumentException e) { verbose(e.toString()); verify(true); }
    try { HNum.make(123.4, "foo,bar").write(); fail(); } catch (IllegalArgumentException e) { verbose(e.toString()); verify(true); }
  }

  public void testStr()
  {
    // equality
    verifyEq(HStr.make("a"), HStr.make("a"));
    verifyNotEq(HStr.make("a"), HStr.make("b"));
    verify(HStr.make("") == HStr.make(""));

    // encoding
    verifyIO(HStr.make("hello"), "\"hello\"");
    verifyIO(HStr.make("_ \\ \" \n \r \t \u0011 _"), "\"_ \\\\ \\\" \\n \\r \\t \\u0011 _\"");
    verifyIO(HStr.make("\u0abc"), "\"\u0abc\"");

    // hex upper and lower case
    verifyEq(HVal.read("\"[\\uabcd \\u1234]\""), HStr.make("[\uabcd \u1234]"));
    verifyEq(HVal.read("\"[\\uABCD \\u1234]\""), HStr.make("[\uABCD \u1234]"));
    try {HVal.read("\"end..."); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("\"end...\n\""); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
    try {HVal.read("\"\\u1x34\""); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
    try {HVal.read("\"hi\" "); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
  }

  public void testUri()
  {
    // equality
    verifyEq(HUri.make("a"), HUri.make("a"));
    verifyNotEq(HUri.make("a"), HUri.make("b"));
    verify(HUri.make("") == HUri.make(""));

    // encoding
    verifyIO(HUri.make("http://foo.com/f?q"), "`http://foo.com/f?q`");

    // errors
    try {HUri.make("`bad`").write(); fail(); } catch (IllegalArgumentException e) { verbose(e.toString()); verify(true); }
    try {HVal.read("`no end"); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
    try {HVal.read("`new\nline`"); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
  }

  public void testRef()
  {
    // equality (ignore dis)
    verifyEq(HRef.make("foo"), HRef.make("foo"));
    verifyEq(HRef.make("foo"), HRef.make("foo", "Foo"));
    verifyNotEq(HRef.make("foo"), HRef.make("Foo"));

    // encoding
    verifyIO(HRef.make("1234-5678"), "<1234-5678>");
    verifyIO(HRef.make("1234-5678", "Foo Bar"), "<1234-5678> \"Foo Bar\"");
    verifyIO(HRef.make("1234-5678", "Foo \"Bar\""), "<1234-5678> \"Foo \\\"Bar\\\"\"");

    // verify bad refs are caught on encoding
    try { HRef.make("<a>").write(); fail(); } catch (Exception e) { verify(true); }
    try { HRef.make("a\n").write(); fail(); } catch (Exception e) { verify(true); }
    try {HVal.read("<end..."); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("<end...\n>"); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
    try {HVal.read("<end> <>"); fail(); } catch (ParseException e) { verbose(e.toString()); verify(true); }
  }

  public void testDate()
  {
    // equality
    verifyEq(HDate.make(2011, 6, 7), HDate.make(2011, 6, 7));
    verifyNotEq(HDate.make(2011, 6, 7), HDate.make(2011, 6, 8));
    verifyNotEq(HDate.make(2011, 6, 7), HDate.make(2011, 2, 7));
    verifyNotEq(HDate.make(2011, 6, 7), HDate.make(2009, 6, 7));

    // encoding
    verifyIO(HDate.make(2011, 6, 7), "2011-06-07");
    verifyIO(HDate.make(2011,10,10), "2011-10-10");
    verifyIO(HDate.make(2011,12,31), "2011-12-31");
    try {HVal.read("2003-xx-02"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("2003-02"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("2003-02-xx"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
  }

  public void testTime()
  {
    // equality
    verifyEq(HTime.make(1, 2, 3, 4), HTime.make(1, 2, 3, 4));
    verifyNotEq(HTime.make(1, 2, 3, 4), HTime.make(9, 2, 3, 4));
    verifyNotEq(HTime.make(1, 2, 3, 4), HTime.make(1, 9, 3, 4));
    verifyNotEq(HTime.make(1, 2, 3, 4), HTime.make(1, 2, 9, 9));

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

    try {HVal.read("3:20:00"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("13:xx:00"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("13:45:0x"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("13:45:00.4561"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
  }

  public void testDateTime()
  {
    // equality
    verifyEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2009, 1, 2, 3, 4, 5, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 9, 2, 3, 4, 5, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 9, 3, 4, 5, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 2, 9, 4, 5, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 2, 3, 9, 5, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 2, 3, 4, 9, "UTC", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 2, 3, 4, 5, "London", 0));
    verifyNotEq(HDateTime.make(2011, 1, 2, 3, 4, 5, "UTC", 0), HDateTime.make(2011, 1, 2, 3, 4, 5, "London", 3600));

    // encoding
    HDateTime ts = HDateTime.make(1307377618069L, TimeZone.getTimeZone("America/New_York"));
    verifyIO(ts, "2011-06-06T12:26:58.069-04:00 New_York");
    verifyEq(ts.date.write(), "2011-06-06");
    verifyEq(ts.time.write(), "12:26:58.069");
    verifyEq(ts.tzOffset, -4*60*60);
    verifyEq(ts.tz, "New_York");
    verifyEq(ts.millis(), 1307377618069L);

    // convert back to millis
    ts = HDateTime.make(ts.date, ts.time, ts.tz, ts.tzOffset);
    verifyEq(ts.millis(), 1307377618069L);

    // different timezones
    ts = HDateTime.make(949478640000L, TimeZone.getTimeZone("America/New_York"));
    verifyIO(ts, "2000-02-02T03:04:00-05:00 New_York");
    ts = HDateTime.make(949478640000L, TimeZone.getTimeZone("Etc/UTC"));
    verifyIO(ts, "2000-02-02T08:04:00Z UTC");
    ts = HDateTime.make(949478640000L, TimeZone.getTimeZone("Asia/Taipei"));
    verifyIO(ts, "2000-02-02T16:04:00+08:00 Taipei");
    verifyIO(HDateTime.make(2011, 6, 7, 11, 3, 43, "GMT+10", -36000),
             "2011-06-07T11:03:43-10:00 GMT+10");
    verifyIO(HDateTime.make(HDate.make(2011, 6, 8), HTime.make(4, 7, 33, 771), "GMT-7", 25200),
             "2011-06-08T04:07:33.771+07:00 GMT-7");

    // errors
    try {HVal.read("2000-02-02T03:04:00-0x:00 New_York"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("2000-02-02T03:04:00-05 New_York"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("2000-02-02T03:04:00-05:!0 New_York"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("2000-02-02T03:04:00-05:00"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
    try {HVal.read("2000-02-02T03:04:00-05:00 @"); fail(); } catch (Exception e) { verbose(e.toString()); verify(true); }
  }

  public void verifyIO(HVal val, String s)
  {
    // println("  :: " + s);
    // println("     " + HVal.read(s));
    verifyEq(val.write(), s);
    verifyEq(HVal.read(s), val);
  }
}