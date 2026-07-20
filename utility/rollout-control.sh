#!/bin/bash
#
# Script to manage the rollout options in Kubernetes secrets.
# ENV should be specified in lowercase, either dev, preprod or prod.
#

menu_function() {
  echo " BVLS admin email addresses - changes require API restart only"
  echo ""
  echo " 1 - Replace with a new list"
  echo " 2 - Add an email address"
  echo " 3 - Remove an email address"
  echo ""
  echo " Grey release prisons - changes require UI restart only"
  echo ""
  echo " 4 - Replace with a new list"
  echo " 5 - Add a grey release prison"
  echo " 6 - Remove a grey release prison"
  echo ""
  echo " Probation only prisons - changes require UI restart only"
  echo ""
  echo " 7 - Replace with a new list"
  echo " 8 - Add a probation only prison"
  echo " 9 - Remove a probation only prison"
  echo ""
  echo " Court only prisons - changes require UI restart only"
  echo ""
  echo " 10 - Replace with a new list"
  echo " 11 - Add a court only prison"
  echo " 12 - Remove a court only prison"
  echo ""
  echo " Availability checker only prisons - changes require Daily Schedule restart only"
  echo ""
  echo " 13 - Replace with a new list"
  echo " 14 - Add an availability checker prison"
  echo " 15 - Remove an availability checker prison"
  echo ""
  echo " Room blocking with times - changes require UI restart only"
  echo ""
  echo " 16 - Toggle the room blocking with times feature switch"
  echo ""
  echo " Restart services"
  echo ""
  echo " 17 - Restart BVLS UI for changes to take effect"
  echo " 18 - Restart BVLS API for changes to take effect"
  echo " 19 - Restart Daily Schedule for changes to take effect"
  echo " 20 - Restart services for changes to take effect"
  echo ""
  echo " 0 - Exit"
  echo "----------------------------"
}

show_current() {
  NAMESPACE=$1
  ENV=$2

  echo "Getting secrets from $ENV ..."

  # Get administration secret
  ADMINISTRATION_EMAILS=$(kubectl -n "$NAMESPACE" get secret administration -o jsonpath='{.data.ADMINISTRATION_EMAILS}' | base64 -d)

  # Get feature-toggles secret values
  KUBE_SECRET=feature-toggles
  read -r FEATURE_GREY_RELEASE_PRISONS FEATURE_PROBATION_ONLY_PRISONS FEATURE_COURT_ONLY_PRISONS FEATURE_ROOM_BLOCKING_WITH_TIMES FEATURE_AVAILABILITY_CHECKER_PRISONS < <(
    kubectl -n "$NAMESPACE" get secret "$KUBE_SECRET" -o json \
    | jq -r '.data | .FEATURE_GREY_RELEASE_PRISONS, .FEATURE_PROBATION_ONLY_PRISONS, .FEATURE_COURT_ONLY_PRISONS, .FEATURE_ROOM_BLOCKING_WITH_TIMES, .FEATURE_AVAILABILITY_CHECKER_PRISONS | @base64d' \
    | tr '\n' ' '
  )

  clear
  echo "-------------------------------------------------------------------------------------"
  echo "Environment                   : $ENV"
  echo ""
  echo "BVLS admin emails             : $ADMINISTRATION_EMAILS"
  echo "Grey release prisons          : $FEATURE_GREY_RELEASE_PRISONS"
  echo "Probation only prisons        : $FEATURE_PROBATION_ONLY_PRISONS"
  echo "Court only prisons            : $FEATURE_COURT_ONLY_PRISONS"
  echo "Room blocking with times      : $FEATURE_ROOM_BLOCKING_WITH_TIMES"
  echo "Availability checker prisons  : $FEATURE_AVAILABILITY_CHECKER_PRISONS"
  echo ""
}

add_list_bvls_admin_emails() {
  echo "Replace existing list with $3 for DPS enabled prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret administration -o jsonpath='{.data.ADMINISTRATION_EMAILS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"ADMINISTRATION_EMAILS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret administration -p $stringData
}

add_bvls_admin_email() {
  echo "Adding $3 to BVLS admin emails in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret administration -o jsonpath='{.data.ADMINISTRATION_EMAILS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"ADMINISTRATION_EMAILS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret administration -p $stringData
}

