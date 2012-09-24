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
 * HGridReader is base class for reading grids from an input stream.
 */
public abstract class HGridReader
{

  /** Read a grid */
  public abstract HGrid readGrid();

}