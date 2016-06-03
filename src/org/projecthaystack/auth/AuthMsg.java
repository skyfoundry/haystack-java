//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import org.projecthaystack.ParseException;
import org.projecthaystack.util.WebUtil;

import java.util.*;

/**
 * AuthMsg models a scheme name and set of parameters according
 * to <a href="https://tools.ietf.org/html/rfc7235">RFC 7235</a>. To simplify
 * parsing, we restrict the grammar to be auth-param and token (the
 * token68 and quoted-string productions are not allowed).
 */
final public class AuthMsg
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /**
   * Parse a list of AuthSchemes such as a list of 'challenge'
   * productions for the WWW-Authentication header per RFC 7235.
   */
  public static AuthMsg[] listFromStr(String s)
  {
    String[] toks = splitList(s);
    List arr = new ArrayList();
    for (int i = 0; i < toks.length; ++i)
    {
      arr.add(AuthMsg.fromStr(toks[i]));
    }
    return (AuthMsg[])arr.toArray(new AuthMsg[arr.size()]);
  }

  public static AuthMsg fromStr(String s) { return fromStr(s, true); }
  public static AuthMsg fromStr(String s, boolean checked)
  {
    try
    {
      return decode(s);
    }
    catch (Exception e)
    {
      if (checked) throw new ParseException(e.toString());
      return null;
    }
  }

  public AuthMsg(String scheme, String[] params)
  {
    this(scheme, listToParams(params));
  }

  public AuthMsg(String scheme, Map params)
  {
    this.scheme = scheme.toLowerCase();
    this.params = Collections.unmodifiableMap(caseInsensitiveMap(params));
    this.toStr  = encode(this.scheme, this.params);
  }

  private static TreeMap caseInsensitiveMap(Map params)
  {
    if (params instanceof TreeMap && ((TreeMap)params).comparator() == String.CASE_INSENSITIVE_ORDER)
      return (TreeMap)params;
    TreeMap treeMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    Iterator iter = params.keySet().iterator();
    while (iter.hasNext())
    {
      String key = (String)iter.next();
      String val = (String)params.get(key);
      treeMap.put(key, val);
    }
    return treeMap;
  }

  private static TreeMap listToParams(String[] params)
  {
    if (params.length % 2 != 0) throw new IllegalArgumentException("odd number of params");
    TreeMap map = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    for (int i = 0; i < params.length; i = i + 2)
    {
      map.put(params[i], params[i+1]);
    }
    return map;
  }

//////////////////////////////////////////////////////////////////////////
// Identity
//////////////////////////////////////////////////////////////////////////

  /** Scheme name normalized to lowercase */
  final public String scheme;

  /** Parameters for scheme (read-only) */
  final Map params;

  final private String toStr;

  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AuthMsg authMsg = (AuthMsg)o;

    if (!scheme.equals(authMsg.scheme)) return false;
    return params.equals(authMsg.params);

  }

  public int hashCode()
  {
    int result = scheme.hashCode();
    Iterator iter = params.keySet().iterator();
    while (iter.hasNext())
    {
      String key = (String)iter.next();
      result = 31 * result + key.toLowerCase().hashCode();
      result = 31 * result + params.get(key).hashCode();
    }
    return result;
  }

  public String toString()
  {
    return toStr;
  }

  /**
   * Convenience for {@link #param(String, boolean) param(name, true)}
   */
  public String param(String name) { return param(name, true); }

  /**
   * Lookup a parameter by name. If not found and checked,
   * throw an Exception, otherwise return null.
   */
  public String param(String name, boolean checked)
  {
    String val = (String)params.get(name);
    if (val != null) return val;
    if (checked) throw new RuntimeException("parameter not found: " + name);
    return null;
  }

//////////////////////////////////////////////////////////////////////////
// Encoding
//////////////////////////////////////////////////////////////////////////

  public static String[] splitList(String s)
  {
    // find break indices (start of each challenge production)
    String[] toks = s.split(",");
    for (int i = 0; i < toks.length; ++i) toks[i] = toks[i].trim();

    List breaks = new ArrayList();
    for (int i = 0; i < toks.length; ++i)
    {
      String tok = toks[i];
      int sp = tok.indexOf(' ');
      String name = (sp < 0 ? tok : tok.substring(0, sp)).trim();
      if (WebUtil.isToken(name) && i > 0) breaks.add(new Integer(i));
    }

    // rejoin tokens into challenge strings
    List acc = new ArrayList();
    int start = 0;
    Iterator iter = breaks.iterator();
    StringBuilder sb = new StringBuilder();
    while (iter.hasNext())
    {
      sb.setLength(0);
      int end = ((Integer)iter.next()).intValue();
      for (int i = start; i < end; ++i)
      {
        if (i > start) sb.append(',');
        sb.append(toks[i]);
      }
      acc.add(sb.toString());
      start = end;
    }
    sb.setLength(0);
    for (int i = start; i < toks.length; ++i)
    {
      if (i > start) sb.append(',');
      sb.append(toks[i]);
    }
    acc.add(sb.toString());
    return (String[])acc.toArray(new String[acc.size()]) ;
  }

  private static AuthMsg decode(String s)
  {
    int sp = s.indexOf(' ');
    String scheme = s;
    TreeMap params = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    if (sp >= 0)
    {
      scheme = s.substring(0, sp);
      String[] paramParts = s.substring(sp+1).split(",");
      for (int i = 0; i< paramParts.length; ++i)
      {
        String part = paramParts[i].trim();
        int eq = part.indexOf('=');
        if (eq < 0) throw new ParseException("Invalid auth-param: " + part);
        params.put(part.substring(0, eq).trim(), part.substring(eq+1).trim());
      }
    }
    return new AuthMsg(scheme, params);
  }

  public static String encode(String scheme, Map params)
  {
    params = caseInsensitiveMap(params);
    StringBuilder sb = new StringBuilder();
    addToken(sb, scheme);
    Iterator iter = params.keySet().iterator();
    boolean first = true;
    while (iter.hasNext())
    {
      String key = (String)iter.next();
      String val = (String)params.get(key);
      if (first) first = false;
      else sb.append(',');
      sb.append(' ');
      addToken(sb, key);
      sb.append('=');
      addToken(sb, val);
    }
    return sb.toString();
  }

  private static void addToken(StringBuilder sb, String val)
  {
    for (int i = 0; i< val.length(); ++i)
    {
      int cp = val.codePointAt(i);
      if (WebUtil.isTokenChar(cp)) sb.append(val.charAt(i));
      else throw new RuntimeException("Invalid char '" + val.charAt(i) + "' in " + val);
    }
  }

}
