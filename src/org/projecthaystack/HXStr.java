//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   09 June 2016  Matthew Giannini   Creation
//
package org.projecthaystack;

import org.projecthaystack.io.HZincWriter;

/**
 * XStr is an extended string which is a type name and value
 * encoded as a string. It is used as a generic value when an
 * XStr is decoded without any predefined type.
 */
public class HXStr extends HVal
{
  public static HVal decode(String type, String val)
  {
    if ("Bin".equals(type)) return HBin.make(val);
    return new HXStr(type, val);
  }

  public static HXStr encode(Object val)
  {
    return new HXStr(val.getClass().getSimpleName(), val.toString());
  }

  private HXStr(String type, String val)
  {
    if (!isValidType(type)) throw new IllegalArgumentException("Invalid type name: " + type);
    this.type = type;
    this.val = val;
  }

  private static boolean isValidType(String t)
  {
    if (t == null || t.isEmpty() || !Character.isUpperCase(t.charAt(0))) return false;
    char[] chars = t.toCharArray();
    for (int i=0; i<chars.length; ++i)
    {
      if (Character.isLetter(chars[i])) continue;
      if (Character.isDigit(chars[i])) continue;
      if (chars[i] == '_') continue;
      return false;
    }
    return true;
  }

  /** Type name */
  public final String type;

  /** String value */
  public final String val;

  public String toZinc()
  {
    StringBuffer s = new StringBuffer();
    s.append(type).append("(\"").append(val).append("\")");
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

    HXStr hxStr = (HXStr) o;

    if (!type.equals(hxStr.type)) return false;
    return val.equals(hxStr.val);

  }

  public int hashCode()
  {
    int result = type.hashCode();
    result = 31 * result + val.hashCode();
    return result;
  }
}
