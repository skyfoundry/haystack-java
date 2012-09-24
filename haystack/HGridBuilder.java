//
// Copyright (c) 2012, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 Sep 2012  Brian Frank  Creation
//
package haystack;

import java.util.ArrayList;

/**
 * HGridBuilder is used to construct an immutable HGrid instance.
 */
public class HGridBuilder
{

  /** Get the builder for the grid meta map */
  public final HDictBuilder meta()
  {
    return meta;
  }

  /** Add new column and return builder for column metadata.
      Columns cannot be added after adding the first row. */
  public final HDictBuilder addCol(String name)
  {
    if (rows.size() > 0)
      throw new IllegalStateException("Cannot add cols after rows have been added");
    BCol col = new BCol(name);
    cols.add(col);
    return col.meta;
  }

  /** Add new row with array of cells which correspond to column
      order.  Return this. */
  public final HGridBuilder addRow(HVal[] cells)
  {
    addRowRaw((HVal[])cells.clone());
    return this;
  }

  /** Add new row with array of cells which correspond to. */
  final void addRowRaw(HVal[] cells)
  {
    if (cols.size() != cells.length)
      throw new IllegalStateException("Row cells size != cols size");
    rows.add(cells);
  }

  /** Convert current state to an immutable HGrid instance */
  public final HGrid toGrid()
  {
    // meta
    HDict meta = this.meta.toDict();

    // cols
    HCol[] hcols = new HCol[this.cols.size()];
    for (int i=0; i<hcols.length; ++i)
    {
      BCol bc = (BCol)this.cols.get(i);
      hcols[i] = new HCol(i, bc.name, bc.meta.toDict());
    }

    // let HGrid constructor do the rest...
    return new HGrid(meta, hcols, rows);
  }

  static class BCol
  {
    BCol(String name) { this.name = name; }
    final String name;
    final HDictBuilder meta = new HDictBuilder();
  }

  private final HDictBuilder meta = new HDictBuilder();
  private final ArrayList cols = new ArrayList();
  private final ArrayList rows = new ArrayList();
}