#!/bin/bash

for value in {1..50}
do
    seq 0 10 | xargs -I '{}' -P 4 ./uploadFileToHshHelper.sh '{}' "clashname$value" comment ./upload.txt > /dev/null
done
