# parameter $1 = identifier
# parameter $2 = filename
# parameter $3 = comment
# parameter $4 = fileToUpload
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
    --data "csrfToken=$CSRF&username=klaus&password=klaus")

RESPONSE=$(curl 'http://localhost:9000/files/upload' \
    -H 'Connection: keep-alive' \
    -H 'Upgrade-Insecure-Requests: 1' \
    -b cookies$1.txt \
    -c cookies$1.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

curl 'http://localhost:9000/files/upload' \
    -F "csrfToken=$CSRF" \
    -F "filename=$2" \
    -F "comment=$3" \
    -F "file=@$4" \
    -b cookies$1.txt \
    -c cookies$1.txt \
    --compressed \
    -s

rm cookies$1.txt
