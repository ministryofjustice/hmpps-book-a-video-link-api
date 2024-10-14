#!/bin/bash

ENV=$1

WHEREABOUTS_NAMESPACE="whereabouts-api-$ENV"
WHEREABOUTS_SECRETS_FILE="whereabouts-api-secrets.yaml"

# Create the secrets YAML file
cat <<EOF > $WHEREABOUTS_SECRETS_FILE
apiVersion: v1
kind: Secret
metadata:
  name: feature-toggles
  namespace: $WHEREABOUTS_NAMESPACE
type: Opaque
stringData:
  LISTEN_FOR_COURT_EVENTS: "false"
  SEND_VIDEO_BOOKING_MIGRATE_EVENTS: "true"
EOF

# Apply the secret
kubectl apply -f ./$WHEREABOUTS_SECRETS_FILE
echo "Applied secrets to $WHEREABOUTS_NAMESPACE"

kubectl -n "$WHEREABOUTS_NAMESPACE" get secrets feature-toggles -o json | jq -r ".data | map_values(@base64d)"

rm -f ./$WHEREABOUTS_SECRETS_FILE

# Restart the pods
echo "Restarting pods on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deploy
