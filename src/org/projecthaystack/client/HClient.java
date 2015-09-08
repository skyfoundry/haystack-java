//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   11 Jul 2011  Brian Frank  Creation
//   26 Sep 2012  Brian Frank  Revamp original code
//
package org.projecthaystack.client;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import org.projecthaystack.*;
import org.projecthaystack.io.*;
import org.projecthaystack.util.*;
import org.projecthaystack.util.Base64;

/**
 * HClient manages a logical connection to a HTTP REST haystack server.
 *
 * @see <a href='http://project-haystack.org/doc/Rest'>Project Haystack</a>
 */
public class HClient extends HProj
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for construction and call to open().
   */
  public static HClient open(String uri, String user, String pass)
  {
    return new HClient(uri, user, pass).open();
  }

  /**
   * Constructor with URI to server's API and authentication credentials.
   */
  public HClient(String uri, String user, String pass)
  {
    // check uri
    if (!uri.startsWith("http://") && !uri.startsWith("https://")) throw new IllegalArgumentException("Invalid uri format: " + uri);
    if (!uri.endsWith("/")) uri = uri + "/";

    // sanity check arguments
    if (user.length() == 0) throw new IllegalArgumentException("user cannot be empty string");

    this.uri  = uri;
    this.user = user;
    this.pass = pass;
  }

//////////////////////////////////////////////////////////////////////////
// State
//////////////////////////////////////////////////////////////////////////

  /** Base URI for connection such as "http://host/api/demo/".
      This string always ends with slash. */
  public final String uri;

  /** Timeout in milliseconds for opening the HTTP socket */
  public int connectTimeout = 60 * 1000;

  /** Timeout in milliseconds for reading from the HTTP socket */
  public int readTimeout = 60 * 1000;

//////////////////////////////////////////////////////////////////////////
// Operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Authenticate the client and return this.
   */
  public HClient open()
  {
    authenticate();
    return this;
  }

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
    HGrid res = readByIds(new HRef[] { id }, false);
    if (res.isEmpty()) return null;
    HDict rec = res.row(0);
    if (rec.missing("id")) return null;
    return rec;
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
    String resStr = postString(uri + "evalAll", reqStr);
    HGrid[] res = new HZincReader(resStr).readGrids();
    if (checked)
    {
      for (int i=0; i<res.length; ++i)
        if (res[i].isErr()) throw new CallErrException(res[i]);
    }
    return res;
  }

