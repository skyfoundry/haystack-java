//
// Copyright (c) 2012, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Sep 2012  Brian Frank  Creation
//
package haystack.server;

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import haystack.*;
import haystack.io.*;

/**
 * HStdOps defines the standard operations available.
 */
public class HStdOps
{
  /** List the registered operations. */
  public static final HOp about = new AboutOp();

  /** List the registered operations. */
  public static final HOp ops = new OpsOp();

  /** List the registered grid formats. */
  public static final HOp formats = new FormatsOp();

  /** Read entity records in database. */
  public static final HOp read = new ReadOp();

  /** Read time series history data. */
  public static final HOp hisRead = new HisReadOp();
}

//////////////////////////////////////////////////////////////////////////
// AboutOp
//////////////////////////////////////////////////////////////////////////

class AboutOp extends HOp
{
  public String name() { return "about"; }
  public String summary() { return "Summary information for server"; }
  public HGrid onService(HServer db, HGrid req)
  {
    return HGridBuilder.dictToGrid(db.about());
  }
}

//////////////////////////////////////////////////////////////////////////
// OpsOp
//////////////////////////////////////////////////////////////////////////

class OpsOp extends HOp
{
  public String name() { return "ops"; }
  public String summary() { return "Operations supported by this server"; }
  public HGrid onService(HServer db, HGrid req)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("name");
    b.addCol("summary");
    HOp[] ops = db.ops();
    for (int i=0; i<ops.length; ++i)
    {
      HOp op = ops[i];
      b.addRow(new HVal[] {
        HStr.make(op.name()),
        HStr.make(op.summary()),
      });
    }
    return b.toGrid();
  }
}

//////////////////////////////////////////////////////////////////////////
// FormatsOp
//////////////////////////////////////////////////////////////////////////

class FormatsOp extends HOp
{
  public String name() { return "formats"; }
  public String summary() { return "Grid data formats supported by this server"; }
  public HGrid onService(HServer db, HGrid req)
  {
    HGridBuilder b = new HGridBuilder();
    b.addCol("mime");
    b.addCol("read");
    b.addCol("write");
    HGridFormat[] formats = HGridFormat.list();
    for (int i=0; i<formats.length; ++i)
    {
      HGridFormat format = formats[i];
      b.addRow(new HVal[] {
        HStr.make(format.mime),
        format.reader != null ? HMarker.VAL : null,
        format.writer != null ? HMarker.VAL : null,
      });
    }
    return b.toGrid();
  }
}

//////////////////////////////////////////////////////////////////////////
// ReadOp
//////////////////////////////////////////////////////////////////////////

class ReadOp extends HOp
{
  public String name() { return "read"; }
  public String summary() { return "Read entity records in database"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    // ensure we have one row
    if (req.isEmpty()) throw new Exception("Request has no rows");

    // perform filter or id read
    HRow row = req.row(0);
    HDict[] recs;
    if (row.has("filter"))
    {
      // filter read
      String filter = row.getStr("filter");
      int limit = row.has("limit") ? row.getInt("limit") : Integer.MAX_VALUE;
      return db.readAll(filter, limit);
    }
    else if (row.has("id"))
    {
      // read by ids
      HRef[] ids = new HRef[req.numRows()];
      for (int i=0; i<ids.length; ++i) ids[i] = req.row(i).id();
      return db.readByIds(ids, false);
    }
    else
    {
      throw new Exception("Missing filter or id columns");
    }
  }
}

//////////////////////////////////////////////////////////////////////////
// HisReadOp
//////////////////////////////////////////////////////////////////////////

class HisReadOp extends HOp
{
  public String name() { return "hisRead"; }
  public String summary() { return "Read time series history data from entity"; }
  public HGrid onService(HServer db, HGrid req) throws Exception
  {
    if (req.isEmpty()) throw new Exception("Request has no rows");
    HRow row = req.row(0);
    HRef id = row.id();
    String range = row.getStr("range");
    HDict rec = db.readById(id);
    HHisItem[] items = db.hisRead(id, range);
    return HGridBuilder.hisItemsToGrid(rec, items);
  }
}