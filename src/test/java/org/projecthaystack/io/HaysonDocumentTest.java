//
// Copyright (c) 2026, Richard McElhinney
// Licensed under the Academic Free License version 3.0
//
// History:
//   05 Oct 2026  Richard McElhinney  Creation
//
package org.projecthaystack.io;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.projecthaystack.HBin;
import org.projecthaystack.HBool;
import org.projecthaystack.HCoord;
import org.projecthaystack.HDate;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HList;
import org.projecthaystack.HMarker;
import org.projecthaystack.HNA;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRemove;
import org.projecthaystack.HSpan;
import org.projecthaystack.HStr;
import org.projecthaystack.HSymbol;
import org.projecthaystack.HTime;
import org.projecthaystack.HUri;
import org.projecthaystack.HVal;
import org.projecthaystack.HXStr;
import org.projecthaystack.HaystackTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * HaysonDocumentTest verifies complete Hayson (Haystack 4 JSON) documents
 * produced by HHaysonWriter.  The fixture is a small site tree
 * (site, floor, equips and points) linked together with refs.
 *
 * Output is parsed with a strict test-only JSON parser and compared
 * structurally, so whitespace and key order do not matter but invalid
 * JSON (duplicate keys, unescaped control characters) is rejected.
 *
 * @see <a href='https://project-haystack.org/doc/docHaystack/Json#v4'>Project Haystack | JSON v4</a>
 * @see <a href='https://github.com/j2inn/hayson/blob/master/hayson-json-schema.json'>Hayson JSON schema</a>
 */
public class HaysonDocumentTest extends HaystackTest
{

//////////////////////////////////////////////////////////////////////////
// Site tree document
//////////////////////////////////////////////////////////////////////////

  @Test
  public void verifySiteTreeGrid()
  {
    Map<String, Object> doc = parseDoc(HHaysonWriter.gridToString(siteTreeGrid()));

    assertEquals(doc.get("_kind"), "grid");
    assertEquals(doc.get("meta"), j(TREE_META_JSON));

    // every row is a dict and empty cells are omitted, not written as null
    List<Object> rows = asList(doc.get("rows"));
    assertEquals(rows.size(), TREE_ROWS_JSON.length);
    for (int i = 0; i < rows.size(); i++)
    {
      Map<String, Object> row = asMap(rows.get(i));
      assertFalse(row.containsKey("_kind") && !"dict".equals(row.get("_kind")), "row " + i + " is not a dict");
      assertFalse(row.containsValue(null), "row " + i + " contains a null cell: " + row);
      assertEquals(row, j(TREE_ROWS_JSON[i]), "row " + i);
    }
  }

  @Test
  public void verifyRefIntegrity()
  {
    Map<String, Object> doc = parseDoc(HHaysonWriter.gridToString(siteTreeGrid()));
    List<Object> rows = asList(doc.get("rows"));

    // collect the id of every record in the document
    Set<String> ids = new HashSet<String>();
    for (int i = 0; i < rows.size(); i++)
    {
      Map<String, Object> row = asMap(rows.get(i));
      Map<String, Object> id = asMap(row.get("id"));
      assertEquals(id.get("_kind"), "ref", "row " + i + " id");
      assertEquals(id.get("dis"), row.get("dis"), "row " + i + " id dis");
      assertTrue(ids.add(refVal(id)), "duplicate id " + id.get("val"));
    }
    assertEquals(ids.size(), 11);

    // every *Ref tag must resolve to a record in the same document
    int refCount = 0;
    for (int i = 0; i < rows.size(); i++)
    {
      Map<String, Object> row = asMap(rows.get(i));
      for (Map.Entry<String, Object> e : row.entrySet())
      {
        if (!e.getKey().endsWith("Ref")) continue;
        Map<String, Object> ref = asMap(e.getValue());
        assertEquals(ref.get("_kind"), "ref", "row " + i + " " + e.getKey());
        assertTrue(ids.contains(refVal(ref)), "row " + i + " " + e.getKey() + " is dangling: " + ref);
        refCount++;
      }
    }
    assertEquals(refCount, 19);

    // walk the tree: points -> equip -> space/site
    Map<String, Object> kw = rowById(rows, "p:demo:r:kw");
    Map<String, Object> meter = rowById(rows, refVal(asMap(kw.get("equipRef"))));
    assertEquals(meter.get("dis"), "Main Elec Meter");
    Map<String, Object> vav = rowById(rows, "vav-1");
    Map<String, Object> ahu = rowById(rows, refVal(asMap(vav.get("equipRef"))));
    assertEquals(ahu.get("dis"), "AHU-1");
    Map<String, Object> floor = rowById(rows, refVal(asMap(ahu.get("spaceRef"))));
    Map<String, Object> site = rowById(rows, refVal(asMap(floor.get("siteRef"))));
    assertEquals(site.get("site"), j(MARKER));
  }

  @Test
  public void verifyGridAndColumnMeta()
  {
    Map<String, Object> doc = parseDoc(HHaysonWriter.gridToString(siteTreeGrid()));

    // ver must appear exactly once (strict parser rejects duplicates)
    Map<String, Object> meta = asMap(doc.get("meta"));
    assertEquals(meta.get("ver"), "3.0");
    assertEquals(meta.get("projName"), "demo");
    assertEquals(meta.get("dis"), "Demo Site Tree");
    assertEquals(meta.size(), 3);

    List<Object> cols = asList(doc.get("cols"));
    assertEquals(cols.size(), TREE_COLS.length);
    for (int i = 0; i < cols.size(); i++)
    {
      Map<String, Object> col = asMap(cols.get(i));
      String name = TREE_COLS[i];
      assertEquals(col.get("name"), name, "col " + i);
      if (name.equals("dis"))
        assertEquals(col.get("meta"), j("{'dis':'Name'}"));
      else if (name.equals("area"))
        assertEquals(col.get("meta"), j("{'dis':'Area','unit':'" + FT2 + "'}"));
      else if (name.equals("curVal"))
        assertEquals(col.get("meta"), j("{'dis':'Current Value'}"));
      else
        assertFalse(col.containsKey("meta"), "col " + name + " should have no meta");
    }
  }

