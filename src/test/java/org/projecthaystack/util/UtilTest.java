//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.util;

import static org.testng.Assert.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;


public class UtilTest
{
  @Test
  public void testIsToken()
  {
    assertEquals(WebUtil.isToken(""), false);
    assertEquals(WebUtil.isToken("x"), true);
    assertEquals(WebUtil.isToken("x y"), false);
    assertEquals(WebUtil.isToken("5a-3dd_33*&^%22!~"), true);
    assertEquals(WebUtil.isToken("(foo)"), false);
    assertEquals(WebUtil.isToken("foo;bar"), false);
    assertEquals(WebUtil.isToken("base64+/"), false);
  }

  @Test
  public void testBase64()
  {
    for (int i = 0; i < 1000; i++)
    {
      String s1 = randomString();

      String enc = Base64.STANDARD.encodeUTF8(s1);
      String s2 = Base64.STANDARD.decodeUTF8(enc);
      assertEquals(s1, s2);

      enc = Base64.STANDARD.encode(s1);
      s2 = Base64.STANDARD.decode(enc);
      assertEquals(s1, s2);

      enc = Base64.URI.encodeUTF8(s1);
      s2 = Base64.URI.decodeUTF8(enc);
      assertEquals(s1, s2);

      enc = Base64.URI.encode(s1);
      s2 = Base64.URI.decode(enc);
      assertEquals(s1, s2);
    }
  }

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

  private final static char[] HEX = "0123456789abcdef".toCharArray();

  private static Random RND;
  static
  {
    long seed = System.currentTimeMillis();
    // System.out.println("TestUtil SEED: " + seed);
    RND = new Random(seed);
  }

  private static String randomString()
  {
    char[] chars = new char[RND.nextInt(100) + 1];
    for (int i = 0; i < chars.length; i++)
      chars[i] = (char) (RND.nextInt(127 - 32) + 32);
    return new String(chars);
  }

}
