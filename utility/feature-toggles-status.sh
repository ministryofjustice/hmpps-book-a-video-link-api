#!/bin/bash

ENV=$1
BVLS_NAMESPACE="hmpps-book-a-video-link-$ENV"

FEATURE_ADMIN_LOCATION_DECORATION=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_ADMIN_LOCATION_DECORATION}' | base64 -d)
FEATURE_ALTERED_COURT_JOURNEY=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_ALTERED_COURT_JOURNEY}' | base64 -d)
FEATURE_MASTER_VLPM_TYPES=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_MASTER_VLPM_TYPES}' | base64 -d)
FEATURE_GREY_RELEASE_PRISONS=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_GREY_RELEASE_PRISONS}' | base64 -d)

echo "--------------------------------------------------------------------------------"
echo
echo "Status of feature toggles in '$BVLS_NAMESPACE'"
echo
echo "FEATURE_ADMIN_LOCATION_DECORATION=$FEATURE_ADMIN_LOCATION_DECORATION"
echo "FEATURE_ALTERED_COURT_JOURNEY=$FEATURE_ALTERED_COURT_JOURNEY"
echo "FEATURE_MASTER_VLPM_TYPES=$FEATURE_MASTER_VLPM_TYPES"
echo "FEATURE_GREY_RELEASE_PRISONS=$FEATURE_GREY_RELEASE_PRISONS"
echo

ACTIVITIES_NAMESPACE="hmpps-activities-management-$ENV"

BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED=$(kubectl -n "$ACTIVITIES_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED}' | base64 -d)

echo "--------------------------------------------------------------------------------"
echo
echo "Status of feature toggles in '$ACTIVITIES_NAMESPACE'"
echo
echo "BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED=$BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED"
echo

DPS_NAMESPACE="digital-prison-services-$ENV"

BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED=$(kubectl -n "$DPS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED}' | base64 -d)

echo "--------------------------------------------------------------------------------"
echo
echo "Status of feature toggles in '$DPS_NAMESPACE'"
echo
echo "BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED=$BVLS_MASTERED_VLPM_FEATURE_TOGGLE_ENABLED"
echo
