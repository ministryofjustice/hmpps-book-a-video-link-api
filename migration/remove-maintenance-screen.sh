#!/bin/bash

ENV=$1

NAMESPACE="hmpps-book-a-video-link-$ENV"

kubectl -n "$NAMESPACE" delete secret maintenance
echo "Removed maintenance secret from $NAMESPACE"

# Restart the deployment pods
echo "Restarting deployment the hmpps-book-a-video-link-ui pods on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deployments/hmpps-book-a-video-link-ui

