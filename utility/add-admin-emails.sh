#!/bin/bash

ENV=$1
EMAILS=$2

# Temporarily disable any prod runs
if [ "$ENV" = "prod" ]; then
  echo "Prod is currently disabled."
  return 1 2> /dev/null || exit 1
fi

while true; do

echo
read -r -p "You are about add admin emails '$EMAILS' to the BVLS service in '$ENV' environment. Do you want to proceed? (y/n) " yn

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
  name: administration
  namespace: $NAMESPACE
type: Opaque
stringData:
  ADMINISTRATION_EMAILS: "$EMAILS"
EOF

# Apply the secret
kubectl apply -f ./$SECRETS_FILE
echo
echo "Applied secrets to $NAMESPACE"

rm -f ./$SECRETS_FILE

# Restart the deployment pods
echo
echo "Restarting BVLS API deployment in namespace $NAMESPACE"
kubectl -n "$NAMESPACE" rollout restart deployments/hmpps-book-a-video-link-api
