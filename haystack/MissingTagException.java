//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 Jun 2011  Brian Frank  My birthday!
//
package haystack;

import java.util.HashMap;

/**
 * MissingTagException is thrown when attempting to perform
 * a checked get on HTags for a tag not present.
 */
public class MissingTagException extends RuntimeException
{

  /** Constructor with message */
  public MissingTagException(String msg)
  {
    super(msg);
  }

}