  @Test
  public void verifyDictDocument()
  {
    HDict site = siteTreeRecs()[0];
    String json = HHaysonWriter.writeVal(new StringWriter(), site);

    assertEquals(parseDoc(json), j(TREE_ROWS_JSON[0]));
    assertEquals(new HHaysonReader(json).readDict(), site);
  }

//////////////////////////////////////////////////////////////////////////
// Other standard documents
//////////////////////////////////////////////////////////////////////////

  @Test
  public void verifyHisGrid()
  {
    Map<String, Object> doc = parseDoc(HHaysonWriter.gridToString(hisGrid()));

    assertEquals(doc.get("meta"), j(
      "{'ver':'3.0'," +
      "'id':{'_kind':'ref','val':'p:demo:r:kw','dis':'Main Elec Meter kW'}," +
      "'hisStart':{'_kind':'dateTime','val':'2024-06-12T00:00:00-04:00','tz':'New_York'}," +
      "'hisEnd':{'_kind':'dateTime','val':'2024-06-13T00:00:00-04:00','tz':'New_York'}}"));
    assertEquals(doc.get("cols"), j("[{'name':'ts'},{'name':'val','meta':{'unit':'kW'}}]"));
    assertEquals(doc.get("rows"), j(
      "[{'ts':{'_kind':'dateTime','val':'2024-06-12T09:00:00-04:00','tz':'New_York'},'val':{'_kind':'number','val':110.5,'unit':'kW'}}," +
      " {'ts':{'_kind':'dateTime','val':'2024-06-12T09:15:00-04:00','tz':'New_York'},'val':{'_kind':'number','val':118.25,'unit':'kW'}}," +
      " {'ts':{'_kind':'dateTime','val':'2024-06-12T09:30:00-04:00','tz':'New_York'},'val':{'_kind':'number','val':123.456789,'unit':'kW'}}," +
      " {'ts':{'_kind':'dateTime','val':'2024-06-12T09:45:00-04:00','tz':'New_York'},'val':{'_kind':'number','val':0,'unit':'kW'}}]"));
  }

  @Test
  public void verifyCommitGrid()
  {
    Map<String, Object> doc = parseDoc(HHaysonWriter.gridToString(commitGrid()));

    assertEquals(doc.get("meta"), j("{'ver':'3.0','commit':'update'}"));
    assertEquals(doc.get("rows"), j(
      "[{'id':{'_kind':'ref','val':'pt-sp','dis':'VAV-1 Zone Temp SP'}," +
      "  'mod':{'_kind':'dateTime','val':'2024-06-12T13:30:05.411Z'}," +
      "  'dis':'VAV-1 Zone Temp Setpoint'," +
      "  'curVal':{'_kind':'remove'}}]"));
  }

  @Test
  public void verifySpecExamples()
  {
    // the two grid examples from the JSON v4 section of the spec
    assertEquals(parseDoc(HHaysonWriter.gridToString(specRtuGrid())), j(SPEC_RTU_JSON));
    assertEquals(parseDoc(HHaysonWriter.gridToString(specNestedGrid())), j(SPEC_NESTED_JSON));
  }

  @Test
  public void verifyStringEscaping()
  {
    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0");
    b.addCol("id");
    b.addCol("dis");
    b.addCol("notes");
    b.addCol("path");
    b.addCol("ctrl");
    b.addCol("intl");
    b.addCol("note");
    b.addRow(new HVal[] {
      HRef.make("esc-1", "HQ \"Main\""),
      HStr.make("Say \"hi\""),
      HStr.make("line1\nline2\ttabbed\r\n"),
      HStr.make("C:\\temp\\file.txt"),
      HStr.make("bell\u0007nul\u0001"),
      HStr.make("Z\u00fcrich \u2713 \u6771\u4eac"),
      HXStr.decode("Note", "he said \"ok\"\\done"),
    });
    HGrid grid = b.toGrid();
    String json = HHaysonWriter.gridToString(grid);

    Map<String, Object> row = asMap(asList(parseDoc(json).get("rows")).get(0));
    assertEquals(asMap(row.get("id")).get("dis"), "HQ \"Main\"");
    assertEquals(row.get("dis"), "Say \"hi\"");
    assertEquals(row.get("notes"), "line1\nline2\ttabbed\r\n");
    assertEquals(row.get("path"), "C:\\temp\\file.txt");
    assertEquals(row.get("ctrl"), "bell\u0007nul\u0001");
    assertEquals(row.get("intl"), "Z\u00fcrich \u2713 \u6771\u4eac");
    assertEquals(asMap(row.get("note")).get("val"), "he said \"ok\"\\done");

    assertGridEquals(new HHaysonReader(json).readGrid(), grid);
  }

//////////////////////////////////////////////////////////////////////////
// Cross-document checks
//////////////////////////////////////////////////////////////////////////

  @Test
  public void verifyAllTypesInDocument()
  {
    // the standard documents must between them exercise every Hayson kind
    Set<String> found = new TreeSet<String>();
    collectKinds(parseDoc(HHaysonWriter.gridToString(siteTreeGrid())), found);
    collectKinds(parseDoc(HHaysonWriter.gridToString(hisGrid())), found);
    collectKinds(parseDoc(HHaysonWriter.gridToString(commitGrid())), found);

    String[] expected = {
      "bool", "coord", "date", "dateTime:tz", "dateTime:utc", "dict", "grid",
      "list", "marker", "na", "number", "number:-INF", "number:INF",
      "number:NaN", "number:unit", "ref", "ref:dis", "remove", "str",
      "symbol", "time", "uri", "xstr:Bin", "xstr:Schedule",
    };
    Set<String> missing = new TreeSet<String>(Arrays.asList(expected));
    missing.removeAll(found);
    assertTrue(missing.isEmpty(), "kinds not found in documents: " + missing + ", found: " + found);
  }

  @Test
  public void verifyRoundTrip()
  {
    HGrid[] grids = { siteTreeGrid(), hisGrid(), commitGrid(), specRtuGrid(), specNestedGrid() };
    for (int i = 0; i < grids.length; i++)
    {
      String json = HHaysonWriter.gridToString(grids[i]);
      assertGridEquals(new HHaysonReader(json).readGrid(), grids[i]);
    }
  }

