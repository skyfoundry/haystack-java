//
// Copyright (c) 2012, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   26 Sep 2012  Brian Frank  Creation
//
package org.projecthaystack.test;

import org.projecthaystack.*;
import org.projecthaystack.io.*;
import org.projecthaystack.client.*;

/**
 * ClientTest -- this test requires an instance of SkySpark
 * running localhost port 8080 with the standard demo project
 * and a user account "haystack/testpass".
 */
public class ClientTest extends Test
{

  final String uri = "http://localhost:8080/api/demo";
  final String user = "haystack";
  final String pass = "testpass";

  HClient client;

//////////////////////////////////////////////////////////////////////////
// Main
//////////////////////////////////////////////////////////////////////////

  public void test() throws Exception
  {
    verifyAuth();
    verifyAbout();
    verifyOps();
    verifyFormats();
    verifyRead();
    verifyEval();
    verifyWatches();
    verifyHisRead();
    verifyHisWrite();
  }

//////////////////////////////////////////////////////////////////////////
// Auth
//////////////////////////////////////////////////////////////////////////

  void verifyAuth() throws Exception
  {
    // get bad credentials
    try { HClient.open(uri, "baduser", "badpass").about(); fail(); } catch (CallAuthException e) { verifyException(e); }
    try { HClient.open(uri, "haystack", "badpass").about(); fail(); } catch (CallAuthException e) { verifyException(e); }

    // create proper client
    this.client = HClient.open(uri, user, pass);
  }

//////////////////////////////////////////////////////////////////////////
// About
//////////////////////////////////////////////////////////////////////////

  void verifyAbout() throws Exception
  {
    HDict r = client.about();
    verifyEq(r.getStr("haystackVersion"), "2.0");
    verifyEq(r.getStr("productName"), "SkySpark");
    verifyEq(r.getStr("tz"), HTimeZone.DEFAULT.name);
  }

//////////////////////////////////////////////////////////////////////////
// Ops
//////////////////////////////////////////////////////////////////////////

  void verifyOps() throws Exception
  {
    HGrid g = client.ops();

    // verify required columns
    verify(g.col("name")  != null);
    verify(g.col("summary") != null);

    // verify required ops
    verifyGridContains(g, "name", "about");
    verifyGridContains(g, "name", "ops");
    verifyGridContains(g, "name", "formats");
    verifyGridContains(g, "name", "read");
  }

//////////////////////////////////////////////////////////////////////////
// Formats
//////////////////////////////////////////////////////////////////////////

  void verifyFormats() throws Exception
  {
    HGrid g = client.formats();

    // verify required columns
    verify(g.col("mime")    != null);
    verify(g.col("receive") != null);
    verify(g.col("send")    != null);

    // verify required ops
    verifyGridContains(g, "mime", "text/plain");
    verifyGridContains(g, "mime", "text/zinc");
  }

//////////////////////////////////////////////////////////////////////////
// Reads
//////////////////////////////////////////////////////////////////////////

  void verifyRead() throws Exception
  {
    // read
    String disA = "Gaithersburg";
    String disB = "Carytown";
    HDict recA = client.read("site and dis==\"" + disA + "\"");
    HDict recB = client.read("site and dis==\"" + disB + "\"");
    verifyEq(recA.dis(), disA);
    verifyEq(client.read("badTagShouldBeThere", false), null);
    try { client.read("badTagShouldBeThere"); fail(); } catch(UnknownRecException e) { verifyException(e); }

    // readAll
    HGrid grid = client.readAll("site");
    verifyGridContains(grid, "dis", disA);
    verifyGridContains(grid, "dis", disB);
    verifyGridContains(grid, "id", recA.id());
    verifyGridContains(grid, "id", recB.id());

    // readAll limit
    verify(grid.numRows() > 2);
    verifyEq(client.readAll("site", 2).numRows(), 2);

    // readById
    HDict rec = client.readById(recA.id());
    verifyEq(rec.dis(), disA);
    HRef badId = HRef.make("badBadId");
    verifyEq(client.readById(badId, false), null);
    try { client.readById(badId); fail(); } catch(UnknownRecException e) { verifyException(e); }

    // readByIds
    grid = client.readByIds(new HRef[] { recA.id(), recB.id() });
    verifyEq(grid.numRows(), 2);
    verifyEq(grid.row(0).dis(), disA);
    verifyEq(grid.row(1).dis(), disB);
    grid = client.readByIds(new HRef[] { recA.id(), badId, recB.id() }, false);
    verifyEq(grid.numRows(), 3);
    verifyEq(grid.row(0).dis(), disA);
    verifyEq(grid.row(1).missing("id"), true);
    verifyEq(grid.row(2).dis(), disB);
    try { client.readByIds(new HRef[] { recA.id(), badId }); fail(); } catch(UnknownRecException e) { verifyException(e); }
  }

//////////////////////////////////////////////////////////////////////////
// Eval
//////////////////////////////////////////////////////////////////////////

