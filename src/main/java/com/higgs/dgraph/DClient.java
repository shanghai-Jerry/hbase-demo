package com.higgs.dgraph;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.JsonFormat;

import com.higgs.client.dgrpah.DgraphClient;
import com.higgs.client.dgrpah.TxnConflictException;
import com.higgs.dgraph.del.NodeDel;
import com.higgs.dgraph.node.EntityNode;
import com.higgs.dgraph.put.EdgeFacetPut;
import com.higgs.dgraph.put.EdgeFacetsPut;
import com.higgs.dgraph.put.Nodeput;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.dgraph.bigchange.DgraphGrpc;
import io.dgraph.bigchange.DgraphProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * User: JerryYou
 *
 * Date: 2018-05-08
 *
 * Copyright (c) 2018 devops
 *
 * 基于DgraphClient上的另一个逻辑实现
 *
 * _STAR_ALL : *
 *
 * <<licensetext>>
 */
public class  DClient {

  private static final Logger logger = LoggerFactory.getLogger(DClient.class);

  private static JsonFormat.Parser parser = JsonFormat.parser();

  public DgraphClient getDgraphClient() {
    return dgraphClient;
  }

  private DgraphClient dgraphClient;

  private static  int deadlineSecs = 300;
  private int retryCompensation = 5;
  // deadline exceed retry max number default 5
  @Deprecated
  private int retryMaxNumber = 5;

  private AtomicInteger retryCounter = new AtomicInteger(0);

  public DClient(List<String> adressList) {
    List<DgraphGrpc.DgraphBlockingStub> clients = new ArrayList<>();
    for (String address : adressList) {
      String[] hosts = address.split(":");
      ManagedChannel channel = ManagedChannelBuilder.forAddress(hosts[0], Integer.parseInt
          (hosts[1])).usePlaintext(true).build();
      DgraphGrpc.DgraphBlockingStub blockingStub = DgraphGrpc.newBlockingStub(channel);
      clients.add(blockingStub);
    }
    dgraphClient = new DgraphClient(clients, deadlineSecs);
  }

  public DClient(String[] adressList) {
    List<DgraphGrpc.DgraphBlockingStub> clients = new ArrayList<>();
    for (String address : adressList) {
      String[] hosts = address.split(":");
      ManagedChannel channel = ManagedChannelBuilder.forAddress(hosts[0], Integer.parseInt
          (hosts[1])).usePlaintext(true).build();
      DgraphGrpc.DgraphBlockingStub blockingStub = DgraphGrpc.newBlockingStub(channel);
      clients.add(blockingStub);
    }
    dgraphClient = new DgraphClient(clients, deadlineSecs);
  }

  /**
   * 删除schema
   */
  public void dropSchema() {
    // Initialize
    dgraphClient.alter(DgraphProto.Operation.newBuilder().setDropAll(true).build());
  }

  /**
   * 修改schema
   * @param schema schema
   */
  public void alterSchema(String schema) {
    DgraphProto.Operation op = DgraphProto.Operation.newBuilder().setSchema(schema).build();
    dgraphClient.alter(op);
  }

  private String edgeDelAll(String src) {
    return String.format("<%s> * * . \n", src);
  }

  /**
   * 批量<uid> <relation> <uid>的方式format
   * @param src start uid
   * @param pred relation
   * @param dest end uid
   * @param isExistUid src是否是uid
   * @return format string
   */
  private String edgeFormat(String src, String pred, String dest, boolean isExistUid) {
    if (isExistUid) {
      return String.format("<%s> <%s> <%s> . \n", src, pred, dest);
    } else {
      return String.format("_:%s <%s> <%s> . \n", src, pred, dest);
    }
  }

  /**
   * 批量<uid> <relation> "attribute"的方式format
   * @param src start uid
   * @param pred relation
   * @param dest end uid
   * @param isExistUid src是否是uid
   * @return format string
   */
  private String attrFormat(String src, String pred, String dest, boolean isExistUid) {
    if (isExistUid) {
      return String.format("<%s> <%s> \"%s\" . \n", src, pred, dest);
    } else {
      return String.format("_:%s <%s> \"%s\" . \n", src, pred, dest);
    }
  }

