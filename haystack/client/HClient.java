//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   11 Jul 2011  Brian Frank  Creation
//   26 Sep 2012  Brian Frank  Revamp original code
//
package haystack.client;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import haystack.*;
import haystack.io.*;

/**
 * HClient manages a logical connection to a HTTP REST haystack server.
 */
public class HClient
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /**
   * Open a connection and authenticate to a Haystack HTTP server.
   */
  public static HClient open(String uri, String user, String pass) throws Exception
  {
    HClient client = new HClient(uri, user, pass);
    client.authenticate();
    return client;
  }

  private HClient(String uri, String user, String pass)
  {
    // check uri
    if (!uri.startsWith("http://")) throw new IllegalArgumentException("Invalid uri format: " + uri);
    if (!uri.endsWith("/")) uri = uri + "/";

    // sanity check arguments
    if (user.length() == 0) throw new IllegalArgumentException("user cannot be empty string");
    if (pass.length() == 0) throw new IllegalArgumentException("password cannot be empty string");

    this.uri  = uri;
    this.user = user;
    this.pass = pass;
  }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /** Base URI for connection such as "http://host/api/demo/".
      This string always ends with slash. */
  public final String uri;

//////////////////////////////////////////////////////////////////////////
// Operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Read "about" to query summary info.
   */
  public HGrid about() throws Exception
  {
    return postGrid("about", HGrid.EMPTY);
  }

  /**
   * Read "ops" to query which operations are supported by server.
   */
  public HGrid ops() throws Exception
  {
    return postGrid("ops", HGrid.EMPTY);
  }

  /**
   * Read "formats" to query which MIME formats are available.
   */
  public HGrid formats() throws Exception
  {
    return postGrid("formats", HGrid.EMPTY);
  }

//////////////////////////////////////////////////////////////////////////
// Implementation
//////////////////////////////////////////////////////////////////////////

  private HGrid postGrid(String op, HGrid req) throws Exception
  {
    String reqStr = HZincWriter.gridToString(req);
    String resStr = postString(op, reqStr);
    return new HZincReader(resStr).readGrid();
  }

  private String postString(String op, String req) throws Exception
  {
    // setup the POST request
    URL url = new URL(uri + op);
    HttpURLConnection c = (HttpURLConnection)url.openConnection();
    try
    {
      c.setRequestMethod("POST");
      c.setInstanceFollowRedirects(false);
      c.setDoOutput(true);
      c.setDoInput(true);
      c.setRequestProperty("Connection", "Close");
      c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
      if (cookie != null) c.setRequestProperty("Cookie", cookie);
      c.connect();

      // post expression
      Writer cout = new OutputStreamWriter(c.getOutputStream(), "UTF-8");
      cout.write(req);
      cout.close();

      // check for successful request
      if (c.getResponseCode() != 200)
        throw new Exception("Request failed HTTP code: " + c.getResponseCode());

      // read response into string
      StringBuilder s = new StringBuilder(1024);
      Reader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
      int n;
      while ((n = r.read()) > 0) s.append((char)n);
      return s.toString();
    }
    finally
    {
      try { c.disconnect(); } catch(Exception e) {}
    }
  }

