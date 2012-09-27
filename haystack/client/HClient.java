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
public class HClient extends HProj
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /**
   * Open a connection and authenticate to a Haystack HTTP server.
   */
  public static HClient open(String uri, String user, String pass)
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
   * Call "about" to query summary info.
   */
  public HDict about()
  {
    return call("about", HGrid.EMPTY).row(0);
  }

  /**
   * Call "ops" to query which operations are supported by server.
   */
  public HGrid ops()
  {
    return call("ops", HGrid.EMPTY);
  }

  /**
   * Call "formats" to query which MIME formats are available.
   */
  public HGrid formats()
  {
    return call("formats", HGrid.EMPTY);
  }

//////////////////////////////////////////////////////////////////////////
// Reads
//////////////////////////////////////////////////////////////////////////

  protected HDict onReadById(HRef id)
  {
    HGrid res = readByIds(new HRef[] { id });
    return res.isEmpty() ? null : res.row(0);
  }

  protected HGrid onReadByIds(HRef[] ids)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    for (int i=0; i<ids.length; ++i)
      b.addRow(new HVal[] { ids[i] });
    HGrid req = b.toGrid();
    return call("read", req);
  }

  protected HGrid onReadAll(String filter, int limit)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("filter");
    b.addCol("limit");
    b.addRow(new HVal[] { HStr.make(filter), HNum.make(limit) });
    HGrid req = b.toGrid();
    return call("read", req);
  }

//////////////////////////////////////////////////////////////////////////
// Evals
//////////////////////////////////////////////////////////////////////////

  /**
   * Call "eval" operation to evaluate a vendor specific
   * expression on the server:
   *   - SkySpark: any Axon expression
   *
   * Raise CallErrException if the server raises an exception.
   */
  public HGrid eval(String expr)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("expr");
    b.addRow(new HVal[] { HStr.make(expr) });
    HGrid req = b.toGrid();
    return call("eval", req);
  }

  /**
   * Convenience for "evalAll(HGrid, true)".
   */
  public HGrid[] evalAll(String[] exprs)
  {
    return evalAll(exprs, true);
  }

  /**
   * Convenience for "evalAll(HGrid, checked)".
   */
  public HGrid[] evalAll(String[] exprs, boolean checked)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("expr");
    for (int i=0; i<exprs.length; ++i)
      b.addRow(new HVal[] { HStr.make(exprs[i]) });
    return evalAll(b.toGrid(), checked);
  }

  /**
   * Call "evalAll" operation to evaluate a batch of vendor specific
   * expressions on the server. See "eval" method for list of vendor
   * expression formats.  The request grid must specify an "expr" column.
   * A separate grid is returned for each row in the request.  If checked
   * is false, then this call does *not* automatically check for error
   * grids.  Client code must individual check each grid for partial
   * failures using "Grid.isErr".  If checked is true and one of the
   * requests failed, then raise CallErrException for first failure.
   */
  public HGrid[] evalAll(HGrid req, boolean checked)
  {
    String reqStr = HZincWriter.gridToString(req);
    String resStr = postString("evalAll", reqStr);
    HGrid[] res = new HZincReader(resStr).readGrids();
    if (checked)
    {
      for (int i=0; i<res.length; ++i)
        if (res[i].isErr()) throw new CallErrException(res[i]);
    }
    return res;
  }

//////////////////////////////////////////////////////////////////////////
// Call
//////////////////////////////////////////////////////////////////////////

  /**
   * Make a call to the given operation.  The request grid is posted
   * to the URI "this.uri+op" and the response is parsed as a grid.
   * Raise CallNetworkException if there is a communication I/O error.
   * Raise CallErrException if there is a server side error and an error
   * grid is returned.
   */
  public HGrid call(String op, HGrid req)
  {
    HGrid res = postGrid(op, req);
    if (res.isErr()) throw new CallErrException(res);
    return res;
  }

  private HGrid postGrid(String op, HGrid req)
  {
    String reqStr = HZincWriter.gridToString(req);
    String resStr = postString(op, reqStr);
    return new HZincReader(resStr).readGrid();
  }

  private String postString(String op, String req)
  {
    try
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
          throw new CallHttpException(c.getResponseCode(), c.getResponseMessage());

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
    catch (Exception e) { throw new CallNetworkException(e); }
  }

//////////////////////////////////////////////////////////////////////////
// Authentication
//////////////////////////////////////////////////////////////////////////

  /**
   * Authenticate with the server.  Currently we just support
   * SkySpark nonce based HMAC SHA-1 mechanism.
   */
  private void authenticate()
  {
    try
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
        if (c.getResponseCode() == 200) return;
        if (authUri == null) throw new CallAuthException("Missing 'Folio-Auth-Api-Uri' header");

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
        String salt = (String)props.get("userSalt"); if (salt == null) throw new CallAuthException("auth missing 'userSalt'");
        String nonce = (String)props.get("nonce");   if (nonce == null) throw new CallAuthException("auth missing 'nonce'");

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
        if (c.getResponseCode() != 200) throw new CallAuthException("Invalid username/password [" + c.getResponseCode() + "]");

        // parse successful authentication to get cookie value
        props = parseResProps(c.getInputStream());
        this.cookie = (String)props.get("cookie");
        if (this.cookie == null) throw new CallAuthException("auth missing 'cookie'");
      }
      finally
      {
        try { if (c != null) c.disconnect(); } catch(Exception e) {}
      }
    }
    catch (CallException e) { throw e; }
    catch (Exception e) { throw new CallNetworkException(e); }
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