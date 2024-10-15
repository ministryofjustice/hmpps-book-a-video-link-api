#!/bin/bash

ENV=$1

WHEREABOUTS_NAMESPACE="whereabouts-api-$ENV"
WHEREABOUTS_SECRETS_FILE="whereabouts-api-secrets.yaml"

echo "Rolling out $WHEREABOUTS_NAMESPACE switches..."

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

rm -f ./$WHEREABOUTS_SECRETS_FILE

COURT_EVENTS=$(kubectl -n "$WHEREABOUTS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.LISTEN_FOR_COURT_EVENTS}' | base64 -d)
MIGRATE_EVENTS=$(kubectl -n "$WHEREABOUTS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.SEND_VIDEO_BOOKING_MIGRATE_EVENTS}' | base64 -d)

echo "LISTEN_FOR_COURT_EVENTS=$COURT_EVENTS"
echo "SEND_VIDEO_BOOKING_MIGRATE_EVENTS=$MIGRATE_EVENTS"

# Restart the pods
echo "Restarting pods on namespace $WHEREABOUTS_NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deploy
