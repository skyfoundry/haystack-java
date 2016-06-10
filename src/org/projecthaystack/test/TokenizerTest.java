//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 June 2016  Matthew Giannini   Creation
//
package org.projecthaystack.test;

import org.projecthaystack.*;
import org.projecthaystack.io.HaystackToken;
import org.projecthaystack.io.HaystackTokenizer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TokenizerTest
 */
public class TokenizerTest extends Test
{
  public void test()
  {
    HaystackToken id   = HaystackToken.id;
    HaystackToken num  = HaystackToken.num;
    HaystackToken date = HaystackToken.date;
    HaystackToken time = HaystackToken.time;
    HaystackToken dt   = HaystackToken.dateTime;
    HaystackToken str  = HaystackToken.str;
    HaystackToken ref  = HaystackToken.ref;
    HaystackToken uri  = HaystackToken.uri;

    // empty
    verifyToks("", new Object[] {,});

    // identifiers
    verifyToks("x", new Object[] {id, "x"});
    verifyToks("fooBar", new Object[] {id, "fooBar"});
    verifyToks("fooBar1999x", new Object[] {id, "fooBar1999x"});
    verifyToks("foo_23", new Object[] {id, "foo_23"});
    verifyToks("Foo", new Object[] {id, "Foo"});

    // ints
    verifyToks("5", new Object[] {num, n(5) });
    verifyToks("0x1234_abcd", new Object[] {num, n(0x1234abcd) });

    // floats
    verifyToks("5.0", new Object[] {num, n(5d)});
    verifyToks("5.42", new Object[] {num, n(5.42d)});
    verifyToks("123.2e32", new Object[] {num, n(123.2e32d)});
    verifyToks("123.2e+32", new Object[] {num, n(123.2e32d)});
    verifyToks("2_123.2e+32", new Object[] {num, n(2123.2e32d)});
    verifyToks("4.2e-7", new Object[] {num, n(4.2e-7d)});

    // number with units
    verifyToks("-40ms", new Object[] {num, n(-40, "ms")});
    verifyToks("1sec", new Object[] {num, n(1, "sec")});
    verifyToks("5hr", new Object[] {num, n(5, "hr")});
    verifyToks("2.5day", new Object[] {num, n(2.5d, "day")});
    verifyToks("12%", new Object[] {num, n(12, "%")});
    verifyToks("987_foo", new Object[] {num, n(987, "_foo")});
    verifyToks("-1.2m/s", new Object[] {num, n(-1.2d, "m/s")});
    verifyToks("12kWh/ft\u00B2", new Object[] {num, n(12, "kWh/ft\u00B2")});
    verifyToks("3_000.5J/kg_dry", new Object[] {num, n(3000.5d, "J/kg_dry")});

    // strings
    verifyToks("\"\"", new Object[] {str, HStr.make("")});
    verifyToks("\"x y\"", new Object[] {str, HStr.make("x y")});
    verifyToks("\"x\\\"y\"", new Object[] {str, HStr.make("x\"y")});
    verifyToks("\"_\\u012f \\n \\t \\\\_\"", new Object[] {str, HStr.make("_\u012f \n \t \\_")});

    // date
    verifyToks("2016-06-06", new Object[] {date, HDate.make(2016, 6, 6)});

    // time
    verifyToks("8:30", new Object[] {time, HTime.make(8,30)});
    verifyToks("20:15", new Object[] {time, HTime.make(20,15)});
    verifyToks("00:00", new Object[] {time, HTime.make(0,0)});
    verifyToks("00:00:00", new Object[] {time, HTime.make(0,0,0)});
    verifyToks("01:02:03", new Object[] {time, HTime.make(1,2,3)});
    verifyToks("23:59:59", new Object[] {time, HTime.make(23,59,59)});
    verifyToks("12:00:12.9", new Object[] {time, HTime.make(12,00,12,900)});
    verifyToks("12:00:12.99", new Object[] {time, HTime.make(12,00,12,990)});
    verifyToks("12:00:12.999", new Object[] {time, HTime.make(12,00,12,999)});
    verifyToks("12:00:12.000", new Object[] {time, HTime.make(12,00,12,0)});
    verifyToks("12:00:12.001", new Object[] {time, HTime.make(12,00,12,1)});

    // datetime
    HTimeZone ny = HTimeZone.make("New_York");
    HTimeZone utc = HTimeZone.UTC;
    HTimeZone london = HTimeZone.make("London");
    verifyToks("2016-01-13T09:51:33-05:00 New_York", new Object[] {dt, HDateTime.make(2016,1,13,9,51,33,ny,tzOffset(-5,0))});
    verifyToks("2016-01-13T09:51:33.353-05:00 New_York", new Object[] {dt, HDateTime.make(HDate.make(2016,1,13), HTime.make(9,51,33,353), ny, tzOffset(-5,0))});
    verifyToks("2010-12-18T14:11:30.924Z", new Object[] {dt, HDateTime.make(HDate.make(2010,12,18), HTime.make(14,11,30,924), utc)});
    verifyToks("2010-12-18T14:11:30.924Z UTC", new Object[] {dt, HDateTime.make(HDate.make(2010,12,18), HTime.make(14,11,30,924), utc)});
    verifyToks("2010-12-18T14:11:30.924Z London", new Object[] {dt, HDateTime.make(HDate.make(2010,12,18), HTime.make(14,11,30,924), london)});
    // Apparently PST8PDT is not valid in java?
//    verifyToks("2015-01-02T06:13:38.701-08:00 PST8PDT", new Object[] {dt, HDateTime.make(HDate.make(2015,1,2), HTime.make(6,13,38,701), HTimeZone.make("PST8PDT"), tzOffset(-8,0))});
    verifyToks("2010-03-01T23:55:00.013-05:00 GMT+5", new Object[] {dt, HDateTime.make(HDate.make(2010,3,1), HTime.make(23,55,0,13), HTimeZone.make("GMT+5"), tzOffset(-5,0))});
    verifyToks("2010-03-01T23:55:00.013+10:00 GMT-10 ", new Object[] {dt, HDateTime.make(HDate.make(2010,3,1), HTime.make(23,55,0,13), HTimeZone.make("GMT-10"), tzOffset(10,0))});

    // ref
    verifyToks("@125b780e-0684e169", new Object[] {ref, HRef.make("125b780e-0684e169")});
    verifyToks("@demo:_:-.~", new Object[] {ref, HRef.make("demo:_:-.~")});

    // uri
    verifyToks("`http://foo/`", new Object[] {uri, HUri.make("http://foo/")});
    verifyToks("`_ \\n \\\\ \\`_`", new Object[] {uri, HUri.make("_ \n \\\\ `_")});

    // newlines and whitespaces
    verifyToks("a\n  b   \rc \r\nd\n\ne",
      new Object[] {
        id, "a", HaystackToken.nl, null,
        id, "b", HaystackToken.nl, null,
        id, "c", HaystackToken.nl, null,
        id, "d", HaystackToken.nl, null, HaystackToken.nl, null,
        id, "e"
      });
  }

  private HNum n(long val) { return HNum.make(val); }
  private HNum n(double val) { return HNum.make(val); }
  private HNum n(double val, String unit) { return HNum.make(val, unit); }
  private int tzOffset(int hours, int mins) { return (hours * 3600) + (mins * 60); }

  private void verifyToks(String zinc, Object[] toks)
  {
    List acc = new ArrayList();
    HaystackTokenizer t = new HaystackTokenizer(new StringReader(zinc));
    while (true)
    {
      HaystackToken x = t.next();
      verifyEq(x, t.tok);
      if (x == HaystackToken.eof) break;
      acc.add(t.tok);
      acc.add(t.val);
    }
    Object[] actual = (Object[])acc.toArray(new Object[acc.size()]);
    if (!Arrays.equals(toks, actual))
    {
      System.out.println("expected: " + Arrays.toString(toks));
      System.out.println("actual:   " + Arrays.toString(actual));
    }
  }
}
