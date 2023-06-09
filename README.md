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
  sleep 10
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
- Enable user workload monitoring
  
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
  oc create -f grafana-sub.yaml
  oc get subscription -n app-monitor
  ```
  Output

  ```bash
  NAME               PACKAGE            SOURCE                CHANNEL
  grafana-operator   grafana-operator   community-operators   v4
  ```

- Create Gafana Instance
  
  ```bash
  oc apply -f grafana.yaml
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
  
  - Test script
  
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
    ```
  - song log
    
    ```log
    
    ```
    
  - song-indexer log
    
    ```log
    
    ```

- Check Jaeger Console for tracing
  
    image here!

## Kafka Connect

- Install MongoDB
  
  ```bash
  helm repo add bitnami https://charts.bitnami.com/bitnami

  helm install mongodb bitnami/mongodb --set podSecurityContext.fsGroup="",containerSecurityContext.enabled=false,podSecurityContext.enabled=false,auth.enabled=false --version 13.6.0 -n song-app
  ```

- Create secret for image registry
  
  For podman

  ```bash
  kubectl create secret generic quayio \
  --from-file=.dockerconfigjson=$HOME/.config/containers/auth.json \
  --type=kubernetes.io/dockerconfigjson -n demo
  ```

  For docker
  
  ```bash
  kubectl create secret generic quayio \
  --from-file=.dockerconfigjson=$HOME/.docker/config.json \
  --type=kubernetes.io/dockerconfigjson -n demo
  ```

- Create config map for connect metrics

  ```bash
  oc create -f connect-metrics.yaml -n demo
  ```
- Create and deploy a Kafka Connect container
  
  - Add role edit to service account strimzi-cluster-operator
    
    ```bash
    oc policy add-role-to-user edit \
    system:serviceaccount:openshift-operators:strimzi-cluster-operator -n demo
    ```
  
  - Create image stream
    
    ```bash
    oc create is oc create is kafka-connect-mongodb
    ```
  
  - Create Kafka connector
    
    ```bash
    oc create -f kafka-connect.yaml
    ```

  - Check builder log
  - Check Kafka connector pod

- Configure the MongoDB sink connector 
  - Create sink connector
    
    ```bash
    oc create -f kafka-connector.yaml
    ```

  - Check kafa connect pod's log

- Check Jaeger Console

   


<!-- oc run --namespace kafka mongodb-client --rm --tty -i --restart='Never' --image docker.io/bitnami/mongodb:4.4.13-debian-10-r9 --command -- bash -->