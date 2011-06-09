//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack;

import java.util.Calendar;

/**
 * HDate models a date (day in year) tag value.
 */
public class HDate extends HVal
{
  /** Construct from basic fields */
  public static HDate make(int year, int month, int day)
  {
    if (year < 1900) throw new IllegalArgumentException("Invalid year");
    if (month < 1 || month > 12) throw new IllegalArgumentException("Invalid month");
    if (day < 1 || day > 31) throw new IllegalArgumentException("Invalid day");
    return new HDate(year, month, day);
  }

  /** Construct from Java calendar instance */
  public static HDate make(Calendar c)
  {
    return new HDate(c.get(Calendar.YEAR),
                     c.get(Calendar.MONTH) + 1,
                     c.get(Calendar.DAY_OF_MONTH));
  }

  /** Private constructor */
  private HDate(int year, int month, int day)
  {
    this.year  = year;
    this.month = month;
    this.day   = day;
  }

  /** Hash is based on year, month, day */
  public int hashCode()
  {
    return (year << 16) ^ (month << 8) ^ day;
  }

    /** Equals is based on year, month, day */
  public boolean equals(Object that)
  {
    if (!(that instanceof HDate)) return false;
    HDate x = (HDate)that;
    return year == x.year && month == x.month && day == x.day;
  }


  /** Four digit year such as 2011 */
  public final int year;

  /** Month as 1-12 (Jan is 1, Dec is 12) */
  public final int month;

  /** Day of month as 1-31 */
  public final int day;

  /** Encode value to string format */
  public void write(StringBuilder s)
  {
    s.append(year).append('-');
    if (month < 10) s.append('0'); s.append(month).append('-');
    if (day < 10) s.append('0');   s.append(day);
  }

}