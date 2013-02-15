#!/bin/sh

# Setup variables
EXEC=jsvc
JAVA_HOME="/usr/libexec/java_home"
CLASS_PATH="./lib/*:./config:./jar/FERNManager.jar"
CLASS=com.spencersevilla.fern.server.MainClass
USER=ssevilla
PID=/tmp/example.pid
LOGFILE=logs/mdns_log.txt
LOG_ERR=/tmp/example.err

do_exec()
{
    $EXEC -cp $CLASS_PATH -outfile $LOGFILE -errfile $LOGFILE -user $USER -pidfile $PID $1 $CLASS $2
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
