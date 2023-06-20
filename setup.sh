#!/bin/bash
oc apply -k kustomize/amq-streams/operator/overlays/demo
oc wait -n demo --for condition=established --timeout=180s \
  crd/kafkas.kafka.strimzi.io \
  crd/kafkatopics.kafka.strimzi.io \
  crd/strimzipodsets.core.strimzi.io
sleep 5
oc get csv -n demo

