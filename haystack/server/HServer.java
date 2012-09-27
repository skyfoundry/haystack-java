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
public abstract class HServer
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
   *   - serverName: Str
   *   - productName: Str
   *   - productVersion: Str
   *   - productUri: Uri
   *   - moduleName: Str
   *   - moduleVersion: Str
   *   - moduleUri: Uri
   */
  public abstract HDict about();

//////////////////////////////////////////////////////////////////////////
// Read by id
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for "readById(id, true)"
   */
  public final HDict readById(HRef id)
  {
    return readById(id, true);
  }

  /**
   * Lookup an entity record by it's unique identifier.  If not found
   ** then return null or throw an UnknownRecException based on checked.
   */
  public final HDict readById(HRef id, boolean checked)
  {
    HDict rec = onReadById(id);
    if (rec != null) return rec;
    if (checked) throw new UnknownRecException(id.toString());
    return null;
  }

  /**
   * Implementation hook for readById.  Return null
   * if id does not resolve an record.
   */
  protected abstract HDict onReadById(HRef id);

//////////////////////////////////////////////////////////////////////////
// Read by filter
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for "read(filter, true)".
   */
  public final HDict read(String filter)
  {
    return read(filter, true);
  }

  /**
   * Return one entity record that matches the given filter.  If there
   * is more than one record, then it is undefined which one is
   * returned.  If there are no matches than return null or raise
   * UnknownRecException based on checked flag.  Raise ParseException
   * is filter is malformed.
   */
  public final HDict read(String filter, boolean checked)
  {
    HDict[] dicts = readAll(HFilter.make(filter), 1);
    if (dicts.length > 0) return dicts[0];
    if (checked) throw new UnknownRecException(filter);
    return null;
  }

  /**
   * Convenience for "readAll(HFilter, int)".
   * Raise ParseException is filter is malformed.
   */
  public final HDict[] readAll(String filter)
  {
    return readAll(HFilter.make(filter), Integer.MAX_VALUE);
  }

  /**
   * Return list of every entity record that matches given filter.
   * Clip number of results by "limit" parameter.
   */
  public final HDict[] readAll(HFilter filter, int limit)
  {
    ArrayList acc = new ArrayList();
    for (Iterator it = iterator(); it.hasNext(); )
    {
      HDict rec = (HDict)it.next();
      if (filter.include(rec, filterPather))
      {
        acc.add(rec);
        if  (acc.size() >= limit) break;
      }
    }
    return (HDict[])acc.toArray(new HDict[acc.size()]);
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