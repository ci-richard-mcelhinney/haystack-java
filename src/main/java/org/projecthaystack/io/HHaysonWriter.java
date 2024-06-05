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
// HGridWriter
//////////////////////////////////////////////////////////////////////////  

  /* Write a grid in Hayson format */
  public void writeGrid(HGrid grid)
  {

  }

  private void writeDict(HDict dict)
  {

  }

  private void writeDictTags(HDict dict, boolean first)
  {
  }

  private void writeVal(HVal val) 
  {

  } 

  private void writeList(HList list)
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

