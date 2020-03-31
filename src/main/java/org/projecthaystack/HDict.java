//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 Jun 2011  Brian Frank  My birthday!
//
package org.projecthaystack;

import org.projecthaystack.io.HZincWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * HDict is an immutable map of name/HVal pairs.  Use HDictBuilder
 * to construct a HDict instance.
 *
 * @see <a href='http://project-haystack.org/doc/TagModel#tags'>Project Haystack</a>
 */
public abstract class HDict extends HVal
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
      return null, otherwise throw UnknownNameException */
  public abstract HVal get(String name, boolean checked);

  /** Create Map.Entry iteratator to walk each name/tag pair */
  public abstract Iterator iterator();

  /** Get the "id" tag as HRef. */
  public HRef id() { return getRef("id"); }

  /**
   * Get display string for this entity:
   *    - dis tag
   *    - id tag
   */
  public String dis()
  {
    HVal v;
    v = get("dis", false); if (v instanceof HStr) return ((HStr)v).val;
    v = get("id", false); if (v != null) return ((HRef)v).dis();
    return "????";
  }

//////////////////////////////////////////////////////////////////////////
// Get Conveniences
//////////////////////////////////////////////////////////////////////////

  /** Get tag as HBool or raise UnknownNameException or ClassCastException. */
  public final boolean getBool(String name) { return ((HBool)get(name)).val; }

  /** Get tag as HStr or raise UnknownNameException or ClassCastException. */
  public final String getStr(String name) { return ((HStr)get(name)).val; }

  /** Get tag as HRef or raise UnknownNameException or ClassCastException. */
  public final HRef getRef(String name) { return (HRef)get(name); }

  /** Get tag as HNum or raise UnknownNameException or ClassCastException. */
  public final int getInt(String name) { return (int)((HNum)get(name)).val; }

  /** Get tag as HNum or raise UnknownNameException or ClassCastException. */
  public final double getDouble(String name) { return ((HNum)get(name)).val; }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /** String format is always "toZinc" */
  public final String toString() { return toZinc(); }

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
        if (val == null) continue;
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
    if (this.size() != x.size()) return false;
    for (Iterator it = iterator(); it.hasNext(); )
    {
      Entry entry = (Entry)it.next();
      String key = (String)entry.getKey();
      Object val = entry.getValue();
      if (val == null) continue;
      if (!val.equals(x.get(key,false))) return false;
    }
    return true;
  }

//////////////////////////////////////////////////////////////////////////
// Encoding
//////////////////////////////////////////////////////////////////////////

  /**
   * Return if the given string is a legal tag name.  The
   * first char must be ASCII lower case letter.  Rest of
   * chars must be ASCII letter, digit, or underbar.
   */
  public static boolean isTagName(String n)
  {
    if (n.length() == 0) return false;
    int first = n.charAt(0);
    if (first < 'a' || first > 'z') return false;
    for (int i=0; i<n.length(); ++i)
    {
      int c = n.charAt(i);
      if (c >= 128 || !tagChars[c]) return false;
    }
    return true;
  }

  private static boolean[] tagChars = new boolean[128];
  static
  {
    for (int i='a'; i<='z'; ++i) tagChars[i] = true;
    for (int i='A'; i<='Z'; ++i) tagChars[i] = true;
    for (int i='0'; i<='9'; ++i) tagChars[i] = true;
    tagChars['_'] = true;
  }

//////////////////////////////////////////////////////////////////////////
// HVal
//////////////////////////////////////////////////////////////////////////

  /** Encode value to zinc format */
  public final String toZinc()
  {
    return HZincWriter.valToString(this);
  }

  /** Encode value to json format */
  public String toJson()
  {
    throw new UnsupportedOperationException();
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
      throw new UnknownNameException(name);
    }

    public Iterator iterator() { return map.entrySet().iterator(); }

    private final HashMap map;
  }

//////////////////////////////////////////////////////////////////////////
// MapEntry
//////////////////////////////////////////////////////////////////////////

  /** Create Map.Entry for given name/value tag pair */
  static Entry toEntry(String key, HVal val) { return new MapEntry(key, val); }

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