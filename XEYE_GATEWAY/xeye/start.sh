#!/bin/sh

echo "XEYE Gateway Starting...!"

LIB_PATH="./resource/lib"

LOG_PATH="./logs"

EXE_JAR="xeye-agent.jar"

PROCESS_ALIAS="XEYE_AGENT"

SERVICE_NAME=xeye_gateway
PID_PATH_NAME=/home/xeye/${SERVICE_NAME}.pid

CLASSPATH="${LIB_PATH}/bacnet4J.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/bluecove-2.1.0.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/bluecove-gpl-2.1.0.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-beanutils-1.9.2.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-codec-1.8.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-collections-3.2.1.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-lang.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-lang3-3.5.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-logging.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-dbcp.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/commons-pool.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/ezmorph-1.0.6.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/jamod-1.2-SNAPSHOT.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/jdom.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/jms-api-1.1-rev-1.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/json-lib.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/log4j-1.2.15.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/pi4j-core.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/quartz-2.2.3.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/seroUtils.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/slf4j-api-1.7.7.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/snmp4j-2.4.1.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/snmp4j-agent-2.4.1.jar:${CLASSPATH}"
CLASSPATH="${LIB_PATH}/RXTXcomm.jar:${CLASSPATH}"

CLASSPATH="${LIB_PATH}/${EXE_JAR}:${CLASSPATH}"
export CLASSPATH

echo "."
echo "CLASSPATH=${CLASSPATH}"
echo "."

if [ ! -d ${LOG_PATH} ]; then
	mkdir ${LOG_PATH}
fi

CNT=`ps ex | grep "${PROCESS_ALIAS}" | grep -v grep | wc -l`
PROCESS=`ps ex | grep "${PROCESS_ALIAS}" | grep -v grep | awk '{print $1}'`

if [ $CNT -ne 0 ]; then
	echo "PID:$PROCESS is already running...!"
else
	nohup java -Xms256m -Xmx256m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:-CMSParallelRemarkEnabled -classpath .:${CLASSPATH} -Du=${PROCESS_ALIAS} com.hoonit.xeye.Main > /dev/null 2>&1 &
	echo $! > $PID_PATH_NAME
	echo "XEYE Gateway is started...!"
fi
