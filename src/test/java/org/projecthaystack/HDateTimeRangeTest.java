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

import java.util.Calendar;

public class HDateTimeRangeTest
{
  @Test
  public void testRange()
  {
    HTimeZone ny = HTimeZone.make("New_York");
    HDate today = HDate.today();
    HDate yesterday = today.minusDays(1);
    HDate x = HDate.make(2011, 7, 4);
    HDate y = HDate.make(2011, 11, 4);
    HDateTime xa = HDateTime.make(x, HTime.make(2, 30), ny);
    HDateTime xb = HDateTime.make(x, HTime.make(22, 5), ny);

    verifyRange(HDateTimeRange.make("today", ny), today, today);
    verifyRange(HDateTimeRange.make("yesterday", ny), yesterday, yesterday);
    verifyRange(HDateTimeRange.make("2011-07-04", ny), x, x);
    verifyRange(HDateTimeRange.make("2011-07-04,2011-11-04", ny), x, y);
    verifyRange(HDateTimeRange.make(""+xa+","+xb, ny), xa, xb);

    HDateTimeRange r = HDateTimeRange.make(xb.toString(), ny);
    assertEquals(r.start, xb);
    assertEquals(r.end.date, today);
    assertEquals(r.end.tz, ny);

    // this week
    HDate sun = today;
    HDate sat = today;
    while (sun.weekday() > Calendar.SUNDAY) sun = sun.minusDays(1);
    while (sat.weekday() < Calendar.SATURDAY) sat = sat.plusDays(1);
    verifyRange(HDateTimeRange.thisWeek(ny), sun, sat);

    // this month
    HDate first = today;
    HDate last = today;
    while (first.day > 1)  first = first.minusDays(1);
    while (last.day < HDate.daysInMonth(today.year, today.month)) last = last.plusDays(1);
    verifyRange(HDateTimeRange.thisMonth(ny), first, last);

    // this year
    first = HDate.make(today.year, 1, 1);
    last = HDate.make(today.year, 12, 31);
    verifyRange(HDateTimeRange.thisYear(ny), first, last);

    // last week
    HDate prev = today.minusDays(7);
    sun = prev;
    sat = prev;
    while (sun.weekday() > Calendar.SUNDAY) sun = sun.minusDays(1);
    while (sat.weekday() < Calendar.SATURDAY) sat = sat.plusDays(1);
    verifyRange(HDateTimeRange.lastWeek(ny), sun, sat);

    // last month
    last = today;
    while (last.month == today.month) last = last.minusDays(1);
    first = HDate.make(last.year, last.month, 1);
    verifyRange(HDateTimeRange.lastMonth(ny), first, last);

    // last year
    first = HDate.make(today.year-1, 1, 1);
    last = HDate.make(today.year-1, 12, 31);
    verifyRange(HDateTimeRange.lastYear(ny), first, last);
  }

  private void verifyRange(HDateTimeRange r, HDate start, HDate end)
  {
    assertEquals(r.start.date,    start);
    assertEquals(r.start.time,    HTime.MIDNIGHT);
    assertEquals(r.start.tz.name, "New_York");
    assertEquals(r.end.date,      end.plusDays(1));
    assertEquals(r.end.time,      HTime.MIDNIGHT);
    assertEquals(r.end.tz.name,   "New_York");
  }

  private void verifyRange(HDateTimeRange r, HDateTime start, HDateTime end)
  {
    assertEquals(r.start, start);
    assertEquals(r.end, end);
  }
}
