#!/bin/bash
# parameter $1 = identifier
# parameter $2 = username
# parameter $3 = currentpassword
# parameter $4 = newpassword

RESPONSE=$(curl 'http://localhost:9000/login' \
    -c activateUserCookie$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/login' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -b activateUserCookie$1.txt \
    -c activateUserCookie$1.txt \
    --compressed \
    -s \
    --data "csrfToken=$CSRF" \
    --data "username=$2" \
    --data-urlencode "password=$3")

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/changePasswordAfterReset' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -b activateUserCookie$1.txt \
    -c activateUserCookie$1.txt \
    --compressed \
    -s \
    --data "csrfToken=$CSRF" \
    --data "username=$2" \
    --data-urlencode "currentPassword=$3" \
    --data "password=$4" \
    --data "passwordRepeat=$4")

rm activateUserCookie$1.txt
