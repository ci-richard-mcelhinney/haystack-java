package org.projecthaystack.io;

import java.io.StringWriter;

import static org.testng.Assert.*;
import org.projecthaystack.*;
import org.testng.annotations.Test;

public class HaysonTest
{
  @Test
  public void testScalars()
  {
    HVal val;
    val = HCoord.make(39.56, 123.45);
    String exp = "\"_kind\": \"coord\", \"lat\": \"39.56\", \"lng\": \"123.45\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HDate.make(2024, 06, 12);
    exp = "\"_kind\": \"date\", \"val\": \"2024-06-12\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
  }

  @Test
  public void testSimpleZinc()
  {
    try
    {
      HGrid grid = new HZincReader(simpleZinc).readGrid();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  private static String simpleZinc;

  static
  {
    StringBuffer buf = new StringBuffer();
    buf.append("ver:\"3.0\" projName:\"test\"");
    buf.append("dis dis:\"Equip Name\",equip,siteRef,installed");
    buf.append("\"RTU-1\",M,@153c-699a \"HQ\",2005-06-01");
    buf.append("\"RTU-2\",M,@153c-699b \"Library\",1999-07-12");

    simpleZinc = buf.toString();
  }
}

