//
// Copyright (c) 2012, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Sep 2012  Brian Frank  Creation
//
package haystack.server;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import haystack.*;
import haystack.io.*;

/**
 * HOp is the base class for server side operations exposed by the REST API.
 * All methods on HOp must be thread safe.
 */
public abstract class HOp
{
  /** Programatic name of the operation. */
  public abstract String name();

  /** Short one line summary of what the operation does. */
  public abstract String summary();

  /**
   * Service the request and return response.
   * This method routes to "onService(HDatabase,HGrid)".
   */
  public void onService(HDatabase db, HttpServletRequest req, HttpServletResponse res)
    throws Exception
  {
    // parse GET query parameters or POST body into grid
    HGrid reqGrid = HGrid.EMPTY;
    String method = req.getMethod();
    if (method.equals("GET"))  reqGrid = getToGrid(req);
    if (method.equals("POST")) reqGrid = postToGrid(req, res);
    if (reqGrid == null) return;

    // route to onService(HDatabase, HGrid)
    HGrid resGrid;
    try
    {
      resGrid = onService(db, reqGrid);
    }
    catch (Throwable e)
    {
      resGrid = HGridBuilder.errToGrid(e);
    }

    // figure out best format to use for response
    HGridFormat format = toFormat(req);

    // send response
    res.setStatus(HttpServletResponse.SC_OK);
    if (format.mime.startsWith("text/"))
    {
      res.setCharacterEncoding("UTF-8");
      res.setContentType(format.mime + "; charset=utf-8");
    }
    else
    {
      res.setContentType(format.mime);
    }
    HGridWriter out = format.makeWriter(res.getOutputStream());
    out.writeGrid(resGrid);
    out.flush();
  }

  /**
   * Service the request and return response.
   */
  public HGrid onService(HDatabase db, HGrid req)
    throws Exception
  {
    throw new UnsupportedOperationException(getClass().getName()+".onService(HDatabase,HGrid)");
  }

  /**
   * Map the GET query parameters to grid with one row
   */
  private HGrid getToGrid(HttpServletRequest req)
  {
    HDictBuilder b = new HDictBuilder();
    Iterator it = req.getParameterMap().entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String name = (String)entry.getKey();
      String valStr = ((String[])entry.getValue())[0];

      HVal val;
      try
      {
        val = new HZincReader(valStr).readScalar();
      }
      catch (Exception e)
      {
        val = HStr.make(valStr);
      }
      b.add(name, val);
    }
    return HGridBuilder.dictToGrid(b.toDict());
  }

  /**
   * Map the POST body to grid
   */
  private HGrid postToGrid(HttpServletRequest req, HttpServletResponse res)
    throws IOException
  {
    // get content type
    String mime = req.getHeader("Content-Type");
    if (mime == null)
    {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'Content-Type' header");
      return null;
    }

    // check if we have a format that supports reading the content type
    HGridFormat format = HGridFormat.find(mime, false);
    if (format == null || format.reader == null)
    {
      res.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "No format reader available for MIME type: " + mime);
      return null;
    }

    // read the grid
    return format.makeReader(req.getInputStream()).readGrid();
  }

  /**
   * Find the best format to use for encoding response using
   * HTTP content negotiation.
   */
  private HGridFormat toFormat(HttpServletRequest req)
  {
    HGridFormat format = null;
    String accept = req.getHeader("Accept");
    if (accept != null)
    {
      String[] mimes = HStr.split(accept, ',', true);
      for (int i=0; i<mimes.length; ++i)
      {
        format = HGridFormat.find(mimes[i], false);
        if (format != null && format.writer != null) break;
      }
    }
    if (format == null) format = HGridFormat.find("text/plain", true);
    return format;
  }
}