  /**
   * 增加边的facets
   * @param src start uid or unique_id
   * @param pred relation
   * @param dest end uid
   * @param facets 边的属性集合
   * @return Nquad string
   */
  private String edgeUidFacetsFormat(String src, String pred, String dest, List<String> facets,
                                  boolean isExistUid) {
    StringBuilder stringBuilder = new StringBuilder();
    int facetSize = facets.size();
    for (int i = 0; i < facetSize; i++) {
      if (i == 0) {
        stringBuilder.append(facets.get(i));
      } else {
        stringBuilder.append("," + facets.get(i));
      }
    }
    if (isExistUid) {
      // uid
      return String.format("<%s> <%s> <%s> (%s) . \n", src, pred, dest, stringBuilder.toString());
    } else {
      // unique_id
      return String.format("_:%s <%s> <%s> (%s) . \n", src, pred, dest, stringBuilder.toString());
    }
  }

  /**
   * 增加边的facets
   * @param src start uid or unique_id
   * @param pred relation
   * @param dest end uid
   * @param facets 边的属性集合
   * @return Nquad string
   */
  private String edgeAttributeFacetsFormat(String src, String pred, String dest, List<String>
      facets, boolean isExistUid) {
    StringBuilder stringBuilder = new StringBuilder();
    int facetSize = facets.size();
    for (int i = 0; i < facetSize; i++) {
      if (i == 0) {
        stringBuilder.append(facets.get(i));
      } else {
        stringBuilder.append("," + facets.get(i));
      }
    }
    if (isExistUid) {
      // uid
      return String.format("<%s> <%s> %s (%s) . \n", src, pred, dest, stringBuilder.toString());
    } else {
      // unique_id
      return String.format("_:%s <%s> %s (%s) . \n", src, pred, dest, stringBuilder.toString());
    }
  }

  public List<String> getAddUidFacet(List<EdgeFacetPut> edgeFacetPutList) {
    List<String> stringList = new ArrayList<>();
    for (EdgeFacetPut edgeFacetPut : edgeFacetPutList) {
      String src = edgeFacetPut.getUidSrc();
      String dst = edgeFacetPut.getDst();
      String predicate = edgeFacetPut.getPredicate();
      EdgeFacetPut.PredicateType predicateType = edgeFacetPut.getPredicateType();
      List<String> facet = edgeFacetPut.getFacets();
      String result = "";
      if (src == null || "".equals(src)) {
        continue;
      }
      if (src.startsWith("0x")) {
        switch (predicateType) {
          case UID:
            result = edgeUidFacetsFormat(src, predicate, dst, facet, true);
            break;
          case ATTRIBUTE:
            result = edgeAttributeFacetsFormat(src, predicate, dst, facet, true);
            break;
        }
      } else if (src != null && !"".equals(src)) {
        switch (predicateType) {
          case UID:
            result = edgeUidFacetsFormat(src, predicate, dst, facet, false);
            break;
          case ATTRIBUTE:
            result = edgeAttributeFacetsFormat(src, predicate, dst, facet, false);
            break;
        }
      }
      if (!"".equals(result)) {
        stringList.add(result);
      }

    }
    return stringList;
  }

  public List<String> getAddFacet(List<EdgeFacetPut> edgeFacetPutList) {
    List<String> stringList = new ArrayList<>();
    for (EdgeFacetPut edgeFacetPut : edgeFacetPutList) {
      String src = edgeFacetPut.getSrc();
      String dst = edgeFacetPut.getDst();
      String predicate = edgeFacetPut.getPredicate();
      EdgeFacetPut.PredicateType predicateType = edgeFacetPut.getPredicateType();
      List<String> facet = edgeFacetPut.getFacets();
      String result = "";
      if (src == null || "".equals(src)) {
        continue;
      }
      if (src.startsWith("0x")) {
        switch (predicateType) {
          case UID:
            result = edgeUidFacetsFormat(src, predicate, dst, facet, true);
            break;
          case ATTRIBUTE:
            result = edgeAttributeFacetsFormat(src, predicate, dst, facet, true);
            break;
        }
      } else if (src != null && !"".equals(src)) {
        switch (predicateType) {
          case UID:
            result = edgeUidFacetsFormat(src, predicate, dst, facet, false);
            break;
          case ATTRIBUTE:
            result = edgeAttributeFacetsFormat(src, predicate, dst, facet, false);
            break;
        }
      }
      if (!"".equals(result)) {
        stringList.add(result);
      }

    }
    return stringList;
  }

