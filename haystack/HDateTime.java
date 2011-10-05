//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * HDateTime models a timestamp with a specific timezone.
 */
public class HDateTime extends HVal
{
  /** Constructor with basic fields */
  public static HDateTime make(HDate date, HTime time, String tz, int tzOffset)
  {
    if (date == null || time == null || tz == null) throw new IllegalArgumentException("null args");
    return new HDateTime(date, time, tz, tzOffset);
  }

  /** Constructor with date and time (to sec) fields */
  public static HDateTime make(int year, int month, int day, int hour, int min, int sec, String tz, int tzOffset)
  {
    return make(HDate.make(year, month, day), HTime.make(hour, min, sec), tz, tzOffset);
  }

  /** Constructor with date and time (to min) fields */
  public static HDateTime make(int year, int month, int day, int hour, int min, String tz, int tzOffset)
  {
    return make(HDate.make(year, month, day), HTime.make(hour, min), tz, tzOffset);
  }

  /** Constructor with Java millis since Java epoch and local timezone */
  public static HDateTime make(long millis)
  {
    return make(millis, TimeZone.getDefault());
  }

  /** Constructor with Java millis and Java TimeZone instance */
  public static HDateTime make(long millis, TimeZone tz)
  {
    // use calendar to decode millis to fields
    Calendar c = new GregorianCalendar(tz);
    c.setTimeInMillis(millis);

    // tzOffset
    int tzOffset = c.get(Calendar.ZONE_OFFSET) / 1000 + c.get(Calendar.DST_OFFSET) / 1000;

    // tz city name
    String tzName = tz.getID();
    int slash = tzName.indexOf('/');
    if (slash > 0) tzName = tzName.substring(slash+1);

    HDateTime ts = new HDateTime(HDate.make(c), HTime.make(c), tzName, tzOffset);
    ts.millis = millis;
    return ts;
  }

  /** Private constructor */
  private HDateTime(HDate date, HTime time, String tz, int tzOffset)
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

  /** Timezone name as Olson database city name */
  public final String tz;

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

  /** Encode value to string format */
  public void write(StringBuilder s)
  {
    date.write(s);
    s.append('T');
    time.write(s);
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