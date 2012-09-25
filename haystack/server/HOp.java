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
    if (req.getMethod().equals("GET"))
    {
      HDictBuilder b = new HDictBuilder();
      Iterator it = req.getParameterMap().entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry entry = (Map.Entry)it.next();
        String name = (String)entry.getKey();
        String val = ((String[])entry.getValue())[0];
        if (val.startsWith("@"))
          b.add(name, HRef.make(val.substring(1)));
        else
          b.add(name, val);
      }
      reqGrid = HGridBuilder.dictToGrid(b.toDict());
    }
    else if (req.getMethod().equals("POST"))
    {
      /* TODO
      StringBuffer buf = new StringBuffer();
      InputStream r = req.getInputStream();
      for (int c; (c = r.read()) >= 0;) buf.append((char)c);
      arg = buf.toString();
      */
    }

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

    // send zinc response
    res.setStatus(HttpServletResponse.SC_OK);
    res.setCharacterEncoding("UTF-8");
    res.setContentType("text/plain; charset=utf-8");
    HZincWriter out = new HZincWriter (res.getOutputStream());
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
}