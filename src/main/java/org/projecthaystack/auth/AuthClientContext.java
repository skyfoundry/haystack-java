//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import org.projecthaystack.client.CallNetworkException;
import org.projecthaystack.client.HClient;
import org.projecthaystack.util.Base64;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

final public class AuthClientContext
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public AuthClientContext(String uri, String user, String pass)
  {
    this.uri  = uri;
    this.user = user;
    this.pass = pass;
  }

//////////////////////////////////////////////////////////////////////////
// State
//////////////////////////////////////////////////////////////////////////

  /** URI used to open the connection */
  public final String uri;

  /** Username used to open the connection */
  public final String user;

  /** Plaintext password for authentication */
  public String pass;

  /** User agent string */
  public String userAgent = "HaystackJava";

  /** Headers we wish to use for authentication requests */
  public Map headers = new HashMap();

  /**
   * Stash allows you to store state between messages
   * while authenticating with the server.
   */
  public Map stash = new HashMap();

  /** Have we successfully authenticated to the server */
  public boolean isAuthenticated() { return this.authenticated; }

//////////////////////////////////////////////////////////////////////////
// Open
//////////////////////////////////////////////////////////////////////////

  public AuthClientContext open()
  {
    try
    {
      // send initial hello message
      HttpURLConnection helloResp = sendHello();
//try { dumpRes(helloResp, false); } catch (Exception e) { e.printStackTrace(); }

      // first try standard authentication va RFC 7235 process
      if (openStd(helloResp)) return success();

      // check if we have a 200
      if (helloResp.getResponseCode() == 200) return success();

      String content = readContent(helloResp);
      AuthScheme[] schemes = AuthScheme.list();
      for (int i = 0; i< schemes.length; ++i)
      {
        if (schemes[i].onClientNonStd(this, helloResp, content))
          return success();
      }

      // give up
      int resCode = helloResp.getResponseCode();
      String resServer = helloResp.getHeaderField("Server");
      if (resCode / 100 >= 4) throw new IOException("HTTP error code: " + resCode); // 4xx or 5xx
      throw new AuthException("No suitable auth scheme for: " + resCode + " " + resServer);
    }
    catch (AuthException e) { throw e; }
    catch (Exception e)
    {
      throw new AuthException("authenticate failed", e);
    }
    finally
    {
      this.pass = null;
      this.stash.clear();
    }
  }

  private HttpURLConnection sendHello() throws Exception
  {
    // hello message
    Map params = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    params.put("username", Base64.URI.encodeUTF8(this.user));
    AuthMsg hello = new AuthMsg("hello", params);
    return getAuth(hello);
  }

  private AuthClientContext success()
  {
    this.authenticated = true;
    return this;
  }

  /**
   * Attempt standard authentication via Haystack/RFC 7235
   *
   * @param resp The response to the hello message
   * @return true if haystack authentciation was used, false if the
   * server does not appear to implement RFC 7235.
   */
  private boolean openStd(HttpURLConnection resp) throws IOException
  {
    // must be 401 challenge with WWW-Authenticate header
    if (resp.getResponseCode() != 401) return false;
    String wwwAuth = resHeader(resp, "WWW-Authenticate");

    // don't use this mechanism for Basic which we
    // handle as a non-standard scheme because the headers
    // don't fit nicely into our restricted AuthMsg format
    if (wwwAuth.toLowerCase().startsWith("basic")) return false;

    // process res/req messages until we have 200 or non-401 failure
    AuthScheme scheme = null;
    for (int loopCount = 0; true; ++loopCount)
    {
      // sanity check that we don't loop too many times
      if (loopCount > 5) throw new AuthException("Loop count exceeded");

      // parse the WWW-Auth header and use the first scheme
      String header     = resHeader(resp, "WWW-Authenticate");
      AuthMsg[] resMsgs = AuthMsg.listFromStr(header);
      AuthMsg resMsg    = resMsgs[0];
      scheme = AuthScheme.find(resMsg.scheme);

      // let scheme handle message
      AuthMsg reqMsg = scheme.onClient(this, resMsg);

      // send request back to the server
      resp = getAuth(reqMsg);
//try { dumpRes(resp, false); } catch (Exception e) { e.printStackTrace(); }

      // 200 means we are done, 401 means keep looping,
      // consider anything else a failure
      if (resp.getResponseCode() == 200) break;
      if (resp.getResponseCode() == 401) continue;
      throw new AuthException("" + resp.getResponseCode() + " " + resp.getResponseMessage());
    }

    // init the bearer token
    String authInfo = resHeader(resp, "Authentication-Info");
    AuthMsg authInfoMsg = AuthMsg.fromStr("bearer " + authInfo);

    // callback to scheme for client success
    scheme.onClientSuccess(this, authInfoMsg);

    // only keep authToken parameter for Authorization header
    authInfoMsg = new AuthMsg("bearer", new String[] {"authToken", authInfoMsg.param("authToken") });
    this.headers.put("Authorization", authInfoMsg.toString());

    // we did it!
    return true;
  }

