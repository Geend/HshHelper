#!/bin/bash
# parameter $1 = identifier
# parameter $2 = fileToUpload
# parameter $3 = username
# parameter $4 = password
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
    --data "csrfToken=$CSRF&credentials=$3&password=$4")

RESPONSE=$(curl 'http://localhost:9000/file/b44f443a-40e9-4546-b26a-023a65782f34' \
    -H 'Connection: keep-alive' \
    -H 'Upgrade-Insecure-Requests: 1' \
    -b cookies$1.txt \
    -c cookies$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/file/b44f443a-40e9-4546-b26a-023a65782f34/update' \
    -F "csrfToken=$CSRF" \
    -F "updatedFile=@$2" \
    -b cookies$1.txt \
    -c cookies$1.txt \
    --compressed \
    -o /dev/null \
    -D - \
    -s)

rm cookies$1.txt
