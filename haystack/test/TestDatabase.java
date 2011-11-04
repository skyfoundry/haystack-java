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
    addSite("A", "Richmond",   "VA", 1000);
    addSite("B", "Richmond",   "VA", 2000);
    addSite("C", "Washington", "DC", 3000);
    addSite("D", "Boston",     "MA", 4000);
  }

  private void addSite(String dis, String geoCity, String geoState, int area)
  {
    HDict site = new HDictBuilder()
      .add("id",       HRef.make(dis))
      .add("dis",      dis)
      .add("site",     HMarker.VAL)
      .add("geoCity",  geoCity)
      .add("geoState", geoState)
      .add("geoAddr",  "" +geoCity + "," + geoState)
      .add("tz",       "New_York")
      .add("area",     HNum.make(area, "ft\u00B2"))
      .toDict();
    recs.put(dis, site);

    addMeter(site, dis+"-Meter");
    addAhu(site,   dis+"-AHU1");
    addAhu(site,   dis+"-AHU2");
  }

  private void addMeter(HDict site, String dis)
  {
    HDict equip = new HDictBuilder()
      .add("id",       HRef.make(dis))
      .add("dis",      dis)
      .add("equip",     HMarker.VAL)
      .add("elecMeter", HMarker.VAL)
      .add("siteMeter", HMarker.VAL)
      .add("siteRef",   site.get("id"))
      .toDict();
    recs.put(dis, equip);
    addPoint(equip, dis+"-KW",  "kW",  "elecKw");
    addPoint(equip, dis+"-KWH", "kWh", "elecKwh");
  }

  private void addAhu(HDict site, String dis)
  {
    HDict equip = new HDictBuilder()
      .add("id",      HRef.make(dis))
      .add("dis",     dis)
      .add("equip",   HMarker.VAL)
      .add("ahu",     HMarker.VAL)
      .add("siteRef", site.get("id"))
      .toDict();
    recs.put(dis, equip);
    addPoint(equip, dis+"-Fan",   null,      "discharge air fan cmd");
    addPoint(equip, dis+"-Cool",  null,      "cool cmd");
    addPoint(equip, dis+"-Heat",  null,      "heat cmd");
    addPoint(equip, dis+"-DTemp", "\u00B0F", "discharge air temp sensor");
    addPoint(equip, dis+"-RTemp", "\u00B0F", "return air temp sensor");
  }

  private void addPoint(HDict equip, String dis, String unit, String markers)
  {
    HDictBuilder b = new HDictBuilder()
      .add("id",       HRef.make(dis))
      .add("dis",      dis)
      .add("point",    HMarker.VAL)
      .add("his",      HMarker.VAL)
      .add("siteRef",  equip.get("siteRef"))
      .add("equipRef", equip.get("id"))
      .add("kind",     unit == null ? "Bool" : "Num")
      .add("tz",       "New_York");
    if (unit != null) b.add("unit", unit);
    StringTokenizer st = new StringTokenizer(markers);
    while (st.hasMoreTokens()) b.add(st.nextToken());
    recs.put(dis, b.toDict());
  }

  protected HDict find(String id) { return (HDict)recs.get(id); }

  protected Iterator iterator() { return recs.values().iterator(); }

  public HDict[] his(HDict entity, HDateTimeRange range)
  {
    // generate dummy 10min data
    ArrayList acc = new ArrayList();
    HDateTime ts = range.start;
    boolean isBool = ((HStr)entity.get("kind")).val.equals("Bool");
    while (ts.compareTo(range.end) <= 0)
    {
      HVal val = isBool ?
        HBool.make(acc.size() % 2 == 0) :
        HNum.make(acc.size());
      HDict item = new HDictBuilder().add("ts", ts).add("val", val).toDict();
      acc.add(item);
      ts = HDateTime.make(ts.millis() + 10*60*1000);
    }
    return (HDict[])acc.toArray(new HDict[acc.size()]);
  }

  HashMap recs = new HashMap();
}