////////////////////////////////////////////////////////////////
// HTTP Messaging
////////////////////////////////////////////////////////////////

  /**
   * Get a new http connection to the given uri.
   */
  public HttpURLConnection openHttpConnection(String uri, String method) throws IOException
  {
    return HClient.openHttpConnection(new URL(uri), method, this.connectTimeout, this.readTimeout);
  }

  public void addCookiesToHeaders(HttpURLConnection c)
  {
    List cookies = (List)c.getHeaderFields().get("Set-Cookie");
    if (cookies == null || cookies.isEmpty()) return;
    Iterator iter = cookies.iterator();
    StringBuffer sb = new StringBuffer();
    boolean first = true;
    while (iter.hasNext())
    {
      String cookie = (String)iter.next();
      int semi = cookie.indexOf(";");
      if (semi <= 0)  continue;

      if (first) first = false;
      else sb.append(";");

      // add name=value pair
      sb.append(cookie.substring(0, semi));
    }
    this.headers.put("Cookie", sb.toString());
  }

  private HttpURLConnection getAuth(AuthMsg msg) throws IOException
  {
    // all AuthClientContext requests are GET message to the /about uri
    HttpURLConnection c = prepare(openHttpConnection(uri, "GET"));

    // set Authorization header
    c.setRequestProperty("Authorization", msg.toString());

    return get(c);
  }

  /**
   * Prepares a {@link HttpURLConnection} instance with the auth cookies/headers
   *
   * @param c the {@link HttpURLConnection} to configure
   * @return the configured {@link HttpURLConnection}
   */
  public HttpURLConnection prepare(HttpURLConnection c)
  {
    // set headers
    if (this.headers == null) this.headers = new HashMap();
    Iterator iter = this.headers.keySet().iterator();
    while (iter.hasNext())
    {
      String key = (String)iter.next();
      String val = (String)this.headers.get(key);
      c.setRequestProperty(key, val);
    }
    if (this.userAgent != null) c.setRequestProperty("User-Agent", this.userAgent);
    return c;
  }

  private HttpURLConnection get(HttpURLConnection c) throws IOException
  {
    // connect and return response
    try
    {
      c.connect();
      return c;
    }
    finally
    {
      try { c.disconnect(); } catch (Exception e) { }
    }
  }

  private String readContent(HttpURLConnection c) throws IOException
  {
    // force HttpURLConnection to run request
    c.getResponseCode();

    // If there is non content-type header, then assume no content.
    if (c.getHeaderField("Content-Type") == null) return null;

    try
    {
      // check for error stream first; if null, then get standard input stream
      InputStream is = c.getErrorStream();
      if (is == null)
        is = c.getInputStream();

      // read content
      StringBuffer sb = new StringBuffer();
      try
      {
        is = new BufferedInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null)
        {
          sb.append(line);
        }
        return sb.toString();
      }
      finally
      {
        if (is != null) try { is.close(); } catch (Exception e) { }
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
      return null;
    }
  }

  private String resHeader(HttpURLConnection c, String name)
  {
    String val = c.getHeaderField(name);
    if (val == null) throw new AuthException("Missing required header: " + name);
    return val;
  }

//////////////////////////////////////////////////////////////////////////
// Debug Utils
//////////////////////////////////////////////////////////////////////////

  private void dumpRes(HttpURLConnection c, boolean body) throws Exception
  {
    System.out.println("====  " + c.getURL());
    System.out.println("res: " + c.getResponseCode() + " " + c.getResponseMessage() );
    for (Iterator it = c.getHeaderFields().keySet().iterator(); it.hasNext(); )
    {
      String key = (String)it.next();
      String val = c.getHeaderField(key);
      System.out.println(key + ": " + val);
    }
    System.out.println();
    if (body)
    {
      InputStream in = c.getInputStream();
      int n;
      while ((n = in.read()) > 0) System.out.print((char)n);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////


  /** Set true after successful authentication */
  private boolean authenticated = false;

  /** Timeout in milliseconds for opening the HTTP socket */
  public int connectTimeout = 60 * 1000;

  /** Timeout in milliseconds for reading from the HTTP socket */
  public int readTimeout = 60 * 1000;
}

