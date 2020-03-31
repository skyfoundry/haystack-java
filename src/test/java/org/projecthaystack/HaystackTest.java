//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Jun 2016  Matthew Giannini  Creation
//
package org.projecthaystack;

public abstract class HaystackTest
{
  protected HNum n(long val) { return HNum.make(val); }
  protected HNum n(double val) { return HNum.make(val); }
  protected HNum n(double val, String unit) { return HNum.make(val, unit); }
}