  @Test
  public void verifySpanRoundTrip()
  {
    HSpan dates = HSpan.make(HDate.make(2024, 1, 1), HDate.make(2024, 1, 31));
    HSpan times = HSpan.make(
      HDateTime.make("2024-01-01T00:00:00-05:00 New_York"),
      HDateTime.make("2024-02-01T00:00:00-05:00 New_York"));

    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0");
    b.addCol("id");
    b.addCol("commissioned");
    b.addRow(new HVal[] { HRef.make("ahu-1"), dates });
    b.addRow(new HVal[] { HRef.make("ahu-2"), times });
    HGrid grid = b.toGrid();

    String json = HHaysonWriter.gridToString(grid);
    assertEquals(asMap(asMap(asList(parseDoc(json).get("rows")).get(0)).get("commissioned")),
      j("{'_kind':'xstr','type':'Span','val':'2024-01-01,2024-01-31'}"));

    HGrid result = new HHaysonReader(json).readGrid();
    assertEquals(result.row(0).get("commissioned"), dates);
    assertEquals(result.row(1).get("commissioned"), times);
  }

  @Test
  public void verifySchemaConformance()
  {
    List<String> errs = new ArrayList<String>();
    validate("siteTree", HHaysonWriter.gridToString(siteTreeGrid()), errs);
    validate("his",      HHaysonWriter.gridToString(hisGrid()), errs);
    validate("commit",   HHaysonWriter.gridToString(commitGrid()), errs);
    validate("specRtu",  HHaysonWriter.gridToString(specRtuGrid()), errs);
    validate("nested",   HHaysonWriter.gridToString(specNestedGrid()), errs);
    validate("siteDict", HHaysonWriter.writeVal(new StringWriter(), siteTreeRecs()[0]), errs);
    assertTrue(errs.isEmpty(), errs.size() + " schema violations:\n  " + join(errs, "\n  "));
  }

//////////////////////////////////////////////////////////////////////////
// Fixtures
//////////////////////////////////////////////////////////////////////////

  private static final String DEG_F = "\u00b0F";
  private static final String FT2   = "ft\u00b2";

  private static final HMarker M = HMarker.VAL;

  private static final HRef SITE     = HRef.make("site-hq", "Demo HQ");
  private static final HRef FLOOR    = HRef.make("floor-1", "Floor 1");
  private static final HRef FLOOR_ID = HRef.make("floor-1");
  private static final HRef AHU      = HRef.make("ahu-1", "AHU-1");
  private static final HRef VAV      = HRef.make("vav-1", "VAV-1");
  private static final HRef METER    = HRef.make("meter-main", "Main Elec Meter");
  private static final HRef KW       = HRef.make("p:demo:r:kw", "Main Elec Meter kW");

  private static final String[] TREE_COLS = {
    "id", "dis", "site", "space", "floor", "equip", "ahu", "vav", "elec",
    "meter", "point", "sensor", "cmd", "sp", "writable", "his", "kind",
    "unit", "siteRef", "spaceRef", "equipRef", "area", "yearBuilt",
    "geoCoord", "tz", "website", "occupiedStart", "photo",
    "primaryFunction", "installed", "config", "schedule", "enabled",
    "phases", "pointDef", "curVal", "curStatus", "minVal", "maxVal",
    "hisEnd", "lastSync", "recent",
  };

  /** Site -> floor -> AHU/VAV/meter -> points, linked by refs */
  private static HDict[] siteTreeRecs()
  {
    return new HDict[] {
      dict("id", SITE, "dis", s("Demo HQ"), "site", M,
           "area", HNum.make(5000, FT2), "yearBuilt", HNum.make(1992),
           "geoCoord", HCoord.make(40.7128, -74.006), "tz", s("New_York"),
           "website", HUri.make("https://example.com/demo"),
           "occupiedStart", HTime.make(7, 30, 0),
           "photo", HBin.make("image/png"), "primaryFunction", s("Office")),
      dict("id", FLOOR, "dis", s("Floor 1"), "space", M, "floor", M,
           "siteRef", SITE),
      dict("id", AHU, "dis", s("AHU-1"), "equip", M, "ahu", M,
           "siteRef", SITE, "spaceRef", FLOOR_ID,
           "installed", HDate.make(2019, 4, 15),
           "config", dict("fanStages", HNum.make(2), "economizer", M),
           "schedule", HXStr.decode("Schedule", "Mon-Fri 07:00-18:00")),
      dict("id", VAV, "dis", s("VAV-1"), "equip", M, "vav", M,
           "siteRef", SITE, "spaceRef", FLOOR_ID, "equipRef", AHU,
           "enabled", HBool.FALSE),
      dict("id", METER, "dis", s("Main Elec Meter"), "equip", M, "elec", M,
           "meter", M, "siteRef", SITE,
           "phases", HList.make(new HVal[] { s("A"), s("B"), s("C") })),
      dict("id", HRef.make("pt-dat", "AHU-1 Discharge Air Temp"),
           "dis", s("AHU-1 Discharge Air Temp"), "point", M, "sensor", M,
           "his", M, "kind", s("Number"), "unit", s(DEG_F),
           "siteRef", SITE, "equipRef", AHU,
           "pointDef", HSymbol.make("discharge-air-temp-sensor"),
           "curVal", HNum.make(55.4, DEG_F)),
      dict("id", HRef.make("pt-fan", "AHU-1 Fan Cmd"),
           "dis", s("AHU-1 Fan Cmd"), "point", M, "cmd", M, "his", M,
           "kind", s("Bool"), "siteRef", SITE, "equipRef", AHU,
           "curVal", HBool.TRUE),
      dict("id", HRef.make("pt-sp", "VAV-1 Zone Temp SP"),
           "dis", s("VAV-1 Zone Temp SP"), "point", M, "sp", M,
           "writable", M, "kind", s("Number"), "unit", s(DEG_F),
           "siteRef", SITE, "equipRef", VAV, "curVal", HNA.VAL,
           "minVal", HNum.NEG_INF, "maxVal", HNum.POS_INF),
      dict("id", HRef.make("pt-flow", "VAV-1 Airflow"),
           "dis", s("VAV-1 Airflow"), "point", M, "sensor", M,
           "kind", s("Number"), "siteRef", SITE, "equipRef", VAV,
           "curVal", HNum.NaN, "curStatus", s("fault")),
      dict("id", HRef.make("pt-mode", "VAV-1 Occ Mode"),
           "dis", s("VAV-1 Occ Mode"), "point", M, "sensor", M,
           "kind", s("Str"), "siteRef", SITE, "equipRef", VAV,
           "curVal", s("Occupied"), "curStatus", s("ok")),
      dict("id", KW, "dis", s("Main Elec Meter kW"), "point", M,
           "sensor", M, "his", M, "kind", s("Number"), "unit", s("kW"),
           "siteRef", SITE, "equipRef", METER,
           "curVal", HNum.make(123.456789, "kW"),
           "hisEnd", HDateTime.make("2024-06-12T09:30:00-04:00 New_York"),
           "lastSync", HDateTime.make("2024-06-12T13:30:05.411Z"),
           "recent", recentGrid()),
    };
  }

