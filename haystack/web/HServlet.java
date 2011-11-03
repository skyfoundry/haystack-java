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

    // parse path, first component of path is action
    String path = req.getPathInfo();
    if (path == null || path.length() == 0) path = "/";
    int slash = path.indexOf('/', 1);
    if (slash < 0) slash = path.length();
    String action = path.substring(1, slash);

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
    HTags[] result = null;
    if (action.equals("query")) result = onQuery(req, res, db, arg);
    else res.sendError(HttpServletResponse.SC_NOT_FOUND);
    if (result == null) return;

    /*
    // debug
    dumpReq(req);
    System.out.println("action = '" + action + "'");
    System.out.println("arg    = '" + arg + "'");
    */

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

   HTags[] onQuery(HttpServletRequest req, HttpServletResponse res,
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