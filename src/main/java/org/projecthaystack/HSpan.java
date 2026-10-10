//
// Copyright (c) 2024, Richard McElhinney
// Licensed under the Academic Free License version 3.0
//
// History:
//   09 Mar 2024  Richard McElhinney  Creation
//
package org.projecthaystack;

/**
 * HSpan models a date or datetime range as an immutable value.
 * It is encoded in Hayson as an XStr with type "Span".
 *
 * @see <a href='https://project-haystack.org/doc/docHaystack/Spans'>Project Haystack | Spans</a>
 */
public class HSpan extends HVal
{
  /** Make a date span from start to end (inclusive) */
  public static HSpan make(HDate start, HDate end)
  {
    if (start == null || end == null) throw new IllegalArgumentException("null args");
    return new HSpan(start, end, start.toString() + "," + end.toString());
  }

  /** Make a datetime span from start to end */
  public static HSpan make(HDateTime start, HDateTime end)
  {
    if (start == null || end == null) throw new IllegalArgumentException("null args");
    return new HSpan(start, end, start.toZinc() + "," + end.toZinc());
  }

  /**
   * Parse the "start,end" XStr encoding produced by the make factories.
   * Both ends must be dates or both datetimes; throw ParseException otherwise.
   */
  public static HSpan make(String val) throws Exception
  {
    int comma = val.indexOf(',');
    if (comma < 0 || val.indexOf(',', comma + 1) >= 0) 
      throw new ParseException("Invalid span: " + val);

    String start = val.substring(0, comma);
    String end   = val.substring(comma + 1);
    boolean startIsDate = start.indexOf('T') < 0;
    boolean endIsDate   = end.indexOf('T') < 0;

    if (startIsDate != endIsDate) 
      throw new ParseException("Mixed date and datetime span: " + val);

    try
    {
      if (startIsDate) return make(HDate.make(start), HDate.make(end));
      return make(HDateTime.make(start), HDateTime.make(end));
    }
    catch (ParseException e) { throw e; }
    catch (RuntimeException e) { throw new ParseException("Invalid span: " + val); }
  }

  private HSpan(HVal start, HVal end, String val)
  {
    this.start = start;
    this.end   = end;
    this.val   = val;
  }

  /** Start of the span (HDate or HDateTime) */
  public final HVal start;

  /** End of the span (HDate or HDateTime) */
  public final HVal end;

  /** Inner string representation used for XStr encoding */
  public final String val;

  /** Encode as {@code Span("start,end")} */
  public String toZinc()
  {
    return "Span(\"" + val + "\")";
  }

  /** Not supported in legacy JSON encoding */
  public String toJson()
  {
    throw new UnsupportedOperationException();
  }

  /** Hash is based on val string */
  public int hashCode() { return val.hashCode(); }

  /** Equals is based on val string */
  public boolean equals(Object that)
  {
    if (!(that instanceof HSpan)) return false;
    return val.equals(((HSpan)that).val);
  }
}
