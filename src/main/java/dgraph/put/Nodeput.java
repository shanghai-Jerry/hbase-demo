package dgraph.put;


import java.util.List;

public class Nodeput {

  String uid;

  String uniqueId;

  List<String> predicates;

  List<Object> valueObjects;

  public List<Object> getValueObjects() {
    return valueObjects;
  }

  public void setValueObjects(List<Object> valueObjects) {
    this.valueObjects = valueObjects;
  }

  public List<String> getPredicates() {
    return predicates;
  }

  public void setPredicates(List<String> predicates) {
    this.predicates = predicates;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

}
