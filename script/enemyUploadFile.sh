#!/bin/bash
# parameter $1 = identifier
# parameter $2 = username
# parameter $3 = password
# parameter $4 = fileToUpload
# parameter $5 = filename
# parameter $6 = comment
RESPONSE=$(curl 'http://localhost:9000/login' \
    -c cookies$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/login' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -b cookies$1.txt \
    -c cookies$1.txt \
    --compressed \
    -s \
    --data "csrfToken=$CSRF&credentials=$2&password=$3")

RESPONSE=$(curl 'http://localhost:9000/fileupload' \
    -H 'Connection: keep-alive' \
    -H 'Upgrade-Insecure-Requests: 1' \
    -b cookies$1.txt \
    -c cookies$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/fileupload' \
    -F "csrfToken=$CSRF" \
    -F "file=@$4" \
    -F "filename=$5" \
    -F "comment=$6" \
    -b cookies$1.txt \
    -c cookies$1.txt \
    --compressed \
    -o /dev/null \
    -D - \
    -s)

rm cookies$1.txt
