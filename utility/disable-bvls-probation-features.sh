#!/bin/bash

ENV=$1

# Temporarily disable any prod runs
if [ "$ENV" = "prod" ]; then
  echo "Prod is currently disabled."
  return 1 2> /dev/null || exit 1
fi

while true; do

read -r -p "You are about to disable the new BVLS features in the '$ENV' environment. Are you sure you want to proceed? (y/n) " yn

case $yn in
	[yY] ) echo ok, proceeding...;
		break;;
	[nN] ) echo exiting...;
		exit;;
	* ) echo invalid response;;
esac
done

NAMESPACE="hmpps-book-a-video-link-$ENV"
SECRETS_FILE="book-a-video-link-secrets.yaml"

# Create the secrets YAML file
cat <<EOF > $SECRETS_FILE
apiVersion: v1
kind: Secret
metadata:
  name: feature-toggles
  namespace: $NAMESPACE
type: Opaque
stringData:
  FEATURE_ADMIN_LOCATION_DECORATION: "false"
  FEATURE_ALTERED_COURT_JOURNEY: "false"
  FEATURE_MASTER_VLPM_TYPES: "false"
  FEATURE_GREY_RELEASE_PRISONS: "BXI"
EOF

# Apply the secret
kubectl apply -f ./$SECRETS_FILE
echo "Applied secrets to $NAMESPACE"

rm -f ./$SECRETS_FILE

# Restart the deployment pods
echo "Restarting deployment hmpps-book-a-video-link-api pods on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deployments/hmpps-book-a-video-link-api
kubectl -n "$NAMESPACE" rollout restart deployments/hmpps-book-a-video-link-ui

source feature-toggles-status.sh "$ENV"