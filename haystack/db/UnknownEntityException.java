//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  My birthday!
//
package haystack;

/**
 * UnresolvedEntityException is thrown when attempting to
 * to resolve an entity which is not found.
 */
public class UnknownEntityException extends RuntimeException
{

  /** Constructor with message */
  public UnknownEntityException(String msg)
  {
    super(msg);
  }

}