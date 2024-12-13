#!/bin/bash

ENV=$1
STARTTIME=$2
ENDTIME=$3

NAMESPACE="hmpps-book-a-video-link-$ENV"
SECRETS_FILE="book-a-video-link-secrets.yaml"

# Create the secrets YAML file
cat <<EOF > $SECRETS_FILE
apiVersion: v1
kind: Secret
metadata:
  name: maintenance
  namespace: $NAMESPACE
type: Opaque
stringData:
  maintenance_mode: "true"
  maintenance_start_datetime: "$STARTTIME"
  maintenance_end_datetime: "$ENDTIME"
EOF

# Apply the secret
kubectl apply -f ./$SECRETS_FILE
echo "Applied secrets to $NAMESPACE"

rm -f ./$SECRETS_FILE

MAINTENANCE_MODE=$(kubectl -n "$NAMESPACE" get secret maintenance -o jsonpath='{.data.maintenance_mode}' | base64 -d)
START_TIME=$(kubectl -n "$NAMESPACE" get secret maintenance -o jsonpath='{.data.maintenance_start_datetime}' | base64 -d)
END_TIME=$(kubectl -n "$NAMESPACE" get secret maintenance -o jsonpath='{.data.maintenance_end_datetime}' | base64 -d)

echo "maintenance_mode=$MAINTENANCE_MODE"
echo "maintenance_start_datetime=$START_TIME"
echo "maintenance_end_datetime=$END_TIME"

# Restart the deployment pods
echo "Restarting deployment the hmpps-book-a-video-link-ui pods on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deployments/hmpps-book-a-video-link-ui