remove_bvls_admin_email() {
  echo "Removing prison $3 from DPS enabled prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret administration -o jsonpath='{.data.ADMINISTRATION_EMAILS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"ADMINISTRATION_EMAILS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret administration -p $stringData
}

add_list_grey_release_prison() {
  echo "Replace existing list with $3 for grey release prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_GREY_RELEASE_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_GREY_RELEASE_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_grey_release_prison() {
  echo "Adding $3 to grey release prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_GREY_RELEASE_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_GREY_RELEASE_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_grey_release_prison() {
  echo "Removing prison $3 from grey release prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_GREY_RELEASE_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_GREY_RELEASE_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_list_probation_only_prison() {
  echo "Replace existing list with $3 for probation only prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_PROBATION_ONLY_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_PROBATION_ONLY_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_probation_only_prison() {
  echo "Adding $3 to probation only prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_PROBATION_ONLY_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_PROBATION_ONLY_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_probation_only_prison() {
  echo "Removing prison $3 from probation only prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_PROBATION_ONLY_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_PROBATION_ONLY_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_list_court_only_prison() {
  echo "Replace existing list with $3 for court only prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_COURT_ONLY_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_COURT_ONLY_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_court_only_prison() {
  echo "Adding $3 to court only prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_COURT_ONLY_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_COURT_ONLY_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_court_only_prison() {
  echo "Removing prison $3 from court only prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_COURT_ONLY_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_COURT_ONLY_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_list_availability_checker_prison() {
  echo "Replace existing list with $3 for availability checker prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_AVAILABILITY_CHECKER_PRISONS}' | base64 -d)
  NEW=$3
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_AVAILABILITY_CHECKER_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

add_availability_checker_prison() {
  echo "Adding $3 to availability checker prisons in $1 namespace $2"
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_AVAILABILITY_CHECKER_PRISONS}' | base64 -d)
  NEW="$CURRENT,$3"
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_AVAILABILITY_CHECKER_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

remove_availability_checker_prison() {
  echo "Removing prison $3 from availability checker prisons in $1 namespace $2"
  prison=$3
  CURRENT=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_AVAILABILITY_CHECKER_PRISONS}' | base64 -d)
  NEW=$(echo ",$CURRENT," | sed "s/,$prison,/,/g; s/^,//; s/,$//")
  echo "Applying new value : $NEW"
  stringData="{\"stringData\":{\"FEATURE_AVAILABILITY_CHECKER_PRISONS\":\"$NEW\"}}"
  kubectl -n "$2" patch secret feature-toggles -p $stringData
}

toggle_room_blocking_times() {
  local env="$1"
  local namespace="$2"
  current_value=$(kubectl -n "$2" get secret feature-toggles -o jsonpath='{.data.FEATURE_ROOM_BLOCKING_WITH_TIMES}' | base64 -d)

  if [[ "$current_value" == "true" ]]; then
     new_value=false
  else
     new_value=true
  fi

  echo "Toggling room blocking times from $current_value to $new_value in $env namespace $namespace"

  stringData="{\"stringData\":{\"FEATURE_ROOM_BLOCKING_WITH_TIMES\":\"$new_value\"}}"
  kubectl -n "$namespace" patch secret feature-toggles -p $stringData
}

restart_bvls_ui() {
   echo "Restarting BVLS UI service in $1 namespace $2"
   kubectl -n "$2" rollout restart deployments/hmpps-book-a-video-link-ui
}

restart_bvls_api() {
   echo "Restarting BVLS API service in $1 namespace $2"
   kubectl -n "$2" rollout restart deployments/hmpps-book-a-video-link-api
}

restart_daily_schedule() {
   echo "Restarting Daily Schedule service in $1 namespace $2"
   kubectl -n "$2" rollout restart deployments/hmpps-video-conference-schedule-ui
}

restart_all_services() {
   restart_bvls_ui "$1" "$2"
   restart_daily_schedule "$1" "$2"
   restart_bvls_api "$1" "$2"
}

ENV=$1
NAMESPACE="hmpps-book-a-video-link-$ENV"

# Temporarily disable any prod runs
if [ "$ENV" = "prod" ]; then
  echo ""
  echo "Prod is currently disabled, you must comment this check out of the script to bypass."
  echo ""
  return 1 2> /dev/null || exit 1
fi

