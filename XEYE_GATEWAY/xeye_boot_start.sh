#!/bin/sh

export JAVA_HOME=/usr/java/jdk1.8.0_73
export PATH=$JAVA_HOME/bin:$PATH

SERVICE_NAME=xeye_gateway
PATH_TO_JAR=/home/xeye
PID_PATH_NAME=/home/xeye/${SERVICE_NAME}.pid
PROCESS_ALIAS="XEYE_AGENT"

cd $PATH_TO_JAR

CLASSPATH=.
CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
CLASSPATH=$CLASSPATH:./resource/lib/bacnet4J.jar
CLASSPATH=$CLASSPATH:./resource/lib/bluecove-2.1.0.jar
CLASSPATH=$CLASSPATH:./resource/lib/bluecove-gpl-2.1.0.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-beanutils-1.9.2.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-codec-1.8.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-collections-3.2.1.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-lang.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-lang3-3.5.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-logging.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-dbcp.jar
CLASSPATH=$CLASSPATH:./resource/lib/commons-pool.jar
CLASSPATH=$CLASSPATH:./resource/lib/ezmorph-1.0.6.jar
CLASSPATH=$CLASSPATH:./resource/lib/jamod-1.2-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:./resource/lib/jdom.jar
CLASSPATH=$CLASSPATH:./resource/lib/jms-api-1.1-rev-1.jar
CLASSPATH=$CLASSPATH:./resource/lib/json-lib.jar
CLASSPATH=$CLASSPATH:./resource/lib/log4j-1.2.15.jar
CLASSPATH=$CLASSPATH:./resource/lib/pi4j-core.jar
CLASSPATH=$CLASSPATH:./resource/lib/quartz-2.2.3.jar
CLASSPATH=$CLASSPATH:./resource/lib/seroUtils.jar
CLASSPATH=$CLASSPATH:./resource/lib/slf4j-api-1.7.7.jar
CLASSPATH=$CLASSPATH:./resource/lib/snmp4j-2.4.1.jar
CLASSPATH=$CLASSPATH:./resource/lib/snmp4j-agent-2.4.1.jar
CLASSPATH=$CLASSPATH:./resource/lib/RXTXcomm.jar
CLASSPATH=$CLASSPATH:./resource/lib/xeye-agent.jar

case $1 in
    start)
        echo "Starting XEYE Gateway..."
        echo $CLASSPATH
        nohup java -Xms256m -Xmx256m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:-CMSParallelRemarkEnabled -classpath $CLASSPATH -Du=$PROCESS_ALIAS com.hoonit.xeye.Main > /dev/null 2>&1 &
                    echo $! > $PID_PATH_NAME
        echo "XEYE Gateway started ..."
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "XEYE Gateway stoping ..."
            kill $PID;
            echo "XEYE Gateway stopped ..."
            rm $PID_PATH_NAME
        else
            echo "XEYE Gateway is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "XEYE Gateway stopping ...";
            kill $PID;
            echo "XEYE Gateway stopped ...";
            rm $PID_PATH_NAME
            echo "XEYE Gateway starting ..."
            nohup java -Xms256m -Xmx256m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:-CMSParallelRemarkEnabled -classpath $CLASSPATH -Du=$PROCESS_ALIAS com.hoonit.xeye.Main > /dev/null 2>&1 &
                        echo $! > $PID_PATH_NAME
            echo "XEYE Gateway started ..."
        else
            echo "XEYE Gateway is not running ..."
        fi
    ;;
    status)
        if [ -f $PID_PATH_NAME ]; then
        echo "XEYE Gateway is running ... (PID:" $(cat $PID_PATH_NAME)")"
        else
        echo "XEYE Gateway is not running ..." 
        exit 1
   fi
   ;;
*)

echo "Usage: $0 {start|stop|status|restart}"

esac 
