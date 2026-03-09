//
// Copyright (c) 2024, Richard McElhinney
// Licensed under the Academic Free License version 3.0
//
// History:
//   09 Mar 2024  Richard McElhinney  Creation
//
package org.projecthaystack.io;

import java.io.*;
import java.util.*;
import org.projecthaystack.*;

/**
 * HHaysonReader parses grids and scalar values from Hayson (JSON) format.
 *
 * @see <a href='https://project-haystack.org/doc/docHaystack/Json'>Project Haystack | JSON</a>
 */
public class HHaysonReader extends HGridReader
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /** Read from UTF-8 input stream */
  public HHaysonReader(InputStream in)
  {
    try
    {
      this.in = new InputStreamReader(in, "UTF-8");
      consume();
    }
    catch (IOException e) { throw new RuntimeException(e); }
  }

  /** Read from string (for tests) */
  public HHaysonReader(String in)
  {
    this.in = new StringReader(in);
    consume();
  }

//////////////////////////////////////////////////////////////////////////
// Public API
//////////////////////////////////////////////////////////////////////////

  /** Read a single value and close the stream */
  public HVal readVal()
  {
    try { return convertVal(parseJsonVal()); }
    finally { close(); }
  }

  /** Read a grid and close the stream */
  public HGrid readGrid()
  {
    try
    {
      Object json = parseJsonVal();
      if (!(json instanceof Map)) throw err("Expected JSON object for grid");
      HVal val = convertObject((Map)json);
      if (!(val instanceof HGrid)) throw err("Expected grid _kind");
      return (HGrid)val;
    }
    finally { close(); }
  }

  /** Read a dict and close the stream */
  public HDict readDict()
  {
    try
    {
      Object json = parseJsonVal();
      if (!(json instanceof Map)) throw err("Expected JSON object for dict");
      return convertDict((Map)json);
    }
    finally { close(); }
  }

  /** Close the underlying reader */
  public void close()
  {
    try { in.close(); } catch (IOException e) { throw new RuntimeException(e); }
  }

