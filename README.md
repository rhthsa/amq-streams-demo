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
  oc apply -k kustomize/amq-streams/operator/overlays/demo
  oc wait -n demo --for condition=established --timeout=180s \
    crd/kafkas.kafka.strimzi.io \
    crd/kafkatopics.kafka.strimzi.io \
    crd/strimzipodsets.core.strimzi.io
  oc get csv -n demo
  ```

  Output

  ```bash
  namespace/demo created
  operatorgroup.operators.coreos.com/amq-streams-operator created
  subscription.operators.coreos.com/amq-streams created
  customresourcedefinition.apiextensions.k8s.io/kafkas.kafka.strimzi.io condition met
  customresourcedefinition.apiextensions.k8s.io/kafkatopics.kafka.strimzi.io condition met
  customresourcedefinition.apiextensions.k8s.io/strimzipodsets.core.strimzi.io condition met
  NAME                  DISPLAY       VERSION   REPLACES              PHASE
  amqstreams.v2.4.0-0   AMQ Streams   2.4.0-0   amqstreams.v2.3.0-3   Succeeded
  ```

## Create Kafka Cluster
- Create Kafka Cluster in namespace demo
  
  ```bash
  oc apply -k kustomize/amq-streams/instance/overlays/demo
  oc -n demo wait --for condition=ready \
     --timeout=180s pod -l  app.kubernetes.io/instance=kafka-demo
  oc get strimzipodsets -n demo
  ```

  Output

  ```bash
  pod/kafka-demo-entity-operator-5fc5595685-nf8fz condition met
  pod/kafka-demo-kafka-0 condition met
  pod/kafka-demo-kafka-1 condition met
  pod/kafka-demo-kafka-2 condition met
  pod/kafka-demo-kafka-exporter-d65ffddd8-wjcfk condition met
  pod/kafka-demo-zookeeper-0 condition met
  pod/kafka-demo-zookeeper-1 condition met
  pod/kafka-demo-zookeeper-2 condition met
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
  NAME                                        READY   STATUS    RESTARTS   AGE
  kafka-demo-kafka-exporter-d65ffddd8-wjcfk   1/1     Running   0          87s
  ```

## Config user workload monitoring
- Enable user workload monitoring on your OpenShift cluster
  
  ```bash
  oc create -k  kustomize/user-workload-monitoring/overlays/demo
  ```

  Check

  ```bash
  oc -n openshift-user-workload-monitoring wait --for condition=ready \
  --timeout=180s pod -l app.kubernetes.io/name=prometheus
  oc -n openshift-user-workload-monitoring wait --for condition=ready \
  --timeout=180s pod -l app.kubernetes.io/name=thanos-ruler
  oc get po -n openshift-user-workload-monitoring
  ```

  Output

  ```bash
  pod/prometheus-user-workload-0 condition met
  pod/prometheus-user-workload-1 condition met
  pod/thanos-ruler-user-workload-0 condition met
  pod/thanos-ruler-user-workload-1 condition met
  NAME                                   READY   STATUS    RESTARTS   AGE
  prometheus-operator-79dc5458f7-brthd   2/2     Running   0          2m36s
  prometheus-user-workload-0             6/6     Running   0          2m34s
  prometheus-user-workload-1             6/6     Running   0          2m34s
  thanos-ruler-user-workload-0           3/3     Running   0          2m29s
  thanos-ruler-user-workload-1           3/3     Running   0          2m29s
  ```
- Create Pod Monitor
  
  ```bash
  oc create -k kustomize/amq-streams/pod-monitor/overlays/demo 
  oc get podmonitor -n demo
  ```

  Output

  ```bash
  podmonitor.monitoring.coreos.com/bridge-metrics created
  podmonitor.monitoring.coreos.com/cluster-operator-metrics created
  podmonitor.monitoring.coreos.com/entity-operator-metrics created
  podmonitor.monitoring.coreos.com/kafka-resources-metrics created
  NAME                       AGE
  bridge-metrics             12s
  cluster-operator-metrics   12s
  entity-operator-metrics    12s
  kafka-resources-metrics    11s
  ```

- Check Pod Monitor with Developer Console by navigate to Observe->Metrics then select Custom query
  
  ![](images/kafka-metrics-dev-console.png)

## Grafana Dashboard

- Install Grafana Operator
  
  ```bash
  oc create -k kustomize/grafana/operator/overlays/demo
  oc get csv -n app-monitor
  oc get po -n app-monitor
  ```
  Output

  ```bash
  NAME                       DISPLAY            VERSION   REPLACES                   PHASE
  grafana-operator.v4.10.1   Grafana Operator   4.10.1    grafana-operator.v4.10.0   Succeeded
  NAME                                                  READY   STATUS    RESTARTS   AGE
  grafana-operator-controller-manager-55c9b6c4d-kvq86   2/2     Running   0          85s
  ```

- Create Gafana Instance
  
  ```bash
  oc create -k kustomize/grafana/instance/overlays/demo
  oc get po -n app-monitor
  ```
  Output
  
  ```bash
  grafana.integreatly.org/grafana created
  NAME                                                  READY   STATUS    RESTARTS   AGE
  grafana-deployment-6cf7948587-qrqp5                   1/1     Running   0          45s
  grafana-operator-controller-manager-55c9b6c4d-72ckc   2/2     Running   0          2m2s
  ```

- Check of Grafana's route
  
  ```bash
  oc get route grafana-route -n app-monitor -o jsonpath='{.spec.host}'
  ```

- Create dashboard

  WIP

## OpenTelemetry

- Install Red Hat OpenShift distributed tracing platform (Jaeger) and Red Hat OpenShift distributed tracing data collection (OTEL) Operators

  ```bash
  oc create -k kustomize/jaeger/operator/overlays/demo
  oc create -k kustomize/opentelemetry/operator/overlays/demo
  oc get csv -n app-monitor
  ```

  Output
  
  ```bash
  NAME                               DISPLAY                                                 VERSION    REPLACES                           PHASE
  grafana-operator.v4.10.1           Grafana Operator                                        4.10.1     grafana-operator.v4.10.0           Succeeded
  jaeger-operator.v1.42.0-5          Red Hat OpenShift distributed tracing platform          1.42.0-5   jaeger-operator.v1.34.1-5          Succeeded
  opentelemetry-operator.v0.74.0-5   Red Hat OpenShift distributed tracing data collection   0.74.0-5   opentelemetry-operator.v0.60.0-2   Succeeded
  ```

- Create Jaeger and OTEL instances
  
  ```bash
  oc create -k kustomize/opentelemetry/instance/overlays/demo
  oc create -k kustomize/jaeger/instance/overlays/demo
  oc get po -n app-monitor
  ```

  Output

  ```bash
  NAME                                                        READY   STATUS    RESTARTS   AGE
  grafana-deployment-6cf7948587-qrqp5                         1/1     Running   0          21m
  grafana-operator-controller-manager-55c9b6c4d-72ckc         2/2     Running   0          23m
  jaeger-584789dc75-qbxgb                                     2/2     Running   0          12s
  opentelemetry-operator-controller-manager-89f5dc665-8wtxb   2/2     Running   0          4m37s
  otel-collector-746df75c54-w4rct                             1/1     Running   0          69s
  ```

## Demo Application

- Deploy song app (producer) and song-indexer (consumer) app
  
  ```bash
  oc apply -k kustomize/song-app/overlays/demo
  oc apply -k kustomize/song-indexer-app/overlays/demo 
  oc get po -n music-streaming-app
  ```
  
  Output

  ```bash
  NAME                          READY   STATUS    RESTARTS   AGE
  song-5ff767c66-r5zdx          1/1     Running   0          5m37s
  song-indexer-7bbf8f9d-8gcds   1/1     Running   0          3m1s
  ```
  <!-- ```bash
  KAFKA_BOOTSTRAP=kafka-demo-kafka-bootstrap.demo.svc:9092
  OTEL_ENDPOINT=otel-collector-headless.app-monitor.svc:4317
  cat song-app.yaml | \
   sed 's/KAFKA_BOOTSTRAP/'$KAFKA_BOOTSTRAP'/' | \
   sed 's/OTEL_ENDPOINT/'$OTEL_ENDPOINT'/' |
   oc apply -f -
  ``` -->

- Check developer console
  
  ![](images/dev-console-song-app-topology.png)

- Run following test scripts and check for both song app and song-indexer app
  - Put a message to topic song
    
    ```bash
    HOST=$(oc get route/song -n music-streaming-app -o jsonpath='{.spec.host}')
    curl -X POST -v -H "Content-Type: application/json" -d '{"author":"Matt Bellamy","name":"Uprising","op":"ADD"}' https://$HOST/songs
    ```

    Output

    ```bash
    *  SSL certificate verify ok.
    * using HTTP/1.x
    > POST /songs HTTP/1.1
    > Host: song-music-streaming-app.apps.cluster-2j5j5.2j5j5.sandbox1138.opentlc.com
    > User-Agent: curl/8.1.2
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 54
    > 
    * TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
    * TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
    * old SSL session ID is stale, removing
    < HTTP/1.1 204 No Content
    < set-cookie: 307fcbf9d775aa21dec85f890f2422aa=63f9ae3e47f4b946088c4346a8d36ff6; path=/; HttpOnly; Secure; SameSite=None
    < 
    * Connection #0 to host song-music-streaming-app.apps.cluster-2j5j5.2j5j5.sandbox1138.opentlc.com left intact
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

    ```bash
    oc logs -n music-streaming-app -f $(oc get po -o custom-columns='Name:.metadata.name' --no-headers -l app=song -n music-streaming-app)
    ```

    Output

    ```log
    05:41:28 INFO  traceId=b898c864222f761fa2e8884bd7fb01d5, parentId=, spanId=d638bff583181f47, sampled=true [or.ac.so.ap.SongResource] (executor-thread-1) song: fa85c0cb-1e7b-4206-826f-5253132266ef, Name: Uprising
    ```
    
  - song-indexer log
    
    ```bash
    oc logs -n music-streaming-app -f $(oc get po -o custom-columns='Name:.metadata.name' --no-headers -l app=song-indexer -n music-streaming-app)
    ```
    
    Output

    ```log
    05:41:31 INFO  traceId=b898c864222f761fa2e8884bd7fb01d5, parentId=5914e681e09bf49f, spanId=3cc11f7a0d77bf49, sampled=true [or.ac.so.in.ap.SongResource] (vert.x-eventloop-thread-0) Key: fa85c0cb-1e7b-4206-826f-5253132266ef, Payload: {"author":"Matt Bellamy","id":"fa85c0cb-1e7b-4206-826f-5253132266ef","name":"Uprising","op":"ADD"}, Metadata: 2023-06-16T05:41:29.873Z
    ```

    Check that Trace ID (*traceId=b898c864222f761fa2e8884bd7fb01d5*) in song app and song-indexer app is the same

- Check Jaeger Console for tracing
  - Open Jaeger Console in namespace app-monitor
    
    ![](images/app-monitor-namespace.png)
  
  - Select service song-app 
    
    ![](images/jaeger-song-app.png)

  - Select trace graph
    
    ![](images/jaeger-trace-graph.png)


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