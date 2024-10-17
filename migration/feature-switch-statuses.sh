#!/bin/bash

# Simple script to show an overall view of the feature flags/switches

read -r -p "Enter the environment you wish to see the feature switches - dev/preprod/prod: " ENVIRONMENT

if [ -z "$ENVIRONMENT" ]
then
  echo "No environment specified."
  exit 99
fi

function whereabouts() {
  COURT_EVENTS=$(kubectl -n "whereabouts-api-$ENVIRONMENT" get secret feature-toggles -o jsonpath='{.data.FEATURE_LISTEN_FOR_COURT_EVENTS}' | base64 -d)
  MIGRATE_EVENTS=$(kubectl -n "whereabouts-api-$ENVIRONMENT" get secret feature-toggles -o jsonpath='{.data.FEATURE_SEND_VIDEO_BOOKING_MIGRATE_EVENTS}' | base64 -d)

  echo "whereabouts-api in environment $ENVIRONMENT"
  echo
  echo "FEATURE_LISTEN_FOR_COURT_EVENTS=$COURT_EVENTS"
  echo "FEATURE_SEND_VIDEO_BOOKING_MIGRATE_EVENTS=$MIGRATE_EVENTS"
}

function digitalPrisonServices() {
  BOOK_A_VIDEO_LINK_API_ENABLED=$(kubectl -n "digital-prison-services-$ENVIRONMENT" get secret feature-toggles -o jsonpath='{.data.BOOK_A_VIDEO_LINK_API_ENABLED}' | base64 -d)

  echo "digital-prison-services in environment $ENVIRONMENT"
  echo
  echo "BOOK_A_VIDEO_LINK_API_ENABLED=$BOOK_A_VIDEO_LINK_API_ENABLED"
}

function activitiesAndAppointments() {
  BOOK_A_VIDEO_LINK_FEATURE_TOGGLE_ENABLED=$(kubectl -n "hmpps-activities-management-$ENVIRONMENT" get secret feature-toggles -o jsonpath='{.data.BOOK_A_VIDEO_LINK_FEATURE_TOGGLE_ENABLED}' | base64 -d)

  echo "hmpps-activities-management in environment $ENVIRONMENT"
  echo
  echo "BOOK_A_VIDEO_LINK_FEATURE_TOGGLE_ENABLED=$BOOK_A_VIDEO_LINK_FEATURE_TOGGLE_ENABLED"
}

function bookAVideoLink() {
  MAINTENANCE_MODE=$(kubectl -n "hmpps-book-a-video-link-$ENVIRONMENT" get secret maintenance -o jsonpath='{.data.maintenance_mode}' | base64 -d)

  echo "hmpps-book-a-video-link in environment $ENVIRONMENT"
  echo
  echo "maintenance_mode=$MAINTENANCE_MODE"
}

echo "The new BLVS service rollout status in the '$ENVIRONMENT' environment is as follows:"

echo "---------------------------------------------------------"
bookAVideoLink
echo "---------------------------------------------------------"
whereabouts
echo "---------------------------------------------------------"
digitalPrisonServices
echo "---------------------------------------------------------"
activitiesAndAppointments
echo "---------------------------------------------------------"

