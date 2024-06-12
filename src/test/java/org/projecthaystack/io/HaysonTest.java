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
}

