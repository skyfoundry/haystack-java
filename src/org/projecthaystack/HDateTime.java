//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package org.projecthaystack;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.projecthaystack.io.HZincReader;

/**
 * HDateTime models a timestamp with a specific timezone.
 *
 * @see <a href='http://project-haystack.org/doc/TagModel#tagKinds'>Project Haystack</a>
 */
public class HDateTime extends HVal
{
  /** Constructor with basic fields */
  public static HDateTime make(HDate date, HTime time, HTimeZone tz, int tzOffset)
  {
    if (date == null || time == null || tz == null) throw new IllegalArgumentException("null args");
    return new HDateTime(date, time, tz, tzOffset);
  }

  /** Constructor with date, time, tz, but no tzOffset */
  public static HDateTime make(HDate date, HTime time, HTimeZone tz)
  {
    // use calendar to decode millis to fields
    GregorianCalendar c = new GregorianCalendar(date.year, date.month-1, date.day, time.hour, time.min, time.sec);
    if (time.ms != 0) c.set(Calendar.MILLISECOND, time.ms);
    c.setTimeZone(tz.java);

    // tzOffset
    int tzOffset = c.get(Calendar.ZONE_OFFSET) / 1000 + c.get(Calendar.DST_OFFSET) / 1000;

    // millis
    long millis = c.getTimeInMillis();

    HDateTime ts = new HDateTime(date, time, tz, tzOffset);
    ts.millis = millis;
    return ts;
  }

  /** Constructor with date and time (to sec) fields */
  public static HDateTime make(int year, int month, int day, int hour, int min, int sec, HTimeZone tz, int tzOffset)
  {
    return make(HDate.make(year, month, day), HTime.make(hour, min, sec), tz, tzOffset);
  }

  /** Constructor with date and time (to min) fields */
  public static HDateTime make(int year, int month, int day, int hour, int min, HTimeZone tz, int tzOffset)
  {
    return make(HDate.make(year, month, day), HTime.make(hour, min), tz, tzOffset);
  }

  /** Constructor with Java millis since Java epoch and local timezone */
  public static HDateTime make(long millis)
  {
    return make(millis, HTimeZone.DEFAULT);
  }

  /** Constructor with Java millis and Java TimeZone instance */
  public static HDateTime make(long millis, HTimeZone tz)
  {
    // use calendar to decode millis to fields
    Calendar c = new GregorianCalendar(tz.java);
    c.setTimeInMillis(millis);

    // tzOffset
    int tzOffset = c.get(Calendar.ZONE_OFFSET) / 1000 + c.get(Calendar.DST_OFFSET) / 1000;

    HDateTime ts = new HDateTime(HDate.make(c), HTime.make(c), tz, tzOffset);
    ts.millis = millis;
    return ts;
  }

  /** Parse from string fomat "YYYY-MM-DD'T'hh:mm:ss.FFFz zzzz"
    * or raise ParseException
    */
  public static HDateTime make(String s)
  {
    // date
    int tIdx = s.indexOf('T');
    if (tIdx < 0) throw new ParseException(s);
    HDate date = HDate.make(s.substring(0, tIdx));

    // find timezone offset and name indices.
    // this, in turn, allows us to find the time
    if (s.endsWith("Z"))
      return HDateTime.make(date, HTime.make(s.substring(tIdx+1, s.length() - 1)), HTimeZone.UTC);
    else if (s.endsWith("Z UTC"))
      return HDateTime.make(date, HTime.make(s.substring(tIdx+1, s.length() - "Z UTC".length())), HTimeZone.UTC);

    // have tz offset and name
    int spIdx = s.indexOf(' ');
    if (spIdx < 0) throw new ParseException("Expected time zone name: " + s);
    // work backward from space to find beginning of offset
    int offsetIdx = spIdx-1;
    while (true)
    {
      if (offsetIdx <= tIdx) throw new ParseException("Expected Z or -/+ for timezone offset: " + s);
      char c = s.charAt(offsetIdx);
      if (c == '-' || c == '+' || c == 'Z') break;
      --offsetIdx;
    }
    String offsetStr = s.substring(offsetIdx, spIdx);
    int offset = "Z".equals(offsetStr) ? 0 : parseOffset(offsetStr);
    HTimeZone tz = HTimeZone.make(s.substring(spIdx+1));

    return HDateTime.make(date, HTime.make(s.substring(tIdx+1, offsetIdx)), tz, offset);
  }

