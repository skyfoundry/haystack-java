//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 2016  Matthew Giannini  Creation
//
package org.projecthaystack.util;

public class WebUtil
{
  public static boolean isToken(String s)
  {
    if (s == null || s.isEmpty()) return false;
    for (int i = 0; i< s.length(); ++i)
    {
      if (!isTokenChar(s.codePointAt(i))) return false;
    }
    return true;

  }

  public static boolean isTokenChar(final int codePoint)
  {
    return codePoint < 127 && tokenChars[codePoint];
  }

  private static boolean[] tokenChars;
  static
  {
    boolean[] m = new boolean[127];
    for (int i = 0; i < 127; ++i)
    {
      m[i] = i > 0x20;
    }
    m['(']  = false;  m[')'] = false;  m['<']  = false;  m['>'] = false;
    m['@']  = false;  m[','] = false;  m[';']  = false;  m[':'] = false;
    m['\\'] = false;  m['"'] = false;  m['/']  = false;  m['['] = false;
    m[']']  = false;  m['?'] = false;  m['=']  = false;  m['{'] = false;
    m['}']  = false;  m[' '] = false;  m['\t'] = false;
    tokenChars = m;
  }


}