//////////////////////////////////////////////////////////////////////////
// Watches
//////////////////////////////////////////////////////////////////////////

  /**
   * Create a new watch with an empty subscriber list.  The dis
   * string is a debug string to keep track of who created the watch.
   */
  public HWatch watchOpen(String dis, HNum lease)
  {
    return new HClientWatch(this, dis, lease);
  }

  /**
   * List the open watches associated with this HClient.
   * This list does *not* contain a watch until it has been successfully
   * subscribed and assigned an identifier by the server.
   */
  public HWatch[] watches()
  {
    return (HWatch[])watches.values().toArray(new HWatch[watches.size()]);
  }

  /**
   * Lookup a watch by its unique identifier associated with this HClient.
   * If not found return null or raise UnknownWatchException based on
   * checked flag.
   */
  public HWatch watch(String id, boolean checked)
  {
    HWatch w = (HWatch)watches.get(id);
    if (w != null) return w;
    if (checked) throw new UnknownWatchException(id);
    return null;
  }

  HGrid watchSub(HClientWatch w, HRef[] ids, boolean checked)
  {
    if (ids.length == 0) throw new IllegalArgumentException("ids are empty");
    if (w.closed) throw new IllegalStateException("watch is closed");

    // grid meta
    HGridBuilder b = new HGridBuilder();
    if (w.id != null) b.meta().add("watchId", w.id);
    if (w.desiredLease != null) b.meta().add("lease", w.desiredLease);
    b.meta().add("watchDis", w.dis);

    // grid rows
    b.addCol("id");
    for (int i=0; i<ids.length; ++i)
      b.addRow(new HVal[] { ids[i] });

    // make request
    HGrid res;
    try
    {
      HGrid req = b.toGrid();
      res = call("watchSub", req);
    }
    catch (CallErrException e)
    {
      // any server side error is considered close
      watchClose(w, false);
      throw e;
    }

    // make sure watch is stored with its watch id
    if (w.id == null)
    {
      w.id = res.meta().getStr("watchId");
      w.lease = (HNum)res.meta().get("lease");
      watches.put(w.id, w);
    }

    // if checked, then check it
    if (checked)
    {
      if (res.numRows() != ids.length && ids.length > 0)
        throw new UnknownRecException(ids[0]);
      for (int i=0; i<res.numRows(); ++i)
        if (res.row(i).missing("id")) throw new UnknownRecException(ids[i]);
    }
    return res;
  }

  void watchUnsub(HClientWatch w, HRef[] ids)
  {
    if (ids.length == 0) throw new IllegalArgumentException("ids are empty");
    if (w.id == null) throw new IllegalStateException("nothing subscribed yet");
    if (w.closed) throw new IllegalStateException("watch is closed");

    // grid meta
    HGridBuilder b = new HGridBuilder();
    b.meta().add("watchId", w.id);

    // grid rows
    b.addCol("id");
    for (int i=0; i<ids.length; ++i)
      b.addRow(new HVal[] { ids[i] });

    // make request
    HGrid req = b.toGrid();
    call("watchUnsub", req);
  }

  HGrid watchPoll(HClientWatch w, boolean refresh)
  {
    if (w.id == null) throw new IllegalStateException("nothing subscribed yet");
    if (w.closed) throw new IllegalStateException("watch is closed");

    // grid meta
    HGridBuilder b = new HGridBuilder();
    b.meta().add("watchId", w.id);
    if (refresh) b.meta().add("refresh");
    b.addCol("empty");

    // make request
    HGrid req = b.toGrid();
    try
    {
      return call("watchPoll", req);
    }
    catch (CallErrException e)
    {
      // any server side error is considered close
      watchClose(w, false);
      throw e;
    }
  }

  void watchClose(HClientWatch w, boolean send)
  {
    // mark flag on watch itself, short circuit if already closed
    if (w.closed) return;
    w.closed = true;

    // remove it from my lookup table
    if (w.id != null) watches.remove(w.id);

    // optionally send close message to server
    if (send)
    {
      try
      {
        HGridBuilder b = new HGridBuilder();
        b.meta().add("watchId", w.id).add("close");
        b.addCol("id");
        call("watchUnsub", b.toGrid());
      }
      catch (Exception e) {}
    }
  }

  static class HClientWatch extends HWatch
  {
    HClientWatch(HClient c, String d, HNum l) { client = c; dis = d; desiredLease = l; }
    public String id() { return id; }
    public HNum lease() { return lease; }
    public String dis() { return dis; }
    public HGrid sub(HRef[] ids, boolean checked) { return client.watchSub(this, ids, checked); }
    public void unsub(HRef[] ids) { client.watchUnsub(this, ids); }
    public HGrid pollChanges() { return client.watchPoll(this, false); }
    public HGrid pollRefresh() { return client.watchPoll(this, true); }
    public void close() { client.watchClose(this, true); }
    public boolean isOpen() { return !closed; }

    final HClient client;
    final String dis;
    final HNum desiredLease;
    String id;
    HNum lease;
    boolean closed;
  }

