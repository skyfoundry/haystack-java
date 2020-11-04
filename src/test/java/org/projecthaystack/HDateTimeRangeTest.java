//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.util.Calendar;

public class HDateTimeRangeTest
{
  @Test
  public void testRange()
  {
    System.out.println("Default timezone is " + HTimeZone.DEFAULT);
    HTimeZone tz = HTimeZone.DEFAULT;
    HDate today = HDate.today();
    HDate yesterday = today.minusDays(1);
    HDate x = HDate.make(2011, 7, 4);
    HDate y = HDate.make(2011, 11, 4);
    HDateTime xa = HDateTime.make(x, HTime.make(2, 30), tz);
    HDateTime xb = HDateTime.make(x, HTime.make(22, 5), tz);

    verifyRange(HDateTimeRange.make("today", tz), today, today);
    verifyRange(HDateTimeRange.make("yesterday", tz), yesterday, yesterday);
    verifyRange(HDateTimeRange.make("2011-07-04", tz), x, x);
    verifyRange(HDateTimeRange.make("2011-07-04,2011-11-04", tz), x, y);
    verifyRange(HDateTimeRange.make(""+xa+","+xb, tz), xa, xb);

    HDateTimeRange r = HDateTimeRange.make(xb.toString(), tz);
    assertEquals(r.start, xb);
    assertEquals(r.end.date, today);
    assertEquals(r.end.tz, tz);

    // this week
    HDate sun = today;
    HDate sat = today;
    while (sun.weekday() > Calendar.SUNDAY) sun = sun.minusDays(1);
    while (sat.weekday() < Calendar.SATURDAY) sat = sat.plusDays(1);
    verifyRange(HDateTimeRange.thisWeek(tz), sun, sat);

    // this month
    HDate first = today;
    HDate last = today;
    while (first.day > 1)  first = first.minusDays(1);
    while (last.day < HDate.daysInMonth(today.year, today.month)) last = last.plusDays(1);
    verifyRange(HDateTimeRange.thisMonth(tz), first, last);

    // this year
    first = HDate.make(today.year, 1, 1);
    last = HDate.make(today.year, 12, 31);
    verifyRange(HDateTimeRange.thisYear(tz), first, last);

    // last week
    HDate prev = today.minusDays(7);
    sun = prev;
    sat = prev;
    while (sun.weekday() > Calendar.SUNDAY) sun = sun.minusDays(1);
    while (sat.weekday() < Calendar.SATURDAY) sat = sat.plusDays(1);
    verifyRange(HDateTimeRange.lastWeek(tz), sun, sat);

    // last month
    last = today;
    while (last.month == today.month) last = last.minusDays(1);
    first = HDate.make(last.year, last.month, 1);
    verifyRange(HDateTimeRange.lastMonth(tz), first, last);

    // last year
    first = HDate.make(today.year-1, 1, 1);
    last = HDate.make(today.year-1, 12, 31);
    verifyRange(HDateTimeRange.lastYear(tz), first, last);
  }

  private void verifyRange(HDateTimeRange r, HDate start, HDate end)
  {
    assertEquals(r.start.date,    start);
    assertEquals(r.start.time,    HTime.MIDNIGHT);
    assertEquals(r.start.tz.name, HTimeZone.DEFAULT.name);
    assertEquals(r.end.date,      end.plusDays(1));
    assertEquals(r.end.time,      HTime.MIDNIGHT);
    assertEquals(r.end.tz.name,   HTimeZone.DEFAULT.name);
  }

  private void verifyRange(HDateTimeRange r, HDateTime start, HDateTime end)
  {
    assertEquals(r.start, start);
    assertEquals(r.end, end);
  }
}
