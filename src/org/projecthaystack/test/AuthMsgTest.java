//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 2016  Matthew Giannini  Creation
//
package org.projecthaystack.test;

import org.projecthaystack.auth.AuthMsg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * AuthMsgTest tests the AuthMsg class
 */
public class AuthMsgTest extends Test
{

//////////////////////////////////////////////////////////////////////////
// Encode/Decode
//////////////////////////////////////////////////////////////////////////

  public void testEncoding()
  {
    // basic identity
    verifyEq(AuthMsg.fromStr("foo"), AuthMsg.fromStr("foo"));
    verifyEq(AuthMsg.fromStr("a x=y"), AuthMsg.fromStr("a x=y"));
    verifyEq(AuthMsg.fromStr("a i=j, x=y"), AuthMsg.fromStr("a i=j, x=y"));
    verifyEq(AuthMsg.fromStr("a i=j, x=y"), AuthMsg.fromStr("a x=y ,i=j"));
    verifyNotEq(AuthMsg.fromStr("foo"), AuthMsg.fromStr("bar"));
    verifyNotEq(AuthMsg.fromStr("foo"), AuthMsg.fromStr("foo k=v"));

    // basics on fromStr
    AuthMsg q = AuthMsg.fromStr("foo alpha=beta, gamma=delta");
    verifyEq(q.scheme, "foo");
    verifyEq(q.param("alpha"), "beta");
    verifyEq(q.param("Alpha"), "beta");
    verifyEq(q.param("ALPHA"), "beta");
    verifyEq(q.param("Gamma"), "delta");

    // fromStr parsing
    HashMap params = new HashMap();
    params.put("alpha","beta");
    verifyEq(AuthMsg.fromStr("foo alpha \t = \t beta"), new AuthMsg("foo", params));

    params.clear();
    params.put("A","b");
    params.put("C","d");
    params.put("E","f");
    params.put("G","h");
    verifyEq(AuthMsg.fromStr("foo a=b, c = d, e=f, g=h"), new AuthMsg("foo", params));

    params.clear();
    params.put("G","h");
    params.put("E","f");
    params.put("C","d");
    params.put("A","b");
    verifyEq(AuthMsg.fromStr("foo a=b, c = d, e=f, g=h"), new AuthMsg("foo", params));

    params.clear();
    params.put("a","b");
    params.put("c","d");
    params.put("e","f");
    params.put("g","h");
    verifyEq(AuthMsg.fromStr("foo g=h, c = d, e=f,  a = b").toString(), "foo a=b, c=d, e=f, g=h");

    try { AuthMsg.fromStr("hmac salt=a=b hash=sha-1"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("hmac salt=abc hash=sha-1 bad/key=val"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("(bad)"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("ok key=val not good"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("ok key not good=val"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("ok key not good=val"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("hmac foo"); verify(false); } catch (Exception e) { verify(true); }
    try { AuthMsg.fromStr("hmac foo=bar xxx"); verify(false); } catch (Exception e) { verify(true); }
  }

//////////////////////////////////////////////////////////////////////////
// Split List
//////////////////////////////////////////////////////////////////////////

  public void testSplitList() throws Exception
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

  public void verifySplitList(String s, String[] expected)
  {
    String[] split = AuthMsg.splitList(s);
    verifyEq(split.length, expected.length);
    for (int i=0; i<split.length; ++i) { verifyEq(split[i], expected[i]); }
    AuthMsg[] msgs = AuthMsg.listFromStr(s);
    verifyEq(msgs.length, expected.length);
  }
}
