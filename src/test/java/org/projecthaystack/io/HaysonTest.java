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
    // HNum
    assertEquals("42", HHaysonWriter.writeVal(new StringWriter(), HNum.make(42)));
    assertEquals("3.14", HHaysonWriter.writeVal(new StringWriter(), HNum.make(3.14)));
    assertEquals("{\"_kind\":\"number\",\"val\":42,\"unit\":\"m\"}", HHaysonWriter.writeVal(new StringWriter(), HNum.make(42, "m")));
    assertEquals("{\"_kind\":\"number\",\"val\":\"NaN\"}", HHaysonWriter.writeVal(new StringWriter(), HNum.NaN));
    assertEquals("{\"_kind\":\"number\",\"val\":\"INF\"}", HHaysonWriter.writeVal(new StringWriter(), HNum.POS_INF));
    assertEquals("{\"_kind\":\"number\",\"val\":\"-INF\"}", HHaysonWriter.writeVal(new StringWriter(), HNum.NEG_INF));

    val = HCoord.make(39.56, 123.45);
    String exp = "{\"_kind\":\"coord\",\"lat\":39.56,\"lng\":123.45}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HDate.make(2024, 06, 12);
    exp = "{\"_kind\":\"date\",\"val\":\"2024-06-12\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HTime.make(17, 19, 23);
    exp = "{\"_kind\":\"time\",\"val\":\"17:19:23\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HMarker.VAL;
    exp = "{\"_kind\":\"marker\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HRemove.VAL;
    exp = "{\"_kind\":\"remove\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HSymbol.make("site");
    exp = "{\"_kind\":\"symbol\",\"val\":\"site\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HUri.make("https://project-haystack.org");
    exp = "{\"_kind\":\"uri\",\"val\":\"https://project-haystack.org\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    val = HRef.make("abc-def");
    exp = "{\"_kind\":\"ref\",\"val\":\"@abc-def\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
    val = HRef.make("abc-def", "Main Elec Meter");
    exp = "{\"_kind\":\"ref\",\"val\":\"@abc-def\",\"dis\":\"Main Elec Meter\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));    

    val = HDateTime.make("2021-03-22T17:56:05.411Z");
    exp = "{\"_kind\":\"dateTime\",\"val\":\"2021-03-22T17:56:05.411Z\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
    val = HDateTime.make("2021-03-22T13:57:00.381-04:00 New_York");
    exp = "{\"_kind\":\"dateTime\",\"val\":\"2021-03-22T13:57:00.381-04:00\",\"tz\":\"New_York\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
  }

  @Test
  public void testBin()
  {
    HVal val = HBin.make("image/jpeg");
    assertEquals("{\"_kind\":\"xstr\",\"type\":\"Bin\",\"val\":\"image/jpeg\"}", HHaysonWriter.writeVal(new StringWriter(), val));
  }

  @Test
  public void testXStr()
  {
    HVal val = HXStr.decode("Func", "main");
    assertEquals("{\"_kind\":\"xstr\",\"type\":\"Func\",\"val\":\"main\"}", HHaysonWriter.writeVal(new StringWriter(), val));
  }

  @Test
  public void testList()
  {
    // empty list
    assertEquals("[]", HHaysonWriter.writeVal(new StringWriter(), HList.EMPTY));

    // list of scalars
    HList list = HList.make(new HVal[] { HNum.make(10), HNum.make(20), HNum.make(30) });
    assertEquals("[10,20,30]", HHaysonWriter.writeVal(new StringWriter(), list));

    // mixed types
    list = HList.make(new HVal[] { HStr.make("hello"), HBool.TRUE, HMarker.VAL });
    assertEquals("[\"hello\",true,{\"_kind\":\"marker\"}]", HHaysonWriter.writeVal(new StringWriter(), list));
  }

  @Test
  public void testSpan()
  {
    // date span
    HVal val = HSpan.make(HDate.make(2023, 1, 1), HDate.make(2023, 1, 31));
    String exp = "{\"_kind\":\"xstr\",\"type\":\"Span\",\"val\":\"2023-01-01,2023-01-31\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));

    // datetime span (UTC)
    HDateTime start = HDateTime.make("2023-01-01T00:00:00Z UTC");
    HDateTime end   = HDateTime.make("2023-01-31T23:59:59Z UTC");
    val = HSpan.make(start, end);
    exp = "{\"_kind\":\"xstr\",\"type\":\"Span\",\"val\":\"2023-01-01T00:00:00Z UTC,2023-01-31T23:59:59Z UTC\"}";
    assertEquals(exp, HHaysonWriter.writeVal(new StringWriter(), val));
  }

  @Test
  public void testSimpleZinc()
  {
    HGrid grid = new HZincReader(simpleZinc).readGrid();
    String result = HHaysonWriter.gridToString(grid);

    assertTrue(result.contains("\"_kind\": \"grid\""));
    assertTrue(result.contains("\"ver\":\"4.0\""));
    assertTrue(result.contains("\"projName\":\"test\""));
    assertTrue(result.contains("\"name\":\"dis\""));
    assertTrue(result.contains("\"name\":\"equip\""));
    assertTrue(result.contains("\"name\":\"siteRef\""));
    assertTrue(result.contains("\"name\":\"installed\""));
    assertTrue(result.contains("\"RTU-1\""));
    assertTrue(result.contains("\"RTU-2\""));
    assertTrue(result.contains("{\"_kind\":\"marker\"}"));
    assertTrue(result.contains("\"_kind\":\"ref\""));
    assertTrue(result.contains("\"_kind\":\"date\""));
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

