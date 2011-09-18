#! /usr/bin/env bash

if [ -n "$1" ]
then
    lein jnlp && jarsigner -keystore keystore $1 self
else
    echo "Please provide the jar file to be signed"
fi

