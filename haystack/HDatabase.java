//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   04 Oct 2011  Brian Frank  Creation
//
package haystack;

/**
 * HDatabase is used to interface the haystack toolkit with
 * an actually database of tagged entities.
 */
public interface HDatabase
{

  /**
   * Given a HRef string identifier, resolve to an entity's
   * HTags respresentation or ref is not found return null.
   */
  public HTags find(String ref);

}