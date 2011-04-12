//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.util.json;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 *
 * @author Ivan Mushketik
 */
public class JSONParserTest extends TestJPF {

  @Test
  public void testOneLevelJSON() {
    String json = "{"
            + "\"key1\" : \"str\","
            + "\"key2\" : 123"
            + "}";
    JSONObject o = parseJSON(json);

    Value key1 = o.getValue("key1");
    assert key1.getString().equals("str");
    Value key2 = o.getValue("key2");
    assert key2.getDouble() == 123;
  }

  private JSONObject parseJSON(String json) {
    JSONLexer lexer = new JSONLexer(json);
    JSONParser parser = new JSONParser(lexer);
    JSONObject o = parser.parse();
    return o;
  }

  @Test
  public void testEmptyObject() {
    String json = "{}";
    JSONObject o = parseJSON(json);

    assert o.getValue("noValue") == null;
  }

  @Test
  public void testArrayParse() {
    String json = "{"
            + "\"key\" : ["
            + "{ \"key1\" : 123 },"
            + "{ \"key2\" : \"str\" } ]"
            + "}";
    JSONObject o = parseJSON(json);

    JSONObject objects[] = o.getArray("key");
    assert objects[0].getValue("key1").getDouble() == 123;
    assert objects[1].getValue("key2").getString().equals("str");
  }

  @Test
  public void testEmptyArray() {
    String json = "{ \"emptyArr\" : [] }";
    JSONObject o = parseJSON(json);

    assert o.getArray("noArray") == null;
  }

  @Test
  public void testIdentifacatorsParsing() {
    String json = "{ "
            + "\"Null\" : null,"
            + "\"True\" : true,"
            + "\"False\": false"
            + "}";
    JSONObject o = parseJSON(json);

    assert o.getValue("Null").getString() == null;
    assert o.getValue("True").getBoolean() == true;
    assert o.getValue("False").getBoolean() == false;
  }
}