  void verifyEval() throws Exception
  {
    HGrid g = client.eval("today()");
    verifyEq(g.row(0).get("val"), HDate.today());

    g = client.eval("readAll(ahu)");
    verify(g.numRows() > 0);
    verifyGridContains(g, "navName", "RTU-1");

    HGrid[] grids = client.evalAll(new String[] { "today()", "[10, 20, 30]", "readAll(site)"});
    verifyEq(grids.length, 3);
    g = grids[0];
    verifyEq(g.numRows(), 1);
    verifyEq(g.row(0).get("val"), HDate.today());
    g = grids[1];
    verifyEq(g.numRows(), 3);
    verifyEq(g.row(0).get("val"), HNum.make(10));
    verifyEq(g.row(1).get("val"), HNum.make(20));
    verifyEq(g.row(2).get("val"), HNum.make(30));
    g = grids[2];
    verify(g.numRows() > 2);
    verifyGridContains(g, "dis", "Carytown");

    grids = client.evalAll(new String[] { "today()", "readById(@badBadBadId)"}, false);
    // for (int i=0; i<grids.length; ++i) grids[i].dump();
    verifyEq(grids.length, 2);
    verifyEq(grids[0].isErr(), false);
    verifyEq(grids[0].row(0).get("val"), HDate.today());
    verifyEq(grids[1].isErr(), true);
    try { client.evalAll(new String[] { "today()", "readById(@badBadBadId)"}); fail(); } catch (CallErrException e) { verifyException(e); }
  }

//////////////////////////////////////////////////////////////////////////
// Watches
//////////////////////////////////////////////////////////////////////////

  void verifyWatches() throws Exception
  {
    final String watchDis = "Java Haystack Test " + System.currentTimeMillis();

    // create new watch
    HWatch w = client.watchOpen(watchDis, HNum.make(3, "min"));
    verifyEq(w.id(), null);
    verifyEq(w.dis(), watchDis);
    verifyEq(w.lease(), null);

    // do query to get some recs
    HGrid recs = client.readAll("ahu");
    verify(recs.numRows() >= 4);
    HDict a = recs.row(0);
    HDict b = recs.row(1);
    HDict c = recs.row(2);
    HDict d = recs.row(3);

    // do first sub
    HGrid sub = w.sub(new HRef[] { a.id(), b.id() });
    verifyEq(sub.numRows(), 2);
    verifyEq(sub.row(0).dis(), a.dis());
    verifyEq(sub.row(1).dis(), b.dis());

    // now add c, bad, d
    HRef badId = HRef.make("badBadBad");
    try { w.sub(new HRef[] { badId }).dump(); fail(); } catch (UnknownRecException e) { verifyException(e); }
    sub = w.sub(new HRef[] { c.id(), badId , d.id() }, false);
    verifyEq(sub.numRows(), 3);
    verifyEq(sub.row(0).dis(), c.dis());
    verifyEq(sub.row(1).missing("id"), true);
    verifyEq(sub.row(2).dis(), d.dis());

    // verify state of watch now
    verify(client.watch(w.id()) == w);
    verifyEq(client.watches().length, 1);
    verify(client.watches()[0] == w);
    verifyEq(w.lease().millis(), 180000L);

    // poll for changes (should be none yet)
    HGrid poll = w.pollChanges();
    verifyEq(poll.numRows(), 0);

    // make change to b and d
    verifyEq(b.has("javaTest"), false);
    verifyEq(d.has("javaTest"), false);
    client.eval("commit(diff(readById(@" + b.id().val + "), {javaTest:123}))");
    client.eval("commit(diff(readById(@" + d.id().val + "), {javaTest:456}))");
    poll = w.pollChanges();
    verifyEq(poll.numRows(), 2);
    HDict newb, newd;
    if (poll.row(0).id().equals(b.id())) { newb = poll.row(0); newd = poll.row(1); }
    else { newb = poll.row(1); newd = poll.row(0); }
    verifyEq(newb.id(), b.id());
    verifyEq(newd.id(), d.id());
    verifyEq(newb.get("javaTest"), HNum.make(123));
    verifyEq(newd.get("javaTest"), HNum.make(456));

    // poll refresh
    poll = w.pollRefresh();
    verifyEq(poll.numRows(), 4);
    verifyGridContains(poll, "id", a.id());
    verifyGridContains(poll, "id", b.id());
    verifyGridContains(poll, "id", c.id());
    verifyGridContains(poll, "id", d.id());

    // remove d, and then poll changes
    w.unsub(new HRef[] { d.id() });
    client.eval("commit(diff(readById(@" + b.id().val + "), {-javaTest}))");
    client.eval("commit(diff(readById(@" + d.id().val + "), {-javaTest}))");
    poll = w.pollChanges();
    verifyEq(poll.numRows(), 1);
    verifyEq(poll.row(0).id(), b.id());
    verifyEq(poll.row(0).has("javaTest"), false);

    // remove a and c and poll refresh
    w.unsub(new HRef[] { a.id(), c.id() });
    poll = w.pollRefresh();
    verifyEq(poll.numRows(), 1);
    verifyEq(poll.row(0).id(), b.id());

    // close
    String expr = "folioDebugWatches().findAll(x=>x->dis.contains(\"" + watchDis + "\")).size";
    verifyEq(client.eval(expr).row(0).getInt("val"), 1);
    w.close();
    try { poll = w.pollRefresh(); fail(); } catch (Exception e) { verifyException(e); }
    verifyEq(client.eval(expr).row(0).getInt("val"), 0);
    verifyEq(client.watch(w.id(), false), null);
    verifyEq(client.watches().length, 0);
  }

//////////////////////////////////////////////////////////////////////////
// His Reads
//////////////////////////////////////////////////////////////////////////

