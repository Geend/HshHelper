#!/bin/bash
# parameter $1 = identifier
# parameter $2 = username
# parameter $3 = email            hsh.helper%2Buser$1%40gmail.com
# parameter $4 = quota

RESPONSE=$(curl 'http://localhost:9000/login' \
    -c createUserCookie$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/login' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -b createUserCookie$1.txt \
    -c createUserCookie$1.txt \
    --compressed \
    -s \
    --data "csrfToken=$CSRF&username=admin&password=admin")

RESPONSE=$(curl 'http://localhost:9000/users/create' \
    -b createUserCookie$1.txt \
    -c createUserCookie$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

RESPONSE=$(curl 'http://localhost:9000/users/create' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -b createUserCookie$1.txt \
    -c createUserCookie$1.txt \
    -s \
    --compressed \
    --data "csrfToken=$CSRF&username=$2&email=$3&quotaLimit=$4")

PASSWORD=$(echo $RESPONSE | grep -oE 'Temporäres Passwort: .{12}' | grep -o '[^ ]*$')
echo $PASSWORD

rm createUserCookie$1.txt
