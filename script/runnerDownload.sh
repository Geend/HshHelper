#!/bin/bash

FILE_ID=5
THREADS=10
RUNS_PER_THREAD=100

for value in $(eval echo "{0..$THREADS}")
do
    ./downloadFileFromHshHelper.sh $value $FILE_ID $RUNS_PER_THREAD &
done
