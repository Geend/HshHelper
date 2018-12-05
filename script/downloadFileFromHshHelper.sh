# parameter $1 = identifier
# parameter $2 = fileId
# parameter $3 = maxRuns
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

for i in $(eval echo "{0..$3}")
do
    curl "http://localhost:9000/files/$2/download" \
        -b cookies$1.txt \
        -c cookies$1.txt \
        --compressed \
        -s \
    > /dev/null
done

rm cookies$1.txt
