//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import org.projecthaystack.util.Base64;
import org.projecthaystack.util.CryptoUtil;

import java.util.TreeMap;

/**
 * HmacScheme implements old HMAC SHA-1 algorithm.
 */
final public class HmacScheme extends AuthScheme
{
  public HmacScheme()
  {
    super("hmac");
  }

  public AuthMsg onClient(AuthClientContext cx, AuthMsg msg)
  {
    // gather request parameters - salt must be converted to "standard" base64
    String user  = cx.user;
    String pass  = cx.pass;
    String hash  = msg.param("hash");
    String salt  = msg.param("salt");
    String nonce = msg.param("nonce");

    // sanity check hash
    if (!"SHA-1".equals(hash)) throw new AuthException("Invalid hash: " + hash);

    // compute secret and then digest of that. AuthMsg must user Base64.URI
    try
    {
      String secret = hmac(user, pass, salt, hash);
      byte[] digestBytes = CryptoUtil.digest(hash, (secret + ":" + nonce).getBytes());
      String digest = Base64.URI.encodeBytes(digestBytes);
      String[] params = new String[] {
          "handshakeToken", Base64.URI.encodeUTF8(user),
          "digest", digest,
          "nonce", nonce,
      };
      AuthMsg req = new AuthMsg(this.name, params);
      return req;
    }
    catch (Exception e)
    {
      throw new AuthException("Failed to compute hmac digest", e);
    }
  }

  public static String hmac(String user, String pass, String salt, String hash) throws Exception
  {
    // ensure salt is standard base64
    salt = Base64.STANDARD.encodeBytes(Base64.decodeUtf8(salt));
    byte[] hmacBytes = CryptoUtil.hmac(hash, (user + ":" + salt).getBytes(), pass.getBytes());
    return Base64.STANDARD.encodeBytes(hmacBytes);
  }
}
