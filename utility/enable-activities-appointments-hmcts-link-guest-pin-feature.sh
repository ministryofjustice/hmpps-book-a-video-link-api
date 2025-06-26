#!/bin/bash

ENV=$1

# Temporarily disable any prod runs
if [ "$ENV" = "prod" ]; then
  echo "Prod is currently disabled."
  return 1 2> /dev/null || exit 1
fi

while true; do

echo
read -r -p "You are about to enable the BVLS HMCTS link and guest pin features in the activities and appointments '$ENV' environment. Are you sure you want to proceed? (y/n) " yn

case $yn in
	[yY] ) echo ok, proceeding...;
		break;;
	[nN] ) echo exiting...;
		exit;;
	* ) echo invalid response;;
esac
done

NAMESPACE="hmpps-activities-management-$ENV"
SECRETS_FILE="activities-appointments-secrets.yaml"

# Create the secrets YAML file
cat <<EOF > $SECRETS_FILE
apiVersion: v1
kind: Secret
metadata:
  name: feature-toggles
  namespace: $NAMESPACE
type: Opaque
stringData:
  BVLS_FEATURE_HMCTS_LINK_GUEST_PIN: "true"
EOF

# Apply the secret
kubectl apply -f ./$SECRETS_FILE
echo
echo "Applied secrets to $NAMESPACE"

rm -f ./$SECRETS_FILE

# Restart the deployment pods
echo
echo "Restarting activities and appointments deployment on namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deployments/hmpps-activities-management
