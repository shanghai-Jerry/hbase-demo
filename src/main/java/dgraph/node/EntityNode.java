package dgraph.node;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import utils.util;

/**
 * User: JerryYou
 *
 * Date: 2018-05-08
 *
 * Copyright (c) 2018 devops
 *
 * 入库： 分两种形式 1. with json object 2. with rdf <uid_1>  <predicate> <uid_2>
 *
 * 多种形式的插入
 *
 * <0xd60> <friend> <0xd57> . _:id <friend> <0xd57> . _:id <name> "value1" . _:id <age> "value2" .
 * 注：value 为int等类型都需""，具体类型转换由schema的类型定义决定
 *
 * <<licensetext>>
 */
public class EntityNode implements Serializable {

  String uid;
  // _:uniqueId <name> value
  // 检查是否存在实体的唯一标识
  List<String> unique_ids;
  String unique_id;

  // 实体名称
  String name;
  // 实体类型
  String type;
  // 实体类别
  Label has_label;

  String label_name;

  public String getUnique_id() {
    return unique_id;
  }

  public void setUnique_id(String unique_id) {
    this.unique_id = unique_id;
  }

  public List<String> getUnique_ids() {
    return unique_ids;
  }

  public void setUnique_ids(List<String> unique_ids) {
    this.unique_ids = unique_ids;
  }

  public String getLabel_name() {
    return label_name;
  }

  public void setLabel_name(String label_name) {
    this.label_name = label_name;
  }

  public Label getHas_label() {
    return has_label;
  }

  public void setHas_label(Label has_label) {
    this.has_label = has_label;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getDeclaredEdgeUid(Object object, Class clazz, String methodName) {
    String ret = "";
    try {
      Method[] methods = clazz.getMethods();
      for (Method method : methods) {
        if (methodName.equals(method.getName())) {
          Object result = method.invoke(object);
          return (String) result;
        }
      }
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
    return ret;

  }

  /**
   * all fileld include sub EntityNode's uid
   * @param object
   * @param clazz
   * @param pre
   * @param values
   * @param edges
   * @param ids
   * @param methodName
   */
  public void getDeclaredFields(Object object, Class clazz, List<String> pre, List<Object>
      values, List<String> edges, List<String> ids, String methodName) {
    Field[] fields = clazz.getDeclaredFields();
    Field.setAccessible(fields, true);
    for (Field field : fields) {
      try {
        String name = field.getName();
        Object value = field.get(object);
        util.println("name:", name);
        util.println("value:", value);
        // .. todo , 如果实体之前的关系通过List<EntityNode>的方式存在，那么以rdf方式如何建立实体之前的关系
        // .. todo, 以json object的方式写入只需要保证子实体的uid写回，无需进行获取uid进行绑定
        if (value instanceof EntityNode) {
          // 绑定单个实体之间的关系
          if (!"".equals(methodName)) {
            util.println("  edge:", name);
            String uid = getDeclaredEdgeUid(value, value.getClass(), methodName);
            util.println("  uid:", uid);
            util.println("  value:", value);
            if (uid != null && !"".equals(uid)) {
              edges.add(name);
              ids.add(uid);
            }
          }
        } else if (value instanceof List) {
          if (((List) value).size() > 0) {
            if (((List) value).get(0) instanceof String) {
              continue;
            } else if (((List) value).get(0) instanceof  EntityNode ){
              // 绑定多个实体之间的关系
              List<EntityNode> entityNodes = (List<EntityNode>)value;
              for (EntityNode entityNode: entityNodes) {
                String uid = getDeclaredEdgeUid(entityNode, entityNode.getClass(), methodName);
                util.println("  list uid:", uid);
                util.println("  list value:", entityNode);
                if (uid != null && !"".equals(uid)) {
                  edges.add(name);
                  ids.add(uid);
                }
              }
            }
          }
        } else {
          // 一般属性值
          if ("uid".equals(name)) {
            continue;
          }
          util.println("  other else:", name);
          if (value != null) {
            util.println("  name:", name);
            util.println("  att value:", value);
            pre.add(name);
            values.add(value);
          }
        }
      } catch (IllegalAccessException e) {
      }
    }
  }

  /**
   * @param pre
   * @param values
   * @param edges
   * @param ids
   */
  public void getValueMap(List<String> pre, List<Object> values, List<String> edges, List<String>
      ids, String methodName) {
    getDeclaredFields(this, this.getClass(), pre, values, edges, ids, methodName);
    getDeclaredFields(this, this.getClass().getSuperclass(), pre, values, edges, ids, methodName);

  }

  @Deprecated
  public void getAttrValueMap(List<String> pre, List<Object> values) {
    getDeclaredFields(this, this.getClass(), pre, values, new ArrayList<String>(), new
        ArrayList<String>(), "");
    getDeclaredFields(this, this.getClass().getSuperclass(), pre, values, new ArrayList<String>()
        , new ArrayList<String>(), "");
  }

  @Deprecated
  public void getEdgeValueMap(List<String> edges, List<String> ids, String methodName) {
    getDeclaredFields(this, this.getClass(), new ArrayList<String>(), new ArrayList<Object>(),
        edges, ids, methodName);
    System.out.println("##### super class :");
    getDeclaredFields(this, this.getClass().getSuperclass(), new ArrayList<String>(), new
        ArrayList<Object>(), edges, ids, methodName);
  }

}
