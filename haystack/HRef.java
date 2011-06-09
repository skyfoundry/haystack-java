//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack;

/**
 * HRef wraps a string reference identifier and optional display name.
 */
public class HRef extends HVal
{
  /** Construct for string identifier and optional display */
  public static HRef make(String val, String dis)
  {
    return new HRef(val, dis);
  }

  /** Construct for string identifier and null display */
  public static HRef make(String val)
  {
    return make(val, null);
  }

  /** Private constructor */
  private HRef(String val, String dis) { this.val = val; this.dis = dis; }

  /** String identifier for reference */
  public final String val;

  /** Display name for reference of null */
  public final String dis;

  /** Hash code is based on val field only */
  public int hashCode() { return val.hashCode(); }

  /** Equals is based on val field only */
  public boolean equals(Object that)
  {
    if (!(that instanceof HRef)) return false;
    return this.val.equals(((HRef)that).val);
  }

  /** Encode value to string format */
  public void write(StringBuilder s)
  {
    s.append('<');
    for (int i=0; i<val.length(); ++i)
    {
      int c = val.charAt(i);
      if (c < ' ' || c == '>') throw new IllegalArgumentException("Invalid ref val'" + val + "', char='" + (char)c + "'");
      s.append((char)c);
    }
    s.append('>');
    if (dis != null)
    {
      s.append(' ');
      HStr.write(s, dis);
    }
  }

}