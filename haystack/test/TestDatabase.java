//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package haystack.test;

import java.util.*;
import haystack.*;
import haystack.db.*;

/**
 * TestDatabase provides a simple implementation of
 * HDatabase with some test entities.
 */
public class TestDatabase extends HDatabase
{
  public TestDatabase()
  {
    addPoint("Point-A", "Num");
    addPoint("Point-B", "Num");
    addPoint("Point-C", "Bool");
    addPoint("Point-D", "Bool");
  }

  private void addPoint(String dis, String kind)
  {
    String id = "id-" + recs.size();
    HTags rec = new HTagsBuilder()
      .add("id",   HRef.make(id))
      .add("dis",  dis)
      .add("kind", kind)
      .add("tz",   "New_York")
      .toTags();
    recs.put(id, rec);
  }

  protected HTags find(String id) { return (HTags)recs.get(id); }

  protected Iterator iterator() { return recs.values().iterator(); }

  public HTags[] his(HTags entity, HDateTime start, HDateTime end)
  {
    // generate dummy 10min data
    ArrayList acc = new ArrayList();
    HDateTime ts = start;
    boolean isBool = ((HStr)entity.get("kind")).val.equals("Bool");
    while (ts.compareTo(end) <= 0)
    {
      HVal val = isBool ?
        HBool.make(acc.size() % 2 == 0) :
        HNum.make(acc.size());
      HTags item = new HTagsBuilder().add("ts", ts).add("val", val).toTags();
      acc.add(item);
      ts = HDateTime.make(ts.millis() + 10*60*1000);
    }
    return (HTags[])acc.toArray(new HTags[acc.size()]);
  }

  HashMap recs = new HashMap();
}