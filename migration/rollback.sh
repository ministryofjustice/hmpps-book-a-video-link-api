#!/bin/bash

# The purpose of this script is to ease the rollback of the new BVLS service by automating the toggling of features
# by applying changes to feature toggles set up as K8's secrets and restarting the pods in the affected namespaces.

read -r -p "Enter the environment you wish to rollback the new BVLS service - dev/preprod/prod: " ENVIRONMENT

if [ -z "$ENVIRONMENT" ]
then
  echo "No environment specified."
  exit 99
fi

shopt -s nocasematch

# Temporarily disable any prod runs
if [ "$ENVIRONMENT" = "prod" ]; then
  echo "Prod is currently disabled."
  return 1 2> /dev/null || exit 1
fi

while true; do

read -r -p "You are about to rollback the new BVLS service in the '$ENVIRONMENT' environment. Are you sure you want to proceed? (y/n) " yn

case $yn in
	[yY] ) echo ok, proceeding...;
		break;;
	[nN] ) echo exiting...;
		exit;;
	* ) echo invalid response;;
esac

done

# Each service that needs changes applying should be added as a separate file and sourced below.
source whereabouts-api.sh "$ENVIRONMENT" back
source activities-appointments.sh "$ENVIRONMENT" back
source digital-prison-services.sh "$ENVIRONMENT" back

echo "The new BLVS service is now rolled back in the '$ENVIRONMENT' environment. Please check all affected service pods have been restarted."
