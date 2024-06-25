#!/bin/sh

echo "XEYE Gateway stopping...!"

PROCESS_ALIAS="XEYE_AGENT"

CNT=`ps ex | grep "${PROCESS_ALIAS}" | grep -v grep | wc -l`
PROCESS=`ps ex | grep "${PROCESS_ALIAS}" | grep -v grep | awk '{print $1}'`

if [ $CNT -ne 0 ]; then
	kill -9 $PROCESS
	echo "PID:$PROCESS is stopped...!"
else
	echo "XEYE Gateway is not working...!"
fi