//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   02 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import org.projecthaystack.util.Base64;

import java.net.HttpURLConnection;

/**
 * BasicScheme
 */
final public class BasicScheme extends AuthScheme
{
  public BasicScheme()
  {
    super("basic");
  }

  public AuthMsg onClient(AuthClientContext cx, AuthMsg msg)
  {
    throw new UnsupportedOperationException();
  }

  public boolean onClientNonStd(AuthClientContext cx, HttpURLConnection resp,
                                String content)
  {
    if (!use(resp, content)) return false;

    String cred = Base64.STANDARD.encodeUTF8(cx.user + ":" + cx.pass);

    // make another qrequest to verify
    String headerKey = "Authorizaton";
    String headerVal = "Basic " + cred;

    HttpURLConnection c = null;
    try
    {
      // make another request to verify
      c = cx.prepare(cx.openHttpConnection(cx.uri, "GET"));
      c.setRequestProperty(headerKey, headerVal);
      c.connect();
      if (c.getResponseCode() != 200)
        throw new AuthException("Basic auth failed: " + c.getResponseCode() + " " + c.getResponseMessage());

      // pass Authorization and Cookie headers for future requests
      cx.headers.put(headerKey, headerVal);
      cx.addCookiesToHeaders(c);
      return true;
    }
    catch (Exception e)
    {
      throw new AuthException("basic authentication failed", e);
    }
    finally
    {
      if (c != null) try { c.disconnect(); } catch (Exception e) { }
    }
  }

  public static boolean use(HttpURLConnection c, String content)
  {
    try
    {
      int resCode = c.getResponseCode();

      String wwwAuth = c.getHeaderField("WWW-Authenticate");
      if (wwwAuth == null) wwwAuth = "";
      wwwAuth = wwwAuth.toLowerCase();

      String server = c.getHeaderField("Server");
      if (server == null) server = "";
      server = server.toLowerCase();

      // standard basic challenge
      if (resCode == 401 && wwwAuth.startsWith("basic")) return true;

      // fallback to basic if server says it's Niagara.
      if (server.startsWith("niagara")) return true;

      // detect N4 by their bug - lolol
      if (resCode == 500 && content != null && content.contains("wrong 4-byte ending")) return true;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return false;
  }
}
