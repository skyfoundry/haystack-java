//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package haystack;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.HashMap;

/**
 * HTimeZone handles the mapping between Haystack timezone
 * names and Java timezones.
 */
public final class HTimeZone
{
  /** Convenience for make(name, true) */
  public static HTimeZone make(String name) { return make(name, true); }

  /**
   * Construct with Haystack timezone name, raise exception or
   * return null on error based on check flag.
   */
  public static HTimeZone make(String name, boolean checked)
  {
    synchronized (cache)
    {
      // lookup in cache
      HTimeZone tz = (HTimeZone)cache.get(name);
      if (tz != null) return tz;

      // map haystack id to Java full id
      String javaId = (String)toJava.get(name);
      if (javaId == null) throw new RuntimeException("Unknown tz: " + name);

      // resolve full id to HTimeZone and cache
      TimeZone java = TimeZone.getTimeZone(javaId);
      tz = new HTimeZone(name, java);
      cache.put(name, tz);
      return tz;
    }
  }

  /** Convenience for make(java, true) */
  public static HTimeZone make(TimeZone java) { return make(java, true); }

  /**
   * Construct from Java timezone.  Throw exception or return
   * null based on checked flag.
   */
  public static HTimeZone make(TimeZone java, boolean checked)
  {
    String name = (String)fromJava.get(java.getID());
    if (name != null) return make(name);
    if (checked) throw new RuntimeException("Invalid Java timezone: " + java.getID());
    return null;
  }

  /** Private constructor */
  private HTimeZone(String name, TimeZone java)
  {
    this.name = name;
    this.java = java;
  }

  /** Haystack timezone name */
  public final String name;

  /** Java representation of this timezone. */
  public final TimeZone java;

  /** Return Haystack timezone name */
  public String toString() { return name; }

  // haystack name -> HTimeZone
  private static HashMap cache = new HashMap();

  // haystack name <-> java name mapping
  private static HashMap toJava;
  private static HashMap fromJava;
  static
  {
    HashMap toJava = new HashMap();
    HashMap fromJava = new HashMap();
    String[] ids = TimeZone.getAvailableIDs();
    for (int i=0; i<ids.length; ++i)
    {
      String java = ids[i];
      int slash = java.indexOf('/');
      if (slash < 0) continue;
      String haystack = java.substring(slash+1);
      toJava.put(haystack, java);
      fromJava.put(java, haystack);
    }
    HTimeZone.toJava   = toJava;
    HTimeZone.fromJava = fromJava;
  }

  /** UTC timezone */
  public static final HTimeZone UTC;

  /** Default timezone for VM */
  public static final HTimeZone DEFAULT;

  static
  {
    HTimeZone utc = null;
    try
    {
      utc = HTimeZone.make(TimeZone.getTimeZone("Etc/UTC"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    HTimeZone def;
    try
    {
      def = HTimeZone.make(TimeZone.getDefault());
    }
    catch (Exception e)
    {
      e.printStackTrace();
      def = utc;
    }

    DEFAULT = def;
    UTC = utc;
  }
}