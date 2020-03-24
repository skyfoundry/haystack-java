//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.io;

import static org.testng.Assert.*;
import org.projecthaystack.*;
import org.testng.annotations.Test;

public class JsonTest
{
  @Test
  public void testWriter()
  {
    HGridBuilder gb = new HGridBuilder();
    gb.addCol("a");
    gb.addCol("b");
    gb.addRow(new HVal[] { null, HBool.TRUE });
    gb.addRow(new HVal[] { HMarker.VAL, null });
    gb.addRow(new HVal[] { HRemove.VAL, HNA.VAL });
    gb.addRow(new HVal[] { HStr.make("test"), HStr.make("with:colon") });
    gb.addRow(new HVal[] { HNum.make(12), HNum.make(72.3, "\u00b0F") });
    gb.addRow(new HVal[] { HNum.make(Double.NEGATIVE_INFINITY), HNum.make(Double.NaN) });
    gb.addRow(new HVal[] { HDate.make(2015, 6, 9), HTime.make(1, 2, 3) });
    gb.addRow(new HVal[] { HDateTime.make(1307377618069L, HTimeZone.make("New_York")), HUri.make("foo.txt") });
    gb.addRow(new HVal[] { HRef.make("abc"), HRef.make("abc", "A B C") });
    gb.addRow(new HVal[] { HBin.make("text/plain"), HCoord.make(90, -123) });
    HGrid grid = gb.toGrid();

    String actual = HJsonWriter.gridToString(grid);
    // System.out.println(actual);
    String[] lines = HStr.split(actual, '\n', false);
    assertEquals(lines[0], "{");
    assertEquals(lines[1], "\"meta\": {\"ver\":\"3.0\"},");
    assertEquals(lines[2], "\"cols\":[");
    assertEquals(lines[3], "{\"name\":\"a\"},");
    assertEquals(lines[4], "{\"name\":\"b\"}");
    assertEquals(lines[5], "],");
    assertEquals(lines[6], "\"rows\":[");
    assertEquals(lines[7], "{\"b\":true},");
    assertEquals(lines[8], "{\"a\":\"m:\"},");
    assertEquals(lines[9], "{\"a\":\"x:\", \"b\":\"z:\"},");
    assertEquals(lines[10], "{\"a\":\"test\", \"b\":\"s:with:colon\"},");
    assertEquals(lines[11], "{\"a\":\"n:12\", \"b\":\"n:72.3 \u00b0F\"},");
    assertEquals(lines[12], "{\"a\":\"n:-INF\", \"b\":\"n:NaN\"},");
    assertEquals(lines[13], "{\"a\":\"d:2015-06-09\", \"b\":\"h:01:02:03\"},");
    assertEquals(lines[14], "{\"a\":\"t:2011-06-06T12:26:58.069-04:00 New_York\", \"b\":\"u:foo.txt\"},");
    assertEquals(lines[15], "{\"a\":\"r:abc\", \"b\":\"r:abc A B C\"},");
    assertEquals(lines[16], "{\"a\":\"b:text/plain\", \"b\":\"c:90.0,-123.0\"}");
    assertEquals(lines[17], "]");
    assertEquals(lines[18], "}");
  }
}
