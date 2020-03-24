//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   01 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack.auth;

import org.projecthaystack.client.CallException;

/**
 * AuthException is thrown by the authentication framework if an error occurs while trying
 * to authenticat a user.
 */
public class AuthException extends CallException
{
  public AuthException(String s)
  {
    super(s);
  }

  public AuthException(String s, Throwable throwable)
  {
    super(s, throwable);
  }
}
