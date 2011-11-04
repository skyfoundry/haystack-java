//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   04 Nov 2011  Brian Frank  My birthday!
//
package haystack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * HHisItem is used to model a timestamp/value pair
 */
public class HHisItem extends HDict
{
  /** Construct from timestamp, value */
  public static HHisItem make(HDateTime ts, HVal val)
  {
    if (ts == null || val == null) throw new IllegalArgumentException("ts or val is null");
    return new HHisItem(ts, val);
  }

  /** Private constructor */
  private HHisItem(HDateTime ts, HVal val)
  {
    this.ts = ts;
    this.val = val;
  }

  /** Timestamp of history sample */
  public final HDateTime ts;

  /** Value of history sample */
  public final HVal val;

  public int size() { return 2; }

  public HVal get(String name, boolean checked)
  {
    if (name.equals("ts")) return ts;
    if (name.equals("val")) return val;
    if (!checked) return null;
    throw new MissingTagException(name);
  }

  public Iterator iterator() { return new FixedIterator(); }

  class FixedIterator implements Iterator
  {
    public boolean hasNext() { return cur < 1; }
    public Object next()
    {
      ++cur;
      if (cur == 0) return new MapEntry("ts", ts);
      if (cur == 1) return new MapEntry("val", val);
      throw new NoSuchElementException();
    }
    public void remove() { throw new UnsupportedOperationException(); }
    int cur = -1;
  }

}