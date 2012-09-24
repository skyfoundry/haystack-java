//
// Copyright (c) 2012, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   24 Sep 2012  Brian Frank  Creation
//
package haystack.io;

import haystack.*;

/**
 * HGridWriter is base class for writing grids to an output stream.
 */
public abstract class HGridWriter
{

  /** Write a grid */
  public abstract void writeGrid(HGrid grid);

}