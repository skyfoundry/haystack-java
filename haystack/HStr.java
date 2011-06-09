//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 Jun 2011  Brian Frank  Creation
//
package haystack;

/**
 * HStr wraps a java.lang.String as a tag value.
 */
public class HStr extends HVal
{
  /** Construct from java.lang.String value */
  public static HStr make(String val)
  {
    if (val.length() == 0) return EMPTY;
    return new HStr(val);
  }

  /** Singleton value for empty string "" */
  private static final HStr EMPTY = new HStr("");

  /** Private constructor */
  private HStr(String val) { this.val = val; }

  /** String value */
  public final String val;

  /** Hash code is same as java.lang.String */
  public int hashCode() { return val.hashCode(); }

  /** Equals is based on java.lang.String */
  public boolean equals(Object that)
  {
    if (!(that instanceof HStr)) return false;
    return this.val.equals(((HStr)that).val);
  }

  /** Encode value to string format */
  public void write(StringBuilder s) { write(s, val); }

  /** Encode value to string format */
  static void write(StringBuilder s, String val)
  {
    s.append('"');
    for (int i=0; i<val.length(); ++i)
    {
      int c = val.charAt(i);
      if (c < ' ' || c == '"' || c == '\\')
      {
        s.append('\\');
        switch (c)
        {
          case '\n':  s.append('n');  break;
          case '\r':  s.append('r');  break;
          case '\t':  s.append('t');  break;
          case '"':   s.append('"');  break;
          case '\\':  s.append('\\'); break;
          default:
            s.append('u').append('0').append('0');
            if (c < 0xf) s.append('0');
            s.append(Integer.toHexString(c));
        }
      }
      else
      {
        s.append((char)c);
      }
    }
    s.append('"');
  }

}