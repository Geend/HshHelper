#!/bin/bash

for value in {1..50}
do
    seq 0 6 | xargs -I '{}' -P 8 ./createUser.sh '{}' "scriptuser$value" "hsh.helper%2Buser$value%40gmail.com" '200'
done
