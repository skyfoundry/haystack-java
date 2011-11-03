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

  HashMap recs = new HashMap();
}