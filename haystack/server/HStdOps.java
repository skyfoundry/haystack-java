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

  /** Query entities in database. */
  public static final HOp query = new QueryOp();

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
  public HGrid onService(HDatabase db, HGrid req)
  {
    HDict about = new HDictBuilder()
                      .add(db.about())
                      .add("haystackVersion", "2.0")
                      .add("serverTime", HDateTime.now())
                      .add("serverBootTime", db.bootTime)
                      .add("tz", HTimeZone.DEFAULT.name)
                      .toDict();
    return HGridBuilder.dictToGrid(about);
  }
}

//////////////////////////////////////////////////////////////////////////
// OpsOp
//////////////////////////////////////////////////////////////////////////

class OpsOp extends HOp
{
  public String name() { return "ops"; }
  public String summary() { return "Operations supported by this server"; }
  public HGrid onService(HDatabase db, HGrid req)
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
  public HGrid onService(HDatabase db, HGrid req)
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
// QueryOp
//////////////////////////////////////////////////////////////////////////

class QueryOp extends HOp
{
  public String name() { return "query"; }
  public String summary() { return "Query entities in database"; }
  public HGrid onService(HDatabase db, HGrid req) throws Exception
  {
    if (req.isEmpty()) throw new Exception("Request has no rows");
    String query = ((HStr)req.row(0).get("filter")).val;
    HDict[] recs = db.query(query);
    return HGridBuilder.dictsToGrid(recs);
  }
}

//////////////////////////////////////////////////////////////////////////
// HisReadOp
//////////////////////////////////////////////////////////////////////////

class HisReadOp extends HOp
{
  public String name() { return "hisRead"; }
  public String summary() { return "Read time series history data from entity"; }
  public HGrid onService(HDatabase db, HGrid req) throws Exception
  {
    if (req.isEmpty()) throw new Exception("Request has no rows");
    HRow reqRow = req.row(0);

    // lookup entity
    HRef id = (HRef)reqRow.get("id");
    HDict rec = db.get(id);

    // check that entity has "his" tag
    if (rec.missing("his"))
      throw new Exception("Entity missing 'his' tag: " + rec.dis());

    // lookup "tz" on entity
    HTimeZone tz = null;
    if (rec.has("tz")) tz = HTimeZone.make(((HStr)rec.get("tz")).val, false);
    if (tz == null)
      throw new Exception("Entity missing 'tz' tag: " + rec.dis());

    // parse date range
    String rangeStr = ((HStr)reqRow.get("range")).val;
    HDateTimeRange range = null;
    try
    {
      range = HDateTimeRange.read(rangeStr, tz);
    }
    catch (ParseException e)
    {
      throw new Exception("Invalid date time range: " + rangeStr);
    }

    // return results
    return HGridBuilder.hisItemsToGrid(rec, db.his(rec, range));
  }
}