  void verifyHisRead() throws Exception
  {
    HDict kw = client.read("power and siteMeter");
    HGrid his = client.hisRead(kw.id(), "yesterday");
    verifyEq(his.meta().id(), kw.id());
    verifyEq(ts(his.meta(), "hisStart").date, HDate.today().minusDays(1));
    verifyEq(ts(his.meta(), "hisEnd").date, HDate.today());
    verify(his.numRows() > 90);
    int last = his.numRows()-1;
    verifyEq(ts(his.row(0)).date, HDate.today().minusDays(1));
    verifyEq(ts(his.row(0)).time, HTime.make(0, 15));
    verifyEq(ts(his.row(last)).date, HDate.today());
    verifyEq(ts(his.row(last)).time, HTime.make(0, 0));
    verifyEq(numVal(his.row(0)).unit, "kW");
  }

  private HDateTime ts(HDict r, String col) { return (HDateTime)r.get(col); }
  private HDateTime ts(HDict r) { return (HDateTime)r.get("ts"); }
  private HNum numVal(HRow r) { return (HNum)r.get("val"); }

//////////////////////////////////////////////////////////////////////////
// His Reads
//////////////////////////////////////////////////////////////////////////

  void verifyHisWrite() throws Exception
  {
    // setup test
    HDict kw = client.read("power and not siteMeter");
    clearHisWrite(kw);

    // create some items
    HDate date = HDate.make(2010, 6, 7);
    HTimeZone tz = HTimeZone.make(kw.getStr("tz"));
    HHisItem[] write = new HHisItem[5];
    for (int i=0; i<write.length; ++i)
    {
      HDateTime ts = HDateTime.make(date, HTime.make(i+1, 0), tz);
      HVal val = HNum.make(i, "kW");
      write[i] = HHisItem.make(ts, val);
    }

    // write and verify
    client.hisWrite(kw.id(), write);
    Thread.sleep(200);
    HGrid read = client.hisRead(kw.id(), "2010-06-07");
    verifyEq(read.numRows(), write.length);
    for (int i=0; i<read.numRows(); ++i)
    {
      verifyEq(read.row(i).get("ts"), write[i].ts);
      verifyEq(read.row(i).get("val"), write[i].val);
    }

    // clean test
    clearHisWrite(kw);
  }

  private void clearHisWrite(HDict rec)
  {
    // existing data and verify we don't have any data for 7 June 20120
    String expr = "hisClear(@" + rec.id().val + ", 2010-06)";
    client.eval(expr);
    HGrid his = client.hisRead(rec.id(), "2010-06-07");
    verifyEq(his.numRows(), 0);
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  void verifyGridContains(HGrid g, String col, String val) { verifyGridContains(g, col, HStr.make(val)); }
  void verifyGridContains(HGrid g, String col, HVal val)
  {
    boolean found = false;
    for (int i=0; i<g.numRows(); ++i)
    {
      HVal x = g.row(i).get(col, false);
      if (x != null && x.equals(val)) { found = true; break; }
    }
    if (!found)
    {
      System.out.println("verifyGridContains " + col + "=" + val + " failed!");
      fail();
    }
  }

}