//
// Copyright (c) 2013, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 Jan 2013  Brian Frank  Creation
//
package org.projecthaystack;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

/**
 * HCoord models a geographic coordinate as latitude and longitude
 *
 * @see <a href='http://project-haystack.org/doc/TagModel#tagKinds'>Project Haystack</a>
 */
public class HCoord extends HVal
{
  /** Parse from string fomat "C(lat,lng)" or raise ParseException */
  public static HCoord make(String s)
  {
    return (HCoord)new HZincReader(s).readVal();
  }

  /** Construct from basic fields */
  public static HCoord make(double lat, double lng)
  {
    return new HCoord((int)(lat * 1000000.0), (int)(lng * 1000000.0));
  }

  /** Package private constructor */
  HCoord(int ulat, int ulng)
  {
    if (ulat < -90000000 || ulat > 90000000) throw new IllegalArgumentException("Invalid lat > +/- 90");
    if (ulng < -180000000 || ulng > 180000000) throw new IllegalArgumentException("Invalid lng > +/- 180");
    this.ulat = ulat;
    this.ulng = ulng;
  }

  /** Return if given latitude is legal value between -90.0 and +90.0 */
  public static boolean isLat(double lat) { return -90.0 <= lat && lat <= 90.0; }

  /** Return if given is longtitude is legal value between -180.0 and +180.0 */
  public static boolean isLng(double lng) { return -180.0 <= lng && lng <= 180.0; }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /** Latitude in decimal degrees */
  public double lat() { return ulat / 1000000.0; }

  /** Longtitude in decimal degrees */
  public double lng() { return ulng / 1000000.0; }

  /** Latitude in micro-degrees */
  public final int ulat;

  /** Longitude in micro-degrees */
  public final int ulng;

  /** Hash is based on lat/lng */
  public int hashCode() { return (ulat << 7) ^ ulng; }

  /** Equality is based on lat/lng */
  public boolean equals(Object that)
  {
    if (!(that instanceof HCoord)) return false;
    HCoord x = (HCoord)that;
    return ulat == x.ulat && ulng == x.ulng;
  }

  /** Return "c:lat,lng" */
  public String toJson()
  {
    StringBuffer s = new StringBuffer();
    s.append("c:");
    s.append(uToStr(ulat));
    s.append(',');
    s.append(uToStr(ulng));
    return s.toString();
  }

  /** Represented as "C(lat,lng)" */
  public String toZinc()
  {
    StringBuffer s = new StringBuffer();
    s.append("C(");
    s.append(uToStr(ulat));
    s.append(',');
    s.append(uToStr(ulng));
    s.append(')');
    return s.toString();
  }

  private static String uToStr(int ud)
  {
    StringBuffer s = new StringBuffer();
    if (ud < 0) { s.append('-'); ud = -ud; }
    if (ud < 1000000.0)
    {
      s.append(new DecimalFormat("0.0#####", new DecimalFormatSymbols(Locale.ENGLISH)).format(ud/1000000.0));
      return s.toString();
    }
    String x = String.valueOf(ud);
    int dot = x.length() - 6;
    int end = x.length();
    while (end > dot+1 && x.charAt(end-1) == '0') --end;
    for (int i=0; i<dot; ++i) s.append(x.charAt(i));
    s.append('.');
    for (int i=dot; i<end; ++i) s.append(x.charAt(i));
    return s.toString();
  }
}