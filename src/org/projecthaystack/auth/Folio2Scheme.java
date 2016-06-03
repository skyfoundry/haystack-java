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

import java.io.*;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.HashMap;

/**
 * Folio2Scheme implements clinet side legacy 2.1 authentication for SkySpark.
 */
final public class Folio2Scheme extends AuthScheme
{
  public Folio2Scheme()
  {
    super("folio2");
  }

  public AuthMsg onClient(AuthClientContext cx, AuthMsg msg)
  {
    throw new UnsupportedOperationException();
  }

  public boolean onClientNonStd(AuthClientContext cx, HttpURLConnection resp, String content)
  {
    String authUri = resp.getHeaderField("Folio-Auth-Api-Uri");
    if (authUri == null) return false;

    // make request to auth URI to get salt, nonce
    String baseUri = cx.uri.substring(0, cx.uri.indexOf('/', 9));
    String uri = baseUri + authUri + "?" + cx.user;
    HttpURLConnection c = null;
    try
    {
      c = cx.openHttpConnection(uri, "GET");
      c.connect();

      // parse response as name:value pairs
      HashMap props = parseResProps(c.getInputStream());

      // get salt and nonce values
      String salt = (String)props.get("userSalt"); if (salt == null) throw new AuthException("auth missing 'userSalt'");
      String nonce = (String)props.get("nonce");   if (nonce == null) throw new AuthException("auth missing 'nonce'");

      // compute hmac
      byte[] hmacBytes = CryptoUtil.hmac("SHA-1", (cx.user + ":" + salt).getBytes(), cx.pass.getBytes());
      String hmac = Base64.STANDARD.encodeBytes(hmacBytes);

      // compute digest with nonce
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update((hmac+":"+nonce).getBytes());
      String digest = Base64.STANDARD.encodeBytes(md.digest());

      // post back nonce/digest to auth URI
      c.disconnect();
      c = cx.openHttpConnection(uri, "POST");
      c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
      c.setDoInput(true);
      c.setDoOutput(true);
      c.setInstanceFollowRedirects(false);
      Writer cout = new OutputStreamWriter(c.getOutputStream(), "UTF-8");
      cout.write("nonce:" + nonce + "\n");
      cout.write("digest:" + digest + "\n");
      cout.close();
      c.connect();
      if (c.getResponseCode() != 200) throw new AuthException("Invalid username/password [" + c.getResponseCode() + "]");

      // parse successful authentication to get cookie value
      props = parseResProps(c.getInputStream());
      String cookie = (String)props.get("cookie");
      if (cookie == null) throw new AuthException("auth missing 'cookie'");
      cx.headers.put("Cookie", cookie);
      return true;
    }
    catch (Exception e)
    {
      throw new AuthException("folio2 failed", e);
    }
    finally
    {
      try { c.disconnect(); } catch (Exception e) { }
    }
  }

  private HashMap parseResProps(InputStream in) throws Exception
  {
    // parse response as name:value pairs
    HashMap props = new HashMap();
    BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    for (String line; (line = r.readLine()) != null; )
    {
      int colon = line.indexOf(':');
      String name = line.substring(0, colon).trim();
      String val  = line.substring(colon+1).trim();
      props.put(name, val);
    }
    return props;
  }
}
