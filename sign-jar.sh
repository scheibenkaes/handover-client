#! /usr/bin/env bash

if [ -n "$1" ]
then
    jarsigner -keystore keystore $1 self && cp $1 handover-webstart.jar
else
    echo "Please provide the jar file to be signed"
fi

