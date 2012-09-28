//
// Copyright (c) 2012, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   26 Sep 2012  Brian Frank  Creation
//
package haystack.test;

import haystack.*;
import haystack.io.*;
import haystack.client.*;

/**
 * ClientTest -- this test requires an instance of SkySpark
 * running localhost port 80 with the standard demo project
 * and a user account "haystack/testpass".
 */
public class ClientTest extends Test
{

  final String uri = "http://localhost/api/demo";
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
    verifyHisRead();
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
    this.client = HClient.open("http://localhost/api/demo", "haystack", "testpass");
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
    verify(g.col("mime")  != null);
    verify(g.col("read") != null);
    verify(g.col("write") != null);

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
    verifyGridContains(g, "dis", "Carytown RTU-1");

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
// His Reads
//////////////////////////////////////////////////////////////////////////

  void verifyHisRead() throws Exception
  {
    HDict kw = client.read("kw and siteMeter");
    HHisItem[] his = client.hisRead(kw.id(), "yesterday");
    //for (int i=0; i<his.length; ++i) System.out.println("  " + his[i]);
    verify(his.length > 90);
    verifyEq(his[20].ts.date, HDate.today().minusDays(1));
    verifyEq(((HNum)his[0].val).unit, "kW");
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