//////////////////////////////////////////////////////////////////////////
// PointWrite
//////////////////////////////////////////////////////////////////////////

  /**
    * Write to a given level of a writable point, and return the current status
    * of a writable point's priority array (see pointWriteArray()).
    *
    * @param id Ref identifier of writable point
    * @param level Number from 1-17 for level to write
    * @param val value to write or null to auto the level
    * @param who optional username performing the write, otherwise user dis is used
    * @param duration Number with duration unit if setting level 8
    */
  public HGrid pointWrite(
    HRef id, int level, String who,
    HVal val, HNum dur)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addCol("level");
    b.addCol("who");
    b.addCol("val");
    b.addCol("duration");

    b.addRow(new HVal[] {
      id,
      HNum.make(level),
      HStr.make(who),
      val,
      dur });

    HGrid req = b.toGrid();
    HGrid res = call("pointWrite", req);
    return res;
  }

  /**
    * Return the current status
    * of a point's priority array.
    * The result is returned grid with following columns:
    * <ul>
    *   <li>level: number from 1 - 17 (17 is default)
    *   <li>levelDis: human description of level
    *   <li>val: current value at level or null
    *   <li>who: who last controlled the value at this level
    * </ul>
    */
  public HGrid pointWriteArray(HRef id)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addRow(new HVal[] { id });

    HGrid req = b.toGrid();
    HGrid res = call("pointWrite", req);
    return res;
  }

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

  /**
   * Read history time-series data for given record and time range. The
   * items returned are exclusive of start time and inclusive of end time.
   * Raise exception if id does not map to a record with the required tags
   * "his" or "tz".  The range may be either a String or a HDateTimeRange.
   * If HTimeDateRange is passed then must match the timezone configured on
   * the history record.  Otherwise if a String is passed, it is resolved
   * relative to the history record's timezone.
   */
  public HGrid hisRead(HRef id, Object range)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("id");
    b.addCol("range");
    b.addRow(new HVal[] { id, HStr.make(range.toString()) });
    HGrid req = b.toGrid();
    HGrid res = call("hisRead", req);
    return res;
  }

  /**
   * Write a set of history time-series data to the given point record.
   * The record must already be defined and must be properly tagged as
   * a historized point.  The timestamp timezone must exactly match the
   * point's configured "tz" tag.  If duplicate or out-of-order items are
   * inserted then they must be gracefully merged.
   */
  public void hisWrite(HRef id, HHisItem[] items)
  {
    HDict meta = new HDictBuilder().add("id", id).toDict();
    HGrid req = HGridBuilder.hisItemsToGrid(meta, items);
    call("hisWrite", req);
  }

