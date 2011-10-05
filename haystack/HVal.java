//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack;

/**
 * HVal is the base class for representing haystack tag
 * scalar values as an immutable class.
 */
public abstract class HVal implements Comparable
{
  /** Package private constructor */
  HVal() {}

  /** String format is always "write" */
  public final String toString() { return write(); }

  /** Encode value to string format */
  public final String write()
  {
    StringBuilder s = new StringBuilder();
    write(s);
    return s.toString();
  }

  /** Hash code is value based */
  public abstract int hashCode();

  /** Equality is value based */
  public abstract boolean equals(Object that);

  /** Return sort order as negative, 0, or positive */
  public int compareTo(Object that)
  {
    return toString().compareTo(that.toString());
  }

  /** Decode a string into a HVal, throw ParseException if
      not formatted correctly */
  public static HVal read(String s)
  {
    return new HReader(s).readValEos();
  }

  /** Encode value to string format */
  public abstract void write(StringBuilder s);

}