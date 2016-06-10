//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Jan 2013  Mike Jarmy  Creation
//
package org.projecthaystack.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
  * Base64 handles various methods of encoding and decoding
  * base 64 format.
  */
public class Base64
{
  /**
    * Return a Base64 codec that uses standard Base64 format.
    */
  public static Base64 STANDARD = new Base64("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray(), "=");

  /**
    * Return a Base64 codec that uses a custom, Uri-friendly Base64 format.
    * <p>
    * This codec <i>mostly</i> follows the RFC 3548 standard for Base64.
    * It uses '-' and '_' instead of '+' and '/' (as per RFC 3548),
    * This coded uses no padding character.
    * <p>
    * This approach allows us to encode and decode HRef instances.
    * HRef has five special chars available for us to use: ':', '.', '-', '_', '~'.
    * We are using three of them here, leaving two still available: ':' and '.'
    */
  public static Base64 URI = new Base64("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray());

////////////////////////////////////////////////////////////////
// constructor
////////////////////////////////////////////////////////////////

  private Base64(char[] alphabet) { this(alphabet, null); }
  private Base64(char[] alphabet, String pad)
  {
    this.base64chars = alphabet;
    this.pad = pad;
    for (int i=0; i<base64inv.length; ++i)   base64inv[i] = -1;
    for (int i=0; i<base64chars.length; ++i) base64inv[base64chars[i]] = i;
  }

////////////////////////////////////////////////////////////////
// Util
////////////////////////////////////////////////////////////////

  /** Sniff the string to determine how it was encoded and then decode it to byte[] */
  public static byte[] decodeUtf8(String str)
  {
    if (str.endsWith("=") || str.indexOf('+') >= 0 || str.indexOf('/') >= 0)
      return Base64.STANDARD.decodeBytes(str);
    else
      return Base64.URI.decodeBytes(str);
  }

////////////////////////////////////////////////////////////////
// API
////////////////////////////////////////////////////////////////

  /**
    * Encode the string to base 64, using the platform's default charset.
    */
  public String encode(String str)
  {
    return encodeBytes(str.getBytes());
  }

  /**
    * Encode the string to base 64, using the UTF8 charset.
    */
  public String encodeUTF8(String str)
  {
    try
    {
      return encodeBytes(str.getBytes("UTF8"));
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
    * Decode the string from base 64, using the platform's default charset.
    */
  public String decode(String str)
  {
    return new String(decodeBytes(str));
  }

  /**
    * Decode the string from base 64, using the UTF8 charset.
    */
  public String decodeUTF8(String str)
  {
    try
    {
      return new String(decodeBytes(str), "UTF8");
    }
    catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
    * Encode the byte array to base 64.
    */
  public String encodeBytes(byte[] buf)
  {
    char[] table = this.base64chars;
    int size = buf.length;
    StringBuilder s = new StringBuilder(size*2);
    int i = 0;

    // append full 24-bit chunks
    int end = size-2;
    for (; i<end; i += 3)
    {
      int n = ((buf[i] & 0xff) << 16) + ((buf[i+1] & 0xff) << 8) + (buf[i+2] & 0xff);
      s.append(table[(n >>> 18) & 0x3f]);
      s.append(table[(n >>> 12) & 0x3f]);
      s.append(table[(n >>> 6) & 0x3f]);
      s.append(table[n & 0x3f]);
    }

    // pad and encode remaining bits
    int rem = size - i;
    if (rem > 0)
    {
      int n = ((buf[i] & 0xff) << 10) | (rem == 2 ? ((buf[size-1] & 0xff) << 2) : 0);
      s.append(table[(n >>> 12) & 0x3f]);
      s.append(table[(n >>> 6) & 0x3f]);

      if (rem == 2) s.append(table[n & 0x3f]);
      else if (hasPad()) s.append(padChar());

      if (hasPad()) s.append(padChar());
    }

    return s.toString();
  }

  /**
    * Decode the byte array from base 64.
    */
  public byte[] decodeBytes(String s)
  {
    int slen = s.length();
    int si = 0;
    int max = slen * 6 / 8;
    byte[] buf = new byte[max];
    int size = 0;

    while (si < slen)
    {
      int n = 0;
      int v = 0;
      for (int j=0; j<4 && si<slen;)
      {
        int ch = s.charAt(si++);
        int c = ch < 128 ? base64inv[ch] : -1;
        if (c >= 0)
        {
          n |= c << (18 - j++ * 6);
          if (!hasPad() || ch != padChar()) v++;
        }
      }

      if (v > 1) buf[size++] = (byte)(n >> 16);
      if (v > 2) buf[size++] = (byte)(n >> 8);
      if (v > 3) buf[size++] = (byte)n;
    }

    return Arrays.copyOfRange(buf, 0, size);
  }

  private boolean hasPad() { return pad != null; }
  private char padChar() { return this.pad.charAt(0); }

////////////////////////////////////////////////////////////////
// Attributes
////////////////////////////////////////////////////////////////

  private final char[] base64chars;
  private final int[] base64inv = new int[128];
  private final String pad;
}