//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack;

/**
 * HMarker is the singleton value for a marker tag.
 */
public class HMarker extends HVal
{
  /** Singleton value */
  public static final HMarker VAL = new HMarker();

  private HMarker() {}

  /** Hash code */
  public int hashCode() { return 0x1379de; }

  /** Equals is based on reference */
  public boolean equals(Object that) { return this == that; }

  /** Encode value to string format */
  public void write(StringBuffer s) { s.append("marker"); }

  /** Zinc encoding is "M" */
  public String toZinc() { return "M"; }

}