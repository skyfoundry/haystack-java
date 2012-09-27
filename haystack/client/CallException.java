//
// Copyright (c) 2012, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   27 Sep 2012  Brian Frank  Creation
//
package haystack.client;

import haystack.*;

/**
 * CallException base class for exceptions thrown HClient.call.
 */
public class CallException extends RuntimeException
{

  /** Constructor with message */
  public CallException(String msg)
  {
    super(msg);
  }

  /** Constructor with message and cause */
  public CallException(String msg, Throwable cause)
  {
    super(msg, cause);
  }

}