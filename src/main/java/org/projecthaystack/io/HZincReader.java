//
// Copyright (c) 2011, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   07 Jun 2011  Brian Frank  Creation
//   24 Sep 2012  Brian Frank  Repurpose HReader
//   07 Jun 2016  Matthew Giannini  Update for haystack 3.0
//
package org.projecthaystack.io;

import java.io.*;
import java.util.*;
import org.projecthaystack.*;

/**
 * HZincReader reads grids using the Zinc format.
 *
 * @see <a href='http://project-haystack.org/doc/Zinc'>Project Haystack</a>
 */
public class HZincReader extends HGridReader
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /** Read from UTF-8 input stream. */
  public HZincReader(InputStream in)
  {
    try
    {
      this.tokenizer = new HaystackTokenizer(new BufferedReader(new InputStreamReader(in, "UTF-8")));
      init();
    }
    catch (IOException e)
    {
      throw err("init failed", e);
    }
  }

  /** Read from in-memory string. */
  public HZincReader(String in)
  {
    this.tokenizer = new HaystackTokenizer(new StringReader(in));
    init();
  }

  private void init()
  {
    consume();
    consume();
  }

//////////////////////////////////////////////////////////////////////////
// Public
//////////////////////////////////////////////////////////////////////////

  /** Close underlying input stream */
  public void close()
  {
    tokenizer.close();
  }


  /** Read a value and auto-close the stream */
  public HVal readVal() { return readVal(true); }

  /** Read a value. Close the stream if close is true */
  public HVal readVal(boolean close)
  {
    try
    {
      HVal val = null;
      if (cur == HaystackToken.id && "ver".equals(curVal))
        val = parseGrid();
      else
        val = parseVal();
      verify(HaystackToken.eof);
      return val;
    }
    finally
    {
      if (close) this.close();
    }

  }

  /** Convenience for {@link #readVal} as Grid */
  public HGrid readGrid()
  {
    return (HGrid)readVal(true);
  }

  /** Read a list of grids separated by blank line from stream */
  public HGrid[] readGrids()
  {
    ArrayList acc = new ArrayList();
    while (cur == HaystackToken.id) acc.add(parseGrid());
    return (HGrid[])acc.toArray(new HGrid[acc.size()]);
  }

  /**
   * Read a set of tags as {@code name:val} pairs separated by space. The
   * tags may optionally be surrounded by '{' and '}'
   */
  public HDict readDict()
  {
    return parseDict();
  }

  /**
   * Read scalar value: Bool, Int, Str, Uri, etc
   *
   * @deprecated Will be removed in future release.
   */
  public HVal readScalar()
  {
    return parseVal();
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  private HVal parseVal()
  {
    // if it's an id
    if (cur == HaystackToken.id)
    {
      String id = (String)curVal;
      consume(HaystackToken.id);

      // check for coord or xstr
      if (cur == HaystackToken.lparen)
      {
        if (peek == HaystackToken.num)
          return parseCoord(id);
        else
          return parseXStr(id);
      }

      // check for keyword
      if ("T".equals(id))   return HBool.TRUE;
      if ("F".equals(id))   return HBool.FALSE;
      if ("N".equals(id))   return null;
      if ("M".equals(id))   return HMarker.VAL;
      if ("NA".equals(id))  return HNA.VAL;
      if ("R".equals(id))   return HRemove.VAL;
      if ("NaN".equals(id)) return HNum.NaN;
      if ("INF".equals(id)) return HNum.POS_INF;

      throw err("Unexpected identifier: " + id);
    }

    // literals
    if (cur.literal) return parseLiteral();

    // -INF
    if (cur == HaystackToken.minus && "INF".equals(peekVal))
    {
      consume(HaystackToken.minus);
      consume(HaystackToken.id);
      return HNum.NEG_INF;
    }

    // nested collections
    if (cur == HaystackToken.lbracket) return parseList();
    if (cur == HaystackToken.lbrace) return parseDict();
    if (cur == HaystackToken.lt2) return parseGrid();

    throw err("Unexpected token: " + curToStr());
  }

  private HCoord parseCoord(String id)
  {
    if (!"C".equals(id)) throw err("Expecting 'C' for coord, not " + id);
    consume(HaystackToken.lparen);
    HNum lat = consumeNum();
    consume(HaystackToken.comma);
    HNum lng = consumeNum();
    consume(HaystackToken.rparen);
    return HCoord.make(lat.val, lng.val);
  }

  private HVal parseXStr(String id)
  {
    if (!Character.isUpperCase(id.charAt(0)))
      throw err("Invalid XStr type: " + id);
    consume(HaystackToken.lparen);
    if (this.version < 3 && "Bin".equals(id)) return parseBinObsolete();
    String val = consumeStr();
    consume(HaystackToken.rparen);
    return HXStr.decode(id, val);
  }

  private HBin parseBinObsolete()
  {
    StringBuffer s = new StringBuffer();
    while (cur != HaystackToken.rparen && cur != HaystackToken.eof)
    {
      if (curVal == null) s.append(cur.dis);
      else s.append(curVal);
      consume();
    }
    consume(HaystackToken.rparen);
    return HBin.make(s.toString());
  }

  private HVal parseLiteral()
  {
    Object val = this.curVal;
    if (cur == HaystackToken.ref && peek == HaystackToken.str)
    {
      val = HRef.make(((HRef)val).val, ((HStr)peekVal).val);
      consume(HaystackToken.ref);
    }
    consume();
    return (HVal)val;
  }

  private HList parseList()
  {
    List arr = new ArrayList();
    consume(HaystackToken.lbracket);
    while (cur != HaystackToken.rbracket && cur != HaystackToken.eof)
    {
      HVal val = parseVal();
      arr.add(val);
      if (cur != HaystackToken.comma) break;
      consume(HaystackToken.comma);
    }
    consume(HaystackToken.rbracket);
    return HList.make(arr);
  }

  private HDict parseDict()
  {
    HDictBuilder db = new HDictBuilder();
    boolean braces = cur == HaystackToken.lbrace;
    if (braces) consume(HaystackToken.lbrace);
    while (cur == HaystackToken.id)
    {
      // tag name
      String id = consumeTagName();
      if (!Character.isLowerCase(id.charAt(0))) throw err("Invalid dict tag name: " + id);

      // tag value
      HVal val = HMarker.VAL;
      if (cur == HaystackToken.colon)
      {
        consume(HaystackToken.colon);
        val = parseVal();
      }
      db.add(id, val);
    }
    if (braces) consume(HaystackToken.rbrace);
    return db.toDict();
  }

  private HGrid parseGrid()
  {
    boolean nested = cur == HaystackToken.lt2;
    if (nested)
    {
      consume(HaystackToken.lt2);
      if (cur == HaystackToken.nl) consume(HaystackToken.nl);
    }

    // ver:"3.0"
    if (cur != HaystackToken.id || !"ver".equals(curVal))
      throw err("Expecting grid 'ver' identifier, not " + curToStr());
    consume();
    consume(HaystackToken.colon);
    this.version = checkVersion(consumeStr());

    // grid meta
    HGridBuilder gb = new HGridBuilder();
    if (cur == HaystackToken.id)
      gb.meta().add(parseDict());
    consume(HaystackToken.nl);

    // column definitions
    int numCols = 0;
    while (cur == HaystackToken.id)
    {
      ++numCols;
      String name = consumeTagName();
      HDict colMeta = HDict.EMPTY;
      if (cur == HaystackToken.id)
        colMeta = parseDict();
      gb.addCol(name).add(colMeta);
      if (cur != HaystackToken.comma) break;
      consume(HaystackToken.comma);
    }
    if (numCols == 0) throw err("No columns defined");
    consume(HaystackToken.nl);

    // grid rows
    while (true)
    {
      if (cur == HaystackToken.nl) break;
      if (cur == HaystackToken.eof) break;
      if (nested && cur == HaystackToken.gt2) break;

      // read cells
      HVal[] cells = new HVal[numCols];
      for (int i = 0; i < numCols; ++i)
      {
        if (cur == HaystackToken.comma || cur == HaystackToken.nl || cur == HaystackToken.eof)
          cells[i] = null;
        else
          cells[i] = parseVal();
        if (i+1 < numCols) consume(HaystackToken.comma);
      }
      gb.addRow(cells);

      // newline or end
      if (nested && cur == HaystackToken.gt2) break;
      if (cur == HaystackToken.eof) break;
      consume(HaystackToken.nl);
    }

    if (cur == HaystackToken.nl) consume(HaystackToken.nl);
    if (nested) consume(HaystackToken.gt2);
    return gb.toGrid();
  }

  private int checkVersion(String s)
  {
    if ("3.0".equals(s)) return 3;
    if ("2.0".equals(s)) return 2;
    throw err("Unsupported version " + s);
  }

//////////////////////////////////////////////////////////////////////////
// Token Reads
//////////////////////////////////////////////////////////////////////////

  private String consumeTagName()
  {
    verify(HaystackToken.id);
    String id = (String)curVal;
    if (id.isEmpty() || !Character.isLowerCase(id.charAt(0)))
      throw err("Invalid dict tag name: " + id);
    consume(HaystackToken.id);
    return id;
  }

  private HNum consumeNum()
  {
    verify(HaystackToken.num);
    HNum num = (HNum)curVal;
    consume(HaystackToken.num);
    return num;
  }

  private String consumeStr()
  {
    verify(HaystackToken.str);
    String val = ((HStr)curVal).val;
    consume(HaystackToken.str);
    return val;
  }

  private void verify(HaystackToken expected)
  {
    if (cur != expected) throw err("Expected " + expected + " not " + curToStr());
  }

  private String curToStr()
  {
    return curVal != null ? cur + " " + curVal : cur.toString();
  }

  private void consume() { consume(null); }
  private void consume(HaystackToken expected)
  {
    if (expected != null) verify(expected);

    cur = peek;
    curVal = peekVal;
    curLine = peekLine;

    peek = tokenizer.next();
    peekVal = tokenizer.val;
    peekLine = tokenizer.line;
  }

  private ParseException err(String msg) { return err(msg, null); }
  private ParseException err(String msg, Exception e) { return new ParseException(msg + " [line " + curLine + "]", e); }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private HaystackTokenizer tokenizer;

  private HaystackToken cur;
  private Object curVal;
  private int curLine;

  private HaystackToken peek;
  private Object peekVal;
  private int peekLine;

  private int version = 3;
  private boolean isTop = true;
}