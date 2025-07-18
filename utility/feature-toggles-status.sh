#!/bin/bash

ENV=$1

BVLS_NAMESPACE="hmpps-book-a-video-link-$ENV"
FEATURE_GREY_RELEASE_PRISONS=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_GREY_RELEASE_PRISONS}' | base64 -d)
FEATURE_HMCTS_LINK_GUEST_PIN=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_HMCTS_LINK_GUEST_PIN}' | base64 -d)
FEATURE_PICK_UP_TIMES=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_PICK_UP_TIMES}' | base64 -d)
FEATURE_PROBATION_ONLY_PRISONS=$(kubectl -n "$BVLS_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.FEATURE_PROBATION_ONLY_PRISONS}' | base64 -d)

ACTIVITIES_NAMESPACE="hmpps-activities-management-$ENV"
AA_BVLS_FEATURE_HMCTS_LINK_GUEST_PIN=$(kubectl -n "$ACTIVITIES_NAMESPACE" get secret feature-toggles -o jsonpath='{.data.BVLS_FEATURE_HMCTS_LINK_GUEST_PIN}' | base64 -d)

DIGITAL_PRISON_SERVICES="digital-prison-services-$ENV"
DPS_BVLS_HMCTS_LINK_GUEST_PIN_FEATURE_TOGGLE_ENABLED=$(kubectl -n "$DIGITAL_PRISON_SERVICES" get secret feature-toggles -o jsonpath='{.data.BVLS_HMCTS_LINK_GUEST_PIN_FEATURE_TOGGLE_ENABLED}' | base64 -d)

echo ENVIRONMENT="$ENV"
echo "-----------------------"
echo BVLS - GREY_RELEASE_PRISONS="$FEATURE_GREY_RELEASE_PRISONS"
echo BVLS - FEATURE_HMCTS_LINK_GUEST_PIN="$FEATURE_HMCTS_LINK_GUEST_PIN"
echo BVLS - FEATURE_PICK_UP_TIMES="$FEATURE_PICK_UP_TIMES"
echo BVLS - FEATURE_PROBATION_ONLY_PRISONS="$FEATURE_PROBATION_ONLY_PRISONS"
echo
echo AA - HMCTS_LINK_GUEST_PIN="$AA_BVLS_FEATURE_HMCTS_LINK_GUEST_PIN"
echo
echo DPS  - FEATURE_HMCTS_LINK_GUEST_PIN="$DPS_BVLS_HMCTS_LINK_GUEST_PIN_FEATURE_TOGGLE_ENABLED"
echo
echo "Manual checks required in DPS Profile - requires env vars for switches"
