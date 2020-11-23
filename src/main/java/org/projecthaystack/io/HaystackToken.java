//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 June 2016  Matthew Giannini   Creation
//
package org.projecthaystack.io;

/**
 * Created by mgiannini on 6/6/16.
 */
public class HaystackToken
{
  public static final HaystackToken eof = new HaystackToken("eof");

  public static final HaystackToken id = new HaystackToken("identifier");
  public static final HaystackToken num = new HaystackToken("Number", true);
  public static final HaystackToken str = new HaystackToken("Str", true);
  public static final HaystackToken ref = new HaystackToken("Ref", true);
  public static final HaystackToken symbol = new HaystackToken("Symbol", true);
  public static final HaystackToken uri = new HaystackToken("Uri", true);
  public static final HaystackToken date = new HaystackToken("Date", true);
  public static final HaystackToken time = new HaystackToken("Time", true);
  public static final HaystackToken dateTime = new HaystackToken("DateTime", true);

  public static final HaystackToken colon = new HaystackToken(":");
  public static final HaystackToken comma = new HaystackToken(",");
  public static final HaystackToken semicolon = new HaystackToken(";");
  public static final HaystackToken minus = new HaystackToken("-");
  public static final HaystackToken eq = new HaystackToken("==");
  public static final HaystackToken notEq = new HaystackToken("!=");
  public static final HaystackToken lt = new HaystackToken("<");
  public static final HaystackToken lt2 = new HaystackToken("<<");
  public static final HaystackToken ltEq = new HaystackToken("<=");
  public static final HaystackToken gt = new HaystackToken(">");
  public static final HaystackToken gt2 = new HaystackToken(">>");
  public static final HaystackToken gtEq = new HaystackToken(">=");
  public static final HaystackToken lbracket = new HaystackToken("[");
  public static final HaystackToken rbracket = new HaystackToken("]");
  public static final HaystackToken lbrace = new HaystackToken("{");
  public static final HaystackToken rbrace = new HaystackToken("}");
  public static final HaystackToken lparen = new HaystackToken("(");
  public static final HaystackToken rparen = new HaystackToken(")");
  public static final HaystackToken arrow = new HaystackToken("->");
  public static final HaystackToken slash = new HaystackToken("/");
  public static final HaystackToken assign = new HaystackToken("=");
  public static final HaystackToken bang = new HaystackToken("!");
  public static final HaystackToken nl = new HaystackToken("newline");

  public HaystackToken(String dis)
  {
    this(dis, false);
  }

  public HaystackToken(String dis, boolean literal)
  {
    this.dis = dis;
    this.literal = literal;
  }

  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaystackToken that = (HaystackToken) o;

    if (literal != that.literal) return false;
    return dis.equals(that.dis);

  }

  public int hashCode()
  {
    int result = dis.hashCode();
    result = 31 * result + (literal ? 1 : 0);
    return result;
  }

  public String toString()
  {
    return dis;
  }

  public final String dis;
  public final boolean literal;
}