  private static int parseOffset(String s)
  {
    if (s.length() != "-HH:MM".length()) throw new ParseException("Invalid tz offset: " + s);
    int sign = s.startsWith("-") ? -1 : 1;
    int tzHours = Integer.parseInt(s.substring(1, 3));
    if (s.charAt(3) != ':') throw new ParseException("Invalid tz offset: " + s);
    int tzMins = Integer.parseInt(s.substring(4));
    return sign * (tzHours * 3600) + (tzMins * 60);
  }

  /** Get HDateTime for current time in default timezone */
  public static HDateTime now()
  {
    return make(System.currentTimeMillis());
  }

  /** Get HDateTime for given timezone */
  public static HDateTime now(HTimeZone tz)
  {
    return make(System.currentTimeMillis(), tz);
  }

  /** Private constructor */
  private HDateTime(HDate date, HTime time, HTimeZone tz, int tzOffset)
  {
    this.date     = date;
    this.time     = time;
    this.tz       = tz;
    this.tzOffset = tzOffset;
  }

  /** Date component of the timestamp */
  public final HDate date;

  /** Time component of the timestamp */
  public final HTime time;

  /** Offset in seconds from UTC including DST offset */
  public final int tzOffset;

  /** Timezone as Olson database city name */
  public final HTimeZone tz;

  /** Get this date time as Java milliseconds since epoch */
  public long millis()
  {
    if (millis <= 0)
    {
      GregorianCalendar c = new GregorianCalendar(date.year, date.month-1, date.day, time.hour, time.min, time.sec);
      c.setTimeZone(utc);
      c.set(Calendar.MILLISECOND, time.ms);
      c.set(Calendar.ZONE_OFFSET, tzOffset*1000);
      millis = c.getTimeInMillis();
    }
    return millis;
  }
  private volatile long millis;

  /** Hash is based on date, time, tzOffset, and tz */
  public int hashCode()
  {
    return date.hashCode() ^ time.hashCode() ^ tzOffset ^ tz.hashCode();
  }

  /** Equals is based on date, time, tzOffset, and tz */
  public boolean equals(Object that)
  {
    if (!(that instanceof HDateTime)) return false;
    HDateTime x = (HDateTime)that;
    return date.equals(x.date) && time.equals(x.time) &&
           tzOffset == x.tzOffset && tz.equals(x.tz);
  }

  /** Comparison based on millis. */
  public int compareTo(Object that)
  {
    long thisMillis = this.millis();
    long thatMillis = ((HDateTime)that).millis();
    if (thisMillis < thatMillis) return -1;
    else if (thisMillis > thatMillis) return 1;
    return 0;
  }

  /** Encode as "t:YYYY-MM-DD'T'hh:mm:ss.FFFz zzzz" */
  public String toJson()
  {
    StringBuffer s = new StringBuffer();
    s.append("t:");
    encode(s);
    return s.toString();
  }

  /** Encode as "YYYY-MM-DD'T'hh:mm:ss.FFFz zzzz" */
  public String toZinc()
  {
    StringBuffer s = new StringBuffer();
    encode(s);
    return s.toString();
  }

  private void encode(StringBuffer s)
  {
    date.encode(s);
    s.append('T');
    time.encode(s);
    if (tzOffset == 0) s.append('Z');
    else
    {
      int offset = this.tzOffset;
      if (offset < 0) { s.append('-'); offset = -offset; }
      else { s.append('+'); }
      int zh = offset / 3600;
      int zm = (offset % 3600) / 60;
      if (zh < 10) s.append('0'); s.append(zh).append(':');
      if (zm < 10) s.append('0'); s.append(zm);
    }
    s.append(' ').append(tz);
  }

  private static final TimeZone utc = TimeZone.getTimeZone("Etc/UTC");

}