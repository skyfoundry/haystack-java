//
// Copyright (c) 2012, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Jan 2013  Mike Jarmy  Creation
//
package org.projecthaystack.test;

import java.util.*;
import org.projecthaystack.util.*;
import org.projecthaystack.util.Base64;

/**
 * UtilTest tests the Base64 encoder
 */
public class UtilTest extends Test
{
  private static String randomString()
  {
    char[] chars = new char[RND.nextInt(100) + 1];
    for (int i = 0; i < chars.length; i++)
      chars[i] = (char) (RND.nextInt(127 - 32) + 32);
    return new String(chars);
  }

  public void testBase64() throws Exception
  {
    for (int i = 0; i < 1000; i++)
    {
      String s1 = randomString();

      String enc = Base64.STANDARD.encodeUTF8(s1);
      String s2 = Base64.STANDARD.decodeUTF8(enc);
      verifyEq(s1, s2);

      enc = Base64.STANDARD.encode(s1);
      s2 = Base64.STANDARD.decode(enc);
      verifyEq(s1, s2);

      enc = Base64.URI.encodeUTF8(s1);
      s2 = Base64.URI.decodeUTF8(enc);
      verifyEq(s1, s2);

      enc = Base64.URI.encode(s1);
      s2 = Base64.URI.decode(enc);
      verifyEq(s1, s2);
    }
  }

  /**
    * testPbk
    *
    * https://tools.ietf.org/html/rfc2898#section-5.2
    * https://tools.ietf.org/html/rfc5802#section-2.2
    *
    * implementation is from 
    * http://stackoverflow.com/questions/9147463/java-pbkdf2-with-hmacsha256-as-the-prf
    *
    * test suite is from 
    * http://stackoverflow.com/questions/5130513/pbkdf2-hmac-sha2-test-vectors/5130543#5130543
    */
  public void testPbk() throws Exception
  {
    doTestPbk(
      strBytes("password"), strBytes("salt"), 1, 32,
      "120fb6cffcf8b32c" +
      "43e7225256c4f837" +
      "a86548c92ccc3548" +
      "0805987cb70be17b");

    doTestPbk(
      strBytes("password"), strBytes("salt"), 2, 32,
      "ae4d0c95af6b46d3" +
      "2d0adff928f06dd0" +
      "2a303f8ef3c251df" +
      "d6e2d85a95474c43");

    doTestPbk(
      strBytes("password"), strBytes("salt"), 4096, 32,
      "c5e478d59288c841" +
      "aa530db6845c4c8d" +
      "962893a001ce4e11" +
      "a4963873aa98134a");
  }

  /**
    * doTestPbk
    */
  private static void doTestPbk(
      byte[] password, byte[] salt, int iterations, int dkLen, String expected)
  throws Exception
  {
    byte[] result = CryptoUtil.pbk(
      "PBKDF2WithHmacSHA256", password, salt, iterations, dkLen);
    String hex = bytesToHex(result);

    if (!hex.equals(expected))
      throw new IllegalStateException();
  }

  /**
    * strBytes
    */
  private static byte[] strBytes(String str) throws Exception
  {
    return str.getBytes("UTF-8");
  }

  /**
    * bytesToHex
    */
  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for ( int j = 0; j < bytes.length; j++ ) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX[v >>> 4];
      hexChars[j * 2 + 1] = HEX[v & 0x0F];
    }
    return new String(hexChars);
  }

////////////////////////////////////////////////////////////////
// attribs
////////////////////////////////////////////////////////////////

  private final static char[] HEX = "0123456789abcdef".toCharArray();

  private static Random RND;
  static
  {
    long seed = System.currentTimeMillis();
    // System.out.println("TestUtil SEED: " + seed);
    RND = new Random(seed);
  }

}
