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

  public void test() throws Exception
  {
    verifyAuth();
    verifyAbout();
    verifyOps();
    verifyFormats();
    verifyRead();
  }

  void verifyAuth() throws Exception
  {
    // get bad credentials
    try { HClient.open(uri, "baduser", "badpass").about(); fail(); } catch (CallAuthException e) { verifyException(e); }
    try { HClient.open(uri, "haystack", "badpass").about(); fail(); } catch (CallAuthException e) { verifyException(e); }

    // create proper client
    this.client = HClient.open("http://localhost/api/demo", "haystack", "testpass");
  }

  void verifyAbout() throws Exception
  {
    HDict r = client.about();
    verifyEq(r.getStr("haystackVersion"), "2.0");
    verifyEq(r.getStr("productName"), "SkySpark");
    verifyEq(r.getStr("tz"), HTimeZone.DEFAULT.name);
  }

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
    HGrid sites = client.readAll("site");
    verifyGridContains(sites, "dis", disA);
    verifyGridContains(sites, "dis", disB);
    verifyGridContains(sites, "id", recA.id());
    verifyGridContains(sites, "id", recB.id());

    // readAll limit
    verify(sites.numRows() > 2);
    verifyEq(client.readAll("site", 2).numRows(), 2);

    // readById
    HDict rec = client.readById(recA.id());
    verifyEq(rec.dis(), disA);
// TODO bad ids

    // readByIds
    sites = client.readByIds(new HRef[] { recA.id(), recB.id() });
    verifyEq(sites.numRows(), 2);
    verifyEq(sites.row(0).dis(), disA);
    verifyEq(sites.row(1).dis(), disB);
// TODO bad ids
  }

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