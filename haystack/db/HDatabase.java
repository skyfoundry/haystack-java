//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package haystack.db;

import java.util.*;
import haystack.*;

/**
 * HDatabase is the interface between HServlet and a database
 * of tag based entities.
 */
public abstract class HDatabase
{

//////////////////////////////////////////////////////////////////////////
// Get
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for get(id, true)
   */
  public HDict get(String id) { return get(id, true); }

  /**
   * Lookup an entity by it's unique identifier.  If not found then
   * return null or throw an UnknownEntityException based on checked.
   */
  public HDict get(String id, boolean checked)
  {
    HDict rec = find(id);
    if (rec != null) return rec;
    if (checked) throw new UnknownEntityException(id);
    return null;
  }

  /**
   * Implementation hook to resolve an identifier into an entity.
   * Return null if id does not resolve an entity
   */
  protected abstract HDict find(String id);

//////////////////////////////////////////////////////////////////////////
// Query
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for query(HQuery.read(queryStr)).
   * Throw ParseException if query is invalid.
   */
  public HDict[] query(String queryStr)
  {
    return query(HQuery.read(queryStr));
  }

  /**
   * Return list of every entity that matches given query.
   */
  public HDict[] query(HQuery query)
  {
    ArrayList acc = new ArrayList();
    for (Iterator it = iterator(); it.hasNext(); )
    {
      HDict rec = (HDict)it.next();
      if (query.include(rec, queryPather)) acc.add(rec);
    }
    return (HDict[])acc.toArray(new HDict[acc.size()]);
  }

  private HQuery.Pather queryPather = new HQuery.Pather()
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
  public abstract HDict[] his(HDict entity, HDateTime start, HDateTime end);

}