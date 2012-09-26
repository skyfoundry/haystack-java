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
 * HDatabase is the interface between HServlet and a database of
 * tag based entities.  All methods on HDatabase must be thread safe.
 */
public abstract class HDatabase
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
// Get
//////////////////////////////////////////////////////////////////////////

  /**
   * Get the about metadata which should be a constant dict with
   * the following tags:
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
// Get
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for get(id, true)
   */
  public HDict get(HRef id) { return get(id, true); }

  /**
   * Lookup an entity by it's unique identifier.  If not found then
   * return null or throw an UnknownEntityException based on checked.
   */
  public HDict get(HRef id, boolean checked)
  {
    HDict rec = find(id);
    if (rec != null) return rec;
    if (checked) throw new UnknownEntityException(id.toString());
    return null;
  }

  /**
   * Implementation hook to resolve an identifier into an entity.
   * Return null if id does not resolve an entity
   */
  protected abstract HDict find(HRef id);

//////////////////////////////////////////////////////////////////////////
// Query
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for query(HFilter.make(queryStr)).
   * Throw ParseException if query is invalid.
   */
  public HDict[] query(String queryStr)
  {
    return query(HFilter.make(queryStr));
  }

  /**
   * Return list of every entity that matches given query.
   */
  public HDict[] query(HFilter query)
  {
    ArrayList acc = new ArrayList();
    for (Iterator it = iterator(); it.hasNext(); )
    {
      HDict rec = (HDict)it.next();
      if (query.include(rec, queryPather)) acc.add(rec);
    }
    return (HDict[])acc.toArray(new HDict[acc.size()]);
  }

  private HFilter.Pather queryPather = new HFilter.Pather()
  {
    public HDict find(String id) { return find(id); }
  };

  /**
   * Implementation hook to iterate every entity in the database.
   */
  protected abstract Iterator iterator();

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

  /**
   * Given a history entity, return all the timestamp/value history
   * samples for the given inclusive timerange.  If no samples are available
   * for the range return an empty array.
   */
  public abstract HHisItem[] his(HDict entity, HDateTimeRange range);

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

 final HDateTime bootTime = HDateTime.now();
 private HashMap opsByName;

}