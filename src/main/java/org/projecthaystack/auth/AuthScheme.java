//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import java.net.HttpURLConnection;
import java.util.TreeMap;

/**
 * AuthScheme is the base class for modeling pluggable authentication algorithms.
 */
public abstract class AuthScheme
{

//////////////////////////////////////////////////////////////////////////
// Registry
//////////////////////////////////////////////////////////////////////////

  /** Convenience for {@link #find(String, boolean) find(name, true)} */
  public static AuthScheme find(String name) { return AuthScheme.find(name, true); }

  /**
   * Lookup an AuthScheme for the given case-insensitive name.
   */
  public static AuthScheme find(String name, boolean checked)
  {
    AuthScheme scheme = (AuthScheme)registry.get(name);
    if (scheme != null) return scheme;
    if (checked) throw new IllegalArgumentException("No auth scheme found for '" + name + "'");
    return null;
  }

  public static AuthScheme[] list()
  {
    return (AuthScheme[])registry.values().toArray(new AuthScheme[registry.size()]);
  }

  private static TreeMap registry = new TreeMap(String.CASE_INSENSITIVE_ORDER);
  static
  {
    registry.put("scram", new ScramScheme());
    registry.put("hmac", new HmacScheme());
    registry.put("folio2", new Folio2Scheme());
    registry.put("basic", new BasicScheme());
  }

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  protected AuthScheme(String name)
  {
    if (name != name.toLowerCase()) throw new IllegalArgumentException("Name must be lowercase: " + name);
    this.name = name;
  }

//////////////////////////////////////////////////////////////////////////
// Overrides
//////////////////////////////////////////////////////////////////////////

  /**
   * Scheme name (always normalized to lowercase)
   */
  public final String name;

  /**
   * Handle a standardized client authentication challenge message from
   * the server using RFC 7235.
   *
   * @param cx the current {@link AuthClientContext}
   * @param msg the {@link AuthMsg} sent by the server
   * @return The {@link AuthMsg} to send back to the server to authenticate
   */
  public abstract AuthMsg onClient(AuthClientContext cx, AuthMsg msg);

  /**
   * Callback after successful authentication with the server.
   * The default implementation is a no-op.
   *
   * @param cx the current {@link AuthClientContext}
   * @param msg the {@link AuthMsg} sent by the server when it authenticated
   *            the client.
   *
   */
  public void onClientSuccess(AuthClientContext cx, AuthMsg msg)
  {
  }

  /**
   * Handle non-standardized client authentication when the standard
   * process (RFC 7235) fails. If this scheme thinks it can handle the
   * given response by sniffing the response code and headers, then it
   * should process and return true.
   *
   * @param cx the current {@link AuthClientContext}
   * @param resp the response message from the server
   * @param content the body of the response if it had one, or null.
   * @return true if the scheme processed the response, false otherwise. Returns false by default.
   */
  public boolean onClientNonStd(AuthClientContext cx, HttpURLConnection resp, String content)
  {
    return false;
  }
}
