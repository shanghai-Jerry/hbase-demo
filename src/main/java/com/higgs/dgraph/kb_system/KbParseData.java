package com.higgs.dgraph.kb_system;

import com.csvreader.CsvReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import io.vertx.core.json.JsonObject;

/**
 * User: JerryYou
 *
 * Date: 2019-08-28
 *
 * Copyright (c) 2018 devops
 *
 * <<licensetext>>
 */
public class KbParseData {

  static Logger logger = LoggerFactory.getLogger(KbParseData.class);

  static public Reader getReader(String relativePath) throws FileNotFoundException {
    try {
      return new InputStreamReader(new FileInputStream(relativePath),  "utf-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return null;
  }

  // data format: id,entity-name
  static public void parseEntity(List<JsonObject> items, String entityType, String path) {
    try {
      CsvReader csvReader = new CsvReader(getReader(path));
      while(csvReader.readRecord()) {
        String [] values = csvReader.getValues();
        if (values.length != 2) {
          continue;
        }
        JsonObject json = new JsonObject();
        json.put("id", Long.parseLong(values[0]))
            .put("name", values[1].toLowerCase())
            .put("type", entityType)
            ;
        items.add(json);
      }
    } catch (FileNotFoundException e) {
      logger.info("[NoFile] =>" + e.getMessage());
    } catch (IOException e) {
      logger.info("[IOExp] =>" + e.getMessage());
    }

  }
  // data format: entity-name, attr_values separated by ','
  static  public void parseAttribute(List<JsonObject> items, String path) {
    try {
      CsvReader csvReader = new CsvReader(getReader(path));
      while(csvReader.readRecord()) {
        String [] values = csvReader.getValues();
        if (values.length != 3) {
          continue;
        }
        JsonObject json = new JsonObject();
        String attr = values[1];
        json.put("name", values[0].toLowerCase())
            .put("attribute_value", attr)
        ;
        if ("属性值类型".equals(attr)) {
          continue;
        }
        items.add(json);
      }
    } catch (FileNotFoundException e) {
      logger.info("[NoFile] =>" + e.getMessage());
    } catch (IOException e) {
      logger.info("[IOExp] =>" + e.getMessage());
    }
  }

  // data format: src,dest,rel_type_int_value,weight
  static  public void parseRelations(List<JsonObject> items, String path) {
    try {
      CsvReader csvReader = new CsvReader(getReader(path));
      while(csvReader.readRecord()) {
        String [] values = csvReader.getValues();
        if (values.length != 4) {
          continue;
        }
        JsonObject json = new JsonObject();
        json.put("in_value", values[0].toLowerCase())
            .put("out_value", values[1].toLowerCase())
            .put("rel_type", Integer.parseInt(values[2]))
            .put("weight", Double.valueOf(values[3]))
        ;
        items.add(json);
      }
    } catch (FileNotFoundException e) {
      logger.info("[NoFile] =>" + e.getMessage());
    } catch (IOException e) {
      logger.info("[IOExp] =>" + e.getMessage());
    }
  }

  // data format: entity-name, attr_values separated by ','
  static  public void parseRelationsInAttribute(List<JsonObject> items,
                                                        String inRel,
                                                        String outRel,
                                                        String path) {
    try {
      CsvReader csvReader = new CsvReader(getReader(path));
      while(csvReader.readRecord()) {
        String [] values = csvReader.getValues();
        if (values.length != 3) {
          continue;
        }
        String attrString = values[1];
        if (attrString.isEmpty()) {
          continue;
        }
        String[] attrs = attrString.split(",");
        for (String att : attrs) {
          JsonObject json = new JsonObject();
          if ("属性值类型".equals(att)) {
            continue;
          }
          json.put("in_value", values[0].toLowerCase())
              .put("in", inRel)
              .put("out", outRel)
              .put("out_value", att.toLowerCase())
          ;
          items.add(json);
        }
      }
    } catch (FileNotFoundException e) {
      logger.info("[NoFile] =>" + e.getMessage());
    } catch (IOException e) {
      logger.info("[IOExp] =>" + e.getMessage());
    }
  }

}
