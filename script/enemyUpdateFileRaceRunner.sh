#!/bin/bash

for value in {1..50}
do
    seq 0 10 | xargs -I '{}' -P 4 ./enemyUpdateFile.sh '{}' ./upload.bin zweiter admin1 > /dev/null
done
