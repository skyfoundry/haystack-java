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
// Lookup and query
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for get(id, true)
   */
  public HTags get(String id) { return get(id, true); }

  /**
   * Lookup an entity by it's unique identifier.  If not found then
   * return null or throw an UnknownEntityException based on checked.
   */
  public HTags get(String id, boolean checked)
  {
    HTags rec = find(id);
    if (rec != null) return rec;
    if (checked) throw new UnknownEntityException(id);
    return null;
  }

  /**
   * Convenience for query(HQuery.read(queryStr)).
   * Throw ParseException if query is invalid.
   */
  public HTags[] query(String queryStr)
  {
    return query(HQuery.read(queryStr));
  }

  /**
   * Return list of every entity that matches given query.
   */
  public HTags[] query(HQuery query)
  {
    ArrayList acc = new ArrayList();
    for (Iterator it = iterator(); it.hasNext(); )
    {
      HTags rec = (HTags)it.next();
      if (query.include(rec, queryPather)) acc.add(rec);
    }
    return (HTags[])acc.toArray(new HTags[acc.size()]);
  }

  private HQuery.Pather queryPather = new HQuery.Pather()
  {
    public HTags find(String id) { return find(id); }
  };

//////////////////////////////////////////////////////////////////////////
// Sub-class hooks
//////////////////////////////////////////////////////////////////////////

  /**
   * Implementation hook to resolve an identifier into an entity.
   * Return null if id does not resolve an entity
   */
  protected abstract HTags find(String id);

  /**
   * Implementation hook to iterate every entity in the database.
   */
  protected abstract Iterator iterator();

}