  public List<String> getAddFacets(List<EdgeFacetsPut> edgeFacetsPutList) {
    List<String> stringList = new ArrayList<>();
    for (EdgeFacetsPut edgeFacetsPut : edgeFacetsPutList) {
      List<String> srcs = edgeFacetsPut.getSrcs();
      List<String> dsts = edgeFacetsPut.getDst();
      List<String> predicates = edgeFacetsPut.getPredicates();
      List<EdgeFacetsPut.PredicateType> predicateTypes = edgeFacetsPut.getPredicateTypes();
      List<List<String>> facets = edgeFacetsPut.getFacets();
      int size = srcs.size();
      for (int i = 0; i < size; i++) {
        String src = srcs.get(i);
        String dst = dsts.get(i);
        String predicate = predicates.get(i);
        EdgeFacetsPut.PredicateType predicateType = predicateTypes.get(i);
        List<String> facet = facets.get(i);
        String result = "";
        if (src.startsWith("0x")) {
          switch (predicateType) {
            case UID:
              result = edgeUidFacetsFormat(src, predicate, dst, facet, true);
              break;
            case ATTRIBUTE:
              result = edgeAttributeFacetsFormat(src, predicate, dst, facet, true);
              break;
          }
        } else if (src != null && !"".equals(src)) {
          switch (predicateType) {
            case UID:
              result = edgeUidFacetsFormat(src, predicate, dst, facet, false);
              break;
            case ATTRIBUTE:
              result = edgeAttributeFacetsFormat(src, predicate, dst, facet, false);
              break;
          }
        }
        if (!"".equals(result)) {
          stringList.add(result);
        }
      }
    }
    return stringList;
  }

  /**
   * 形式: <uid> <> <> (key=value,...)
   * @param edgeFacetPutList
   */
  public void entityAddFacets(List<EdgeFacetPut> edgeFacetPutList) {
    List<String> stringList = getAddUidFacet(edgeFacetPutList);
    if (stringList.size() > 0) {
      multiplyEdgesMutation(stringList, false);
    }
  }

  /**
   * 形式: <uid> * * .
   * @param nodeDelList  node 属性的上一层抽象
   */
  public void entityDel(List<NodeDel> nodeDelList) {
    List<String> stringList = new ArrayList<>();
    for (NodeDel nodeDel : nodeDelList) {
      String uid = nodeDel.getUid();
      if (uid == null || "".equals(uid)) {
        continue;
      }
      String result = edgeDelAll(uid);
      stringList.add(result);
    }
    if (stringList.size() > 0) {
      multiplyEdgesDeleteMutation(stringList);
    }
  }