  private static HGrid siteTreeGrid()
  {
    HDict meta = dict("ver", s("3.0"), "projName", s("demo"), "dis", s("Demo Site Tree"));
    Map<String, HDict> colMeta = new LinkedHashMap<String, HDict>();
    colMeta.put("dis", dict("dis", s("Name")));
    colMeta.put("area", dict("dis", s("Area"), "unit", s(FT2)));
    colMeta.put("curVal", dict("dis", s("Current Value")));
    return toGrid(meta, TREE_COLS, colMeta, siteTreeRecs());
  }

  /** Small nested grid stored as a tag value on the kW point */
  private static HGrid recentGrid()
  {
    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0");
    b.addCol("ts");
    b.addCol("val");
    b.addRow(new HVal[] { HDateTime.make("2024-06-12T09:15:00-04:00 New_York"), HNum.make(118.25, "kW") });
    b.addRow(new HVal[] { HDateTime.make("2024-06-12T09:30:00-04:00 New_York"), HNum.make(123.456789, "kW") });
    return b.toGrid();
  }

  /** Result of a hisRead on the kW point */
  private static HGrid hisGrid()
  {
    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0")
      .add("id", KW)
      .add("hisStart", HDateTime.make("2024-06-12T00:00:00-04:00 New_York"))
      .add("hisEnd", HDateTime.make("2024-06-13T00:00:00-04:00 New_York"));
    b.addCol("ts");
    b.addCol("val").add("unit", "kW");
    b.addRow(new HVal[] { HDateTime.make("2024-06-12T09:00:00-04:00 New_York"), HNum.make(110.5, "kW") });
    b.addRow(new HVal[] { HDateTime.make("2024-06-12T09:15:00-04:00 New_York"), HNum.make(118.25, "kW") });
    b.addRow(new HVal[] { HDateTime.make("2024-06-12T09:30:00-04:00 New_York"), HNum.make(123.456789, "kW") });
    b.addRow(new HVal[] { HDateTime.make("2024-06-12T09:45:00-04:00 New_York"), HNum.make(0, "kW") });
    return b.toGrid();
  }

  /** Commit update that renames a point and removes its curVal tag */
  private static HGrid commitGrid()
  {
    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0").add("commit", "update");
    b.addCol("id");
    b.addCol("mod");
    b.addCol("dis");
    b.addCol("curVal");
    b.addRow(new HVal[] {
      HRef.make("pt-sp", "VAV-1 Zone Temp SP"),
      HDateTime.make("2024-06-12T13:30:05.411Z"),
      s("VAV-1 Zone Temp Setpoint"),
      HRemove.VAL,
    });
    return b.toGrid();
  }

  /** First grid example from the JSON v4 section of the spec */
  private static HGrid specRtuGrid()
  {
    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0").add("foo", "bar");
    b.addCol("dis").add("dis", "Equip Name");
    b.addCol("equip");
    b.addCol("siteRef");
    b.addCol("installed");
    b.addRow(new HVal[] { s("RTU-1"), M, HRef.make("153c-699a", "HQ"), HDate.make(2005, 6, 1) });
    b.addRow(new HVal[] { s("RTU-2"), M, HRef.make("153c-699b", "Library"), HDate.make(1999, 7, 12) });
    return b.toGrid();
  }

  /** Nested list/dict/grid example from the JSON v4 section of the spec */
  private static HGrid specNestedGrid()
  {
    HGridBuilder inner = new HGridBuilder();
    inner.meta().add("ver", "3.0");
    inner.addCol("a");
    inner.addCol("b");
    inner.addRow(new HVal[] { HNum.make(1), HNum.make(2) });
    inner.addRow(new HVal[] { HNum.make(3), HNum.make(4) });

    HGridBuilder b = new HGridBuilder();
    b.meta().add("ver", "3.0");
    b.addCol("type");
    b.addCol("val");
    b.addRow(new HVal[] { s("list"), HList.make(new HVal[] { HNum.make(1), HNum.make(2), HNum.make(3) }) });
    b.addRow(new HVal[] { s("dict"), dict("dis", s("Dict!"), "foo", M) });
    b.addRow(new HVal[] { s("grid"), inner.toGrid() });
    b.addRow(new HVal[] { s("scalar"), s("simple string") });
    return b.toGrid();
  }

//////////////////////////////////////////////////////////////////////////
// Expected JSON (single quotes are converted to double quotes by j())
//////////////////////////////////////////////////////////////////////////

  private static final String MARKER = "{'_kind':'marker'}";

  private static final String SITE_J  = "{'_kind':'ref','val':'site-hq','dis':'Demo HQ'}";
  private static final String FLOOR_J = "{'_kind':'ref','val':'floor-1'}";
  private static final String AHU_J   = "{'_kind':'ref','val':'ahu-1','dis':'AHU-1'}";
  private static final String VAV_J   = "{'_kind':'ref','val':'vav-1','dis':'VAV-1'}";
  private static final String METER_J = "{'_kind':'ref','val':'meter-main','dis':'Main Elec Meter'}";

  private static final String TREE_META_JSON =
    "{'ver':'3.0','projName':'demo','dis':'Demo Site Tree'}";

