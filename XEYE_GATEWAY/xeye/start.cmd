set CLASSPATH=.

set CLASSPATH=%CLASSPATH%;./resource/lib/bacnet4J.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/bluecove-2.1.1-SNAPSHOT.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-beanutils-1.9.2.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-codec-1.8.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-collections-3.2.1.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-lang.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-lang3-3.5.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-logging.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-dbcp.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/commons-pool.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/ezmorph-1.0.6.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/jamod-1.2-SNAPSHOT.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/jdom.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/jms-api-1.1-rev-1.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/json-lib.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/log4j-1.2.15.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/pi4j-core.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/quartz-2.2.3.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/seroUtils.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/slf4j-api-1.7.7.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/snmp4j-2.4.1.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/snmp4j-agent-2.4.1.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/RXTXcomm.jar
set CLASSPATH=%CLASSPATH%;./resource/lib/xeye-agent.jar

java -Xmx256m -Xms256m -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:-CMSParallelRemarkEnabled -classpath "%CLASSPATH%" com.hoonit.xeye.Main