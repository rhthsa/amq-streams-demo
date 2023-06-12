# AMQ Sterams on OpenShift
- [AMQ Sterams on OpenShift](#amq-sterams-on-openshift)
  - [Install AMQ Streams Operators](#install-amq-streams-operators)
  - [Create Kafka Cluster](#create-kafka-cluster)
    - [Config user workload monitoring](#config-user-workload-monitoring)
    - [Grafana Dashboard](#grafana-dashboard)
    - [OpenTelemetry](#opentelemetry)
  - [Demo Application](#demo-application)
  - [Kafka Connect](#kafka-connect)

## Install AMQ Streams Operators
- Install AMQ Streams Operator
  
  ```bash
  oc create -f amqstreams-operator.yaml
  sleep 60
  oc wait --for condition=established --timeout=180s \
    crd/kafkas.kafka.strimzi.io \
    crd/kafkatopics.kafka.strimzi.io \
    crd/strimzipodsets.core.strimzi.io
  oc get csv
  ```

  Output

  ```bash
  customresourcedefinition.apiextensions.k8s.io/kafkas.kafka.strimzi.io condition met
  customresourcedefinition.apiextensions.k8s.io/kafkatopics.kafka.strimzi.io condition met
  customresourcedefinition.apiextensions.k8s.io/strimzipodsets.core.strimzi.io condition met
  NAME                  DISPLAY       VERSION   REPLACES              PHASE
  amqstreams.v2.4.0-0   AMQ Streams   2.4.0-0   amqstreams.v2.3.0-3   Succeeded
  ```

## Create Kafka Cluster
- Create Kafka Cluster in namespace demo
  
  ```bash
  oc apply -f kafka-with-metrics.yaml
  oc -n demo wait --for condition=ready \
     --timeout=180s pod -l  strimzi.io/component-type=kafka
  oc get strimzipodsets -n demo
  ```

  Output

  ```bash
  pod/kafka-demo-kafka-0 condition met
  pod/kafka-demo-kafka-1 condition met
  pod/kafka-demo-kafka-2 condition met
  NAME                   PODS   READY PODS   CURRENT PODS   AGE
  kafka-demo-kafka       3      3            3              57s
  kafka-demo-zookeeper   3      3            3              102
  ```

  Check for exporter pod

  ```bash
  oc get po -n demo  -l strimzi.io/component-type=kafka-exporter
  ```

  Output

  ```bash
  NAME                                         READY   STATUS    RESTARTS   AGE
  kafka-demo-kafka-exporter-5cdd8d9bd5-plzxs   1/1     Running   0          3m21s
  ```

### Config user workload monitoring
- Enable user workload monitoring on your OpenShift cluster
  
  ```bash
  oc apply -f  user-workload-monitoring.yaml
  ```

- Create Pod Monitor
  
  ```bash
  cat strimzi-pod-monitor.yaml|sed 's/myproject/demo/g'|oc create -n demo -f -
   oc get podmonitor -n demo
  ```

  Output

  ```bash
  NAME                       AGE
  bridge-metrics             12s
  cluster-operator-metrics   12s
  entity-operator-metrics    12s
  kafka-resources-metrics    11s
  ```

- Check Pod Monitor with Developer Console by navigate to Observe->Metrics then select Custom query
  
  ![](images/kafka-metrics-dev-console.png)

### Grafana Dashboard

- Install Grafana Operator
  
  ```bash
  oc create -f app-monitor.yaml
  cat grafana-sub.yaml|sed 's/PROJECT/'app-demo'/' | oc apply -f -
  oc get subscription -n app-monitor
  oc get po -n app-monitor
  ```
  Output

  ```bash
  NAME               PACKAGE            SOURCE                CHANNEL
  grafana-operator   grafana-operator   community-operators   v4
  NAME                                                   READY   STATUS    RESTARTS   AGE
  grafana-operator-controller-manager-54d8cc5fc4-tjhjh   2/2     Running   0          25s
  ```

- Create Gafana Instance
  
  ```bash
  oc apply -f grafana.yaml -n app-monitor
  oc get po -n app-monitor
  ```
  Output
  
  ```bash
  NAME                                                   READY   STATUS    RESTARTS   AGE
  grafana-deployment-6cf7948587-7nbj5                    1/1     Running   0          38s
  grafana-operator-controller-manager-689fd9fdbf-v4z82   2/2     Running   0          11m
  ```

- Check of Grafana's route
  
  ```bash
  oc get route grafana-route -n app-monitor -o jsonpath='{.spec.host}'
  ```

- Create dashboard

  WIP

### OpenTelemetry

- Install Red Hat OpenShift distributed tracing platform (Jaeger) and Red Hat OpenShift distributed tracing data collection (OTEL) Operators

  ```bash
  oc create -f jaeger-sub.yaml
  oc create -f otel-sub.yaml
  oc get csv
  ```

  Output
  
  ```bash
  NAME                               DISPLAY                                                 VERSION    REPLACES                           PHASE
  amqstreams.v2.4.0-0                AMQ Streams                                             2.4.0-0    amqstreams.v2.3.0-3                Succeeded
  jaeger-operator.v1.42.0-5          Red Hat OpenShift distributed tracing platform          1.42.0-5   jaeger-operator.v1.34.1-5          Succeeded
  opentelemetry-operator.v0.74.0-5   Red Hat OpenShift distributed tracing data collection   0.74.0-5   opentelemetry-operator.v0.60.0-2   Succeeded
  ```

- Create Jaeger and OTEL instances
  
  ```bash
  oc create -f jaeger.yaml -n app-monitor
  cat otel-collector.yaml | sed 's/PROJECT/app-monitor/' | oc create -n app-monitor -f  -
  oc get po -n app-monitor
  ```

  Output

  ```bash
  NAME                                                   READY   STATUS    RESTARTS   AGE
  grafana-deployment-6cf7948587-7nbj5                    1/1     Running   0          23m
  grafana-operator-controller-manager-689fd9fdbf-v4z82   2/2     Running   0          35m
  jaeger-55d56f4b5-gjmfg                                 2/2     Running   0          69s
  otel-collector-7d4fdb7655-k9hq2                        1/1     Running   0          70s
  ```

## Demo Application

- Deploy song app
  
  ```bash
  KAFKA_BOOTSTRAP=kafka-demo-kafka-bootstrap.demo.svc:9092
  OTEL_ENDPOINT=otel-collector-headless.app-monitor.svc:4317
  cat song-app.yaml | \
   sed 's/KAFKA_BOOTSTRAP/'$KAFKA_BOOTSTRAP'/' | \
   sed 's/OTEL_ENDPOINT/'$OTEL_ENDPOINT'/' |
   oc apply -f -
  ```

- Deploy song-indexer app
  
  ```bash
  KAFKA_BOOTSTRAP=kafka-demo-kafka-bootstrap.demo.svc:9092
  OTEL_ENDPOINT=otel-collector-headless.app-monitor.svc:4317
  cat song-indexer-app.yaml | \
   sed 's/KAFKA_BOOTSTRAP/'$KAFKA_BOOTSTRAP'/' | \
   sed 's/OTEL_ENDPOINT/'$OTEL_ENDPOINT'/' |
   oc apply -f -
  ```

- Check developer console
  
  ![](images/dev-console-song-app-topology.png)

- Run following test scripts and check for both song app and song-indexer app
  - Put a message to topic song
    
    ```bash
    HOST=$(oc get route/song -n song-app -o jsonpath='{.spec.host}')
    curl -X POST -v -H "Content-Type: application/json" -d '{"author":"Matt Bellamy","name":"Uprising","op":"ADD"}' http://$HOST/songs
    ```

    Output

    ```bash
    < HTTP/1.1 204 No Content
    < set-cookie: 6cd509e946777fe23935695b880e30c9=3234ba0c2e1d042593cf9159cb257545; path=/; HttpOnly
    <
    * Connection #0 to host song-song-app.apps.cluster-9czlk.9czlk.sandbox1314.opentlc.com left intact
    ```

  <!-- - Test script
  
    ```bash
    HOST=$(oc get route/song -n song-app -o jsonpath='{.spec.host}')
    MAX=100
    CUR=0
    DELAY_SEC=1
    while [ $CUR  -lt $MAX ];
    do
      curl -X POST -H "Content-Type: application/json" -d '{"author":"Matt Bellamy","name":"Uprising","op":"ADD"}' http://$HOST/songs
      sleep $DELAY_SEC
      CUR=$(expr $CUR + 1)
      echo "Count :$CUR"
    done
    ``` -->
  - song log
    
    ```log
    08:45:26 INFO  traceId=98d7660397c5785e70987cb103ea1246, parentId=, spanId=1118224f11544f12, sampled=true [or.ac.so.ap.SongResource] (executor-thread-1) song: 77a0ec18-2c8d-444b-9630-b4bb6e38ae7c, Name: Uprising 
    ```
    
  - song-indexer log
    
    ```log
    08:45:29 INFO  traceId=98d7660397c5785e70987cb103ea1246, parentId=cc67a922ce0ef1be, spanId=4593bb2cacf690a4, sampled=true [or.ac.so.in.ap.SongResource] (vert.x-eventloop-thread-0) Key: 77a0ec18-2c8d-444b-9630-b4bb6e38ae7c, Payload: {"author":"Matt Bellamy","id":"77a0ec18-2c8d-444b-9630-b4bb6e38ae7c","name":"Uprising","op":"ADD"}, Metadata: 2023-06-12T08:45:27.799Z
    ```

    Check that Trace ID in song app and song-indexer app is the same

- Check Jaeger Console for tracing
  - Open Jaeger Console
    
    ```bash
    oc get route jaeger -n app-monitor -o jsonpath='{.spec.host}'
    ```
  
  - Select Service song-app 
    


## Kafka Connect

- Install MongoDB
  
  ```bash
  helm repo add bitnami https://charts.bitnami.com/bitnami

  helm install mongodb bitnami/mongodb --set podSecurityContext.fsGroup="",containerSecurityContext.enabled=false,podSecurityContext.enabled=false,auth.enabled=false --version 13.6.0 -n song-app
  ```

- Create secret for image registry if you want to use external container registry
  
    For podman

    ```bash
    oc create secret generic quayio \
    --from-file=.dockerconfigjson=$HOME/.config/containers/auth.json \
    --type=kubernetes.io/dockerconfigjson -n demo
    ```

    For docker
    
    ```bash
    oc create secret generic quayio \
    --from-file=.dockerconfigjson=$HOME/.docker/config.json \
    --type=kubernetes.io/dockerconfigjson -n demo
    ```

- Create config map for connect metrics

  ```bash
  oc create -f connect-metrics.yaml -n demo
  ```
- Create and deploy a Kafka Connect container
  
  <!-- - Add role edit to service account strimzi-cluster-operator
    
    ```bash
    oc policy add-role-to-user edit \
    system:serviceaccount:openshift-operators:strimzi-cluster-operator -n demo
    ``` -->
  
  - Create image stream
    
    ```bash
    oc create  is kafka-connect-mongodb -n demo
    ```
  
  - Create Kafka connector
    
    ```bash
    oc create -f kafka-connect.yaml
    ```

  - Check builder log
    
    ```bash
    oc logs -f $(oc get po -n demo | grep build | awk '{print $1}') -n demo
    ```
    
    Output

    ```log
    Pushing image image-registry.openshift-image-registry.svc:5000/demo/kafka-connect-mongodb:latest ...
    Getting image source signatures
    Copying blob sha256:100b6b7b72efb5c00b91e54a4e5eeb08ec9e532a3064db800f5874b2b6d0a16b
    Copying blob sha256:28ff5ee6facbc15dc879cb26daf949072ec01118d3463efd1f991d9b92e175ef
    Copying blob sha256:4ef1d5473f3c6dccd2daf49f54b9b7ba6e2d9b77e9c264053d2077033de20baa
    Copying config sha256:c2b9c71e1d90a09f5a85eb4c09076ed965c1ce5af26581703deee948deb63ba5
    Writing manifest to image destination
    Storing signatures
    Successfully pushed image-registry.openshift-image-registry.svc:5000/demo/kafka-connect-mongodb@sha256:ed0ba56f817213bb558cb002fa950a269828eccba42b99b0618c32773869e1c2
    Push successful
    ```

  - Check Kafka connector pod
    
    ```bash
    oc get po -l app.kubernetes.io/instance=mongodb-sink-connect-cluster -n demo
    ```

    Output

    ```bash
    NAME                                                    READY   STATUS    RESTARTS   AGE
    mongodb-sink-connect-cluster-connect-5bb64c9dd7-wmz6p   1/1     Running   0          106s
    ```

- Configure the MongoDB sink connector 
  - Create sink connector
    
    ```bash
    oc create -f kafka-connector.yaml
    ```

  - Check kafa connect pod's log
    
    ```log
    2023-06-12 09:18:27,767 INFO [connector-mongodb-sink|task-0] [Consumer clientId=connector-consumer-connector-mongodb-sink-0, groupId=connect-connector-mongodb-sink] Found no committed offset for partition songs-0 (org.apache.kafka.clients.consumer.internals.ConsumerCoordinator) [task-thread-connector-mongodb-sink-0]
    2023-06-12 09:18:27,781 INFO [connector-mongodb-sink|task-0] [Consumer clientId=connector-consumer-connector-mongodb-sink-0, groupId=connect-connector-mongodb-sink] Resetting offset for partition songs-0 to position FetchPosition{offset=0, offsetEpoch=Optional.empty, currentLeader=LeaderAndEpoch{leader=Optional[kafka-demo-kafka-1.kafka-demo-kafka-brokers.demo.svc:9093 (id: 1 rack: null)], epoch=2}}. (org.apache.kafka.clients.consumer.internals.SubscriptionState) [task-thread-connector-mongodb-sink-0]
    ```
  
- Check Jaeger Console
    
  - Trace

    ![](images/kafka-connect-trace.png)

  - Trace Graph

    ![](images/kafka-connect-trace-graph.png)

<!-- oc run --namespace kafka mongodb-client --rm --tty -i --restart='Never' --image docker.io/bitnami/mongodb:4.4.13-debian-10-r9 --command -- bash -->