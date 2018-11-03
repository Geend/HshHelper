#CSRFTOKEN=$(curl localhost:9000/users/create -b cookies.txt | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}')
#echo $CSRFTOKEN
#curl localhost:9000/users/create -b cookies.txt -d "csrfToken=$CSRFTOKEN" -d "email=test@test.de" -d "quotaLimit=3" -d "username=script" -v --trace-ascii /dev/stdout
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

RESPONSE=$(curl 'http://localhost:9000/login' \
    -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0' \
    -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' \
    -H 'Accept-Language: en-US,en;q=0.5' \
    -H 'Referer: http://localhost:9000/login' \
    -H 'Connection: keep-alive' \
    -H 'Upgrade-Insecure-Requests: 1' \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -b cookies.txt \
    -c cookies.txt \
    --compressed \
    -s \
    --data "csrfToken=$CSRF&username=admin&password=admin")


for i in {1000..10000}
do
    RESPONSE=$(curl 'http://localhost:9000/users/create' \
        -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0' \
        -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' \
        -H 'Accept-Language: en-US,en;q=0.5' \
        -H 'Referer: http://localhost:9000/' \
        -H 'Connection: keep-alive' \
        -H 'Upgrade-Insecure-Requests: 1' \
        -b cookies.txt \
        -c cookies.txt \
        -s \
        --compressed)

    CSRF=$(echo $RESPONSE | grep -Eo -m 1 '[0-9a-f]{40}-[0-9a-f]{13}-[0-9a-f]{24}' | sort -u)
    echo $CSRF

    curl 'http://localhost:9000/users/create' \
        -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0' \
        -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' \
        -H 'Accept-Language: en-US,en;q=0.5' \
        -H 'Referer: http://localhost:9000/users/create' \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -H 'Connection: keep-alive' \
        -H 'Upgrade-Insecure-Requests: 1' \
        -b cookies.txt \
        -c cookies.txt \
        -s \
        --compressed \
        --data "csrfToken=$CSRF&username=script$i&email=test$i%40test.de&quotaLimit=4" > /dev/null
done


rm cookies.txt
