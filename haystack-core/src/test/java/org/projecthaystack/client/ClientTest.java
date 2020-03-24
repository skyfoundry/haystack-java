//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.client;

import org.projecthaystack.*;
import org.projecthaystack.auth.AuthException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * ClientTest -- this test requires an instance of SkySpark
 * running localhost port 8080 with the standard demo project
 * and a user account "haystack/testpass".
 */
/*
@Test(groups = {"client"}, enabled = false)
public class ClientTest
{
  // TODO: figure out how to parameterize this for TestNG
  final String uri = "http://localhost:8080/api/demo";
  final String user = "su";
  final String pass = "su";

  HClient client;

  @BeforeTest
  public void setup()
  {
    this.client = HClient.open(uri, user, pass);
  }

  @Test(expectedExceptions = AuthException.class)
  public void testBadUser()
  {
    HClient.open(uri, "baduser", pass);
  }

  @Test(expectedExceptions = AuthException.class)
  public void testBadPass()
  {
    HClient.open(uri, user, "badpass");
  }

  @Test
  public void testAbout()
  {
    HDict r = client.about();
    System.out.println(r);
    //assertEquals(r.getStr("haystackVersion"), "2.0");
    assertEquals(r.getStr("productName"), "SkySpark");
    assertEquals(r.getStr("tz"), HTimeZone.DEFAULT.name);
  }

  @Test
  public void testOps()
  {
    HGrid g = client.ops();

    // verify required columns
    assertNotNull(g.col("name"));
    assertNotNull(g.col("summary"));

    // verify required ops
    verifyGridContains(g, "name", "about");
    verifyGridContains(g, "name", "ops");
    verifyGridContains(g, "name", "formats");
    verifyGridContains(g, "name", "read");
  }

  @Test
  public void testFormats()
  {
    HGrid g = client.formats();

    // verify required columns
    assertNotNull(g.col("mime"));
    assertNotNull(g.col("receive"));
    assertNotNull(g.col("send"));

    // verify required ops
    verifyGridContains(g, "mime", "text/plain");
    verifyGridContains(g, "mime", "text/zinc");
  }

  @Test
  public void testRead()
  {
    // read
    String disA = "Gaithersburg";
    String disB = "Carytown";
    HDict recA = client.read("site and dis==\"" + disA + "\"");
    HDict recB = client.read("site and dis==\"" + disB + "\"");
    assertEquals(recA.dis(), disA);
    assertNull(client.read("badTagShouldBeThere", false));
    try { client.read("badTagShouldBeThere"); fail(); } catch(UnknownRecException e) { assertTrue(true); }

    // readAll
    HGrid grid = client.readAll("site");
    verifyGridContains(grid, "dis", disA);
    verifyGridContains(grid, "dis", disB);
    verifyGridContains(grid, "id", recA.id());
    verifyGridContains(grid, "id", recB.id());

    // readAll limit
    assertTrue(grid.numRows() > 2);
    assertEquals(client.readAll("site", 2).numRows(), 2);

    // readById
    HDict rec = client.readById(recA.id());
    assertEquals(rec.dis(), disA);
    HRef badId = HRef.make("badBadId");
    assertNull(client.readById(badId, false));
    try { client.readById(badId); fail(); } catch(UnknownRecException e) { assertTrue(true); }

    // readByIds
    grid = client.readByIds(new HRef[] { recA.id(), recB.id() });
    assertEquals(grid.numRows(), 2);
    assertEquals(grid.row(0).dis(), disA);
    assertEquals(grid.row(1).dis(), disB);
    grid = client.readByIds(new HRef[] { recA.id(), badId, recB.id() }, false);
    assertEquals(grid.numRows(), 3);
    assertEquals(grid.row(0).dis(), disA);
    assertTrue(grid.row(1).missing("id"));
    assertEquals(grid.row(2).dis(), disB);
    try { client.readByIds(new HRef[] { recA.id(), badId }); fail(); } catch(UnknownRecException e) { assertTrue(true); }
  }

  @Test
  public void verifyEval() 
  {
    HGrid g = client.eval("today()");
    assertEquals(g.row(0).get("val"), HDate.today());

    g = client.eval("readAll(ahu)");
    assertTrue(g.numRows() > 0);
    verifyGridContains(g, "navName", "RTU-1");

    HGrid[] grids = client.evalAll(new String[] { "today()", "[10, 20, 30]", "readAll(site)"});
    assertEquals(grids.length, 3);
    g = grids[0];
    assertEquals(g.numRows(), 1);
    assertEquals(g.row(0).get("val"), HDate.today());
    g = grids[1];
    assertEquals(g.numRows(), 3);
    assertEquals(g.row(0).get("val"), HNum.make(10));
    assertEquals(g.row(1).get("val"), HNum.make(20));
    assertEquals(g.row(2).get("val"), HNum.make(30));
    g = grids[2];
    assertTrue(g.numRows() > 2);
    verifyGridContains(g, "dis", "Carytown");

    grids = client.evalAll(new String[] { "today()", "readById(@badBadBadId)"}, false);
    // for (int i=0; i<grids.length; ++i) grids[i].dump();
    assertEquals(grids.length, 2);
    assertFalse(grids[0].isErr());
    assertEquals(grids[0].row(0).get("val"), HDate.today());
    assertTrue(grids[1].isErr());
    try { client.evalAll(new String[] { "today()", "readById(@badBadBadId)"}); fail(); } catch (CallErrException e) { }
  }
  
  @Test
  public void verifyWatches() 
  {
    final String watchDis = "Java Haystack Test " + System.currentTimeMillis();

    // create new watch
    HWatch w = client.watchOpen(watchDis, HNum.make(3, "min"));
    assertNull(w.id());
    assertEquals(w.dis(), watchDis);
    assertNull(w.lease());

    // do query to get some recs
    HGrid recs = client.readAll("ahu");
    assertTrue(recs.numRows() >= 4);
    HDict a = recs.row(0);
    HDict b = recs.row(1);
    HDict c = recs.row(2);
    HDict d = recs.row(3);

    // do first sub
    HGrid sub = w.sub(new HRef[] { a.id(), b.id() });
    assertEquals(sub.numRows(), 2);
    assertEquals(sub.row(0).dis(), a.dis());
    assertEquals(sub.row(1).dis(), b.dis());

    // now add c, bad, d
    HRef badId = HRef.make("badBadBad");
    try { w.sub(new HRef[] { badId }).dump(); fail(); } catch (UnknownRecException e) { }
    sub = w.sub(new HRef[] { c.id(), badId , d.id() }, false);
    assertEquals(sub.numRows(), 3);
    assertEquals(sub.row(0).dis(), c.dis());
    assertTrue(sub.row(1).missing("id"));
    assertEquals(sub.row(2).dis(), d.dis());

    // verify state of watch now
    assertTrue(client.watch(w.id()) == w);
    assertEquals(client.watches().length, 1);
    assertTrue(client.watches()[0] == w);
    assertEquals(w.lease().millis(), 180000L);

    // poll for changes (should be none yet)
    HGrid poll = w.pollChanges();
    assertEquals(poll.numRows(), 0);

    // make change to b and d
    assertFalse(b.has("javaTest"));
    assertFalse(d.has("javaTest"));
    client.eval("commit(diff(readById(@" + b.id().val + "), {javaTest:123}))");
    client.eval("commit(diff(readById(@" + d.id().val + "), {javaTest:456}))");
    poll = w.pollChanges();
    assertEquals(poll.numRows(), 2);
    HDict newb, newd;
    if (poll.row(0).id().equals(b.id())) { newb = poll.row(0); newd = poll.row(1); }
    else { newb = poll.row(1); newd = poll.row(0); }
    assertEquals(newb.id(), b.id());
    assertEquals(newd.id(), d.id());
    assertEquals(newb.get("javaTest"), HNum.make(123));
    assertEquals(newd.get("javaTest"), HNum.make(456));

    // poll refresh
    poll = w.pollRefresh();
    assertEquals(poll.numRows(), 4);
    verifyGridContains(poll, "id", a.id());
    verifyGridContains(poll, "id", b.id());
    verifyGridContains(poll, "id", c.id());
    verifyGridContains(poll, "id", d.id());

    // remove d, and then poll changes
    w.unsub(new HRef[] { d.id() });
    client.eval("commit(diff(readById(@" + b.id().val + "), {-javaTest}))");
    client.eval("commit(diff(readById(@" + d.id().val + "), {-javaTest}))");
    poll = w.pollChanges();
    assertEquals(poll.numRows(), 1);
    assertEquals(poll.row(0).id(), b.id());
    assertFalse(poll.row(0).has("javaTest"));

    // remove a and c and poll refresh
    w.unsub(new HRef[] { a.id(), c.id() });
    poll = w.pollRefresh();
    assertEquals(poll.numRows(), 1);
    assertEquals(poll.row(0).id(), b.id());

    // close
    String expr = "folioDebugWatches().findAll(x=>x->dis.contains(\"" + watchDis + "\")).size";
    assertEquals(client.eval(expr).row(0).getInt("val"), 1);
    w.close();
    try { poll = w.pollRefresh(); fail(); } catch (Exception e) { }
    assertEquals(client.eval(expr).row(0).getInt("val"), 0);
    assertNull(client.watch(w.id(), false));
    assertEquals(client.watches().length, 0);
  }

  @Test
  public void testHisRead()
  {
    HDict kw = client.read("power and siteMeter");
    HGrid his = client.hisRead(kw.id(), "yesterday");
    assertEquals(his.meta().id(), kw.id());
    assertEquals(ts(his.meta(), "hisStart").date, HDate.today().minusDays(1));
    assertEquals(ts(his.meta(), "hisEnd").date, HDate.today());
    assertTrue(his.numRows() > 90);
    int last = his.numRows()-1;
    assertEquals(ts(his.row(0)).date, HDate.today().minusDays(1));
    assertEquals(ts(his.row(0)).time, HTime.make(0, 15));
    assertEquals(ts(his.row(last)).date, HDate.today());
    assertEquals(ts(his.row(last)).time, HTime.make(0, 0));
    assertEquals(numVal(his.row(0)).unit, "kW");
  }

  private HDateTime ts(HDict r, String col) { return (HDateTime)r.get(col); }
  private HDateTime ts(HDict r) { return (HDateTime)r.get("ts"); }
  private HNum numVal(HRow r) { return (HNum)r.get("val"); }

  // TODO:FIXIT: this test is failing. I think it is because of changes
  // to start/end exclusivity for hisReads but haven't really looked into it
  @Test(enabled = false)
  public void testHisWrite() throws Exception
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
    assertEquals(read.numRows(), write.length);
    for (int i=0; i<read.numRows(); ++i)
    {
      assertEquals(read.row(i).get("ts"), write[i].ts);
      assertEquals(read.row(i).get("val"), write[i].val);
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
    assertEquals(his.numRows(), 0);
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  private void verifyGridContains(HGrid g, String col, String val) { verifyGridContains(g, col, HStr.make(val)); }
  private void verifyGridContains(HGrid g, String col, HVal val)
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
*/