  private static final String[] TREE_ROWS_JSON = {
    // site
    "{'id':" + SITE_J + ",'dis':'Demo HQ','site':" + MARKER + "," +
    " 'area':{'_kind':'number','val':5000,'unit':'" + FT2 + "'},'yearBuilt':1992," +
    " 'geoCoord':{'_kind':'coord','lat':40.7128,'lng':-74.006},'tz':'New_York'," +
    " 'website':{'_kind':'uri','val':'https://example.com/demo'}," +
    " 'occupiedStart':{'_kind':'time','val':'07:30:00'}," +
    " 'photo':{'_kind':'xstr','type':'Bin','val':'image/png'}," +
    " 'primaryFunction':'Office'}",

    // floor
    "{'id':{'_kind':'ref','val':'floor-1','dis':'Floor 1'},'dis':'Floor 1'," +
    " 'space':" + MARKER + ",'floor':" + MARKER + ",'siteRef':" + SITE_J + "}",

    // ahu
    "{'id':" + AHU_J + ",'dis':'AHU-1','equip':" + MARKER + ",'ahu':" + MARKER + "," +
    " 'siteRef':" + SITE_J + ",'spaceRef':" + FLOOR_J + "," +
    " 'installed':{'_kind':'date','val':'2019-04-15'}," +
    " 'config':{'fanStages':2,'economizer':" + MARKER + "}," +
    " 'schedule':{'_kind':'xstr','type':'Schedule','val':'Mon-Fri 07:00-18:00'}}",

    // vav
    "{'id':" + VAV_J + ",'dis':'VAV-1','equip':" + MARKER + ",'vav':" + MARKER + "," +
    " 'siteRef':" + SITE_J + ",'spaceRef':" + FLOOR_J + ",'equipRef':" + AHU_J + "," +
    " 'enabled':false}",

    // meter
    "{'id':" + METER_J + ",'dis':'Main Elec Meter','equip':" + MARKER + "," +
    " 'elec':" + MARKER + ",'meter':" + MARKER + ",'siteRef':" + SITE_J + "," +
    " 'phases':['A','B','C']}",

    // discharge air temp
    "{'id':{'_kind':'ref','val':'pt-dat','dis':'AHU-1 Discharge Air Temp'}," +
    " 'dis':'AHU-1 Discharge Air Temp','point':" + MARKER + ",'sensor':" + MARKER + "," +
    " 'his':" + MARKER + ",'kind':'Number','unit':'" + DEG_F + "'," +
    " 'siteRef':" + SITE_J + ",'equipRef':" + AHU_J + "," +
    " 'pointDef':{'_kind':'symbol','val':'discharge-air-temp-sensor'}," +
    " 'curVal':{'_kind':'number','val':55.4,'unit':'" + DEG_F + "'}}",

    // fan cmd
    "{'id':{'_kind':'ref','val':'pt-fan','dis':'AHU-1 Fan Cmd'}," +
    " 'dis':'AHU-1 Fan Cmd','point':" + MARKER + ",'cmd':" + MARKER + ",'his':" + MARKER + "," +
    " 'kind':'Bool','siteRef':" + SITE_J + ",'equipRef':" + AHU_J + ",'curVal':true}",

    // zone temp sp
    "{'id':{'_kind':'ref','val':'pt-sp','dis':'VAV-1 Zone Temp SP'}," +
    " 'dis':'VAV-1 Zone Temp SP','point':" + MARKER + ",'sp':" + MARKER + "," +
    " 'writable':" + MARKER + ",'kind':'Number','unit':'" + DEG_F + "'," +
    " 'siteRef':" + SITE_J + ",'equipRef':" + VAV_J + ",'curVal':{'_kind':'na'}," +
    " 'minVal':{'_kind':'number','val':'-INF'},'maxVal':{'_kind':'number','val':'INF'}}",

    // airflow
    "{'id':{'_kind':'ref','val':'pt-flow','dis':'VAV-1 Airflow'}," +
    " 'dis':'VAV-1 Airflow','point':" + MARKER + ",'sensor':" + MARKER + "," +
    " 'kind':'Number','siteRef':" + SITE_J + ",'equipRef':" + VAV_J + "," +
    " 'curVal':{'_kind':'number','val':'NaN'},'curStatus':'fault'}",

    // occ mode
    "{'id':{'_kind':'ref','val':'pt-mode','dis':'VAV-1 Occ Mode'}," +
    " 'dis':'VAV-1 Occ Mode','point':" + MARKER + ",'sensor':" + MARKER + "," +
    " 'kind':'Str','siteRef':" + SITE_J + ",'equipRef':" + VAV_J + "," +
    " 'curVal':'Occupied','curStatus':'ok'}",

    // meter kW
    "{'id':{'_kind':'ref','val':'p:demo:r:kw','dis':'Main Elec Meter kW'}," +
    " 'dis':'Main Elec Meter kW','point':" + MARKER + ",'sensor':" + MARKER + "," +
    " 'his':" + MARKER + ",'kind':'Number','unit':'kW'," +
    " 'siteRef':" + SITE_J + ",'equipRef':" + METER_J + "," +
    " 'curVal':{'_kind':'number','val':123.456789,'unit':'kW'}," +
    " 'hisEnd':{'_kind':'dateTime','val':'2024-06-12T09:30:00-04:00','tz':'New_York'}," +
    " 'lastSync':{'_kind':'dateTime','val':'2024-06-12T13:30:05.411Z'}," +
    " 'recent':{'_kind':'grid','meta':{'ver':'3.0'},'cols':[{'name':'ts'},{'name':'val'}],'rows':[" +
    "   {'ts':{'_kind':'dateTime','val':'2024-06-12T09:15:00-04:00','tz':'New_York'},'val':{'_kind':'number','val':118.25,'unit':'kW'}}," +
    "   {'ts':{'_kind':'dateTime','val':'2024-06-12T09:30:00-04:00','tz':'New_York'},'val':{'_kind':'number','val':123.456789,'unit':'kW'}}]}}",
  };

  private static final String SPEC_RTU_JSON =
    "{'_kind':'grid'," +
    " 'meta':{'ver':'3.0','foo':'bar'}," +
    " 'cols':[{'name':'dis','meta':{'dis':'Equip Name'}},{'name':'equip'},{'name':'siteRef'},{'name':'installed'}]," +
    " 'rows':[" +
    "  {'dis':'RTU-1','equip':{'_kind':'marker'}," +
    "   'siteRef':{'_kind':'ref','val':'153c-699a','dis':'HQ'}," +
    "   'installed':{'_kind':'date','val':'2005-06-01'}}," +
    "  {'dis':'RTU-2','equip':{'_kind':'marker'}," +
    "   'siteRef':{'_kind':'ref','val':'153c-699b','dis':'Library'}," +
    "   'installed':{'_kind':'date','val':'1999-07-12'}}]}";

