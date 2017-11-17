#!/bin/bash

if [ "$TRAVIS_OS_NAME" == "linux" ]; then
	pwd
	sudo service mosquitto stop
	mosquitto -h
	mosquitto -c test/tls-testing/mosquitto.conf &
fi

if [ "$TRAVIS_OS_NAME" == "osx" ]; then
	pwd
	brew update
	brew install openssl mosquitto
	brew services stop mosquitto
	/usr/local/sbin/mosquitto -c test/tls-testing/mosquitto.conf &
fi
