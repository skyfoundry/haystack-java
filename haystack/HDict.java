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
public abstract class HDict
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /** Singleton for empty set of tags. */
  public static final HDict EMPTY = new MapImpl(new HashMap(11));

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /** Return if size is zero */
  public final boolean isEmpty() { return size() == 0; }

  /** Return number of tag name/value pairs */
  public abstract int size();

  /** Return if the given tag is present */
  public final boolean has(String name) { return get(name, false) != null; }

  /** Return if the given tag is not present */
  public final boolean missing(String name) { return get(name, false) == null; }

  /** Convenience for "get(name, true)" */
  public final HVal get(String name) { return get(name, true); }

  /** Get a tag by name.  If not found and checked if false then
      return null, otherwise throw MissingTagException */
  public abstract HVal get(String name, boolean checked);

  /** Create Map.Entry iteratator to walk each name/tag pair */
  public abstract Iterator iterator();

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /** String format is always "write" */
  public final String toString() { return write(); }

  /** Hash code is based on tags */
  public final int hashCode()
  {
    if (hashCode == 0)
    {
      int x = 33;
      for (Iterator it = iterator(); it.hasNext();)
      {
        Entry entry = (Entry)it.next();
        Object key = entry.getKey();
        Object val = entry.getValue();
        x ^= (key.hashCode() << 7) ^ val.hashCode();
      }
      hashCode = x;
    }
    return hashCode;
  }
  private int hashCode;

  /** Equality is tags */
  public final boolean equals(Object that)
  {
    if (!(that instanceof HDict)) return false;
    HDict x = (HDict)that;
    if (size() != x.size()) return false;
    for (Iterator it = iterator(); it.hasNext(); )
    {
      Entry entry = (Entry)it.next();
      String key = (String)entry.getKey();
      Object val = entry.getValue();
      if (!val.equals(x.get(key,false))) return false;
    }
    return true;
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
    StringBuffer s = new StringBuffer();
    write(s);
    return s.toString();
  }

  /** Encode value to string format */
  public final void write(StringBuffer s)
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
// MapImpl
//////////////////////////////////////////////////////////////////////////

  static class MapImpl extends HDict
  {
    MapImpl(HashMap map) { this.map = map; }

    public int size() { return map.size(); }

    public HVal get(String name, boolean checked)
    {
      HVal val = (HVal)map.get(name);
      if (val != null) return val;
      if (!checked) return null;
      throw new MissingTagException(name);
    }

    public Iterator iterator() { return map.entrySet().iterator(); }

    private final HashMap map;
  }

//////////////////////////////////////////////////////////////////////////
// MapEntry
//////////////////////////////////////////////////////////////////////////

  /** Create Map.Entry for given name/value tag pair */
  protected static Entry toEntry(String key, HVal val) { return new MapEntry(key, val); }

  static class MapEntry implements Entry
  {
    MapEntry(String key, Object val) { this.key = key; this.val = val; }
    public Object getKey() { return key; }
    public Object getValue() { return val; }
    public Object setValue(Object v) { throw new UnsupportedOperationException(); }
    public boolean equals(Object o)
    {
      Entry e1 = this;
      Entry e2 = (Entry)o;
      return (e1.getKey()==null ?
              e2.getKey()==null : e1.getKey().equals(e2.getKey()))  &&
             (e1.getValue()==null ?
              e2.getValue()==null : e1.getValue().equals(e2.getValue()));
    }
    public int hashCode()
    {
      return key.hashCode() ^ val.hashCode();
    }
    private String key;
    private Object val;
  }

}