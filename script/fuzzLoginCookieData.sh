while [ 1 ]
do
RESPONSE=$(curl 'http://localhost:9000/login' \
    -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0' \
    -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' \
    -H 'Accept-Language: en-US,en;q=0.5' \
    -H 'Referer: http://localhost:9000/' \
    -H 'Connection: keep-alive' \
    -H 'Upgrade-Insecure-Requests: 1' \
    -c cookies.txt \
    -s \
    --compressed)

CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)

COUNT=$(( RANDOM % 1000  ))
PLAYSESSIONDATA=$(cat /dev/random | base64 | head -c $COUNT)
echo $PLAYSESSIONDATA

RESPONSE=$(curl 'http://localhost:9000/login' \
    -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0' \
    -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' \
    -H 'Accept-Language: en-US,en;q=0.5' \
    -H "Cookie: PLAY_SESSION=$PLAYSESSIONDATA" \
    -H 'Referer: http://localhost:9000/login' \
    -H 'Connection: keep-alive' \
    -H 'Upgrade-Insecure-Requests: 1' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --compressed \
    -s \
    --data "csrfToken=$CSRF&username=admin&password=admin")
done