//////////////////////////////////////////////////////////////////////////
// Layer 1: Raw JSON Parser
//////////////////////////////////////////////////////////////////////////

  private Object parseJsonVal()
  {
    skipWhitespace();
    if (cur == '"')  return parseJsonString();
    if (cur == '{')  return parseJsonObject();
    if (cur == '[')  return parseJsonArray();
    if (cur == 't')  { consumeKeyword("true");  return Boolean.TRUE; }
    if (cur == 'f')  { consumeKeyword("false"); return Boolean.FALSE; }
    if (cur == 'n')  { consumeKeyword("null");  return null; }
    if (cur == '-' || Character.isDigit(cur)) return parseJsonNumber();
    throw err("Unexpected char: '" + (char)cur + "'");
  }

  private Map parseJsonObject()
  {
    Map map = new LinkedHashMap();
    consumeChar('{');
    skipWhitespace();
    if (cur == '}') { consumeChar('}'); return map; }
    while (true)
    {
      skipWhitespace();
      String key = parseJsonString();
      skipWhitespace();
      consumeChar(':');
      Object val = parseJsonVal();
      map.put(key, val);
      skipWhitespace();
      if (cur == '}') { consumeChar('}'); break; }
      consumeChar(',');
    }
    return map;
  }

  private List parseJsonArray()
  {
    List list = new ArrayList();
    consumeChar('[');
    skipWhitespace();
    if (cur == ']') { consumeChar(']'); return list; }
    while (true)
    {
      list.add(parseJsonVal());
      skipWhitespace();
      if (cur == ']') { consumeChar(']'); break; }
      consumeChar(',');
    }
    return list;
  }

  private String parseJsonString()
  {
    consumeChar('"');
    StringBuffer sb = new StringBuffer();
    while (cur != '"')
    {
      if (cur < 0) throw err("Unexpected end of string");
      if (cur == '\\')
      {
        consume();
        switch (cur)
        {
          case '"':  sb.append('"');  break;
          case '\\': sb.append('\\'); break;
          case '/':  sb.append('/');  break;
          case 'n':  sb.append('\n'); break;
          case 'r':  sb.append('\r'); break;
          case 't':  sb.append('\t'); break;
          case 'b':  sb.append('\b'); break;
          case 'f':  sb.append('\f'); break;
          case 'u':
            int code = 0;
            for (int i = 0; i < 4; i++)
            {
              consume();
              code = (code << 4) | hexDigit(cur);
            }
            sb.append((char)code);
            break;
          default: throw err("Invalid escape: \\" + (char)cur);
        }
        consume();
      }
      else
      {
        sb.append((char)cur);
        consume();
      }
    }
    consumeChar('"');
    return sb.toString();
  }

  private Double parseJsonNumber()
  {
    StringBuffer sb = new StringBuffer();
    if (cur == '-') { sb.append('-'); consume(); }
    while (Character.isDigit(cur)) { sb.append((char)cur); consume(); }
    if (cur == '.')
    {
      sb.append('.'); consume();
      while (Character.isDigit(cur)) { sb.append((char)cur); consume(); }
    }
    if (cur == 'e' || cur == 'E')
    {
      sb.append((char)cur); consume();
      if (cur == '+' || cur == '-') { sb.append((char)cur); consume(); }
      while (Character.isDigit(cur)) { sb.append((char)cur); consume(); }
    }
    return Double.valueOf(sb.toString());
  }

  private int hexDigit(int c)
  {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    throw err("Invalid hex digit: " + (char)c);
  }

  private void consumeKeyword(String kw)
  {
    for (int i = 0; i < kw.length(); i++)
    {
      if (cur != kw.charAt(i)) throw err("Unexpected char parsing '" + kw + "'");
      consume();
    }
  }

  private void consumeChar(int expected)
  {
    if (cur != expected)
      throw err("Expected '" + (char)expected + "' but got '" + (char)cur + "'");
    consume();
  }

  private void skipWhitespace()
  {
    while (cur == ' ' || cur == '\t' || cur == '\n' || cur == '\r')
      consume();
  }

  private void consume()
  {
    try { cur = in.read(); }
    catch (IOException e) { throw new RuntimeException(e); }
  }

  private RuntimeException err(String msg)
  {
    return new RuntimeException("HHaysonReader: " + msg);
  }

