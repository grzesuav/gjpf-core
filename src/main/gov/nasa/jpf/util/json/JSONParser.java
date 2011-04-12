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

import gov.nasa.jpf.JPFException;
import java.util.ArrayList;

/**
 * JSON parser. Read tokenized stream from JSONTokenizer and returns root JSON
 * node.
 * @author Ivan Mushketik
 */
public class JSONParser {

  JSONLexer lexer;
  // Last token returned by lexer
  Token lastReadToken;
  // true if parser bactracked to previous token
  boolean backtrack;

  public JSONParser(JSONLexer lexer) {
    this.lexer = lexer;
  }

  /**
   * Parse JSON document
   * @return root node of JSON tree.
   */
  public JSONObject parse() {
    return parseObject();
  }

  /**
   * Read next token from lexer output stream. If parser backtraced return previously
   * read token
   * @return
   */
  private Token next() {
    if (lastReadToken != null && lastReadToken.getType() == Token.Type.DocumentEnd) {
      return lastReadToken;
    }

    if (backtrack) {
      backtrack = false;
      return lastReadToken;
    }

    lastReadToken = lexer.getNextToken();

    return lastReadToken;
  }

  /**
   * Backtrack to previous token
   */
  private void back() {
    if (backtrack == true) {
      throw new JPFException("Attempt to bactrack twice. Posibly an error. Please report");
    }

    if (lastReadToken == null) {
      throw new JPFException("Attempt to backtrack before starting to read token stream. Please report");
    }

    backtrack = true;
  }

  /**
   * Read next token and check it's type. If type is wrong method throws exception
   * else it returns read token
   * @param type - type of the following token.
   * @return read token if it has correct type
   */
  private Token consume(Token.Type type) {
    Token t = next();

    if (t.getType() != type) {
      throw new JPFException("Unexpected token " + t + " expected " + type);
    }

    return t;
  }

  /**
   * Parse JSON object
   * @return
   */
  private JSONObject parseObject() {
    JSONObject pn = new JSONObject();
    consume(Token.Type.ObjectStart);  
    Token t = next();

    // Check if object is empty
    if (t.getType() != Token.Type.ObjectEnd) {
      back();
      while (true) {
        Token key = consume(Token.Type.String);
        consume(Token.Type.KeyValueSeparator);

        t = next();
        switch (t.getType()) {
          case Number:
            pn.addValue(key.getValue(), new DoubleValue(t.getValue()));
            break;

          case String:
            pn.addValue(key.getValue(), new StringValue(t.getValue()));
            break;

          case ArrayStart:
            back();
            pn.addArray(key.getValue(), parseArray());
            break;

          case ObjectStart:
            back();
            parseObject();
            break;

          case Identificator:
            back();
            pn.addValue(key.getValue(), parseIdentificator());
            break;

          default:
            error("Unexpected token");
            break;
        }
        

        t = next();
        // If next token is comma there is one more key-value pair to read
        if (t.getType() != Token.Type.Comma) {
          back();
          break;
        }
      }
      consume(Token.Type.ObjectEnd);
    }
    return pn;
  }

  /**
   * Parse array of JSON objects
   * @return parsed array of JSON objects
   */
  private JSONObject[] parseArray() {
    consume(Token.Type.ArrayStart);
    ArrayList<JSONObject> arrList = new ArrayList<JSONObject>();
    Token t = next();
    if (t.getType() != Token.Type.ArrayEnd) {
      back();
      while (true) {
        JSONObject n = parseObject();
        arrList.add(n);

        t = next();
        // If next token is comma there is one more object to parse
        if (t.getType() != Token.Type.Comma) {
          back();
          break;
        }
      }
    }
    else {
      back();
    }
    consume(Token.Type.ArrayEnd);

    JSONObject nodes[] = new JSONObject[arrList.size()];
    return arrList.toArray(nodes);
  }

  /**
   * Parse identifier. Identifier can be "null", "true" or "false"
   * @return appropriate value object
   */
  // <2do> add ChoiseGenerator call
  private Value parseIdentificator() {
    Token id = consume(Token.Type.Identificator);

    String val = id.getValue();
    if (val.equals("true")) {
      return new BooleanValue(true);
    }
    else if (val.equals("false")) {
      return new BooleanValue(false);
    }
    else if (val.equals("null")) {
      return new NullValue();
    }

    error("Unknown identifier");
    return null;
  }

  // <2do> add explainable error
  private void error(String string) {
    throw new JPFException(string);
  }
}
