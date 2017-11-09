//
// Copyright (c) 2012, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Sep 2012  Brian Frank  Creation
//
package org.projecthaystack.io;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import org.projecthaystack.*;

/**
 * HZincWriter is used to write grids in the Zinc format
 *
 * @see <a href='http://project-haystack.org/doc/Zinc'>Project Haystack</a>
 */
public class HZincWriter extends HGridWriter
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /** Write using UTF-8 */
  public HZincWriter(OutputStream out)
  {
    try
    {
      this.out = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  /** Write a grid to an in-memory a string */
  public static String gridToString(HGrid grid)
  {
    StringWriter out = new StringWriter(grid.numCols() * grid.numRows() * 16);
    new HZincWriter(out).writeGrid(grid);
    return out.toString();
  }

  public static String valToString(HVal val)
  {
    StringWriter out = (val instanceof HGrid)
      ? new StringWriter(((HGrid)val).numCols() * ((HGrid)val).numRows() * 16)
      : new StringWriter();
    new HZincWriter(out).writeVal(val);
    return out.toString();
  }

  private HZincWriter(StringWriter out) { this.out = new PrintWriter(out); }

  /** Flush underlying output stream */
  public void flush()
  {
    out.flush();
  }

  /** Close underlying output stream */
  public void close()
  {
    out.close();
  }

  /** Write a zinc value */
  public HZincWriter writeVal(HVal val)
  {
    if (val instanceof HGrid)
    {
      HGrid grid = (HGrid)val;
      try
      {
        boolean insideGrid = gridDepth > 0;
        ++gridDepth;
        if (insideGrid) writeNestedGrid(grid);
        else writeGrid(grid);
      }
      finally
      {
        --gridDepth;
      }
    }
    else if (val instanceof HList) writeList((HList)val);
    else if (val instanceof HDict) writeDict((HDict)val);
    else writeScalar(val);
    return this;
  }

  private void writeNestedGrid(HGrid grid)
  {
    p("<<").nl();
    writeGrid(grid);
    p(">>");

  }

  private void writeList(HList list)
  {
    p('[');
    for (int i=0; i<list.size(); ++i)
    {
      if (i > 0) p(',');
      writeVal(list.get(i));
    }
    p(']');
  }

  private void writeDict(HDict dict)
  {
    p('{').writeDictKeyVals(dict).p('}');
  }

  private void writeScalar(HVal val)
  {
    if (val == null) p('N');
    else if (val instanceof HBin) writeBin((HBin)val);
    else if (val instanceof HXStr) writeXStr((HXStr)val);
    else p(val.toZinc());
  }


  private void writeBin(HBin bin)
  {
    if (this.version < 3)
    {
      p("Bin(").p(bin.mime).p(')');
    }
    else
    {
      p(bin.toZinc());
      p("Bin(").p('"').p(bin.mime).p('"').p(')');
    }
  }

  private void writeXStr(HXStr xstr)
  {
    if (this.version < 3) throw new RuntimeException("XStr not supported for version: " + this.version);
    p(xstr.toZinc());

  }

//////////////////////////////////////////////////////////////////////////
// HGridWriter
//////////////////////////////////////////////////////////////////////////

  /** Write a grid */
  public void writeGrid(HGrid grid)
  {
    // meta
    p("ver:\"").p(version).p(".0\"").writeMeta(grid.meta()).nl();

    // cols
    if (grid.numCols() == 0)
    {
      // technically this shoudl be illegal, but
      // for robustness handle it here
    }
    else
    {
      for (int i = 0; i < grid.numCols(); ++i)
      {
        if (i > 0) p(',');
        writeCol(grid.col(i));
      }
    }
    nl();

    // rows
    for (int i=0; i<grid.numRows(); ++i)
    {
      writeRow(grid, grid.row(i));
      nl();
    }
  }

  private HZincWriter writeMeta(HDict meta)
  {
    if (meta.isEmpty()) return this;
    p(' ');
    return writeDictKeyVals(meta);
  }

  private HZincWriter writeDictKeyVals(HDict dict)
  {
    if (dict.isEmpty()) return this;
    boolean first = true;
    for (Iterator it = dict.iterator(); it.hasNext(); )
    {
      Map.Entry entry = (Map.Entry)it.next();
      String name = (String)entry.getKey();
      HVal val = (HVal)entry.getValue();
      if (!first) p(' ');
      p(name);
      if (val != HMarker.VAL)
      {
        p(':').writeVal(val);
      }
      first = false;
    }
    return this;
  }

  private void writeCol(HCol col)
  {
    p(col.name()).writeMeta(col.meta());
  }

  private void writeRow(HGrid grid, HRow row)
  {
    for (int i=0; i<grid.numCols(); ++i)
    {
      HVal val = row.get(grid.col(i), false);
      if (i > 0) out.write(',');
      if (val == null)
      {
        if (i == 0) out.write('N');
      }
      else
      {
        out.write(val.toZinc());
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  private HZincWriter p(int i) { out.print(i); return this; }
  private HZincWriter p(char c) { out.print(c); return this; }
  private HZincWriter p(Object obj) { out.print(obj); return this; }
  private HZincWriter nl() { out.print('\n'); return this; }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  /** Version of Zinc to write */
  public int version = 3;

  private PrintWriter out;
  private int gridDepth = 0;

}