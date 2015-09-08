//
// Copyright (c) 2014, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   22 Apr 2014  Brian Frank  Creation
//

package org.projecthaystack.util;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;


/**
  * Crypto utilities
  */
public class CryptoUtil
{
  /**
   * Implementation of HMAC algorithm since Java's implementation
   * doesn't allow empty passwords
   */
  public static byte[] hmac(String algorithm, byte[] data, byte[] key)
    throws NoSuchAlgorithmException
  {
    // get digest algorthim
    MessageDigest md = MessageDigest.getInstance(algorithm);
    int blockSize = 64;

    // key is greater than block size we hash it first
    int keySize = key.length;
    if (keySize > blockSize)
    {
      md.update(key, 0, keySize);
      key = md.digest();
      keySize = key.length;
      md.reset();
    }

    // RFC 2104:
    //   ipad = the byte 0x36 repeated B times
    //   opad = the byte 0x5C repeated B times
    //   H(K XOR opad, H(K XOR ipad, text))

    // inner digest: H(K XOR ipad, text)
    for (int i=0; i<blockSize; ++i)
    {
      if (i < keySize)
        md.update((byte)(key[i] ^ 0x36));
      else
        md.update((byte)0x36);
    }
    md.update(data, 0, data.length);
    byte[] innerDigest = md.digest();

    // outer digest: H(K XOR opad, innerDigest)
    md.reset();
    for (int i=0; i<blockSize; ++i)
    {
      if (i < keySize)
        md.update((byte)(key[i] ^ 0x5C));
      else
        md.update((byte)0x5C);
    }
    md.update(innerDigest);

    // return result
    return md.digest();
  }

  /**
    * Derive a Password-Based Key.  The only currently supported algorithm
    * is "PBKDF2WithHmacSHA256".
    */
  public static byte[] pbk(
    String algorithm,
    byte[] password, 
    byte[] salt, 
    int iterationCount,
    int derivedKeyLength)
    throws Exception
  {
    if (!algorithm.equals("PBKDF2WithHmacSHA256"))
      throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);

    return Pbkdf2.deriveKey(password, salt, iterationCount, derivedKeyLength);
  }

  /**
    * Pbkdf2
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
  static class Pbkdf2
  {
    /**
      * deriveKey
      */
    static byte[] deriveKey(
      byte[] password, 
      byte[] salt, 
      int iterations,
      int dkLen)
    throws NoSuchAlgorithmException, InvalidKeyException
    {
      SecretKeySpec keyspec = new SecretKeySpec(password, "HmacSHA256");
      Mac prf = Mac.getInstance("HmacSHA256");
      prf.init(keyspec);

      int hLen = prf.getMacLength(); // 20 for SHA1
      int l = Math.max(dkLen, hLen); //  1 for 128bit (16-byte) keys
      int r = dkLen - (l-1)*hLen;    // 16 for 128bit (16-byte) keys
      byte T[] = new byte[l * hLen];
      int ti_offset = 0;
      for (int i = 1; i <= l; i++) {
        F(T, ti_offset, prf, salt, iterations, i);
        ti_offset += hLen;
      }

      if (r < hLen) {
        // Incomplete last block
        byte DK[] = new byte[dkLen];
        System.arraycopy(T, 0, DK, 0, dkLen);
        return DK;
      }
      return T;
    } 

    static void F(byte[] dest, int offset, Mac prf, byte[] S, int c, int blockIndex) 
    {
      final int hLen = prf.getMacLength();
      byte U_r[] = new byte[ hLen ];
      byte U_i[] = new byte[S.length + 4];
      System.arraycopy(S, 0, U_i, 0, S.length);
      INT(U_i, S.length, blockIndex);
      for(int i = 0; i < c; i++) {
        U_i = prf.doFinal(U_i);
        xor(U_r, U_i);
      }

      System.arraycopy(U_r, 0, dest, offset, hLen);
    }

    static void xor(byte[] dest, byte[] src) 
    {
      for(int i = 0; i < dest.length; i++)
        dest[i] ^= src[i];
    }

    static void INT(byte[] dest, int offset, int i) 
    {
      dest[offset + 0] = (byte) (i / (256 * 256 * 256));
      dest[offset + 1] = (byte) (i / (256 * 256));
      dest[offset + 2] = (byte) (i / (256));
      dest[offset + 3] = (byte) (i);
    } 
  }
}