  private static final String SPEC_NESTED_JSON =
    "{'_kind':'grid'," +
    " 'meta':{'ver':'3.0'}," +
    " 'cols':[{'name':'type'},{'name':'val'}]," +
    " 'rows':[" +
    "  {'type':'list','val':[1,2,3]}," +
    "  {'type':'dict','val':{'dis':'Dict!','foo':{'_kind':'marker'}}}," +
    "  {'type':'grid','val':{'_kind':'grid','meta':{'ver':'3.0'}," +
    "    'cols':[{'name':'a'},{'name':'b'}]," +
    "    'rows':[{'a':1,'b':2},{'a':3,'b':4}]}}," +
    "  {'type':'scalar','val':'simple string'}]}";

//////////////////////////////////////////////////////////////////////////
// Schema validation
//////////////////////////////////////////////////////////////////////////

  // patterns taken from hayson-json-schema.json
  private static final Pattern TAG_NAME  = Pattern.compile("^[a-z][0-9a-zA-Z_]*$");
  private static final Pattern REF_VAL   = Pattern.compile("^[0-9a-zA-Z_:\\-.~]+$");
  private static final Pattern UNIT      = Pattern.compile("^([a-zA-Z%_/$]|[\\x{80}-\\x{FFFF}])+$");
  private static final Pattern DATE      = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
  private static final Pattern TIME      = Pattern.compile("^(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)?$");
  private static final Pattern DATE_TIME = Pattern.compile(
    "^(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T" +
    "(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)?(Z|[-+](2[0-3]|[01][0-9]):[0-5][0-9])?$");
  private static final Pattern XSTR_TYPE = Pattern.compile("^[A-Z][a-zA-Z0-9_]*$");

  private static final Set<String> NUM_SPECIALS = new HashSet<String>(Arrays.asList("INF", "-INF", "NaN"));

  private static void validate(String name, String json, List<String> errs)
  {
    Object doc;
    try { doc = StrictJson.parse(json); }
    catch (IllegalArgumentException e) { errs.add(name + ": " + e.getMessage()); return; }
    validateVal(doc, name, errs);
  }