//////////////////////////////////////////////////////////////////////////
// Actions
//////////////////////////////////////////////////////////////////////////

  /**
   * Invoke a remote action using the "invokeAction" REST operation.
   */
  public HGrid invokeAction(HRef id, String action, HDict args)
  {
    HDict meta = new HDictBuilder().add("id", id).add("action", action).toDict();
    HGrid req = HGridBuilder.dictsToGrid(meta, new HDict[] { args });
    return call("invokeAction", req);
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
    String resStr = postString(uri + op, reqStr);
    return new HZincReader(resStr).readGrid();
  }

  private String postString(String uriStr, String req)
  {
    return postString(uriStr, req, null);
  }

  private String postString(String uriStr, String req, String mimeType)
  {
    try
    {
      // setup the POST request
      URL url = new URL(uriStr);
      HttpURLConnection c = openHttpConnection(url, "POST");
      try
      {
        c.setDoOutput(true);
        c.setDoInput(true);
        c.setRequestProperty("Connection", "Close");
        c.setRequestProperty("Content-Type", 
            (mimeType == null) ?
                "text/plain; charset=utf-8":
                mimeType);
        if (authProperty   != null) c.setRequestProperty(authProperty.key,   authProperty.value);
        if (cookieProperty != null) c.setRequestProperty(cookieProperty.key, cookieProperty.value);
        c.connect();

        // post expression
        Writer cout = new OutputStreamWriter(c.getOutputStream(), "UTF-8");
        cout.write(req);
        cout.close();

        // check for successful request
        if (c.getResponseCode() != 200)
          throw new CallHttpException(c.getResponseCode(), c.getResponseMessage());

        // check for response cookie
        checkSetCookie(c);

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

  private void checkSetCookie(HttpURLConnection c)
  {
    // if auth is already cookie based, we don't want to overwrite it
    if (authProperty != null && authProperty.key.equals("Cookie")) return;

    // check for Set-Cookie
    String header = c.getHeaderField("Set-Cookie");
    if (header == null) return;

    // parse cookie name=value pair
    int semi = header.indexOf(";");
    if  (semi > 0) header = header.substring(0, semi);

    // save cookie for future requests
    cookieProperty = new Property("Cookie", header);
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
        c = openHttpConnection(url, "GET");
        c.connect();

        // if client returned 200, then it is not running without security
        int respCode = c.getResponseCode();
        if (respCode == 200) return;

        String wwwAuth = c.getHeaderField("WWW-Authenticate");
        wwwAuth = (wwwAuth == null) ? "" : wwwAuth.toLowerCase();

        String server  = c.getHeaderField("Server");
        server = (server == null) ? "" : server.toLowerCase();

        // if Folio-Auth-Api-Uri then this must be folio
        String folioAuthUri = c.getHeaderField("Folio-Auth-Api-Uri");
        if (folioAuthUri != null)
        {
          authenticateFolio(c);
          return;
        }

        // if 401 with WWW-Authenticate Basic header
        if (respCode == 401 && wwwAuth.startsWith("basic")) {
            authenticateBasic(c);
            return;
        }

        // 302 from Niagara AX, switch to Basic
        if (respCode == 302 && server.startsWith("niagara")) {
            authenticateBasic(c);
            return;
        }

        // 302 from Niagara 4 (TODO: we need better info in headers)
        if (respCode == 302 && server.startsWith("jetty")) {
            authenticateNiagaraScram(c);
            return;
        }

        // 4xx or 5xx
        if (respCode / 100 >= 4) 
            throw new CallHttpException(respCode, "HTTP error");

        // give up
        throw new CallAuthException(
            "No suitable auth algorithm for: " + respCode + " " + server);
      }
      finally
      {
        try { if (c != null) c.disconnect(); } catch(Exception e) {}
      }
    }
    catch (CallException e) { throw e; }
    catch (Exception e) { throw new CallNetworkException(e); }
  }

  /**
   * Authenticate using Basic HTTP
   */
  private void authenticateBasic(HttpURLConnection c) throws Exception
  {
    // According to http://en.wikipedia.org/wiki/Basic_access_authentication,
    // we are supposed to get a "WWW-Authenticate" header, that has the 'realm' in it.
    // We don't get it, but it doesn't matter.  Just set up a Property
    // to send back Basic Authorization on subsequent requests.

    this.authProperty = new Property(
      "Authorization",
      "Basic " + Base64.STANDARD.encode(user + ":" + pass));
  }

  /**
   * Authenticate with SkySpark nonce based HMAC SHA-1 mechanism.
   */
  private void authenticateFolio(HttpURLConnection c) throws Exception
  {
    String authUri = c.getHeaderField("Folio-Auth-Api-Uri");
    c.disconnect();
    if (authUri == null) throw new CallAuthException("Missing 'Folio-Auth-Api-Uri' header");

    // make request to auth URI to get salt, nonce
    String baseUri = uri.substring(0, uri.indexOf('/', 9));
    URL url = new URL(baseUri + authUri + "?" + user);
    c = openHttpConnection(url, "GET");
    c.connect();

    // parse response as name:value pairs
    HashMap props = parseResProps(c.getInputStream());

    // get salt and nonce values
    String salt = (String)props.get("userSalt"); if (salt == null) throw new CallAuthException("auth missing 'userSalt'");
    String nonce = (String)props.get("nonce");   if (nonce == null) throw new CallAuthException("auth missing 'nonce'");

    // compute hmac
    byte[] hmacBytes = CryptoUtil.hmac("SHA-1", (user + ":" + salt).getBytes(), pass.getBytes());
    String hmac = Base64.STANDARD.encodeBytes(hmacBytes);

    // compute digest with nonce
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update((hmac+":"+nonce).getBytes());
    String digest = Base64.STANDARD.encodeBytes(md.digest());

    // post back nonce/digest to auth URI
    c.disconnect();
    c = openHttpConnection(url, "POST");
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
    String cookie = (String)props.get("cookie");
    if (cookie == null) throw new CallAuthException("auth missing 'cookie'");

    this.authProperty = new Property("Cookie", cookie);
  }

////////////////////////////////////////////////////////////////
// niagara SCRAM
////////////////////////////////////////////////////////////////

  /**
   * Authenticate using Niagara's implementation of 
   * Salted Challenge Response (SCRAM) HTTP Authentication Mechanism
   *
   * https://www.ietf.org/archive/id/draft-melnikov-httpbis-scram-auth-01.txt
   */
  private void authenticateNiagaraScram(HttpURLConnection c) throws Exception
  {
    // authentication uri
    URI uri = new URI(c.getURL().toString());
    String authUri = uri.getScheme() + "://" + uri.getAuthority() + "/j_security_check/";

    // nonce
    byte[] bytes = new byte[16];
    (new Random()).nextBytes(bytes);
    String clientNonce = Base64.STANDARD.encodeBytes(bytes);

    c.disconnect();
    (new NiagaraScram(authUri, clientNonce)).authenticate();
  }

  /**
    * NiagaraScram
    */
  class NiagaraScram 
  {
      NiagaraScram(String authUri, String clientNonce) throws Exception
      {
        this.authUri = authUri;
        this.clientNonce = clientNonce;
      }

      void authenticate() throws Exception
      {
        firstMsg();
        finalMsg();
        upgradeInsecureReqs();
      }

      private void firstMsg() throws Exception
      {
        // create first message
        this.firstMsgBare = "n=" + user + ",r=" + clientNonce;

        // create request content
        String content = encodePost("sendClientFirstMessage",
          "clientFirstMessage", "n,," + firstMsgBare);

        // set cookie
        cookieProperty = new Property("Cookie", "niagara_userid=" + user);

        // post
        String res = postString(authUri, content, MIME_TYPE);

        // save the resulting sessionId
        String cookie = cookieProperty.value;
        int a = cookie.indexOf("JSESSIONID=");
        int b = cookie.indexOf(";", a);
        sessionId = (b == -1) ? 
            cookie.substring(a + "JSESSIONID=".length()) :
            cookie.substring(a + "JSESSIONID=".length(), b);

        // store response
        this.firstMsgResult = res;
      }

      private void finalMsg() throws Exception
      {
        // parse first msg response
        Map firstMsg = decodeMsg(firstMsgResult);
        String nonce = (String) firstMsg.get("r");
        int iterations = Integer.parseInt((String) firstMsg.get("i"));
        String salt = (String) firstMsg.get("s");

        // check client nonce
        if (!clientNonce.equals(nonce.substring(0, clientNonce.length())))
          throw new CallAuthException("Authentication failed");

        // create salted password
        byte[] saltedPassword = CryptoUtil.pbk(
            "PBKDF2WithHmacSHA256", 
            strBytes(pass),
            Base64.STANDARD.decodeBytes(salt),
            iterations, 32);

        // create final message
        String finalMsgWithoutProof = "c=biws,r=" + nonce;
        String authMsg = firstMsgBare + "," + firstMsgResult + "," + finalMsgWithoutProof;
        String clientProof = createClientProof(saltedPassword, strBytes(authMsg));
        String clientFinalMsg = finalMsgWithoutProof + ",p=" + clientProof;

        // create request content
        String content = encodePost("sendClientFinalMessage",
          "clientFinalMessage", clientFinalMsg);

        // set cookie
        cookieProperty = new Property("Cookie", 
            "JSESSIONID=" + sessionId + "; " + 
            "niagara_userid=" + user);

        // post
        postString(authUri, content, MIME_TYPE);
      }

      /**
        * upgradeInsecureReqs
        */
      private void upgradeInsecureReqs()
      {
        try
        {
          URL url = new URL(authUri);
          HttpURLConnection c = openHttpConnection(url, "GET");
          try
          {
            c.setRequestProperty("Connection", "Close");
            c.setRequestProperty("Content-Type", "text/plain");
            c.setRequestProperty("Upgrade-Insecure-Requests", "1");
            c.setRequestProperty(cookieProperty.key, cookieProperty.value);

            c.connect();

            // check for 302
            if (c.getResponseCode() != 302)
              throw new CallHttpException(c.getResponseCode(), c.getResponseMessage());

            // discard response
            Reader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            int n;
            while ((n = r.read()) > 0);
          }
          finally
          {
            try { c.disconnect(); } catch(Exception e) {}
          }
        }
        catch (Exception e) { throw new CallNetworkException(e); }
      }

      /**
        * createClientProof
        */
      private String createClientProof(byte[] saltedPassword, byte[] authMsg) throws Exception
      {
        byte[] clientKey = CryptoUtil.hmac("SHA-256", strBytes("Client Key"), saltedPassword);
        byte[] storedKey = MessageDigest.getInstance("SHA-256").digest(clientKey);
        byte[] clientSig = CryptoUtil.hmac("SHA-256", authMsg, storedKey);

        byte[] clientProof = new byte[clientKey.length];
        for (int i = 0; i < clientKey.length; i++)
            clientProof[i] = (byte) (clientKey[i] ^ clientSig[i]);

        return Base64.STANDARD.encodeBytes(clientProof);
      }

      /**
        * the message is a comma-delimited sequence of properties
        * of the form "<key>=<value>"
        */
      private Map decodeMsg(String str)
      {
        Map map = new HashMap();
        int a = 0;
        int b = 1;
        while (b < str.length())
        {
          if (str.charAt(b) == ',') {
            String entry = str.substring(a,b);
            int n = entry.indexOf("=");
            map.put(entry.substring(0,n), entry.substring(n+1));
            a = b+1;
            b = a+1;
          }
          else {
            b++;
          }
        }
        String entry = str.substring(a);
        int n = entry.indexOf("=");
        map.put(entry.substring(0,n), entry.substring(n+1));
        return map;
      }

      /**
        * encodePost
        */
      private String encodePost(String action, String msgKey, String msgVal)
      {
        return "action=" + action + "&" + msgKey + "=" + msgVal;
      }

      /**
        * strBytes
        */
      private byte[] strBytes(String text) throws Exception
      {
        return text.getBytes("UTF-8");
      }

      ////////////////////////////////////////////////////////////////
      // attribs

      private static final String MIME_TYPE = "application/x-niagara-login-support; charset=UTF-8";

      private final String authUri;
      private final String clientNonce;
      private String firstMsgBare;
      private String firstMsgResult;
      private String sessionId;
  }

////////////////////////////////////////////////////////////////
// util
////////////////////////////////////////////////////////////////

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

  private HttpURLConnection openHttpConnection(URL url, String method)
    throws IOException, ProtocolException
  {
    HttpURLConnection c = (HttpURLConnection)url.openConnection();
    c.setRequestMethod(method);
    c.setInstanceFollowRedirects(false);
    c.setConnectTimeout(connectTimeout);
    c.setReadTimeout(readTimeout);
    return c;
  }

////////////////////////////////////////////////////////////////
// Property
////////////////////////////////////////////////////////////////

  static class Property
  {
    Property(String key, String value)
    {
      this.key = key;
      this.value = value;
    }

    public String toString()
    {
      return "[Property " +
        "key:" + key + ", " +
        "value:" + value + "]";
    }

    final String key;
    final String value;
  }

//////////////////////////////////////////////////////////////////////////
// Debug Utils
//////////////////////////////////////////////////////////////////////////

  /*
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
  */

////////////////////////////////////////////////////////////////
// main
////////////////////////////////////////////////////////////////

  static HClient makeClient(String uri, String user, String pass) throws Exception
  {
    // get bad credentials
    try { 
      HClient.open(uri, "baduser", "badpass").about(); 
      throw new IllegalStateException();
    } catch (CallException e) { }

    try { 
        HClient.open(uri, "haystack", "badpass").about();  
        throw new IllegalStateException();
    } catch (CallException e) {  }

    // create proper client
    return HClient.open(uri, user, pass);
  }

  public static void main(String[] args) throws Exception
  {
    if (args.length != 3) {
        System.out.println("usage: HClient <uri> <user> <pass>");
        System.exit(0);
    }

    HClient client = makeClient(args[0], args[1], args[2]);
    System.out.println(client.about());
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private final String user;
  private final String pass;
  private Property authProperty;
  private Property cookieProperty;

  private HashMap watches = new HashMap();

}
