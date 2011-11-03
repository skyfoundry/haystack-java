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
 * HDictBuilder is used to construct an immutable HDict instance.
 */
public class HDictBuilder
{

  /** Convenience for <code>add(name, HMarker.VAL)</code> */
  public final HDictBuilder add(String name)
  {
    return add(name, HMarker.VAL);
  }

  /** Convenience for <code>add(name, HBool.make(val))</code> */
  public final HDictBuilder add(String name, boolean val)
  {
    return add(name, HBool.make(val));
  }

  /** Convenience for <code>add(name, HNum.make(val))</code> */
  public final HDictBuilder add(String name, long val)
  {
    return add(name, HNum.make(val));
  }

  /** Convenience for <code>add(name, HNum.make(val, unit))</code> */
  public final HDictBuilder add(String name, long val, String unit)
  {
    return add(name, HNum.make(val, unit));
  }

  /** Convenience for <code>add(name, HNum.make(val))</code> */
  public final HDictBuilder add(String name, double val)
  {
    return add(name, HNum.make(val));
  }

  /** Convenience for <code>add(name, HNum.make(val, unit))</code> */
  public final HDictBuilder add(String name, double val, String unit)
  {
    return add(name, HNum.make(val, unit));
  }

  /** Convenience for <code>add(name, HStr.make(val))</code> */
  public final HDictBuilder add(String name, String val)
  {
    return add(name, HStr.make(val));
  }

  /** Add tag name and value.  Return this. */
  public HDictBuilder add(String name, HVal val)
  {
    if (map == null) map = new HashMap(37);
    map.put(name, val);
    return this;
  }

  /** Convert current state to an immutable HDict instance */
  public final HDict toDict()
  {
    if (map == null || map.isEmpty()) return HDict.EMPTY;
    HDict dict = new HDict(this.map);
    this.map = null;
    return dict;
  }

  private HashMap map;
}