  /**
   * 形式: <uid> <> <>
   * 优化： 应对value的特殊字符，此接口比:entityAdd(List<Nodeput> putList)更通用
   */
  public DgraphProto.Assigned entityAddWithUid(List<Nodeput> putList) {
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    int ids = putList.size();
    List<DgraphProto.NQuad> quads = new ArrayList<DgraphProto.NQuad>();
    for (int j = 0; j < ids; j++) {
      String uid = putList.get(j).getUid();
      List<String> predicates = putList.get(j).getPredicates();
      List<Object> values = putList.get(j).getValueObjects();
      List<String> edge_pred = putList.get(j).getEdge_predicates();
      List<String> objectIds = putList.get(j).getObjectIds();
      int size = predicates.size();
      if (size != values.size() || edge_pred.size() != objectIds.size()) {
        logger.fatal("entity add with uid predicates length not equal values ");
      }
      if (uid == null || "".equals(uid)) {
        continue;
      }
      for (int i = 0; i < size; i++) {
        DgraphProto.NQuad.Builder builder =
            DgraphProto.NQuad.newBuilder()
                .setSubject(uid)
                .setPredicate(predicates.get(i));

        Object value = values.get(i);
        if (value instanceof Integer || value instanceof Long) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setIntVal(Long.valueOf(value
              .toString())).build());
        } else if (value instanceof String) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setStrVal((String) value).build());
        } else if (value instanceof Double || value instanceof Float) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setDoubleVal(Double.valueOf(value
              .toString())).build());
        } else if (value instanceof Boolean) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setBoolVal((Boolean) value).build());
        } else if (value == null) {
          // field 没有设置属性的过滤
          continue;
        } else {
          // 需要处理predicate为uid类型，... todo
          continue;
        }
        quads.add(builder.build());
      }
      // edge feed
      for (int k = 0; k < edge_pred.size(); k++) {
        DgraphProto.NQuad quad = DgraphProto.NQuad.newBuilder()
            .setSubject(uid)
            .setPredicate(edge_pred.get(k))
            .setObjectId(objectIds.get(k))
            .build();
        quads.add(quad);
      }
    }
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder()
        .addAllSet(quads)
        .build();
    DgraphProto.Assigned ag = null;
    try {
      ag = txn.mutate(mu);
      txn.commit();
    } catch (Exception e) {
      logger.info("[entity add with uid Exception]:" + e.getMessage() + ", exception:" + e +
          ", retryCounter:" + retryCounter.get());
      if (e instanceof StatusRuntimeException) {
        int code = ((StatusRuntimeException) e).getStatus().getCode().value();
        logger.info("[StatusRuntimeException code value]:" + code);
      }
      ag = mutateRetry(mu, e);
    } finally {
      txn.discard();
    }
    if (ag == null) {
      logger.info("[Final] Retry Error!!");
    }
    return ag;
  }

  /**
   * 形式: <uid> <> <>
   * @param putList  node 属性的上一层抽象
   */
  /**
   * 形式: <uid> <> <>
   */
  public DgraphProto.Assigned entityAdd(List<Nodeput> putList) {
    int ids = putList.size();
    StringBuffer stringBuffer = new StringBuffer();
    List<String> stringList = new ArrayList<>();
    for (int j = 0; j < ids; j++) {
      Nodeput nodeput = putList.get(j);
      String uid = nodeput.getUid();
      List<String> predicates = nodeput.getPredicates();
      List<Object> values = nodeput.getValueObjects();
      List<String> edge_pred = nodeput.getEdge_predicates();
      List<String> objectIds = nodeput.getObjectIds();
      if (edge_pred.size() != objectIds.size()) {
        logger.fatal("entity add predicates edge length not equal values ");
      }
      int size = predicates.size();
      if (size != values.size()) {
        logger.fatal("entity add predicates attr length not equal values ");
      }
      for (int i = 0; i < size; i++) {
        String value = String.valueOf(values.get(i).toString());
        String pred = predicates.get(i);
        String result = attrFormat(uid, pred, value, true);
        stringBuffer.append(result);
        stringList.add(result);
      }
      // edge feed
      for (int k = 0; k < edge_pred.size(); k++) {
        String edgePredicate = edge_pred.get(k);
        String objectId = objectIds.get(k);
        String result = edgeFormat(uid, edgePredicate, objectId, true);
        stringBuffer.append(result);
        stringList.add(result);
      }
    }
    DgraphProto.Assigned ag = null;
    if (stringList.size() > 0) {
      ag = multiplyEdgesMutation(stringList, true);
    }
    return ag;
  }

  /**
   * 形式: <_:id> <> <>
   * rdf 格式
   * @param putList node 属性的上一层抽象
   * @return rdf 形式的实体插入
   */
  @Deprecated
  public DgraphProto.Assigned newEntityInitial(List<Nodeput> putList, List<EdgeFacetPut> edgeFacetsPutList) {
    List<String> stringList = new ArrayList<>();
    for (Nodeput nodeput : putList) {
      String uniqueId = nodeput.getUniqueId();
      List<String> predicates = nodeput.getPredicates();
      List<Object> values = nodeput.getValueObjects();
      List<String> edge_pred = nodeput.getEdge_predicates();
      List<String> objectIds = nodeput.getObjectIds();
      int size = predicates.size();
      if (size != values.size() || edge_pred.size() != objectIds.size()) {
        logger.fatal("entity initial predicates length not equal values ");
      }
      // value feed : value can not be uid in here
      for (int i = 0; i < size; i++) {
        String pred = predicates.get(i);
        Object value = values.get(i);
        String result = attrFormat(uniqueId, pred, value.toString(),false);
        stringList.add(result);
      }
      // edge feed
      for (int k = 0; k < edge_pred.size(); k++) {
        String edgePredicate = edge_pred.get(k);
        String objectId = objectIds.get(k);
        String result = edgeFormat(uniqueId, edgePredicate, objectId,false);
        stringList.add(result);
      }
    }
    // ..todo 容易因存在特殊字符导致异常的发生
    if (edgeFacetsPutList.size() > 0) {
      stringList.addAll(getAddFacet(edgeFacetsPutList));
    }
    DgraphProto.Assigned ag;
    ag = multiplyEdgesMutation(stringList, false);
    return ag;
  }

  /**
   * 形式: <_:id> <> <>
   * rdf 格式
   * @param putList node 属性的上一层抽象
   * @return rdf 形式的实体插入
   */
  public DgraphProto.Assigned entityInitial(List<Nodeput> putList) {
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    List<DgraphProto.NQuad> quads = new ArrayList<>();
    for (Nodeput nodeput : putList) {
      String uniqueId = nodeput.getUniqueId();
      List<String> predicates = nodeput.getPredicates();
      List<Object> values = nodeput.getValueObjects();
      List<String> edge_pred = nodeput.getEdge_predicates();
      List<String> objectIds = nodeput.getObjectIds();
      int size = predicates.size();
      if (size != values.size() || edge_pred.size() != objectIds.size()) {
        logger.fatal("entity inital predicates length not equal values ");
      }
      // value feed : value can not be uid in here
      for (int i = 0; i < size; i++) {
        String pred = predicates.get(i);
        Object value = values.get(i);
        // logger.info("uniqueId:" + uniqueId+ ", pred:" + pred + ", valueObject:" + value);
        DgraphProto.NQuad.Builder builder = DgraphProto.NQuad.newBuilder().setSubject(String
            .format("_:%s", uniqueId)).setPredicate(pred);
        if (value instanceof Integer || value instanceof Long) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setIntVal(Long.valueOf(value
              .toString())).build());
        } else if (value instanceof String) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setStrVal((String) value).build());
        } else if (value instanceof Double || value instanceof Float) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setDoubleVal(Double.valueOf(value
              .toString())).build());
        } else if (value instanceof Boolean) {
          builder.setObjectValue(DgraphProto.Value.newBuilder().setBoolVal((Boolean) value).build
              ());
        } else if (value == null) {
          // field 没有设置属性的过滤
          continue;
        } else {
          // 需要处理predicate为uid类型，... 见 edge feed
          continue;
        }
        quads.add(builder.build());
      }
      // edge feed
      for (int k = 0; k < edge_pred.size(); k++) {
        String edgePredicate = edge_pred.get(k);
        String objectId = objectIds.get(k);
        // logger.info("edge pred:" + edgePredicate + ", object id:" + objectId);
        // 存在uid
        DgraphProto.NQuad quad = DgraphProto.NQuad.newBuilder().setSubject(String.format("_:%s",
            uniqueId)).setPredicate(edgePredicate).setObjectId(objectId).build();
        quads.add(quad);
      }
    }
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder().addAllSet(quads).build();
    DgraphProto.Assigned ag;
    try {
      ag = txn.mutate(mu);
      txn.commit();
    }  catch (Exception e) {
      logger.info("[entityInitial Expection]:" + e.getMessage());
      ag = mutateRetry(mu, e);
    } finally {
      txn.discard();
    }
    if (ag == null) {
      logger.info("[Final] Retry Error!!");
    }
    return ag;
  }

  /**
   * 批量<uid> <relation> <uid>的方式写入
   * exception retry mutate
   * @param mu mutation
   * @param exception exception
   * @return uid assigned
   */
  private DgraphProto.Assigned mutateRetry(DgraphProto.Mutation mu, Exception exception) {
    /** retry multiply times as possible
      1. io.grpc.StatusRuntimeException: UNAVAILABLE: Channel in TRANSIENT_FAILURE state code: 14

      2. io.grpc.StatusRuntimeException: DEADLINE_EXCEEDED code: 4

      3. io.grpc.StatusRuntimeException: Please retry again, server is not ready to accept
         request code: 2
      4. io.grpc.StatusRuntimeException: UNKNOWN: Predicate is being moved, please retry
         later, code: 2
      5. io.grpc.StatusRuntimeException:While proposing to RAFT group, err: context deadline
         exceeded, code: 2
      6. io.grpc.StatusRuntimeException: UNKNOWN: Only leader can decide to commit or abort,
         code: - 2
      7. 可能的异常: TxnConflictException,*/
    DgraphProto.Assigned assigned = null;
    int code;
    if (exception instanceof StatusRuntimeException) {
      code = ((StatusRuntimeException) exception).getStatus().getCode().value();
      logger.info("[StatusRuntimeException code value]:" + code);
    } else if (exception instanceof TxnConflictException) {
      code = 4;
    } else {
      logger.info("[OtherException]:" + exception);
      return assigned;
    }
    // 重试直到非retry exception为止
    while(code == 4 || code == 2) {
      code = 0;
      try {
        Thread.sleep(retryCompensation * retryCounter.incrementAndGet());
      } catch (InterruptedException e) {
        logger.info("[Sleep error Exception]:" + e.getMessage());
      }
      DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
      try {
        assigned = txn.mutate(mu);
        txn.commit();
      } catch (Exception e) {
        logger.info("[multiplyEdgeMutation Retry Exception] => " + e.getMessage() +
            ", retry times:" + retryCounter.get());
        if (e instanceof StatusRuntimeException) {
          code = ((StatusRuntimeException) exception).getStatus().getCode().value();
        } else if (exception instanceof TxnConflictException) {
          code = 4;
        }
        assigned = null;
      } finally {
        txn.discard();
      }
    }
    retryCounter.set(0);
    return assigned;
  }

  /**
   * 批量对象json的方式写入，写入实体需继承EntityNode
   * @param entities 实体数组
   * @param <T> 支持实体泛型，继承自EntityNode
   * @return uid assigned
   */
  public <T extends EntityNode> DgraphProto.Assigned multiplyMutationEntity(List<T> entities) {
    DgraphClient.Transaction txnInner = this.dgraphClient.newTransaction();
    DgraphProto.Assigned assigned;
    String text;
    int size = entities.size();
    if (size <= 0) {
      return null;
    }
    Gson gson = new Gson();
    text = gson.toJson(entities);
    logger.info("text:" + text);
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder().setSetJson(ByteString
        .copyFromUtf8(text)).build();
    try {
      assigned = txnInner.mutate(mu);
      txnInner.commit();
    } catch (Exception e) {
      logger.info("[multiplyMutationEntity Exception]:" + e.getMessage());
      assigned = mutateRetry(mu, e);
    } finally {
      txnInner.discard();
    }
    return assigned;
  }


  /**
   * 批量<uid> <relation> <uid>的方式删除
   * <0xf11168064b01135b> <died> "1998" .
   * <0xf11168064b01135b> <author.of> * .
   * <0xf11168064b01135b> * * .
   * @param edges 属性数组
   * @return uid assigned
   */
  private DgraphProto.Assigned multiplyEdgesDeleteMutation(List<String> edges) {
    List<ByteString> newEdges = new ArrayList<>();
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    DgraphProto.Assigned assigned = null;
    for (String edge : edges) {
      logger.info("edge ===> " + edge);
      newEdges.add(ByteString.copyFromUtf8(edge));
    }
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder()
        .setDelNquads(ByteString.copyFrom(newEdges))
        .build();
    try {
      assigned = txn.mutate(mu);
      txn.commit();
    } catch (Exception e) {
      logger.info("[multiplyEdgesDeleteMutation Exception] =>" + e.getMessage());
      assigned = mutateRetry(mu, e);
    } finally {
      txn.discard();
    }
    if (assigned == null) {
      logger.info("[Final] Retry Error!!");
    }
    return assigned;
  }

  public DgraphProto.Assigned mutation(List<DgraphProto.NQuad> nQuads, boolean needRetry) {
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    DgraphProto.Assigned assigned = null;
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder()
        .addAllSet(nQuads)
        .build();
    try {
      assigned = txn.mutate(mu);
      txn.commit();
    } catch (Exception e) {
      logger.info("[multiplyEdgeMutation Exception] =>" + e.getMessage());
      if (needRetry) {
        assigned = mutateRetry(mu, e);
      } else {
        assigned = null;
      }
    } finally {
      txn.discard();
    }
    if (assigned == null) {
      logger.info("[Final] Retry Error!!");
    }
    return assigned;
  }

  /**
   * 批量<uid> <relation> <uid>的方式写入
   * @param edges 属性数组
   * @return uid assigned
   */
  public DgraphProto.Assigned multiplyEdgesMutation(List<String> edges, boolean needRetry) {
    List<ByteString> newEdges = new ArrayList<>();
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    DgraphProto.Assigned assigned = null;
    for (String edge : edges) {
      logger.info("edge ===> " + edge);
      newEdges.add(ByteString.copyFromUtf8(edge));
    }
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder()
        .setSetNquads(ByteString.copyFrom(newEdges))
        .build();
    try {
      assigned = txn.mutate(mu);
      txn.commit();
    } catch (Exception e) {
      logger.info("[multiplyEdgeMutation Exception] =>" + e.getMessage());
      if (needRetry) {
        assigned = mutateRetry(mu, e);
      } else {
        assigned = null;
      }
    } finally {
      txn.discard();
    }
    if (assigned == null) {
      logger.info("[Final] Retry Error!!");
    }
    return assigned;
  }

  /**
   * 批量<uid> <relation> <uid>的方式写入
   * @param edges 熟悉
   * @return uid assigned
   */
  @Deprecated
  public DgraphProto.Assigned multiplyEdgesMutation(String edges) {

    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    DgraphProto.Assigned assigned = null;
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder().setSetNquads(ByteString
        .copyFromUtf8(edges))
        .build();
    try {
      assigned = txn.mutate(mu);
      txn.commit();
    } catch (Exception e) {
      logger.info("[multiplyEdgeMutation Exception] =>" + e.getMessage());
      assigned = null;
    } finally {
      txn.discard();
    }

    return assigned;
  }

  /**
   * 单个string json对象写入
   * @param json 单个string json写入dgraph
   * @return uid assigned
   */
  public DgraphProto.Assigned mutation(String json) {
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder().setSetJson(ByteString
        .copyFromUtf8(json)).build();
    DgraphProto.Assigned assigned = txn.mutate(mu);
    txn.commit();
    txn.discard();
    return assigned;
  }

  /**
   * dgraph的查询
   * @param did 查询id
   * @param className 实体对象类名
   * @param methodName 获取实体的哪个方法名
   * @return uid
   */
  public String QueryById(String did, String className, String methodName){
    String query = "query did($a: string){\n" + "isExist(func: eq(id, $a)) {\n" + "uid\n" + "  "
        + "}\n" + "}";
    Map<String, String> vars = Collections.singletonMap("$a", did);
    DgraphProto.Response res = dgraphClient.newTransaction().queryWithVars(query, vars);
    Class classNameClass;
    Object people;
    String ret = "";
    try {
      classNameClass = Class.forName(className);
      // people = classNameClass.newInstance();
      String resp = res.getJson().toStringUtf8();
      people = new Gson().fromJson(resp, Class.forName(className));
      Method[] methods = classNameClass.getMethods();
      // 循环查找想要的方法
      for (Method method : methods) {
        if (methodName.equals(method.getName())) {
          // 调用这个方法，invoke第一个参数是类名，后面是方法需要的参数
          Object result = method.invoke(people);
          return (String) result;
        }
      }
    } catch (IllegalAccessException | ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {

    }
    return ret;

  }
  public <T extends EntityNode> DgraphProto.Assigned getAssignedUid(List<T> entities) {
    List<String> stringList = new ArrayList<>();
    for (T item : entities) {
      String unique_id = item.getUnique_id();
      String nquad = attrFormat(unique_id, "unique_id", unique_id, false);
      stringList.add(nquad);
    }
    DgraphProto.Assigned assigned = null;
    List<ByteString> newEdges = new ArrayList<>();
    for (String edge : stringList) {
      newEdges.add(ByteString.copyFromUtf8(edge));
    }
    DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder()
        .setSetNquads(ByteString.copyFrom(newEdges))
        .build();
    DgraphClient.Transaction txn = this.dgraphClient.newTransaction();
    try {
      assigned = txn.mutate(mu);
    }  catch (Exception e) {
      logger.info("[getAssignedUid exception] => " + e.getMessage());
    } finally {
    }
    return assigned;
  }

  public static void main(final String[] args) {
    // ...todo
  }
}
