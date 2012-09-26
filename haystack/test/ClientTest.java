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
  }

  void verifyAuth() throws Exception
  {
    // get bad credentials
    try { HClient.open(uri, "baduser", "badpass").about(); fail(); } catch (Exception e) { verifyException(e); }
    try { HClient.open(uri, "haystack", "badpass").about(); fail(); } catch (Exception e) { verifyException(e); }

    // create proper client
    this.client = HClient.open("http://localhost/api/demo", "haystack", "testpass");
  }

  void verifyAbout() throws Exception
  {
    HGrid g = client.about();
    HRow r = g.row(0);
    verifyEq(g.numRows(), 1);
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
    verifyGridContains(g, "name", HStr.make("about"));
    verifyGridContains(g, "name", HStr.make("ops"));
    verifyGridContains(g, "name", HStr.make("formats"));
    //verifyGridContains(g, "name", HStr.make("read"));
  }

  void verifyFormats() throws Exception
  {
    HGrid g = client.formats();

    // verify required columns
    verify(g.col("mime")  != null);
    verify(g.col("read") != null);
    verify(g.col("write") != null);

    // verify required ops
    verifyGridContains(g, "mime", HStr.make("text/plain"));
    verifyGridContains(g, "mime", HStr.make("text/zinc"));
  }

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