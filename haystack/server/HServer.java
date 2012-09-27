//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package haystack.server;

import java.util.*;
import haystack.*;

/**
 * HServer is the interface between HServlet and a database of
 * tag based entities.  All methods on HServer must be thread safe.
 */
public abstract class HServer extends HProj
{

//////////////////////////////////////////////////////////////////////////
// Operations
//////////////////////////////////////////////////////////////////////////

  /**
   * Return the operations supported by this database.
   */
  public abstract HOp[] ops();

  /**
   * Lookup an operation by name.  If no operation is registered
   * for the given name, then return null or raise UnknownNameException
   * base on check flag.
   */
  public HOp op(String name, boolean checked)
  {
    // lazily build lookup map
    if (this.opsByName == null)
    {
      HashMap map = new HashMap();
      HOp[] ops = ops();
      for (int i=0; i<ops.length; ++i)
      {
        HOp op = ops[i];
        if (map.get(op.name()) != null)
          System.out.println("WARN: duplicate HOp name: " + op.name());
        map.put(op.name(), op);
      }
      this.opsByName = map;
    }

    // lookup
    HOp op = (HOp)opsByName.get(name);
    if (op != null) return op;
    if (checked) throw new UnknownNameException(name);
    return null;
  }

//////////////////////////////////////////////////////////////////////////
// About
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the about metadata which should contain following tags:
   */
  public final HDict about()
  {
    return new HDictBuilder()
        .add(onAbout())
        .add("haystackVersion", "2.0")
        .add("serverTime", HDateTime.now())
        .add("serverBootTime", this.bootTime)
        .add("tz", HTimeZone.DEFAULT.name)
        .toDict();
  }

  /**
   * Implementation hook for "about" method.
   * Should return these tags:
   *   - serverName: Str
   *   - productName: Str
   *   - productVersion: Str
   *   - productUri: Uri
   *   - moduleName: Str
   *   - moduleVersion: Str
   *   - moduleUri: Uri
   */
  protected abstract HDict onAbout();

//////////////////////////////////////////////////////////////////////////
// Reads
//////////////////////////////////////////////////////////////////////////

  /**
   * Default implementation routes to onReadById
   */
  protected HGrid onReadByIds(HRef[] ids)
  {
    HDict[] recs = new HDict[ids.length];
    for (int i=0; i<ids.length; ++i)
      recs[i] = onReadById(ids[i]);
    return HGridBuilder.dictsToGrid(recs);
  }

  /**
   * Default implementation scans all records using "iterator"
   */
  protected HGrid onReadAll(String filter, int limit)
  {
    HFilter f = HFilter.make(filter);
    ArrayList acc = new ArrayList();
    for (Iterator it = iterator(); it.hasNext(); )
    {
      HDict rec = (HDict)it.next();
      if (f.include(rec, filterPather))
      {
        acc.add(rec);
        if  (acc.size() >= limit) break;
      }
    }
    return HGridBuilder.dictsToGrid((HDict[])acc.toArray(new HDict[acc.size()]));
  }

  private HFilter.Pather filterPather = new HFilter.Pather()
  {
    public HDict find(String id) { return find(id); }
  };

  /**
   * Implementation hook to iterate every entity record in
   * the database as a HDict.
   */
  protected abstract Iterator iterator();

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

  /**
   * Given a history record, return all the timestamp/value history
   * samples for the given inclusive timerange.  If no samples are available
   * for the range return an empty array.
   */
  public final HHisItem[] hisRead(HDict rec, HDateTimeRange range)
  {
    return onHisRead(rec, range);
  }

  /**
   * Implementation hook for onHisRead.
   */
  protected abstract HHisItem[] onHisRead(HDict rec, HDateTimeRange range);

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

 final HDateTime bootTime = HDateTime.now();
 private HashMap opsByName;

}