//////////////////////////////////////////////////////////////////////////
// Layer 2: Haystack Converter
//////////////////////////////////////////////////////////////////////////

  private HVal convertVal(Object json)
  {
    if (json == null)              return null;
    if (json instanceof Boolean)   return ((Boolean)json) ? HBool.TRUE : HBool.FALSE;
    if (json instanceof Double)    return HNum.make((Double)json);
    if (json instanceof String)    return HStr.make((String)json);
    if (json instanceof List)      return convertList((List)json);
    if (json instanceof Map)       return convertObject((Map)json);
    throw err("Unexpected JSON type: " + json.getClass());
  }

  private HVal convertObject(Map obj)
  {
    String kind = (String)obj.get("_kind");
    if (kind == null) return convertDict(obj);
    if (kind.equals("number"))   return convertNumber(obj);
    if (kind.equals("marker"))   return HMarker.VAL;
    if (kind.equals("remove"))   return HRemove.VAL;
    if (kind.equals("na"))       return HNA.VAL;
    if (kind.equals("ref"))      return convertRef(obj);
    if (kind.equals("date"))     return HDate.make((String)obj.get("val"));
    if (kind.equals("time"))     return HTime.make((String)obj.get("val"));
    if (kind.equals("dateTime")) return convertDateTime(obj);
    if (kind.equals("uri"))      return HUri.make((String)obj.get("val"));
    if (kind.equals("symbol"))   return HSymbol.make((String)obj.get("val"));
    if (kind.equals("coord"))    return convertCoord(obj);
    if (kind.equals("xstr"))     return HXStr.decode((String)obj.get("type"), (String)obj.get("val"));
    if (kind.equals("grid"))     return convertGrid(obj);
    throw err("Unknown _kind: " + kind);
  }

  private HNum convertNumber(Map obj)
  {
    Object valObj = obj.get("val");
    String unit   = (String)obj.get("unit");
    double val;
    if (valObj instanceof String)
    {
      String s = (String)valObj;
      if      (s.equals("NaN"))  val = Double.NaN;
      else if (s.equals("INF"))  val = Double.POSITIVE_INFINITY;
      else if (s.equals("-INF")) val = Double.NEGATIVE_INFINITY;
      else                       val = Double.parseDouble(s);
    }
    else
    {
      val = ((Double)valObj).doubleValue();
    }
    return unit != null ? HNum.make(val, unit) : HNum.make(val);
  }

  private HRef convertRef(Map obj)
  {
    String val = (String)obj.get("val");
    String dis  = (String)obj.get("dis");
    String id   = val.startsWith("@") ? val.substring(1) : val;
    return dis != null ? HRef.make(id, dis) : HRef.make(id);
  }

  private HDateTime convertDateTime(Map obj)
  {
    String val = (String)obj.get("val");
    String tz   = (String)obj.get("tz");
    if (tz != null) return HDateTime.make(val + " " + tz);
    return HDateTime.make(val);
  }

  private HCoord convertCoord(Map obj)
  {
    double lat = ((Double)obj.get("lat")).doubleValue();
    double lng = ((Double)obj.get("lng")).doubleValue();
    return HCoord.make(lat, lng);
  }

  private HList convertList(List arr)
  {
    HVal[] vals = new HVal[arr.size()];
    for (int i = 0; i < arr.size(); i++)
      vals[i] = convertVal(arr.get(i));
    return HList.make(vals);
  }

  private HDict convertDict(Map obj)
  {
    HDictBuilder db = new HDictBuilder();
    Iterator it = obj.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String key = (String)entry.getKey();
      if (key.equals("_kind")) continue;
      HVal val = convertVal(entry.getValue());
      if (val != null) db.add(key, val);
    }
    return db.toDict();
  }

  private HGrid convertGrid(Map obj)
  {
    HGridBuilder gb = new HGridBuilder();

    // meta
    Map meta = (Map)obj.get("meta");
    if (meta != null)
    {
      Iterator it = meta.entrySet().iterator();
      while (it.hasNext())
      {
        Map.Entry entry = (Map.Entry)it.next();
        String key = (String)entry.getKey();
        HVal val = convertVal(entry.getValue());
        if (val != null) gb.meta().add(key, val);
      }
    }

    // cols
    List cols = (List)obj.get("cols");
    if (cols != null)
    {
      for (int i = 0; i < cols.size(); i++)
      {
        Map col = (Map)cols.get(i);
        String name = (String)col.get("name");
        HDictBuilder colMeta = gb.addCol(name);
        Map colMetaJson = (Map)col.get("meta");
        if (colMetaJson != null)
        {
          Iterator it = colMetaJson.entrySet().iterator();
          while (it.hasNext())
          {
            Map.Entry entry = (Map.Entry)it.next();
            HVal val = convertVal(entry.getValue());
            if (val != null) colMeta.add((String)entry.getKey(), val);
          }
        }
      }
    }

    // rows
    List rows = (List)obj.get("rows");
    if (rows != null && cols != null)
    {
      for (int ri = 0; ri < rows.size(); ri++)
      {
        Map row = (Map)rows.get(ri);
        HVal[] cells = new HVal[cols.size()];
        for (int ci = 0; ci < cols.size(); ci++)
        {
          String colName = (String)((Map)cols.get(ci)).get("name");
          Object cellJson = row.get(colName);
          cells[ci] = cellJson == null ? null : convertVal(cellJson);
        }
        gb.addRow(cells);
      }
    }

    return gb.toGrid();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private Reader in;
  private int cur;
}
