//
// Copyright (c) 2013, Richard McElhinney
// Licensed under the Academic Free License version 3.0
//
// History:
//   05 Jun 2024  Richard McElhinney  Creation
//
package org.projecthaystack.io;

import java.io.*;
import java.util.Iterator;
import java.util.Map.Entry;
import org.projecthaystack.*;

/**
 * HHaysonWriter is used to write grids in JSON format.  
 * It is a plain text format used for serialization of data.
 * Haystack v4.0 specifies the exact format of writing types 
 * using the Hayson format.
 *
 * @see <a href='https://project-haystack.org/doc/docHaystack/Json'> Project Haystack | JSON</a>
 */
public class HHaysonWriter extends HGridWriter
{
//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  /* Write using UTF-8 */
  public HHaysonWriter(OutputStream out)
  {
    try
    {
      this.out = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    } 
  }

  /** Write a grid to an in-memory string */
  public static String gridToString(HGrid grid)
  {
    StringWriter out = new StringWriter(grid.numCols() * grid.numRows() * 32);
    new HHaysonWriter(out).writeGrid(grid);
    return out.toString();
  }

  private HHaysonWriter(StringWriter out) { this.out = new PrintWriter(out); }

//////////////////////////////////////////////////////////////////////////
// HHaysonWriter
//////////////////////////////////////////////////////////////////////////  

  public static String writeVal(StringWriter out, HVal val)
  {
    HHaysonWriter hw = new HHaysonWriter(out);
    hw.writeVal(val);
    hw.flush();

    return out.toString();
  }

  /** Write any Haystack value */
  public HHaysonWriter writeVal(HVal val)
  {
    if      (val == null)          out.println("null");
    else if (val instanceof HList) writeList((HList) val);
    else if (val instanceof HDict) writeDict((HDict) val);
    else if (val instanceof HGrid) writeGrid((HGrid) val);
    else                           writeScalar((HVal) val);

    return this;
  }

  /* Write a grid in Hayson format */
  public void writeGrid(HGrid grid)
  {
    out.print("{\n");
    out.print("\"_kind\": \"grid\",\n");
    
    // meta
    HDict meta = grid.meta();
    String ver = meta.has("ver") ? meta.getStr("ver") : "4.0";
    out.print("\"meta\": {\"ver\":"+ver+"\"");
    writeDictTags(grid.meta(), false);
    out.print("},\n");

    // columns
    boolean firstCol = true;
    out.print("\"cols:\" [\n"); 
    for (int i = 0; i < grid.numCols(); i++)
    {
      if (firstCol) firstCol = false; else out.print(",\n");
      out.print("{");

      HCol col = grid.col(i);
      out.print("\"name\": " + col.name());
      if (!col.meta().isEmpty())
      {
        out.print(",");
        out.print("\"meta\": ");
        writeDict(col.meta());
      }
      out.print("}");
    }
    out.print("\n],\n");

    // rows
    boolean firstRow = true;
    int numRows = grid.numRows();
    out.print("\"rows\": [\n");
    for (int i = 0; i < numRows; i++)
    {
      if (firstRow) firstRow = false; else out.print(",\n");
      HRow row = grid.row(i);
      writeDict(row);
    }
    out.print("\n],\n");

    //grid end
    out.print("}\n");
    out.flush();
  }

  private HHaysonWriter writeDict(HDict dict)
  {
    out.print("{");
    writeDictTags(dict, true);
    out.print("}");

    return this;
  }

  private void writeDictTags(HDict dict, boolean first)
  {
    Iterator i = dict.iterator();
    while (i.hasNext())
    {
      if (first) first = false; else out.print(", ");
      Entry entry = (Entry) i.next();
      String name = (String) entry.getKey();
      HVal val = (HVal) entry.getValue();

      out.print("\"" + name + "\":");
      writeVal(val);
    }
  }

  private void writeList(HList list)
  {

  }

  private void writeScalar(HVal val)
  {
    if (val == null)                      out.print("null");
    else if (val instanceof HStr)         writeStr((HStr) val);
    else if (val instanceof HBool)        writeBool((HBool) val);
    else if (val instanceof HNum)         writeNum((HNum) val);
//    else if (val instanceof HNumber)      writeNumber((HNumber) val);
    else if (val instanceof HRef)         writeRef((HRef) val);
    else if (val instanceof HDate)        writeDate((HDate) val);
    else if (val instanceof HTime)        writeTime((HTime) val);
    else if (val instanceof HDateTime)    writeDateTime((HDateTime) val);
    else if (val instanceof HUri)         writeUri((HUri) val);
    else if (val instanceof HSymbol)      writeSymbol((HSymbol)val);
    else if (val instanceof HCoord)       writeCoord((HCoord) val);
    else if (val instanceof HXStr)        writeXStr((HXStr) val);
    else if (val == HMarker.VAL)          out.print("{\"_kind\":\"marker\"}");
    else if (val == HRemove.VAL)          out.print("{\"_kind\":\"remove\"}");
//    else if (val == HNA.val)              out.print("{\"_kind\":\"na\"}");
// TODO    else if (val instanceof HSpan)        writeScalar(XStr(val));
// TODO    else if (val instanceof HBin)         writeScalar(XStr(val));
    else throw new RuntimeException("Unrecognized scalar: ");
  }

  private void writeStr(HStr val)
  {
    out.print("\"" + val.toString() + "\"");
  }

  private void writeBool(HBool val)
  {
    out.print(val.toString());
  }

  private void writeNum(HNum val)
  {

  }

//  private void writeNumber(HNumber val)
//  {
//  }

  private void writeRef(HRef val)
  {
    out.print("\"_kind\": \"ref\", \"val\": ");
    out.print("\"" + val.toCode() + "\"");

    if (val.dis != null)
    {
      out.print(", \"dis\": " + "\"" + val.dis + "\"");
    }
  }

  private void writeDate(HDate val)
  {
    out.print("\"_kind\": \"date\", \"val\": \"" + val.toString() + "\"");
  }

  private void writeTime(HTime val)
  {
    out.print("\"_kind\": \"time\", \"val\": \"" + val.toString() + "\"");
  }

  private void writeDateTime(HDateTime val)
  {
    out.print("\"_kind\": \"time\", \"val:\"  \"" + val.toString() + "\"");
    if (!val.tz.equals(HTimeZone.DEFAULT))
    {
      out.print("\"tz: \"" + val.tz.toString() + "\"");
    }
  }

  private void writeUri(HUri val)
  {
    out.print("\"_kind\": \"uri\", \"val\": \"" + val.toString() + "\"");
  }

  private void writeSymbol(HSymbol val)
  {
    out.print("\"_kind\": \"symbol\", \"val\": \"" + val.toString() + "\"");
  }

  private void writeCoord(HCoord val)
  {
    out.print("\"_kind\": \"coord\", ");
    out.print("\"lat\": \"" + HCoord.uToStr(val.ulat) + "\", ");
    out.print("\"lng\": \"" + HCoord.uToStr(val.ulng) + "\"");
  }

  private void writeXStr(HXStr val)
  {
  }

  /* Flush the underlying output stream */
  public void flush()
  {
    out.flush();
  }

  /* Close underlying output stream */
  public void close()
  {
    out.close();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  private PrintWriter out;
}

