//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 Jun 2011  Brian Frank  My birthday!
//
package haystack;

import java.util.HashMap;

/**
 * HTagsBuilder is used to construct an immutable HTags instance.
 */
public class HTagsBuilder
{

  /** Convenience for <code>add(name, HMarker.VAL)</code> */
  public final HTagsBuilder add(String name)
  {
    return add(name, HMarker.VAL);
  }

  /** Convenience for <code>add(name, HBool.make(val))</code> */
  public final HTagsBuilder add(String name, boolean val)
  {
    return add(name, HBool.make(val));
  }

  /** Convenience for <code>add(name, HNum.make(val))</code> */
  public final HTagsBuilder add(String name, long val)
  {
    return add(name, HNum.make(val));
  }

  /** Convenience for <code>add(name, HNum.make(val, unit))</code> */
  public final HTagsBuilder add(String name, long val, String unit)
  {
    return add(name, HNum.make(val, unit));
  }

  /** Convenience for <code>add(name, HNum.make(val))</code> */
  public final HTagsBuilder add(String name, double val)
  {
    return add(name, HNum.make(val));
  }

  /** Convenience for <code>add(name, HNum.make(val, unit))</code> */
  public final HTagsBuilder add(String name, double val, String unit)
  {
    return add(name, HNum.make(val, unit));
  }

  /** Convenience for <code>add(name, HStr.make(val))</code> */
  public final HTagsBuilder add(String name, String val)
  {
    return add(name, HStr.make(val));
  }

  /** Add tag name and value.  Return this. */
  public HTagsBuilder add(String name, HVal val)
  {
    if (map == null) map = new HashMap(37);
    map.put(name, val);
    return this;
  }

  /** Convert current state to an immutable HTags instance */
  public final HTags toTags()
  {
    if (map == null || map.isEmpty()) return HTags.EMPTY;
    HTags tags = new HTags(this.map);
    this.map = null;
    return tags;
  }

  private HashMap map;
}