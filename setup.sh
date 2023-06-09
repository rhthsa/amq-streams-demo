#!/bin/sh
CONFIG_DIR=.
oc create -f $CONFIG_DIR/amqstreams-operator.yaml
sleep 10
oc wait --for condition=established --timeout=180s \
   crd/kafkas.kafka.strimzi.io \
   crd/kafkatopics.kafka.strimzi.io \
   crd/strimzipodsets.core.strimzi.io
oc get csv