//////////////////////////////////////////////////////////////////////////
// Authentication
//////////////////////////////////////////////////////////////////////////

  /**
   * Authenticate with the server.  Currently we just support
   * SkySpark nonce based HMAC SHA-1 mechanism.
   */
  private void authenticate() throws Exception
  {
    HttpURLConnection c = null;
    try
    {
      // make request to about to get headers
      URL url = new URL(this.uri + "about");
      c = (HttpURLConnection)url.openConnection();
      c.setRequestMethod("GET");
      c.setInstanceFollowRedirects(false);
      c.connect();
      String authUri = c.getHeaderField("Folio-Auth-Api-Uri");
      c.disconnect();
      if (authUri == null) throw new Exception("Missing 'Folio-Auth-Api-Uri' header");

      // make request to auth URI to get salt, nonce
      String baseUri = uri.substring(0, uri.indexOf('/', 9));
      url = new URL(baseUri + authUri + "?" + user);
      c = (HttpURLConnection)url.openConnection();
      c.setRequestMethod("GET");
      c.setInstanceFollowRedirects(false);
      c.connect();

      // parse response as name:value pairs
      HashMap props = parseResProps(c.getInputStream());

      // get salt and nonce values
      String salt = (String)props.get("userSalt"); if (salt == null) throw new Exception("auth missing 'userSalt'");
      String nonce = (String)props.get("nonce");   if (nonce == null) throw new Exception("auth missing 'nonce'");

      // compute hmac (note Java doesn't allow empty password)
      Mac mac = Mac.getInstance("HmacSHA1");
      SecretKeySpec secret = new SecretKeySpec(pass.getBytes(),"HmacSHA1");
      mac.init(secret);
      String hmac = toBase64(mac.doFinal((user + ":" + salt).getBytes()));

      // compute digest with nonce
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.update((hmac+":"+nonce).getBytes());
      String digest = toBase64(md.digest());

      // post back nonce/digest to auth URI
      c.disconnect();
      c = (HttpURLConnection)url.openConnection();
      c.setRequestMethod("POST");
      c.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
      c.setDoInput(true);
      c.setDoOutput(true);
      c.setInstanceFollowRedirects(false);
      Writer cout = new OutputStreamWriter(c.getOutputStream(), "UTF-8");
      cout.write("nonce:" + nonce + "\n");
      cout.write("digest:" + digest + "\n");
      cout.close();
      c.connect();
      if (c.getResponseCode() != 200) throw new Exception("Invalid username/password [" + c.getResponseCode() + "]");

      // parse successful authentication to get cookie value
      props = parseResProps(c.getInputStream());
      this.cookie = (String)props.get("cookie");
      if (this.cookie == null) throw new Exception("auth missing 'cookie'");
    }
    finally
    {
      try { if (c != null) c.disconnect(); } catch(Exception e) {}
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

//////////////////////////////////////////////////////////////////////////
// Debug Utils
//////////////////////////////////////////////////////////////////////////

  /*
  private void dumpRes(HttpURLConnection c) throws Exception
  {
    System.out.println("====  " + c.getURL());
    System.out.println("res: " + c.getResponseCode() + " " + c.getResponseMessage() );
    for (int i=1; c.getHeaderFieldKey(i) != null; ++i)
    {
      System.out.println(c.getHeaderFieldKey(i) + ": " + c.getHeaderField(i));
      i++;
    }
    System.out.println();
    InputStream in = c.getInputStream();
    int n;
    while ((n = in.read()) > 0) System.out.print((char)n);
  }
  */

//////////////////////////////////////////////////////////////////////////
// Base64 Utils
//////////////////////////////////////////////////////////////////////////

  private static String toBase64(byte[] buf) { return toBase64(buf, buf.length); }
  private static String toBase64(byte[] buf, int size)
  {
    StringBuilder s = new StringBuilder(size*2);
    int i = 0;

    // append full 24-bit chunks
    int end = size-2;
    for (; i<end; i += 3)
    {
      int n = ((buf[i] & 0xff) << 16) + ((buf[i+1] & 0xff) << 8) + (buf[i+2] & 0xff);
      s.append(base64chars[(n >>> 18) & 0x3f]);
      s.append(base64chars[(n >>> 12) & 0x3f]);
      s.append(base64chars[(n >>> 6) & 0x3f]);
      s.append(base64chars[n & 0x3f]);
    }

    // pad and encode remaining bits
    int rem = size - i;
    if (rem > 0)
    {
      int n = ((buf[i] & 0xff) << 10) | (rem == 2 ? ((buf[size-1] & 0xff) << 2) : 0);
      s.append(base64chars[(n >>> 12) & 0x3f]);
      s.append(base64chars[(n >>> 6) & 0x3f]);
      s.append(rem == 2 ? base64chars[n & 0x3f] : '=');
      s.append('=');
    }

    return s.toString();
  }

  static char[] base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private final String user;
  private final String pass;
  private String cookie;

}