//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   06 June 2016  Matthew Giannini   Creation
//
package org.projecthaystack.io;

import org.projecthaystack.*;

import java.io.IOException;
import java.io.Reader;

/**
 * Stream based tokenizer for Haystack formats such as Zinc and Filters
 */
public class HaystackTokenizer
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  public HaystackTokenizer(Reader in)
  {
    this.in = in;
    this.tok = HaystackToken.eof;
    consume();
    consume();
  }

  boolean close()
  {
    try
    {
      in.close();
      return true;
    }
    catch (IOException e)
    {
      return false;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Tokenizing
//////////////////////////////////////////////////////////////////////////

  public HaystackToken next()
  {
    // reset
    val = null;

    // skip non-meaningful whitespace and comments
    int startLine = line;
    while (true)
    {
      // treat space, tab, non-breaking space as whitespace
      if (cur == ' ' || cur == '\t' || cur == 0xa0) { consume(); continue; }

      // comments
      if (cur == '/')
      {
        if (peek == '/') { skipCommentsSL(); continue; }
        if (peek == '*') { skipCommentsML(); continue; }
      }

      break;
    }

    // newlines
    if (cur == '\n' || cur == '\r')
    {
      if (cur == '\r' && peek == '\n') consume('\r');
      consume();
      ++line;
      return tok = HaystackToken.nl;
    }

    // handle various starting chars
    if (isIdStart(cur)) return tok = id();
    if (cur == '"')     return tok = str();
    if (cur == '@')     return tok = ref();
    if (isDigit(cur))   return tok = num();
    if (cur == '`')     return tok = uri();
    if (cur == '-' && isDigit(peek)) return tok = num();

    return tok = symbol();
  }

//////////////////////////////////////////////////////////////////////////
// Token Productions
//////////////////////////////////////////////////////////////////////////

  private HaystackToken id()
  {
    StringBuffer s = new StringBuffer();
    while (isIdPart(cur))
    {
      s.append((char)cur);
      consume();
    }
    this.val = s.toString();
    return HaystackToken.id;
  }

  private static boolean isIdStart(int cur)
  {
    if ('a' <= cur && cur <= 'z') return true;
    if ('A' <= cur && cur <= 'Z') return true;
    return false;
  }

  private static boolean isIdPart(int cur)
  {
    if (isIdStart(cur)) return true;
    if (isDigit(cur)) return true;
    if (cur == '_') return true;
    return false;
  }

  private static boolean isDigit(int cur)
  {
    return '0' <= cur && cur <= '9';
  }

  private HaystackToken num()
  {
    // hex number (no unit allowed)
    boolean isHex = cur == '0' && peek == 'x';
    if (isHex)
    {
      consume('0');
      consume('x');
      StringBuffer s = new StringBuffer();
      while (true)
      {
        if (isHex(cur)) { s.append((char)cur); consume(); continue; }
        if (cur == '_') { consume(); continue; }
        break;
      }
      this.val = HNum.make(Long.parseLong(s.toString(), 16));
      return HaystackToken.num;
    }

    // consume all things that might be part of this number token
    StringBuffer s = new StringBuffer().append((char)cur);
    consume();
    int colons = 0;
    int dashes = 0;
    int unitIndex = 0;
    boolean exp = false;
    while (true)
    {
      if (!Character.isDigit(cur))
      {
        if (exp && (cur == '+' || cur == '-')) { }
        else if (cur == '-') { ++dashes; }
        else if (cur == ':' && Character.isDigit(peek)) { ++colons; }
        else if ((exp || colons >= 1) && cur == '+') { }
        else if (cur == '.') { if (!Character.isDigit(peek)) break; }
        else if ((cur == 'e' || cur == 'E') && (peek == '-' || peek == '+' || Character.isDigit(peek))) { exp = true; }
        else if (Character.isLetter(cur) || cur == '%' || cur == '$' || cur == '/' || cur > 128) { if (unitIndex == 0) unitIndex = s.length(); }
        else if (cur == '_') { if (unitIndex == 0 && Character.isDigit(peek)) { consume(); continue; }  else { if (unitIndex == 0) unitIndex = s.length(); } }
        else { break; }
      }
      s.append((char)cur);
      consume();
    }

    if (dashes == 2 && colons == 0) return date(s.toString());
    if (dashes == 0 && colons >= 1) return time(s, colons == 1);
    if (dashes >= 2) return dateTime(s);
    return number(s.toString(), unitIndex);
  }

  private static boolean isHex(int cur)
  {
    cur = Character.toLowerCase(cur);
    if ('a' <= cur && cur <= 'f') return true;
    if (isDigit(cur)) return true;
    return false;
  }

  private HaystackToken date(String s)
  {
    try
    {
      this.val = HDate.make(s);
      return HaystackToken.date;
    }
    catch (ParseException e)
    {
      throw err(e.getMessage());
    }
  }

  /** we don't require hour to be two digits and we don't require seconds */
  private HaystackToken time(StringBuffer s, boolean addSeconds)
  {
    try
    {
      if (s.charAt(1) == ':') s.insert(0, '0');
      if (addSeconds) s.append(":00");
      this.val = HTime.make(s.toString());
      return HaystackToken.time;
    }
    catch (ParseException e)
    {
      throw err(e.getMessage());
    }
  }

  private HaystackToken dateTime(StringBuffer s)
  {
    // xxx timezone
    if (cur != ' ' || !Character.isUpperCase(peek))
    {
      if (s.charAt(s.length()-1) == 'Z') s.append(" UTC");
      else throw err("Expecting timezone");
    }
    else
    {
      consume();
      s.append(' ');
      while (isIdPart(cur)) { s.append((char)cur); consume(); }

      // handle GMT+xx or GMT-xx
      if ((cur == '+' || cur == '-') && s.toString().endsWith("GMT"))
      {
        s.append((char)cur); consume();
        while (isDigit(cur)) { s.append((char)cur); consume(); }
      }
    }
    try
    {
      this.val = HDateTime.make(s.toString());
      return HaystackToken.dateTime;
    }
    catch (ParseException e)
    {
      throw err(e.getMessage());
    }
  }

  private HaystackToken number(String s, int unitIndex)
  {
    try
    {
      if (unitIndex == 0)
      {
        this.val = HNum.make(Double.parseDouble(s));
      }
      else
      {
        String doubleStr = s.substring(0, unitIndex);
        String unitStr   = s.substring(unitIndex);
        this.val = HNum.make(Double.parseDouble(doubleStr), unitStr);
      }
    }
    catch (Exception e)
    {
      throw err("Invalid Number literal: " + s);
    }
    return HaystackToken.num;
  }

  private HaystackToken str()
  {
    consume('"');
    StringBuffer s = new StringBuffer();
    while (true)
    {
      if (cur == eof) throw err("Unexpected end of str");
      if (cur == '"') { consume('"'); break; }
      if (cur == '\\') { s.append(escape()); continue; }
      s.append((char)cur);
      consume();
    }
    this.val = HStr.make(s.toString());
    return HaystackToken.str;
  }

  private HaystackToken ref()
  {
    consume('@');
    StringBuffer s = new StringBuffer();
    while (true)
    {
      if (HRef.isIdChar((char)cur))
      {
        s.append((char)cur);
        consume();
      }
      else
      {
        break;
      }
    }
    this.val = HRef.make(s.toString(), null);
    return HaystackToken.ref;
  }

  private HaystackToken uri()
  {
    consume('`');
    StringBuffer s = new StringBuffer();
    while (true)
    {
      if (cur == '`') { consume('`'); break; }
      if (cur == eof || cur == '\n') throw err("Unexpected end of uri");
      if (cur == '\\')
      {
        switch (peek)
        {
          case ':':
          case '/':
          case '?':
          case '#':
          case '[':
          case ']':
          case '@':
          case '\\':
          case '&':
          case '=':
          case ';':
            s.append((char)cur);
            s.append((char)peek);
            consume();
            consume();
            break;
          default:
            s.append(escape());
        }
      }
      else
      {
        s.append((char)cur);
        consume();
      }
    }
    this.val = HUri.make(s.toString());
    return HaystackToken.uri;
  }

  private char escape()
  {
    consume('\\');
    switch (cur)
    {
      case 'b':  consume(); return '\b';
      case 'f':  consume(); return '\f';
      case 'n':  consume(); return '\n';
      case 'r':  consume(); return '\r';
      case 't':  consume(); return '\t';
      case '"':  consume(); return '"';
      case '$':  consume(); return '$';
      case '\'': consume(); return '\'';
      case '`':  consume(); return '`';
      case '\\': consume(); return '\\';
    }

    // check for uxxxx
    StringBuffer esc = new StringBuffer();
    if (cur == 'u')
    {
      consume('u');
      esc.append((char)cur); consume();
      esc.append((char)cur); consume();
      esc.append((char)cur); consume();
      esc.append((char)cur); consume();
      try
      {
        return (char) Integer.parseInt(esc.toString(), 16);
      }
      catch (NumberFormatException e)
      {
        throw new ParseException("Invalid unicode escape: " + esc.toString());
      }
    }
    throw err("Invalid escape sequence: " + (char)cur);
  }

  private HaystackToken symbol()
  {
    int c = cur;
    consume();
    switch(c)
    {
      case ',':
        return HaystackToken.comma;
      case ':':
        return HaystackToken.colon;
      case ';':
        return HaystackToken.semicolon;
      case '[':
        return HaystackToken.lbracket;
      case ']':
        return HaystackToken.rbracket;
      case '{':
        return HaystackToken.lbrace;
      case '}':
        return HaystackToken.rbrace;
      case '(':
        return HaystackToken.lparen;
      case ')':
        return HaystackToken.rparen;
      case '<':
        if (cur == '<') { consume('<'); return HaystackToken.lt2; }
        if (cur == '=') { consume('='); return HaystackToken.ltEq; }
        return HaystackToken.lt;
      case '>':
        if (cur == '>') { consume('>'); return HaystackToken.gt2; }
        if (cur == '=') { consume('='); return HaystackToken.gtEq; }
        return HaystackToken.gt;
      case '-':
        if (cur == '>') { consume('>'); return HaystackToken.arrow; }
        return HaystackToken.minus;
      case '=':
        if (cur == '=') { consume('='); return HaystackToken.eq; }
        return HaystackToken.assign;
      case '!':
        if (cur == '=') { consume('='); return HaystackToken.notEq; }
        return HaystackToken.bang;
      case '/':
        return HaystackToken.slash;
   }
    if (c == eof) return HaystackToken.eof;
    throw err("Unexpected symbol: '" + (char)c + "' (0x" + Integer.toHexString(c) + ")");
  }

//////////////////////////////////////////////////////////////////////////
// Comments
//////////////////////////////////////////////////////////////////////////

  private void skipCommentsSL()
  {
    consume('/');
    consume('/');
    while (true)
    {
      if (cur == '\n' || cur == eof) break;
      consume();
    }
  }

  private void skipCommentsML()
  {
    consume('/');
    consume('*');
    int depth = 1;
    while (true)
    {
      if (cur == '*' && peek == '/') { consume('*'); consume('/'); depth--; if (depth <= 0) break; }
      if (cur == '/' && peek == '*') { consume('/'); consume('*'); depth++; continue; }
      if (cur == '\n') ++line;
      if (cur == eof) throw err("Multi-line comment not closed");
      consume();
    }
  }

//////////////////////////////////////////////////////////////////////////
// Error Handling
//////////////////////////////////////////////////////////////////////////

  private ParseException err(String msg)
  {
    return new ParseException(msg + " [line " + line + "]");
  }

//////////////////////////////////////////////////////////////////////////
// Char
//////////////////////////////////////////////////////////////////////////

  private void consume(int expected)
  {
    if (cur != expected) throw err("Expected " + (char)expected);
    consume();
  }

  private void consume()
  {
    try
    {
      cur = peek;
      peek = in.read();
    }
    catch (IOException e)
    {
      cur  = eof;
      peek = eof;
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public HaystackToken tok; // current token type
  public Object val;        //token literal or identifier
  int line = 1;             // current line number

  private final Reader in;   // underlying stream
  private int cur;           // current char
  private int peek;          // next char
  private static final int eof = -1;
}
