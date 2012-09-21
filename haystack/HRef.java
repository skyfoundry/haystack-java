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
    if (val == null || val.length() == 0) throw new IllegalArgumentException("Invalid id val: \"" + val + "\"");
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
  public void write(StringBuffer s)
  {
    s.append('@');
    for (int i=0; i<val.length(); ++i)
    {
      int c = val.charAt(i);
      if (!isIdChar(c)) throw new IllegalArgumentException("Invalid ref val'" + val + "', char='" + (char)c + "'");
      s.append((char)c);
    }
    if (dis != null)
    {
      s.append(' ');
      HStr.write(s, dis);
    }
  }

  /** Is the given character valid in the identifier part */
  public static boolean isIdChar(int ch)
  {
    return ch >= 0 && ch < idChars.length && idChars[ch];
  }

  private static boolean[] idChars = new boolean[127];
  static
  {
    for (int i='a'; i<='z'; ++i) idChars[i] = true;
    for (int i='A'; i<='Z'; ++i) idChars[i] = true;
    for (int i='0'; i<='9'; ++i) idChars[i] = true;
    idChars['_'] = true;
    idChars[':'] = true;
    idChars['-'] = true;
    idChars['.'] = true;
  }

}