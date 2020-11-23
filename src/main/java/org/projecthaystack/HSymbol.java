//
// Copyright (c) 2020, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   23 Nov 2020  Matthew Giannini   Creation
//
package org.projecthaystack;

/**
 * HSymbol is a name to a def in the meta-model namespace.
 */
public class HSymbol extends HVal
{
  /**
   * Create a symbol from a string.
   * @param str The symbol string.
   * @return the symbol.
   */
  public static HSymbol make(final String str)
  {
    if (str.isEmpty()) throw new ParseException("Empty str");
    if (!Character.isLowerCase(str.charAt(0))) throw new ParseException("Invalid start char: " + str);
    int colon = -1;
    int dot = -1;
    int dash = -1;
    for (int i = 0; i< str.length(); ++i)
    {
      int c = str.charAt(i);
      if (c == ':') { if (colon >= 0) throw new ParseException("Too many colons: " + str); colon = i; }
      else if (c == '.') { if (dot >= 0) throw new ParseException("Too many dots: " + str); dot = i; }
      else if (c == '-') { dash = i; }
      else if (!isTagChar(c)) { throw new ParseException("Invalid char at pos: " + i + ": " + str); }
    }
    if (dot > 0) throw new ParseException("Compose symbols deprecated: " + str);
    if (colon > 0) return new HSymbol(str, str.substring( colon+1));
    return new HSymbol(str);
  }

  private static boolean isTagChar(final int c)
  {
    return Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';
  }

  private HSymbol(final String str)
  {
    this(str, str);
  }

  private HSymbol(final String str, final String name)
  {
    this.str = str;
    this.name = name;
  }

  /** Internal representation */
  private final String str;

  private final String name;

  /**
   *
   * @return the simple name.
   */
  public String name() { return name; }

  @Override
  public String toZinc()
  {
    return '^' + str;
  }

  @Override
  public String toJson()
  {
    return "y:" + str;
  }

  @Override
  public int hashCode()
  {
    return str.hashCode();
  }

  @Override
  public boolean equals(Object that)
  {
    if (!(that instanceof HSymbol)) return false;
    final HSymbol x = (HSymbol)that;
    return this.str.equals(x.str);
  }

  @Override
  public String toString()
  {
    return str;
  }
}