  private static void validateVal(Object v, String path, List<String> errs)
  {
    if (v == null || v instanceof String || v instanceof Boolean || v instanceof Double) return;
    if (v instanceof List)
    {
      List<Object> list = asList(v);
      for (int i = 0; i < list.size(); i++) validateVal(list.get(i), path + "[" + i + "]", errs);
      return;
    }

    Map<String, Object> obj = asMap(v);
    Object kind = obj.get("_kind");
    if (kind == null || "dict".equals(kind)) { validateDict(obj, path, errs); return; }
    if (!(kind instanceof String)) { errs.add(path + ": _kind is not a string"); return; }

    String k = (String)kind;
    if (k.equals("marker") || k.equals("remove") || k.equals("na"))
    {
      checkKeys(obj, path, errs, new String[] {}, new String[] {});
    }
    else if (k.equals("number"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] { "unit" });
      Object val = obj.get("val");
      if (val instanceof String)
      {
        if (!NUM_SPECIALS.contains(val)) errs.add(path + ": number val '" + val + "' is not INF, -INF or NaN");
        if (obj.containsKey("unit")) errs.add(path + ": special number must not have a unit");
      }
      else if (!(val instanceof Double)) errs.add(path + ": number val must be a number or special");
      if (obj.containsKey("unit")) checkStr(obj, "unit", UNIT, path, errs);
    }
    else if (k.equals("ref"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] { "dis" });
      checkStr(obj, "val", REF_VAL, path, errs);
      if (obj.containsKey("dis")) checkStr(obj, "dis", null, path, errs);
    }
    else if (k.equals("symbol"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] {});
      checkStr(obj, "val", REF_VAL, path, errs);
    }
    else if (k.equals("uri"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] {});
      checkStr(obj, "val", null, path, errs);
    }
    else if (k.equals("date"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] {});
      checkStr(obj, "val", DATE, path, errs);
    }
    else if (k.equals("time"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] {});
      checkStr(obj, "val", TIME, path, errs);
    }
    else if (k.equals("dateTime"))
    {
      checkKeys(obj, path, errs, new String[] { "val" }, new String[] { "tz" });
      checkStr(obj, "val", DATE_TIME, path, errs);
      if (obj.containsKey("tz") && "".equals(checkStr(obj, "tz", null, path, errs)))
        errs.add(path + ": tz must not be empty");
    }
    else if (k.equals("coord"))
    {
      checkKeys(obj, path, errs, new String[] { "lat", "lng" }, new String[] {});
      if (!(obj.get("lat") instanceof Double)) errs.add(path + ": lat must be a number");
      if (!(obj.get("lng") instanceof Double)) errs.add(path + ": lng must be a number");
    }
    else if (k.equals("xstr"))
    {
      checkKeys(obj, path, errs, new String[] { "type", "val" }, new String[] {});
      checkStr(obj, "type", XSTR_TYPE, path, errs);
      checkStr(obj, "val", null, path, errs);
    }
    else if (k.equals("grid"))
    {
      validateGrid(obj, path, errs);
    }
    else
    {
      errs.add(path + ": unknown _kind '" + k + "'");
    }
  }

  private static void validateGrid(Map<String, Object> grid, String path, List<String> errs)
  {
    checkKeys(grid, path, errs, new String[] { "meta" }, new String[] { "cols", "rows" });

    Object meta = grid.get("meta");
    if (meta instanceof Map)
    {
      Map<String, Object> m = asMap(meta);
      if (!"3.0".equals(m.get("ver"))) errs.add(path + ".meta: ver must be \"3.0\" but was " + m.get("ver"));
      validateDict(m, path + ".meta", errs);
    }
    else errs.add(path + ": meta must be an object");

    if (grid.containsKey("cols"))
    {
      List<Object> cols = asList(grid.get("cols"));
      for (int i = 0; i < cols.size(); i++)
      {
        String p = path + ".cols[" + i + "]";
        Map<String, Object> col = asMap(cols.get(i));
        Set<String> extra = new TreeSet<String>(col.keySet());
        extra.removeAll(Arrays.asList("name", "meta"));
        if (!extra.isEmpty()) errs.add(p + ": unexpected keys " + extra);
        checkStr(col, "name", null, p, errs);
        if (col.containsKey("meta")) validateDict(asMap(col.get("meta")), p + ".meta", errs);
      }
    }

    if (grid.containsKey("rows"))
    {
      List<Object> rows = asList(grid.get("rows"));
      for (int i = 0; i < rows.size(); i++)
      {
        String p = path + ".rows[" + i + "]";
        if (!(rows.get(i) instanceof Map)) { errs.add(p + ": row must be an object"); continue; }
        Map<String, Object> row = asMap(rows.get(i));
        Object kind = row.get("_kind");
        if (kind != null && !"dict".equals(kind)) errs.add(p + ": row must be a dict, not " + kind);
        validateDict(row, p, errs);
      }
    }
  }

  private static void validateDict(Map<String, Object> dict, String path, List<String> errs)
  {
    for (Map.Entry<String, Object> e : dict.entrySet())
    {
      String key = e.getKey();
      if (key.equals("_kind")) continue;
      if (!TAG_NAME.matcher(key).matches()) errs.add(path + ": invalid tag name '" + key + "'");
      if (e.getValue() == null) errs.add(path + "." + key + ": null is not a valid tag value");
      validateVal(e.getValue(), path + "." + key, errs);
    }
  }

  /** Check _kind plus required keys are present and nothing else appears */
  private static void checkKeys(Map<String, Object> obj, String path, List<String> errs, String[] required, String[] optional)
  {
    for (int i = 0; i < required.length; i++)
      if (!obj.containsKey(required[i])) errs.add(path + ": missing required key '" + required[i] + "'");
    Set<String> extra = new TreeSet<String>(obj.keySet());
    extra.remove("_kind");
    extra.removeAll(Arrays.asList(required));
    extra.removeAll(Arrays.asList(optional));
    if (!extra.isEmpty()) errs.add(path + " (" + obj.get("_kind") + "): unexpected keys " + extra);
  }

  /** Check key holds a string matching pattern (if given); return it */
  private static String checkStr(Map<String, Object> obj, String key, Pattern pattern, String path, List<String> errs)
  {
    Object v = obj.get(key);
    if (!(v instanceof String)) { errs.add(path + "." + key + ": must be a string"); return null; }
    if (pattern != null && !pattern.matcher((String)v).matches())
      errs.add(path + "." + key + ": '" + v + "' does not match " + pattern.pattern());
    return (String)v;
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  private static HStr s(String val) { return HStr.make(val); }

  /** Build a dict from name/value pairs */
  private static HDict dict(Object... tags)
  {
    HDictBuilder b = new HDictBuilder();
    for (int i = 0; i < tags.length; i += 2)
      b.add((String)tags[i], (HVal)tags[i + 1]);
    return b.toDict();
  }

  /** Build a grid with a fixed column order from a list of records */
  private static HGrid toGrid(HDict meta, String[] cols, Map<String, HDict> colMeta, HDict[] recs)
  {
    Set<String> colSet = new HashSet<String>(Arrays.asList(cols));
    HGridBuilder b = new HGridBuilder();
    b.meta().add(meta);
    for (int i = 0; i < cols.length; i++)
    {
      HDictBuilder cm = b.addCol(cols[i]);
      if (colMeta.containsKey(cols[i])) cm.add(colMeta.get(cols[i]));
    }
    for (int r = 0; r < recs.length; r++)
    {
      for (Iterator it = recs[r].iterator(); it.hasNext(); )
      {
        String name = (String)((Map.Entry)it.next()).getKey();
        if (!colSet.contains(name)) throw new IllegalStateException("fixture tag not in cols: " + name);
      }
      HVal[] cells = new HVal[cols.length];
      for (int c = 0; c < cols.length; c++) cells[c] = recs[r].get(cols[c], false);
      b.addRow(cells);
    }
    return b.toGrid();
  }

  /** Grid equality in both directions plus a cell-by-cell check for clearer failures */
  private static void assertGridEquals(HGrid actual, HGrid expected)
  {
    assertEquals(actual.meta(), expected.meta(), "grid meta");
    assertEquals(actual.numCols(), expected.numCols(), "numCols");
    assertEquals(actual.numRows(), expected.numRows(), "numRows");
    for (int c = 0; c < expected.numCols(); c++)
    {
      assertEquals(actual.col(c).name(), expected.col(c).name(), "col " + c);
      assertEquals(actual.col(c).meta(), expected.col(c).meta(), "col " + c + " meta");
    }
    for (int r = 0; r < expected.numRows(); r++)
      for (int c = 0; c < expected.numCols(); c++)
      {
        String name = expected.col(c).name();
        assertEquals(actual.row(r).get(name, false), expected.row(r).get(name, false), "row " + r + " " + name);
      }
    assertEquals(actual, expected);
    assertEquals(expected, actual);
  }

  /** Record which Hayson kinds appear anywhere in a parsed document */
  private static void collectKinds(Object v, Set<String> found)
  {
    if (v instanceof String)  { found.add("str"); return; }
    if (v instanceof Boolean) { found.add("bool"); return; }
    if (v instanceof Double)  { found.add("number"); return; }
    if (v instanceof List)
    {
      found.add("list");
      for (Object item : asList(v)) collectKinds(item, found);
      return;
    }
    if (!(v instanceof Map)) return;

    Map<String, Object> obj = asMap(v);
    Object kind = obj.get("_kind");
    if (kind == null || "dict".equals(kind))
    {
      found.add("dict");
      for (Map.Entry<String, Object> e : obj.entrySet())
        if (!e.getKey().equals("_kind")) collectKinds(e.getValue(), found);
      return;
    }

    String k = (String)kind;
    if (k.equals("number"))
    {
      Object val = obj.get("val");
      if (val instanceof String) found.add("number:" + val);
      if (obj.containsKey("unit")) found.add("number:unit");
    }
    else if (k.equals("dateTime")) found.add(obj.containsKey("tz") ? "dateTime:tz" : "dateTime:utc");
    else if (k.equals("xstr"))     found.add("xstr:" + obj.get("type"));
    else if (k.equals("ref"))
    {
      found.add("ref");
      if (obj.containsKey("dis")) found.add("ref:dis");
    }
    else if (k.equals("grid"))
    {
      found.add("grid");
      collectKinds(obj.get("meta"), found);
      collectKinds(obj.get("rows"), found);
    }
    else found.add(k);
  }

  private static Map<String, Object> rowById(List<Object> rows, String id)
  {
    for (int i = 0; i < rows.size(); i++)
    {
      Map<String, Object> row = asMap(rows.get(i));
      if (id.equals(refVal(asMap(row.get("id"))))) return row;
    }
    fail("no row with id " + id);
    return null;
  }

  /** Ref id, which per the spec and schema must not carry Zinc's '@' prefix */
  private static String refVal(Map<String, Object> ref)
  {
    String val = (String)ref.get("val");
    assertFalse(val.startsWith("@"), "ref val must not include '@': " + val);
    return val;
  }

  /** Parse writer output, failing with the full document on invalid JSON */
  private static Map<String, Object> parseDoc(String json)
  {
    try { return asMap(StrictJson.parse(json)); }
    catch (IllegalArgumentException e)
    {
      fail(e.getMessage() + "\n--- document ---\n" + json);
      return null;
    }
  }

  /** Parse expected JSON written with single quotes */
  private static Object j(String text) { return StrictJson.parse(text.replace('\'', '"')); }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) { return (Map<String, Object>)o; }

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object o) { return (List<Object>)o; }

  private static String join(List<String> items, String sep)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < items.size(); i++)
    {
      if (i > 0) sb.append(sep);
      sb.append(items.get(i));
    }
    return sb.toString();
  }

