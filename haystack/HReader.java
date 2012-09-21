//
// Copyright (c) 2011, SkyFoundry, LLC
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 Jun 2011  Brian Frank  Creation
//
package haystack;

import java.io.*;
import java.util.*;

/**
 * HReader is used to parse tags and scalar values from an input stream.
 */
public class HReader
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /** Read from UTF-8 input stream. */
  public HReader(InputStream in)
  {
    try
    {
      this.in = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      init();
    }
    catch (IOException e)
    {
      throw err(e);
    }
  }

  /** Read from in-memory string. */
  public HReader(String in)
  {
    this.in = new StringReader(in);
    init();
  }

  private void init()
  {
    consume();
    consume();
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  /** Close underlying input stream */
  public void close()
  {
    try { in.close(); } catch (IOException e) { e.printStackTrace(); }
  }

  /** Read dict and check that we read whole stream. */
  HDict readDictEos()
  {
    HDict dict = readDict();
    if (cur >= 0) throw errChar("Expected end of stream");
    return dict;
  }

  /** Read a set of name/value tags. */
  public HDict readDict()
  {
    // emtpy
    if (!isIdStart(cur)) return HDict.EMPTY;

    // parse pairs
    HDictBuilder b = new HDictBuilder();
    while (true)
    {
      // name
      String name = readId();

      // marker or :val
      HVal val = HMarker.VAL;
      skipSpace();
      if (cur == ':')
      {
        consume();
        skipSpace();
        val = readVal();
        skipSpace();
      }
      b.add(name, val);

      // if comma keep chugging otherwise done
      if (cur != ',') break;
      consume();
      skipSpace();
    }
    return b.toDict();
  }

  private String readId()
  {
    if (!isIdStart(cur)) throw errChar("Invalid name start char");
    StringBuffer s = new StringBuffer();
    while (isId(cur)) { s.append((char)cur); consume(); }
    return s.toString();
  }

//////////////////////////////////////////////////////////////////////////
// HVals
//////////////////////////////////////////////////////////////////////////

  /** Read single scalar value and check that we read whole stream. */
  HVal readValEos()
  {
    HVal val = readVal();
    if (cur >= 0) throw errChar("Expected end of stream");
    return val;
  }

  /** Read a single scalar value from the stream. */
  public HVal readVal()
  {
    if (isDigit(cur)) return readNumVal();
    if (isAlpha(cur)) return readWordVal();
    switch (cur)
    {
      case '@': return readRefVal();
      case '"': return readStrVal();
      case '`': return readUriVal();
      case '-':
        if (peek == 'I') return readWordVal();
        return readNumVal();
      default:  throw errChar("Unexpected char for start of value");
    }
  }

  private HVal readWordVal()
  {
    // read into string
    StringBuffer s = new StringBuffer();
    do { s.append((char)cur); consume(); } while (isAlpha(cur));
    String word = s.toString();

    // match identifier
    if (word.equals("true")) return HBool.TRUE;
    if (word.equals("false")) return HBool.FALSE;
    if (word.equals("NaN")) return HNum.NaN;
    if (word.equals("INF")) return HNum.POS_INF;
    if (word.equals("-INF")) return HNum.NEG_INF;
    throw err("Unknown value identifier: " + word);
  }

  private HVal readNumVal()
  {
    // parse numeric part
    StringBuffer s = new StringBuffer();
    s.append((char)cur);
    consume();
    while (isDigit(cur) || cur == '.')
    {
      s.append((char)cur);
      consume();
      if (cur == 'e' || cur == 'E')
      {
        if (peek == '-' || isDigit(peek))
        {
          s.append((char)cur); consume();
          s.append((char)cur); consume();
        }
      }
    }
    double val = Double.parseDouble(s.toString());

    // HDate - check for dash
    HDate date = null;
    HTime time = null;
    int hour = -1;
    if (cur == '-')
    {
      int year;
      try { year = Integer.parseInt(s.toString()); }
      catch (Exception e) { throw err("Invalid year for date value: " + s); }
      consume(); // dash
      int month = readTwoDigits("Invalid digit for month in date value");
      if (cur != '-') throw errChar("Expected '-' for date value");
      consume();
      int day = readTwoDigits("Invalid digit for day in date value");
      date = HDate.make(year, month, day);

      // check for 'T' date time
      if (cur != 'T') return date;

      // parse next two digits and drop down to HTime parsing
      consume();
      hour = readTwoDigits("Invalid digit for hour in date time value");
    }

    // HTime - check for colon
    if (cur == ':')
    {
      // hour (may have been parsed already in date time)
      if (hour < 0)
      {
        if (s.length() != 2) { throw err("Hour must be two digits for time value: " + s); }
        try { hour = Integer.parseInt(s.toString()); }
        catch (Exception e) { throw err("Invalid hour for time value: " + s); }
      }
      consume(); // colon
      int min = readTwoDigits("Invalid digit for minute in time value");
      if (cur != ':') throw errChar("Expected ':' for time value");
      consume();
      int sec = readTwoDigits("Invalid digit for seconds in time value");
      int ms = 0;
      if (cur == '.')
      {
        consume();
        int places = 0;
        while (isDigit(cur))
        {
          ms = (ms * 10) + (cur - '0');
          consume();
          places++;
        }
        switch (places)
        {
          case 1: ms *= 100; break;
          case 2: ms *= 10; break;
          case 3: break;
          default: throw err("Too many digits for milliseconds in time value");
        }
      }
      time = HTime.make(hour, min, sec, ms);
      if (date == null) return time;
    }

    // HDateTime (if we have date and time)
    if (date != null)
    {
      // timezone offset "Z" or "-/+hh:mm"
      int tzOffset = 0;
      if (cur == 'Z') consume();
      else
      {
        boolean neg = cur == '-';
        if (cur != '-' && cur != '+') throw errChar("Expected -/+ for timezone offset");
        consume();
        int tzHours = readTwoDigits("Invalid digit for timezone offset");
        if (cur != ':') throw errChar("Expected colon for timezone offset");
        consume();
        int tzMins  = readTwoDigits("Invalid digit for timezone offset");
        tzOffset = (tzHours * 3600) + (tzMins * 60);
        if (neg) tzOffset = -tzOffset;
      }

      // timezone name
      if (cur != ' ') throw errChar("Expected space between timezone offset and name");
      consume();
      StringBuffer tz = new StringBuffer();
      if (!isTz(cur)) throw errChar("Expected timezone name");
      while (isTz(cur)) { tz.append((char)cur); consume(); }

      return HDateTime.make(date, time, HTimeZone.make(tz.toString()), tzOffset);
    }

    // if we have unit, parse that
    String unit = null;
    if (isUnit(cur))
    {
      s = new StringBuffer();
      while (isUnit(cur)) { s.append((char)cur); consume(); }
      unit = s.toString();
    }

    return HNum.make(val, unit);
  }

  private int readTwoDigits(String errMsg)
  {
    if (!isDigit(cur)) throw errChar(errMsg);
    int tens = (cur - '0') * 10;
    consume();
    if (!isDigit(cur)) throw errChar(errMsg);
    int val = tens + (cur - '0');
    consume();
    return val;
  }

  private int readTimeMs()
  {
    int ms = 0;
    return ms;
  }

  private HVal readRefVal()
  {
    consume(); // opening @
    StringBuffer s = new StringBuffer();
    while (HRef.isIdChar(cur))
    {
      if (cur < 0) throw err("Unexpected end of ref literal");
      if (cur == '\n' || cur == '\r') throw err("Unexpected newline in ref literal");
      s.append((char)cur);
      consume();
    }
    skipSpace();

    String dis = null;
    if (cur == '"') dis = readStrLiteral();

    return HRef.make(s.toString(), dis);
  }

  private HVal readStrVal()
  {
    return HStr.make(readStrLiteral());
  }

  private String readStrLiteral()
  {
    consume(); // opening quote
    StringBuffer s = new StringBuffer();
    while (cur != '"')
    {
      if (cur < 0) throw err("Unexpected end of str literal");
      if (cur == '\n' || cur == '\r') throw err("Unexpected newline in str literal");
      if (cur == '\\')
      {
        s.append((char)readEscChar());
      }
      else
      {
        s.append((char)cur);
        consume();
      }
    }
    consume(); // closing quote
    return s.toString();
  }

  private int readEscChar()
  {
    consume();  // back slash

    // check basics
    switch (cur)
    {
      case 'b':   consume(); return '\b';
      case 'f':   consume(); return '\f';
      case 'n':   consume(); return '\n';
      case 'r':   consume(); return '\r';
      case 't':   consume(); return '\t';
      case '"':   consume(); return '"';
      case '$':   consume(); return '$';
      case '\'':  consume(); return '\'';
      case '`':   consume(); return '`';
      case '\\':  consume(); return '\\';
    }

    // check for uxxxx
    if (cur == 'u')
    {
      consume();
      int n3 = toNibble(cur); consume();
      int n2 = toNibble(cur); consume();
      int n1 = toNibble(cur); consume();
      int n0 = toNibble(cur); consume();
      return (n3 << 12) | (n2 << 8) | (n1 << 4) | (n0);
    }

    throw err("Invalid escape sequence");
  }

  private int toNibble(int c)
  {
    if ('0' <= c && c <= '9') return c - '0';
    if ('a' <= c && c <= 'f') return c - 'a' + 10;
    if ('A' <= c && c <= 'F') return c - 'A' + 10;
    throw errChar("Invalid hex char");
  }

  private HVal readUriVal()
  {
    consume(); // opening backtick
    StringBuffer s = new StringBuffer();
    while (cur != '`')
    {
      if (cur < 0) throw err("Unexpected end of uri literal");
      if (cur == '\n' || cur == '\r') throw err("Unexpected newline in uri literal");
      s.append((char)cur);
      consume();
    }
    consume(); // opening backtick
    return HUri.make(s.toString());
  }

//////////////////////////////////////////////////////////////////////////
// HQuery
//////////////////////////////////////////////////////////////////////////

  /** Read a HQuery from stream */
  HQuery readQueryEos()
  {
    skipSpace();
    HQuery q = readQueryOr();
    skipSpace();
    if (cur >= 0) throw errChar("Expected end of stream");
    return q;
  }

  private HQuery readQueryOr()
  {
    HQuery q = readQueryAnd();
    skipSpace();
    if (cur != 'o') return q;
    if (!readId().equals("or")) throw err("Expecting 'or' keyword");
    skipSpace();
    return q.or(readQueryOr());
  }

  private HQuery readQueryAnd()
  {
    HQuery q = readQueryAtomic();
    skipSpace();
    if (cur != 'a') return q;
    if (!readId().equals("and")) throw err("Expecting 'and' keyword");
    skipSpace();
    return q.and(readQueryAnd());
  }

  private HQuery readQueryAtomic()
  {
    skipSpace();
    if (cur == '(') return readQueryParens();

    HQuery.Path path = readQueryPath();
    skipSpace();

    if (path.toString().equals("not")) { return new HQuery.Missing(readQueryPath()); }

    if (cur == '=' && peek == '=') { consumeCmp(); return new HQuery.Eq(path, readVal()); }
    if (cur == '!' && peek == '=') { consumeCmp(); return new HQuery.Ne(path, readVal()); }
    if (cur == '<' && peek == '=') { consumeCmp(); return new HQuery.Le(path, readVal()); }
    if (cur == '>' && peek == '=') { consumeCmp(); return new HQuery.Ge(path, readVal()); }
    if (cur == '<')                { consumeCmp(); return new HQuery.Lt(path, readVal()); }
    if (cur == '>')                { consumeCmp(); return new HQuery.Gt(path, readVal()); }

    return new HQuery.Has(path);
  }

  private HQuery readQueryParens()
  {
    consume();
    skipSpace();
    HQuery q = readQueryOr();
    if (cur != ')') throw err("Expecting ')'");
    consume();
    return q;
  }

  private void consumeCmp()
  {
    consume();
    if (cur == '=') consume();
    skipSpace();
  }

  private HQuery.Path readQueryPath()
  {
    // read first tag name
    String id = readId();

    // if not pathed, optimize for common case
    if (cur != '-' || peek != '>') return new HQuery.Path1(id);

    // parse path
    StringBuffer s = new StringBuffer().append(id);
    ArrayList acc = new ArrayList();
    acc.add(id);
    while (cur == '-' || peek == '>')
    {
      consume();
      consume();
      id = readId();
      acc.add(id);
      s.append('-').append('>').append(id);
    }
    return new HQuery.PathN(s.toString(), (String[])acc.toArray(new String[acc.size()]));
  }

//////////////////////////////////////////////////////////////////////////
// Char Reads
//////////////////////////////////////////////////////////////////////////

  private ParseException errChar(String msg)
  {
    if (cur < 0) msg += " (end of stream)";
    else
    {
      msg += " (char=0x" + Integer.toHexString(cur);
      if (cur >= ' ') msg += " '" + (char)cur + "'";
      msg += ")";
    }
    return err(msg, null);
  }

  private ParseException err(String msg) { return err(msg, null); }
  private ParseException err(Throwable ex) { return err(ex.toString(), ex); }
  private ParseException err(String msg, Throwable ex)
  {
    return new ParseException(msg + " [Line " + lineNum + "]", ex);
  }

  private void skipSpace()
  {
    while (cur == ' ' || cur == '\t') consume();
  }

  int cur() { return cur; }

  void consume()
  {
    try
    {
      cur  = peek;
      peek = in.read();
      if (cur == '\n') lineNum++;
    }
    catch (IOException e)
    {
      throw err(e);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Char Types
//////////////////////////////////////////////////////////////////////////

  private static boolean isDigit(int c)   { return c > 0 && c < 128 && (charTypes[c] & DIGIT) != 0; }
  private static boolean isAlpha(int c)   { return c > 0 && c < 128 && (charTypes[c] & ALPHA) != 0; }
  private static boolean isUnit(int c)    { return c > 0 && (c >= 128 || (charTypes[c] & UNIT) != 0); }
  private static boolean isTz(int c)      { return c > 0 && c < 128 && (charTypes[c] & TZ) != 0; }
  private static boolean isIdStart(int c) { return c > 0 && c < 128 && (charTypes[c] & ID_START) != 0; }
  private static boolean isId(int c)      { return c > 0 && c < 128 && (charTypes[c] & ID) != 0; }

  private static final byte[] charTypes = new byte[128];
  private static final int DIGIT    = 0x01;
  private static final int ALPHA_LO = 0x02;
  private static final int ALPHA_UP = 0x04;
  private static final int ALPHA    = ALPHA_UP | ALPHA_LO;
  private static final int UNIT     = 0x08;
  private static final int TZ       = 0x10;
  private static final int ID_START = 0x20;
  private static final int ID       = 0x40;
  static
  {
    for (int i='0'; i<='9'; ++i) charTypes[i] = (DIGIT | TZ | ID);
    for (int i='a'; i<='z'; ++i) charTypes[i] = (ALPHA_LO | UNIT | TZ | ID_START | ID);
    for (int i='A'; i<='Z'; ++i) charTypes[i] = (ALPHA_UP | UNIT | TZ | ID);
    charTypes['%'] = UNIT;
    charTypes['_'] = UNIT | TZ | ID;
    charTypes['/'] = UNIT;
    charTypes['$'] = UNIT;
    charTypes['-'] = TZ;
    charTypes['+'] = TZ;
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private Reader in;
  private int cur;
  private int peek;
  private int lineNum = 1;
}