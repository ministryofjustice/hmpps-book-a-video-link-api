#
# This script generates curl commands to send VIDEO_BOOKING_AMENDED events
# for a list of video_booking_ids provided in an input file. These curls
# can only be executed locally on a k8s BVLS API pod, through a commandline 
# bash shell.

dest='http://localhost:8080/utility/publish'
headers='-H "Content-Type: application/json" -H "Accept: text/plain"'
numbers=""
let maxBatchSize=100
let lineNo=0

while read -r line
do
  if [ $lineNo -eq 0 ]
  then
    numbers="-$line"
  else
    numbers="$numbers,-$line"
  fi

  lineNo=$((lineNo+1))

  if [ $lineNo -eq $maxBatchSize ]
  then
   cat <<EOF
    curl -X POST $dest $headers --data '{ "event": "VIDEO_BOOKING_AMENDED", "identifiers":[$numbers] }'
EOF
    echo "-------------------------------------------------"
    let lineNo=0
  fi
done < input-file.txt

# Get any remaining lines
cat <<EOF
    curl -X POST $dest $headers --data '{ "event": "VIDEO_BOOKING_AMENDED", "identifiers":[$numbers] }'
EOF

