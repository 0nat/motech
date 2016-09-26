#!/usr/bin/env bash

if [ "$TRAVIS_EVENT_TYPE" != "cron" ]; then
    git clone https://github.com/motech/motech.git ../motech-master -b master --single-branch
    cd ../motech-master/platform/mds/mds-performance-tests/
    mvn clean install -Dmds.performance.quantity=10000 -U -PMDSP
fi