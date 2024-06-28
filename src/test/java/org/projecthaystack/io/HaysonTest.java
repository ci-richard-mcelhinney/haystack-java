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

    val = HTime.make(17, 19, 23);
    exp = "\"_kind\": \"time\", \"val\": \"17:19:23\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HMarker.VAL;
    exp = "{\"_kind\":\"marker\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HRemove.VAL;
    exp = "{\"_kind\":\"remove\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HSymbol.make("site");
    exp = "\"_kind\": \"symbol\", \"val\": \"site\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HUri.make("https://project-haystack.org"); 
    exp = "\"_kind\": \"uri\", \"val\": \"https://project-haystack.org\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HRef.make("abc-def");
    exp = "\"_kind\": \"ref\", \"val\": \"@abc-def\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
    val = HRef.make("abc-def", "Main Elec Meter");
    exp = "\"_kind\": \"ref\", \"val\": \"@abc-def\", \"dis\": \"Main Elec Meter\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));    

    val = HDateTime.make("2021-03-22T17:56:05.411Z");
    exp = "\"_kind\": \"dateTime\", \"val\": \"2021-03-22T17:56:05.411Z\"";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
    val = HDateTime.make("2021-03-22T13:57:00.381-04:00", "New_York");
    exp = "\"_kind\": \"dateTime\", \"val\": \"2021-03-22T17:56:05.411Z\" \"tz\": \"New_York\"";
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
    buf.append("ver:\"3.0\" projName:\"test\"\n");
    buf.append("dis dis:\"Equip Name\",equip,siteRef,installed\n");
    buf.append("\"RTU-1\",M,@153c-699a \"HQ\",2005-06-01\n");
    buf.append("\"RTU-2\",M,@153c-699b \"Library\",1999-07-12\n");

    simpleZinc = buf.toString();
  }
}

