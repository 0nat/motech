#!/usr/bin/env bash

set -e

PERF_DIR=./platform/mds/mds-performance-tests

PERF_RES_DIR=$PERF_DIR/src/test/resources

TRESHOLDS_FILE=/tmp/tresholds.csv

touch $TRESHOLDS_FILE
mkdir $PERF_DIR/target
touch $PERF_DIR/target/performanceTestResult.log

cat > $TRESHOLDS_FILE <<EOL
MdsStressIT,stressTestCreating,40000
MdsStressIT,stressTestRetrieval,300
MdsStressIT,stressTestUpdating,40000
MdsStressIT,stressTestDeleting,250000
MdsDiskSpaceUsageIT,testEudeDiskSpaceUsage,25
EOL

cat $PERF_RES_DIR/performanceCheck.sh -d ~/perf $PERF_DIR/target/performanceTestResult.log $TRESHOLDS_FILE
RESULT=$?

rm -f $TRESHOLDS_FILE

cat $PERF_DIR/target/performanceTestResult.log

exit $RESULT
