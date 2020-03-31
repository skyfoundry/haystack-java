//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import static org.testng.Assert.*;

import org.projecthaystack.ParseException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;

public class AuthMsgTest
{
  @Test
  public void testEncoding()
  {
    // basic identity
    assertEquals(AuthMsg.fromStr("foo"), AuthMsg.fromStr("foo"));
    assertEquals(AuthMsg.fromStr("a x=y"), AuthMsg.fromStr("a x=y"));
    assertEquals(AuthMsg.fromStr("a i=j, x=y"), AuthMsg.fromStr("a i=j, x=y"));
    assertEquals(AuthMsg.fromStr("a i=j, x=y"), AuthMsg.fromStr("a x=y ,i=j"));
    assertNotEquals(AuthMsg.fromStr("foo"), AuthMsg.fromStr("bar"));
    assertNotEquals(AuthMsg.fromStr("foo"), AuthMsg.fromStr("foo k=v"));

    // basics on fromStr
    AuthMsg q = AuthMsg.fromStr("foo alpha=beta, gamma=delta");
    assertEquals(q.scheme, "foo");
    assertEquals(q.param("alpha"), "beta");
    assertEquals(q.param("Alpha"), "beta");
    assertEquals(q.param("ALPHA"), "beta");
    assertEquals(q.param("Gamma"), "delta");

    // fromStr parsing
    HashMap<String,String> params = new HashMap<String,String>();
    params.put("alpha","beta");
    assertEquals(AuthMsg.fromStr("foo alpha \t = \t beta"), new AuthMsg("foo", params));

    params.clear();
    params.put("A","b");
    params.put("C","d");
    params.put("E","f");
    params.put("G","h");
    assertEquals(AuthMsg.fromStr("foo a=b, c = d, e=f, g=h"), new AuthMsg("foo", params));

    params.clear();
    params.put("G","h");
    params.put("E","f");
    params.put("C","d");
    params.put("A","b");
    assertEquals(AuthMsg.fromStr("foo a=b, c = d, e=f, g=h"), new AuthMsg("foo", params));

    params.clear();
    params.put("a","b");
    params.put("c","d");
    params.put("e","f");
    params.put("g","h");
    assertEquals(AuthMsg.fromStr("foo g=h, c = d, e=f,  a = b").toString(), "foo a=b, c=d, e=f, g=h");
  }

  @Test(expectedExceptions = ParseException.class,
        dataProvider = "BadStrProvider")
  public void testBadFromStr(String s)
  {
    AuthMsg.fromStr(s);
  }

  @DataProvider
  public Object[][] BadStrProvider()
  {
    return new Object[][] {
      {"hmac salt=a=b hash=sha-1"},
      {"hmac salt=abc hash=sha-1 bad/key=val"},
      {"(bad)"},
      {"ok key=val not good"},
      {"ok key not good=val"},
      {"ok key not good=val"},
      {"hmac foo"},
      {"hmac foo=bar xxx"},
    };
  }

  @Test
  public void testSplitList()
  {
    verifySplitList("a,b", new String[] {"a", "b"});
    verifySplitList("a \t,  b", new String[] {"a", "b"});
    verifySplitList("a, b, c", new String[] {"a", "b", "c"});
    verifySplitList("a b=c", new String[] {"a b=c"});
    verifySplitList("a b=c, d=e", new String[] {"a b=c,d=e"});
    verifySplitList("a b=c, d=e \t,\t f=g", new String[] {"a b=c,d=e,f=g"});
    verifySplitList("a b=c, d=e, f g=h", new String[] {"a b=c,d=e", "f g=h"});
    verifySplitList("a b=c, d=e, f, g h=i,j=k", new String[] {"a b=c,d=e", "f", "g h=i,j=k"});
  }

  private void verifySplitList(String s, String[] expected)
  {
    String[] split = AuthMsg.splitList(s);
    assertEquals(split.length, expected.length);
    for (int i=0; i<split.length; ++i) { assertEquals(split[i], expected[i]); }
    AuthMsg[] msgs = AuthMsg.listFromStr(s);
    assertEquals(msgs.length, expected.length);
  }
}
