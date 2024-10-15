#!/bin/bash

ENV=$1
ACTION=$2

NAMESPACE="digital-prison-services-$ENV"
SECRETS_FILE="digital-prison-services-secrets.yaml"

if [ "$ACTION" = "out" ]; then
  BVLS_TOGGLED_ON="true"
elif [ "$ACTION" = "back" ]; then
  BVLS_TOGGLED_ON="false"
else
  echo "Invalid action '$ACTION' for '$NAMESPACE', terminating process."
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
  BOOK_A_VIDEO_LINK_API_ENABLED: "$BVLS_TOGGLED_ON"
EOF

# Apply the secret
kubectl apply -f ./$SECRETS_FILE
echo "Applied secrets to $NAMESPACE"

rm -f ./$SECRETS_FILE

BOOK_A_VIDEO_LINK_API_ENABLED=$(kubectl -n "$NAMESPACE" get secret feature-toggles -o jsonpath='{.data.BOOK_A_VIDEO_LINK_API_ENABLED}' | base64 -d)

echo "BOOK_A_VIDEO_LINK_API_ENABLED=$BOOK_A_VIDEO_LINK_API_ENABLED"

# Restart the pods
echo "Restarting pods on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deploy
