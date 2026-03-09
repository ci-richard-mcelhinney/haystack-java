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
  public void testNA()
  {
    assertEquals("{\"_kind\":\"na\"}", HHaysonWriter.writeVal(new StringWriter(), HNA.VAL));
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
  public void testReadScalars()
  {
    // plain numbers
    assertEquals(HNum.make(42),   new HHaysonReader("42").readVal());
    assertEquals(HNum.make(3.14), new HHaysonReader("3.14").readVal());

    // number with unit, NaN, INF
    assertEquals(HNum.make(42, "m"),      new HHaysonReader("{\"_kind\":\"number\",\"val\":42,\"unit\":\"m\"}").readVal());
    assertEquals(HNum.NaN,                new HHaysonReader("{\"_kind\":\"number\",\"val\":\"NaN\"}").readVal());
    assertEquals(HNum.POS_INF,            new HHaysonReader("{\"_kind\":\"number\",\"val\":\"INF\"}").readVal());
    assertEquals(HNum.NEG_INF,            new HHaysonReader("{\"_kind\":\"number\",\"val\":\"-INF\"}").readVal());

    // string and bool
    assertEquals(HStr.make("hello"),  new HHaysonReader("\"hello\"").readVal());
    assertEquals(HBool.TRUE,          new HHaysonReader("true").readVal());
    assertEquals(HBool.FALSE,         new HHaysonReader("false").readVal());

    // marker, remove, NA
    assertEquals(HMarker.VAL, new HHaysonReader("{\"_kind\":\"marker\"}").readVal());
    assertEquals(HRemove.VAL, new HHaysonReader("{\"_kind\":\"remove\"}").readVal());
    assertEquals(HNA.VAL,     new HHaysonReader("{\"_kind\":\"na\"}").readVal());

    // ref
    assertEquals(HRef.make("abc-def"),                  new HHaysonReader("{\"_kind\":\"ref\",\"val\":\"@abc-def\"}").readVal());
    assertEquals(HRef.make("abc-def", "Main Elec Meter"), new HHaysonReader("{\"_kind\":\"ref\",\"val\":\"@abc-def\",\"dis\":\"Main Elec Meter\"}").readVal());

    // date, time, datetime
    assertEquals(HDate.make(2024, 6, 12),   new HHaysonReader("{\"_kind\":\"date\",\"val\":\"2024-06-12\"}").readVal());
    assertEquals(HTime.make(17, 19, 23),    new HHaysonReader("{\"_kind\":\"time\",\"val\":\"17:19:23\"}").readVal());
    assertEquals(HDateTime.make("2021-03-22T17:56:05.411Z"),             new HHaysonReader("{\"_kind\":\"dateTime\",\"val\":\"2021-03-22T17:56:05.411Z\"}").readVal());
    assertEquals(HDateTime.make("2021-03-22T13:57:00.381-04:00 New_York"), new HHaysonReader("{\"_kind\":\"dateTime\",\"val\":\"2021-03-22T13:57:00.381-04:00\",\"tz\":\"New_York\"}").readVal());

    // uri, symbol, coord
    assertEquals(HUri.make("https://project-haystack.org"),  new HHaysonReader("{\"_kind\":\"uri\",\"val\":\"https://project-haystack.org\"}").readVal());
    assertEquals(HSymbol.make("site"),                        new HHaysonReader("{\"_kind\":\"symbol\",\"val\":\"site\"}").readVal());
    assertEquals(HCoord.make(39.56, 123.45),                  new HHaysonReader("{\"_kind\":\"coord\",\"lat\":39.56,\"lng\":123.45}").readVal());

    // xstr, bin
    assertEquals(HXStr.decode("Func", "main"), new HHaysonReader("{\"_kind\":\"xstr\",\"type\":\"Func\",\"val\":\"main\"}").readVal());
    assertEquals(HBin.make("image/jpeg"),       new HHaysonReader("{\"_kind\":\"xstr\",\"type\":\"Bin\",\"val\":\"image/jpeg\"}").readVal());

    // list
    assertEquals(HList.make(new HVal[]{HNum.make(10), HNum.make(20), HNum.make(30)}), new HHaysonReader("[10,20,30]").readVal());
  }

  @Test
  public void testReadGrid()
  {
    // round-trip: write simpleZinc grid then read it back
    HGrid original = new HZincReader(simpleZinc).readGrid();
    String hayson = HHaysonWriter.gridToString(original);
    HGrid result  = new HHaysonReader(hayson).readGrid();

    assertEquals(original.numCols(), result.numCols());
    assertEquals(original.numRows(), result.numRows());
    for (int i = 0; i < original.numCols(); i++)
      assertEquals(original.col(i).name(), result.col(i).name());
    for (int ri = 0; ri < original.numRows(); ri++)
      for (int ci = 0; ci < original.numCols(); ci++)
      {
        String colName = original.col(ci).name();
        assertEquals(original.row(ri).get(colName, false), result.row(ri).get(colName, false));
      }
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

