//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  Creation
//
package haystack.test;

import java.net.*;
import java.util.*;
import haystack.*;
import haystack.server.*;

/**
 * TestDatabase provides a simple implementation of
 * HDatabase with some test entities.
 */
public class TestDatabase extends HServer
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

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

//////////////////////////////////////////////////////////////////////////
// Ops
//////////////////////////////////////////////////////////////////////////

  public HOp[] ops()
  {
    return new HOp[] {
      HStdOps.about,
      HStdOps.ops,
      HStdOps.formats,
      HStdOps.read,
      HStdOps.nav,
      HStdOps.hisRead,
    };
  }

  public HDict onAbout() { return about; }
  private final HDict about = new HDictBuilder()
    .add("serverName",  hostName())
    .add("vendorName", "Haystack Java Toolkit")
    .add("vendorUri", HUri.make("http://project-haystack.org/"))
    .add("productName", "Haystack Java Toolkit")
    .add("productVersion", "2.0.0")
    .add("productUri", HUri.make("http://project-haystack.org/"))
    .toDict();

  private static String hostName()
  {
    try { return InetAddress.getLocalHost().getHostName(); }
    catch (Exception e) { return "Unknown"; }
  }

//////////////////////////////////////////////////////////////////////////
// Reads
//////////////////////////////////////////////////////////////////////////

  protected HDict onReadById(HRef id) { return (HDict)recs.get(id.val); }

  protected Iterator iterator() { return recs.values().iterator(); }

//////////////////////////////////////////////////////////////////////////
// Navigation
//////////////////////////////////////////////////////////////////////////

  protected HGrid onNav(String navId)
  {
    // test database navId is record id
    HDict base = null;
    if (navId != null) base = readById(HRef.make(navId));

    // map base record to site, equip, or point
    String filter = "site";
    if (base != null)
    {
      if (base.has("site")) filter = "equip and siteRef==" + base.id().toCode();
      else if (base.has("equip")) filter = "point and equipRef==" + base.id().toCode();
      else filter = "navNoChildren";
    }

    // read children of base record
    HGrid grid = readAll(filter);

    // add navId column to results
    HDict[] rows = new HDict[grid.numRows()];
    Iterator it = grid.iterator();
    for (int i=0; it.hasNext(); ) rows[i++] = (HDict)it.next();
    for (int i=0; i<rows.length; ++i)
      rows[i] = new HDictBuilder().add(rows[i]).add("navId", rows[i].id().val).toDict();
    return HGridBuilder.dictsToGrid(rows);
  }

//////////////////////////////////////////////////////////////////////////
// Watches
//////////////////////////////////////////////////////////////////////////

  protected HWatch onWatchOpen(String dis)
  {
    throw new UnsupportedOperationException();
  }

  protected HWatch[] onWatches()
  {
    throw new UnsupportedOperationException();
  }

  protected HWatch onWatch(String id)
  {
    throw new UnsupportedOperationException();
  }

//////////////////////////////////////////////////////////////////////////
// History
//////////////////////////////////////////////////////////////////////////

  public HHisItem[] onHisRead(HDict entity, HDateTimeRange range)
  {
    // generate dummy 15min data
    ArrayList acc = new ArrayList();
    HDateTime ts = range.start;
    boolean isBool = ((HStr)entity.get("kind")).val.equals("Bool");
    while (ts.compareTo(range.end) <= 0)
    {
      HVal val = isBool ?
        (HVal)HBool.make(acc.size() % 2 == 0) :
        (HVal)HNum.make(acc.size());
      HDict item = HHisItem.make(ts, val);
      if (ts != range.start) acc.add(item);
      ts = HDateTime.make(ts.millis() + 15*60*1000);
    }
    return (HHisItem[])acc.toArray(new HHisItem[acc.size()]);
  }

  public void onHisWrite(HDict rec, HHisItem[] items)
  {
    throw new RuntimeException("Unsupported");
  }

  HashMap recs = new HashMap();
}