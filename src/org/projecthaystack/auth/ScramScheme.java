//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   02 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import org.projecthaystack.util.Base64;
import org.projecthaystack.util.CryptoUtil;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * ScramScheme implements the salted challenge response authentication
 * mechanism as defined in <a href="https://tools.ietf.org/html/rfc5802">RFC 5802</a>
 */
final public class ScramScheme extends AuthScheme
{
  public ScramScheme()
  {
    super("scram");
  }

  public AuthMsg onClient(AuthClientContext cx, AuthMsg msg)
  {
    return msg.param("data", false) == null
        ? firstMsg(cx, msg)
        : finalMsg(cx, msg);
  }

  private AuthMsg firstMsg(AuthClientContext cx, AuthMsg msg)
  {
    // construct client-first-message
    String c_nonce = genNonce();
    String c1_bare = "n=" + cx.user + ",r=" + c_nonce;
    String c1_msg  = gs2_header + c1_bare;

    // stash for final msg
    cx.stash.put("c_nonce", c_nonce);
    cx.stash.put("c1_bare", c1_bare);

    // build auth msg
    Map params = new HashMap();
    params.put("data", Base64.URI.encodeUTF8(c1_msg));
    return new AuthMsg(name, injectHandshakeToken(msg, params));
  }

  private AuthMsg finalMsg(AuthClientContext cx, AuthMsg msg)
  {
    // decode server-first-message
    String s1_msg = Base64.URI.decodeUTF8(msg.param("data"));
    Map data      = decodeMsg(s1_msg);

    // c2-no-proof
    String cbind_input     = gs2_header;
    String channel_binding = Base64.URI.encodeUTF8(cbind_input);
    String nonce           = (String)data.get("r");
    String c2_no_proof     = "c=" + channel_binding + ",r=" + nonce;

    // proof
    String hash = msg.param("hash");
    String salt = (String)data.get("s");
    int iterations = Integer.parseInt((String)data.get("i"));
    String c1_bare = (String)cx.stash.get("c1_bare");
    String authMsg = c1_bare + "," + s1_msg + "," + c2_no_proof;

    String c2_msg = null;
    try
    {
      byte[] saltedPassword = pbk(hash, cx.pass, salt, iterations);
      String clientProof    = createClientProof(hash, saltedPassword, strBytes(authMsg));
      c2_msg = c2_no_proof + ",p=" + clientProof;
    }
    catch (Exception e)
    {
      throw new AuthException("Failed to compute scram", e);
    }

    // build auth msg
    Map params = new HashMap();
    params.put("data", Base64.URI.encodeUTF8(c2_msg));
    return new AuthMsg(name, injectHandshakeToken(msg, params));
  }

  public void onClientSuccess(AuthClientContext cx, AuthMsg msg)
  {
    super.onClientSuccess(cx, msg);
  }

  /** Generate a random nonce string */
  private String genNonce()
  {
    byte[] bytes = new byte[clientNonceBytes];
    (new SecureRandom()).nextBytes(bytes);
    return Base64.URI.encodeBytes(bytes);
  }

  /** If the msg contains a handshake token, inject it into the given params */
  private static Map injectHandshakeToken(AuthMsg msg, Map params)
  {
    String tok = msg.param("handshakeToken", false);
    if (tok != null) params.put("handshakeToken", tok);
    return params;
  }

  /** Decode a raw scram message */
  private static Map decodeMsg(String s)
  {
    Map data = new HashMap();
    String[] toks = s.split(",");
    for (int i = 0; i < toks.length; ++i)
    {
      String tok = toks[i];
      int n = tok.indexOf('=');
      if (n < 0) continue;
      String key = tok.substring(0, n);
      String val = tok.substring(n+1);
      data.put(key, val);
    }
    return data;
  }

  private static byte[] pbk(String hash, String password, String salt, int iterations) throws Exception
  {
    String algorithm = "PBKDF2WithHmac" + hash.replace("-", "");
    int keyBytes = keyBits(hash) / 8;
    byte[] saltBytes = Base64.STANDARD.decodeBytes(salt);
    byte[] bytes= CryptoUtil.pbk(algorithm,
        strBytes(password),
        Base64.STANDARD.decodeBytes(salt),
        iterations,
        keyBytes);
    return bytes;
  }

