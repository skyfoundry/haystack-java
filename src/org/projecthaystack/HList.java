//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 June 2016  Matthew Giannini   Creation
//
package org.projecthaystack;

import java.util.Arrays;
import java.util.List;

/**
 * HList is an immutable list of HVal items.
 */
public class HList extends HVal
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  public static final HList EMPTY = new HList(new HVal[0]);

  /** Create a list of the given items. The items are copied */
  public static HList make(HVal[] items)
  {
    HVal[] copy = new HVal[items.length];
    System.arraycopy(items, 0, copy, 0, items.length);
    return new HList(copy);
  }

  /** Create a list from the given items. The items are copied */
  public static HList make(List items)
  {
    HVal[] copy = (HVal[])items.toArray(new HVal[items.size()]);
    return new HList(copy);
  }

  private HList(HVal[] items)
  {
    this.items = items;
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /** Get the number of items in the list */
  public int size() { return items.length; }

  /** Get the HVal at the given index */
  public HVal get(int i) { return items[i]; }

//////////////////////////////////////////////////////////////////////////
// HVal
//////////////////////////////////////////////////////////////////////////

  public String toZinc()
  {
    StringBuffer s = new StringBuffer();
    s.append('[');
    for (int i=0; i<items.length; ++i)
    {
      if (i > 0) s.append(',');
      s.append(items[i].toZinc());
    }
    s.append(']');
    return s.toString();
  }

  public String toJson()
  {
    throw new UnsupportedOperationException();
  }

  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HList hList = (HList) o;

    return Arrays.equals(items, hList.items);
  }

  public int hashCode()
  {
    return Arrays.hashCode(items);
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private final HVal[] items;
}
