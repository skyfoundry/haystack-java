//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 Jun 2011  Brian Frank  My birthday!
//
package haystack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * HDict is an immutable map of name/HVal pairs.  Use HDictBuilder
 * to construct a HDict instance.
 */
public class HDict
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /** Singleton for empty set of tags. */
  public static final HDict EMPTY = new HDict(new HashMap(11));

  /** Package private constructor used by HDictBuilder */
  HDict(HashMap map) { this.map = map; }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /** Return if size is zero */
  public boolean isEmpty() { return map.isEmpty(); }

  /** Return number of tag name/value pairs */
  public int size() { return map.size(); }

  /** Return if the given tag is present */
  public boolean has(String name) { return map.get(name) != null; }

  /** Return if the given tag is not present */
  public boolean missing(String name) { return map.get(name) == null; }

  /** Convenience for "get(name, true)" */
  public HVal get(String name) { return get(name, true); }

  /** Get a tag by name.  If not found and checked if false then
      return null, otherwise throw MissingTagException */
  public HVal get(String name, boolean checked)
  {
    HVal val = (HVal)map.get(name);
    if (val != null) return val;
    if (!checked) return null;
    throw new MissingTagException(name);
  }

  /** Create Map.Entry iteratator to walk each name/tag pair */
  public Iterator iterator() { return map.entrySet().iterator(); }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /** String format is always "write" */
  public final String toString() { return write(); }

  /** Hash code is based on tags */
  public int hashCode() { return map.hashCode(); }

  /** Equality is tags */
  public boolean equals(Object that)
  {
    if (!(that instanceof HDict)) return false;
    return map.equals(((HDict)that).map);
  }

//////////////////////////////////////////////////////////////////////////
// Encoding
//////////////////////////////////////////////////////////////////////////

  /** Decode a string into a HDict, throw ParseException if
      not formatted correctly */
  public static HDict read(String s)
  {
    return new HReader(s).readDictEos();
  }

  /** Encode value to string format */
  public final String write()
  {
    StringBuilder s = new StringBuilder();
    write(s);
    return s.toString();
  }

  /** Encode value to string format */
  public void write(StringBuilder s)
  {
    boolean first = true;
    for (Iterator it = iterator(); it.hasNext(); )
    {
      Entry e = (Entry)it.next();
      String name = (String)e.getKey();
      HVal val    = (HVal)e.getValue();
      if (first) first = false; else s.append(',');
      s.append(name);
      if (val != HMarker.VAL) { s.append(':'); val.write(s); }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private final HashMap map;
}