  private static int keyBits(String hash)
  {
    if ("SHA-1".equals(hash))   return 160;
    if ("SHA-256".equals(hash)) return 256;
    if ("SHA-512".equals(hash)) return 512;
    throw new IllegalArgumentException("Unsupported hash function: " + hash);
  }

  private static String createClientProof(String hash, byte[] saltedPassword, byte[] authMsg) throws Exception
  {
    byte[] clientKey = CryptoUtil.hmac(hash, strBytes("Client Key"), saltedPassword);
    byte[] storedKey = MessageDigest.getInstance(hash).digest(clientKey);
    byte[] clientSig = CryptoUtil.hmac(hash, authMsg, storedKey);

    byte[] clientProof = new byte[clientKey.length];
    for (int i = 0; i < clientKey.length; i++)
        clientProof[i] = (byte) (clientKey[i] ^ clientSig[i]);

    return Base64.STANDARD.encodeBytes(clientProof);
  }

  private static byte[] strBytes(String s) throws UnsupportedEncodingException
  {
    return s.getBytes("UTF-8");
  }

  private static final int clientNonceBytes = 16;
  private static final String gs2_header = "n,,";
}

//////////////////////////////////////////////////////////////////
//// niagara SCRAM
//////////////////////////////////////////////////////////////////
//
//  /**
//   * Authenticate using Niagara's implementation of
//   * Salted Challenge Response (SCRAM) HTTP Authentication Mechanism
//   *
//   * https://www.ietf.org/archive/id/draft-melnikov-httpbis-scram-auth-01.txt
//   */
//  private void authenticateNiagaraScram(HttpURLConnection c) throws Exception
//  {
//    // authentication uri
//    URI uri = new URI(c.getURL().toString());
//    String authUri = uri.getScheme() + "://" + uri.getAuthority() + "/j_security_check/";
//
//    // nonce
//    byte[] bytes = new byte[16];
//    (new Random()).nextBytes(bytes);
//    String clientNonce = Base64.STANDARD.encodeBytes(bytes);
//
//    c.disconnect();
//    (new NiagaraScram(authUri, clientNonce)).authenticate();
//  }
//
//  class NiagaraScram
//  {
//    NiagaraScram(String authUri, String clientNonce) throws Exception
//    {
//      this.authUri = authUri;
//      this.clientNonce = clientNonce;
//    }
//
//    void authenticate() throws Exception
//    {
//      firstMsg();
//      finalMsg();
//      upgradeInsecureReqs();
//    }
//
//    private void firstMsg() throws Exception
//    {
//      // create first message
//      this.firstMsgBare = "n=" + user + ",r=" + clientNonce;
//
//      // create request content
//      String content = encodePost("sendClientFirstMessage",
//        "clientFirstMessage", "n,," + firstMsgBare);
//
//      // set cookie
//      cookieProperty = new Property("Cookie", "niagara_userid=" + user);
//
//      // post
//      String res = postString(authUri, content, MIME_TYPE);
//
//      // save the resulting sessionId
//      String cookie = cookieProperty.value;
//      int a = cookie.indexOf("JSESSIONID=");
//      int b = cookie.indexOf(";", a);
//      sessionId = (b == -1) ?
//          cookie.substring(a + "JSESSIONID=".length()) :
//          cookie.substring(a + "JSESSIONID=".length(), b);
//
//      // store response
//      this.firstMsgResult = res;
//    }
//
//    private void finalMsg() throws Exception
//    {
//      // parse first msg response
//      Map firstMsg = decodeMsg(firstMsgResult);
//      String nonce = (String) firstMsg.get("r");
//      int iterations = Integer.parseInt((String) firstMsg.get("i"));
//      String salt = (String) firstMsg.get("s");
//
//      // check client nonce
//      if (!clientNonce.equals(nonce.substring(0, clientNonce.length())))
//        throw new CallAuthException("Authentication failed");
//
//      // create salted password
//      byte[] saltedPassword = CryptoUtil.pbk(
//          "PBKDF2WithHmacSHA256",
//          strBytes(pass),
//          Base64.STANDARD.decodeBytes(salt),
//          iterations, 32);
//
//      // create final message
//      String finalMsgWithoutProof = "c=biws,r=" + nonce;
//      String authMsg = firstMsgBare + "," + firstMsgResult + "," + finalMsgWithoutProof;
//      String clientProof = createClientProof(saltedPassword, strBytes(authMsg));
//      String clientFinalMsg = finalMsgWithoutProof + ",p=" + clientProof;
//
//      // create request content
//      String content = encodePost("sendClientFinalMessage",
//        "clientFinalMessage", clientFinalMsg);
//
//      // set cookie
//      cookieProperty = new Property("Cookie",
//          "JSESSIONID=" + sessionId + "; " +
//          "niagara_userid=" + user);
//
//      // post
//      postString(authUri, content, MIME_TYPE);
//    }
//
//    private void upgradeInsecureReqs()
//    {
//      try
//      {
//        URL url = new URL(authUri);
//        HttpURLConnection c = openHttpConnection(url, "GET");
//        try
//        {
//          c.setRequestProperty("Connection", "Close");
//          c.setRequestProperty("Content-Type", "text/plain");
//          c.setRequestProperty("Upgrade-Insecure-Requests", "1");
//          c.setRequestProperty(cookieProperty.key, cookieProperty.value);
//
//          c.connect();
//
//          // check for 302
//          if (c.getResponseCode() != 302)
//            throw new CallHttpException(c.getResponseCode(), c.getResponseMessage());
//
//          // discard response
//          Reader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
//          int n;
//          while ((n = r.read()) > 0);
//        }
//        finally
//        {
//          try { c.disconnect(); } catch(Exception e) {}
//        }
//      }
//      catch (Exception e) { throw new CallNetworkException(e); }
//    }
//
//    private String createClientProof(byte[] saltedPassword, byte[] authMsg) throws Exception
//    {
//      byte[] clientKey = CryptoUtil.hmac("SHA-256", strBytes("Client Key"), saltedPassword);
//      byte[] storedKey = MessageDigest.getInstance("SHA-256").digest(clientKey);
//      byte[] clientSig = CryptoUtil.hmac("SHA-256", authMsg, storedKey);
//
//      byte[] clientProof = new byte[clientKey.length];
//      for (int i = 0; i < clientKey.length; i++)
//          clientProof[i] = (byte) (clientKey[i] ^ clientSig[i]);
//
//      return Base64.STANDARD.encodeBytes(clientProof);
//    }
//
//    private Map decodeMsg(String str)
//    {
//      // parse comma-delimited sequence of props formatted "<key>=<value>"
//      Map map = new HashMap();
//      int a = 0;
//      int b = 1;
//      while (b < str.length())
//      {
//        if (str.charAt(b) == ',') {
//          String entry = str.substring(a,b);
//          int n = entry.indexOf("=");
//          map.put(entry.substring(0,n), entry.substring(n+1));
//          a = b+1;
//          b = a+1;
//        }
//        else {
//          b++;
//        }
//      }
//      String entry = str.substring(a);
//      int n = entry.indexOf("=");
//      map.put(entry.substring(0,n), entry.substring(n+1));
//      return map;
//    }
//
//    private String encodePost(String action, String msgKey, String msgVal)
//    {
//      return "action=" + action + "&" + msgKey + "=" + msgVal;
//    }
//
//    private byte[] strBytes(String text) throws Exception
//    {
//      return text.getBytes("UTF-8");
//    }
//
//    private static final String MIME_TYPE = "application/x-niagara-login-support; charset=UTF-8";
//
//    private final String authUri;
//    private final String clientNonce;
//    private String firstMsgBare;
//    private String firstMsgResult;
//    private String sessionId;
//  }

