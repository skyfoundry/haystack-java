//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package haystack.web;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import haystack.*;
import haystack.db.*;

/**
 * HServlet implements the haystack HTTP REST API for
 * querying entities and history data.
 */
public class HServlet extends HttpServlet
{

//////////////////////////////////////////////////////////////////////////
// Database Hook
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the database to use for this servlet.
   * If not overridden then a test database is created.
   */
  public HDatabase db()
  {
    return new haystack.test.TestDatabase();
  }

//////////////////////////////////////////////////////////////////////////
// HttpServlet Hooks
//////////////////////////////////////////////////////////////////////////

  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    onService("GET", req, res);
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    onService("POST", req, res);
  }

//////////////////////////////////////////////////////////////////////////
// Service
//////////////////////////////////////////////////////////////////////////

  void onService(String method, HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    // get the database
    HDatabase db = db();

    // parse URI path into "/{action}/{id}
    String path = req.getPathInfo();
    if (path == null || path.length() == 0) path = "/";
    int slash = path.indexOf('/', 1);
    if (slash < 0) slash = path.length();
    String action = path.substring(1, slash);
    String id = slash+1 >= path.length() ? "" : path.substring(slash+1);

    // argument is query string on GET or content on POST
    String arg;
    if (method == "GET")
    {
      arg = req.getQueryString();
      if (arg == null) arg = "";
      else arg = URLDecoder.decode(arg);
    }
    else
    {
      StringBuffer buf = new StringBuffer();
      Reader r = req.getReader();
      for (int c; (c = r.read()) >= 0;) buf.append((char)c);
      arg = buf.toString();
    }

    // process action
    HDict[] result = null;
    if (action.equals("query"))    result = onQuery(req, res, db, arg);
    else if (action.equals("his")) result = onHis(req, res, db, id, arg);
    else res.sendError(HttpServletResponse.SC_NOT_FOUND);
    if (result == null) return;

    // debug
    dumpReq(req);
    System.out.println("action = '" + action + "'");
    System.out.println("id     = '" + id + "'");
    System.out.println("arg    = '" + arg + "'");

    // setup response
    res.setStatus(HttpServletResponse.SC_OK);
    res.setCharacterEncoding("UTF-8");
    res.setContentType("text/plain; charset=utf-8");
    Writer out = new OutputStreamWriter(res.getOutputStream(), "UTF-8");

    // write result
    for (int i=0; i<result.length; ++i)
    {
      out.write(result[i].write());
      out.write('\n');
    }
    out.flush();
  }

//////////////////////////////////////////////////////////////////////////
// Query
//////////////////////////////////////////////////////////////////////////

   HDict[] onQuery(HttpServletRequest req, HttpServletResponse res,
                  HDatabase db, String queryStr)
      throws ServletException, IOException
   {
     // parse query
     HQuery query;
     try
     {
       query = HQuery.read(queryStr);
     }
     catch (ParseException e)
     {
       res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid query: " + queryStr);
       return null;
     }

     return db.query(query);
   }

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

   HDict[] onHis(HttpServletRequest req, HttpServletResponse res,
                 HDatabase db, String id, String queryStr)
      throws ServletException, IOException
   {
      // lookup entity
      HDict rec = db.get(id, false);
      if (rec == null)
      {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown entity: " + id);
        return null;
      }

      // check that entity has "his" tag
      if (rec.missing("his"))
      {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Entity not tagged as his: " + id);
        return null;
      }

      // lookup "tz" on entity
      HTimeZone tz = null;
      if (rec.has("tz")) tz = HTimeZone.make(((HStr)rec.get("tz")).val, false);
      if (tz == null)
      {
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Entity missing tz tag: " + id);
        return null;
      }

      // parse date range
      HDateTimeRange range = null;
      try
      {
        range = HDateTimeRange.read(queryStr, tz);
      }
      catch (ParseException e)
      {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date time range: " + queryStr);
        return null;
      }

      // return results
      return db.his(rec, range);
   }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  void dumpReq(HttpServletRequest req) { dumpReq(req, null); }
  void dumpReq(HttpServletRequest req, PrintWriter out)
  {
    if (out == null) out = new PrintWriter(System.out);
    out.println("==========================================");
    out.println("method    = " + req.getMethod());
    out.println("pathInfo  = " + req.getPathInfo());
    out.println("query     = " + (req.getQueryString() == null ? "null" : URLDecoder.decode(req.getQueryString())));
    out.println("headers:");
    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements())
    {
      String key = (String)e.nextElement();
      String val = req.getHeader(key);
      out.println("  " + key + " = " + val);
    }
    out.flush();
  }
}