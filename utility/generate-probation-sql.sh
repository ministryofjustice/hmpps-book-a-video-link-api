#!/bin/bash
#
# Script to generate the SQL to add new probation teams and contact emails.
# This takes a CSV file with 3 values: team description, code, email address
# The file should have no blank lines or missing values
#
# Usage:  ./generate-probation-sql.sh <file>

# Check if a filename was provided
if [[ $# -eq 0 ]]; then
    echo "Usage: $0 filename.csv"
    exit 1
fi

FILE=$1

# Check if the file exists
if [[ ! -f "$FILE" ]]; then
    echo "Error: File '$FILE' not found."
    exit 1
fi

# Get number of lines/teams in the file
numberOfTeams=$(wc -l < "$FILE")
echo "$numberOfTeams teams"

# Generate creation of probation teams for Flyway migration
echo "----------- Team creation SQL (for Flyway migration) -------------------"
echo ""

((lineNo=1))

echo "-- New probation teams"
echo "insert into probation_team (code, description, enabled, read_only, notes, created_by, created_time)"
echo "values "

while IFS=',' read -r description code email; do
  if [[ $lineNo -eq $numberOfTeams ]]; then
    echo "('$code', '$description', true, false, null, 'TIM', current_timestamp);"
  else
    echo "('$code', '$description', true, false, null, 'TIM', current_timestamp),"
  fi

  ((lineNo++))
done < "$FILE"
echo ""

# Generate uniqueness check - manually run

echo "----------- Uniqueness check SQL (manually run) -------------------"
echo ""

((lineNo=1))

echo "select * from probation_team where code in ("

while IFS=',' read -r description code email; do
  if [[ $lineNo -eq $numberOfTeams ]]; then
    echo "'$code'"
    echo ");"
  else
    echo "'$code',"
  fi

  ((lineNo++))
done < "$FILE"
echo ""

# Generate SQL to add contacts to preprod and prod

echo "----------- Team contact creation (manually run) -------------------"
echo ""

while IFS=',' read -r description code email; do
  echo ""
  echo "-- Contact for $description"
  echo "insert into probation_team_contact (probation_team_id, name, email, enabled, notes, primary_contact, created_by, created_time)"
  echo "select probation_team_id, 'Probation administration', '$email', true, '', true, 'TIM', current_timestamp"
  echo "from probation_team where code = '$code';"
done < "$FILE"

# End