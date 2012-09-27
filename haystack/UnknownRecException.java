//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   03 Nov 2011  Brian Frank  My birthday!
//
package haystack;

/**
 * UnresolvedRecException is thrown when attempting to
 * to resolve an entity record which is not found.
 */
public class UnknownRecException extends RuntimeException
{

  /** Constructor with message */
  public UnknownRecException(String msg)
  {
    super(msg);
  }

  /** Constructor with Ref */
  public UnknownRecException(HRef ref)
  {
    this(String.valueOf(ref));
  }

}