//////////////////////////////////////////////////////////////////////////
// StrictJson
//////////////////////////////////////////////////////////////////////////

  /**
   * Minimal RFC 8259 parser used to check writer output independently of
   * HHaysonReader.  Rejects duplicate keys, unescaped control characters,
   * malformed numbers and trailing content.  Objects become LinkedHashMap,
   * arrays ArrayList, numbers Double.
   */
  static final class StrictJson
  {
    static Object parse(String s)
    {
      StrictJson p = new StrictJson(s);
      p.ws();
      Object v = p.val();
      p.ws();
      if (p.pos != s.length()) throw p.err("trailing content");
      return v;
    }

    private StrictJson(String s) { this.s = s; }

    private Object val()
    {
      char c = peek();
      if (c == '{') return obj();
      if (c == '[') return arr();
      if (c == '"') return str();
      if (c == 't') { lit("true");  return Boolean.TRUE; }
      if (c == 'f') { lit("false"); return Boolean.FALSE; }
      if (c == 'n') { lit("null");  return null; }
      if (c == '-' || (c >= '0' && c <= '9')) return num();
      throw err("unexpected character");
    }

    private Map<String, Object> obj()
    {
      Map<String, Object> map = new LinkedHashMap<String, Object>();
      expect('{');
      ws();
      if (peek() == '}') { pos++; return map; }
      while (true)
      {
        ws();
        if (peek() != '"') throw err("expected string key");
        int keyPos = pos;
        String key = str();
        if (map.containsKey(key)) { pos = keyPos; throw err("duplicate key \"" + key + "\""); }
        ws();
        expect(':');
        ws();
        map.put(key, val());
        ws();
        if (peek() == ',') { pos++; continue; }
        expect('}');
        return map;
      }
    }

    private List<Object> arr()
    {
      List<Object> list = new ArrayList<Object>();
      expect('[');
      ws();
      if (peek() == ']') { pos++; return list; }
      while (true)
      {
        ws();
        list.add(val());
        ws();
        if (peek() == ',') { pos++; continue; }
        expect(']');
        return list;
      }
    }

    private String str()
    {
      expect('"');
      StringBuilder sb = new StringBuilder();
      while (true)
      {
        if (pos >= s.length()) throw err("unterminated string");
        char c = s.charAt(pos++);
        if (c == '"') return sb.toString();
        if (c < 0x20) { pos--; throw err("unescaped control character U+" + String.format("%04X", (int)c) + " in string"); }
        if (c != '\\') { sb.append(c); continue; }
        if (pos >= s.length()) throw err("unterminated escape");
        char e = s.charAt(pos++);
        switch (e)
        {
          case '"':  sb.append('"');  break;
          case '\\': sb.append('\\'); break;
          case '/':  sb.append('/');  break;
          case 'b':  sb.append('\b'); break;
          case 'f':  sb.append('\f'); break;
          case 'n':  sb.append('\n'); break;
          case 'r':  sb.append('\r'); break;
          case 't':  sb.append('\t'); break;
          case 'u':
            if (pos + 4 > s.length()) throw err("bad unicode escape");
            try { sb.append((char)Integer.parseInt(s.substring(pos, pos + 4), 16)); }
            catch (NumberFormatException ex) { throw err("bad unicode escape"); }
            pos += 4;
            break;
          default: pos--; throw err("invalid escape \\" + e);
        }
      }
    }

    private Double num()
    {
      Matcher m = NUMBER.matcher(s);
      m.region(pos, s.length());
      if (!m.lookingAt()) throw err("malformed number");
      pos = m.end();
      return Double.valueOf(m.group());
    }

    private void lit(String word)
    {
      if (!s.startsWith(word, pos)) throw err("expected " + word);
      pos += word.length();
    }

    private void expect(char c)
    {
      if (peek() != c) throw err("expected '" + c + "'");
      pos++;
    }

    private char peek() { return pos < s.length() ? s.charAt(pos) : '\0'; }

    private void ws()
    {
      while (pos < s.length())
      {
        char c = s.charAt(pos);
        if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
        pos++;
      }
    }

    private IllegalArgumentException err(String msg)
    {
      int from = Math.max(0, pos - 30);
      int to   = Math.min(s.length(), pos + 30);
      String near = s.substring(from, to).replace("\n", "\\n");
      return new IllegalArgumentException("Invalid JSON at offset " + pos + ": " + msg + " near: ..." + near + "...");
    }

    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

    private final String s;
    private int pos;
  }
}