while true; do
  show_current "$NAMESPACE" "$ENV"
  menu_function "$ENV"
  read -p "Select an option: " choice

  case $choice in
      1)  echo "Replace BVLS admin emails with a new list"
          read -p "Enter a comma-separated list of emails to replace the current list : " email_list
          add_list_bvls_admin_emails "$ENV" "$NAMESPACE" "$email_list"
          ;;
      2)  echo "Add an email to the admin emails list"
          read -p "Enter an email to add : " email
          add_bvls_admin_email "$ENV" "$NAMESPACE" "$email"
          ;;
      3)  echo "Remove an email from the admin emails list"
          read -p "Enter an email to remove : " email
          remove_bvls_admin_email "$ENV" "$NAMESPACE" "$email"
          ;;
      4)  echo "Replace grey release prisons with a new list"
          read -p "Enter a comma-separated list of grey prisons to replace the current list : " grey_prison_list
          add_list_grey_release_prison "$ENV" "$NAMESPACE" "$grey_prison_list"
          ;;
      5)  echo "Add a prison to the grey release prisons list"
          read -p "Enter a grey prison to add : " grey_prison
          add_grey_release_prison "$ENV" "$NAMESPACE" "$grey_prison"
          ;;
      6)  echo "Remove a prison from the grey release prisons list"
          read -p "Enter a grey prison to remove : " grey_prison
          remove_grey_release_prison "$ENV" "$NAMESPACE" "$grey_prison"
          ;;
      7)  echo "Replace probation only prisons with a new list"
          read -p "Enter a comma-separated list of probation only prisons to replace the current list : " probation_only_prison_list
          add_list_probation_only_prison "$ENV" "$NAMESPACE" "$probation_only_prison_list"
          ;;
      8)  echo "Add a prison to the probation only prisons list"
          read -p "Enter a probation only prison to add : " probation_only_prison
          add_probation_only_prison "$ENV" "$NAMESPACE" "$probation_only_prison"
          ;;
      9)  echo "Remove a prison from the probation only prisons list"
          read -p "Enter a probation only prison to remove : " probation_only_prison
          remove_probation_only_prison "$ENV" "$NAMESPACE" "$probation_only_prison"
          ;;
      10)  echo "Replace court only prisons with a new list"
          read -p "Enter a comma-separated list of court only prisons to replace the current list : " court_only_prison_list
          add_list_court_only_prison "$ENV" "$NAMESPACE" "$court_only_prison_list"
          ;;
      11)  echo "Add a prison to the court only prisons list"
          read -p "Enter a court only prison to add : " court_only_prison
          add_court_only_prison "$ENV" "$NAMESPACE" "$court_only_prison"
          ;;
      12)  echo "Remove a prison from the court only prisons list"
          read -p "Enter a court only prison to remove : " court_only_prison
          remove_court_only_prison "$ENV" "$NAMESPACE" "$court_only_prison"
          ;;
      13) echo "Replace availability checker prisons with a new list"
          read -p "Enter a comma-separated list of availability checker prisons to replace the current list : " availability_checker_prison_list
          add_list_availability_checker_prison "$ENV" "$NAMESPACE" "$availability_checker_prison_list"
          ;;
      14) echo "Add a prison to the availability checker prisons list"
          read -p "Enter an availability checker prison to add : " availability_checker_prison
          add_availability_checker_prison "$ENV" "$NAMESPACE" "$availability_checker_prison"
          ;;
      15) echo "Remove a prison from the availability checker prisons list"
          read -p "Enter an availability checker prison to remove : " availability_checker_prison
          remove_availability_checker_prison "$ENV" "$NAMESPACE" "$availability_checker_prison"
          ;;
      16)  echo "Toggle the room blocking with times value"
           toggle_room_blocking_times "$ENV" "$NAMESPACE"
          ;;
      17) echo "Restarting BVLS UI"
          restart_bvls_ui "$ENV" "$NAMESPACE"
          ;;
      18) echo "Restarting BVLS API"
          restart_bvls_api "$ENV" "$NAMESPACE"
          ;;
      19) echo "Restarting Daily Schedule"
          restart_daily_schedule "$ENV" "$NAMESPACE"
          ;;
      20) echo "Restarting all services"
          restart_all_services "$ENV" "$NAMESPACE"
          ;;
      0)  echo "Exiting..."
          exit 0
          ;;
      *)  echo "Invalid selection. Please try again."
          ;;
  esac
  echo ""
done

# End
