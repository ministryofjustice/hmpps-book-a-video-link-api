#!/bin/bash

ENV=$1
ACTION=$2

NAMESPACE="whereabouts-api-$ENV"
SECRETS_FILE="whereabouts-api-secrets.yaml"

if [ "$ACTION" = "out" ]; then
  LISTEN="false"
  SEND="true"
elif [ "$ACTION" = "back" ]; then
  LISTEN="true"
  SEND="false"
else
  echo "Invalid action '$ACTION' for whereabouts-api, terminating process."
  return 1 2> /dev/null || exit 1
fi

echo "Rolling $ACTION $NAMESPACE switches..."

# Create the secrets YAML file
cat <<EOF > $SECRETS_FILE
apiVersion: v1
kind: Secret
metadata:
  name: feature-toggles
  namespace: $NAMESPACE
type: Opaque
stringData:
  FEATURE_LISTEN_FOR_COURT_EVENTS: "$LISTEN"
  FEATURE_SEND_VIDEO_BOOKING_MIGRATE_EVENTS: "$SEND"
EOF

# Apply the secret
kubectl apply -f ./$SECRETS_FILE
echo "Applied secrets to $NAMESPACE"

rm -f ./$SECRETS_FILE

COURT_EVENTS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_LISTEN_FOR_COURT_EVENTS}' | base64 -d)
MIGRATE_EVENTS=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_SEND_VIDEO_BOOKING_MIGRATE_EVENTS}' | base64 -d)

echo "FEATURE_LISTEN_FOR_COURT_EVENTS=$COURT_EVENTS"
echo "FEATURE_SEND_VIDEO_BOOKING_MIGRATE_EVENTS=$MIGRATE_EVENTS"

# Restart the deployment pods
echo "Restarting deployment whereabouts-api-generic-service on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deployments/whereabouts-api-generic-service
