#!/bin/sh

# Setup variables
EXEC=jsvc
JAVA_HOME=/usr/lib/jvm/java-6-sun
CLASS_PATH="./lib/*:./config:./jar/MultiDNS.jar"
CLASS=com.spencersevilla.server_mdns.MainClass
USER=spencer
PID=/tmp/example.pid
LOGFILE=logs/mdns_log.txt
LOG_ERR=/tmp/example.err

do_exec()
{
    $EXEC -home $JAVA_HOME -cp $CLASS_PATH -outfile $LOGFILE -errfile $LOGFILE -user $USER -pidfile $PID $1 $CLASS $2
}

case "$1" in
    start)
        do_exec
            ;;
    stop)
        do_exec "-stop"
            ;;
    restart)
        if [ -f "$PID" ]; then
            do_exec "-stop"
            do_exec
        else
            echo "service not running, will do nothing"
            exit 1
        fi
            ;;
    *)
            echo "usage: daemon {start|stop|restart}" >&2
            exit 3
            ;;
esac
