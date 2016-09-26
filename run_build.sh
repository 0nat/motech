#!/usr/bin/env bash

if [ "$TRAVIS_EVENT_TYPE" != "cron" ]; then
    git clone https://github.com/motech/motech.git ../motech-master -b master --single-branch
    cd ../motech-master/platform/mds/mds-performance-tests/
    mvn clean install -Dmds.performance.quantity=10000 -U -PMDSP

    set -e

    PERF_DIR=.

    PERF_RES_DIR=$PERF_DIR/src/test/resources

    TRESHOLDS_FILE=/tmp/tresholds.csv

    touch $TRESHOLDS_FILE

    cat > $TRESHOLDS_FILE <<EOL
    MdsStressIT,stressTestCreating,40000
    MdsStressIT,stressTestRetrieval,300
    MdsStressIT,stressTestUpdating,40000
    MdsStressIT,stressTestDeleting,250000
    MdsDiskSpaceUsageIT,testEudeDiskSpaceUsage,25
    EOL

    $PERF_RES_DIR/performanceCheck.sh -d ~/perf $PERF_DIR/target/performanceTestResult.log $TRESHOLDS_FILE
    RESULT=$?

    rm -f $TRESHOLDS_FILE

    exit $RESULT
fi