#!/bin/bash

# The purpose of this script is to ease the rollout of the new BVLS service by automating the toggling of features
# by applying changes to feature toggles set up as K8's secrets and restarting the pods in the affected namespaces.

read -r -p "Enter the environment you wish to rollout the new BVLS service - dev/preprod/prod: " ENVIRONMENT

if [ -z "$ENVIRONMENT" ]
then
  echo "No environment specified."
  exit 99
fi

shopt -s nocasematch

while true; do

read -r -p "You are about to rollout the new BVLS service in the '$ENVIRONMENT' environment. Are you sure you want to proceed? (y/n) " yn

case $yn in
	[yY] ) echo ok, proceeding...;
		break;;
	[nN] ) echo exiting...;
		exit;;
	* ) echo invalid response;;
esac

done

# Each service that needs changes applying should be added as a separate file and sourced below.
source whereabouts-api.sh "$ENVIRONMENT" out
source activities-appointments.sh "$ENVIRONMENT" out
source digital-prison-services.sh "$ENVIRONMENT" out

echo "The new BLVS service is now rolled out in the '$ENVIRONMENT' environment. Please check all affected service pods have been restarted."
