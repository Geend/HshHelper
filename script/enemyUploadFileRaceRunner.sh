#!/bin/bash


for value in {501..2000}
do
    seq 0 4 | xargs -I '{}' -P 4 ./enemyUploadFile.sh '{}' admin admin1 ~/Desktop/upload.txt fromscript$value hi
done
