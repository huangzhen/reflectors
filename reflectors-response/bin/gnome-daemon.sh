#!/usr/bin/env bash
#
# Runs a GNOME command as a daemon.
#
# Environment Variables
#
#   GNOME_CONF_DIR       Alternate gnome conf dir. Default is ${GNOME_HOME}/conf.
#   GNOME_LOG_DIR        Where log files are stored.  PWD by default.
#   GNOME_PID_DIR        The pid files are stored. /tmp by default.
#   GNOME_IDENT_STRING   A string representing this instance of gnome. $USER by default
#   GNOME_NICENESS       The scheduling priority for daemons. Defaults to 0.
#   GNOME_STOP_TIMEOUT   Time, in seconds, after which we kill -9 the server if it has not stopped.
#                        Default 1200 seconds.
#

usage="Usage: gnome-daemon.sh [--config <conf-dir>]\
 (start|foreground_start|stop|restart) <gnome-command> \
 <args...>"

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

bin=$(dirname "${BASH_SOURCE-$0}")
bin=$(cd "$bin">/dev/null; pwd)

. "$bin"/gnome-config.sh
. "$bin"/gnome-common.sh

# get arguments
startStop=$1
shift

command=$1
shift

gnome_rotate_log (){
    log=$1;
    num=5;

    if [ -n "$2" ]; then
        num=$2
    fi

    # rotate logs
    if [ -f "$log" ]; then
        while [ $num -gt 1 ]; do
            prev=`expr $num - 1`
            [ -f "$log.$prev" ] && mv -f "$log.$prev" "$log.$num"
            num=$prev
        done
        mv -f "$log" "$log.$num";
    fi
}

cleanAfterRun() {
  if [ -f ${GNOME_PID} ]; then
    # If the process is still running time to tear it down.
    kill -9 `cat ${GNOME_PID}` > /dev/null 2>&1
    rm -f ${GNOME_PID} > /dev/null 2>&1
  fi
}

check_before_start(){
    #ckeck if the process is not running
    mkdir -p "$GNOME_PID_DIR"
    if [ -f $GNOME_PID ]; then
      if kill -0 `cat $GNOME_PID` > /dev/null 2>&1; then
        echo $command running as process `cat $GNOME_PID`.  Stop it first.
        exit 1
      fi
    fi
}

wait_until_done (){
    p=$1
    cnt=${GNOME_SLAVE_TIMEOUT:-300}
    origcnt=$cnt

    while kill -0 $p > /dev/null 2>&1; do
      if [ $cnt -gt 1 ]; then
        cnt=`expr $cnt - 1`
        sleep 1
      else
        echo "Process did not complete after $origcnt seconds, killing."
        kill -9 $p
        exit 1
      fi
    done

    return 0
}

# get log directory
if [ "$GNOME_LOG_DIR" = "" ]; then
  export GNOME_LOG_DIR="$GNOME_HOME/logs"
fi
mkdir -p "$GNOME_LOG_DIR"

if [ "$GNOME_PID_DIR" = "" ]; then
  GNOME_PID_DIR="$GNOME_HOME"
fi

if [ "$GNOME_IDENT_STRING" = "" ]; then
  export GNOME_IDENT_STRING="$USER"
fi

# Some variables
# Work out java location so can print version into log.
if [ "$JAVA_HOME" != "" ]; then
  #echo "run java in $JAVA_HOME"
  JAVA_HOME=$JAVA_HOME
fi
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

JAVA=$JAVA_HOME/bin/java
export GNOME_LOG_PREFIX=gnome-$GNOME_IDENT_STRING-$command-$HOSTNAME
export GNOME_LOGFILE=$GNOME_LOG_PREFIX.log

if [ -z "${GNOME_ROOT_LOGGER}" ]; then
    export GNOME_ROOT_LOGGER=${GNOME_ROOT_LOGGER:-"INFO,RFA"}
fi

GNOME_LOGOUT=${GNOME_LOGOUT:-"$GNOME_LOG_DIR/$GNOME_LOG_PREFIX.out"}
GNOME_LOGGC=${GNOME_LOGGC:-"$GNOME_LOG_DIR/$GNOME_LOG_PREFIX.gc"}
GNOME_LOGLOG=${GNOME_LOGLOG:-"${GNOME_LOG_DIR}/${GNOME_LOGFILE}"}
GNOME_PID=$GNOME_PID_DIR/gnome-$GNOME_IDENT_STRING-$command.pid

if [ -n "$SERVER_GC_OPTS" ]; then
  export SERVER_GC_OPTS=${SERVER_GC_OPTS/"-Xloggc:<FILE-PATH>"/"-Xloggc:${GNOME_LOGGC}"}
fi
if [ -n "$CLIENT_GC_OPTS" ]; then
  export CLIENT_GC_OPTS=${CLIENT_GC_OPTS/"-Xloggc:<FILE-PATH>"/"-Xloggc:${GNOME_LOGGC}"}
fi

# Set default scheduling priority
if [ "$GNOME_NICENESS" = "" ]; then
    export GNOME_NICENESS=0
fi

thiscmd="$bin/$(basename ${BASH_SOURCE-$0})"
args=$@

case $startStop in

(start)
    check_before_start
    gnome_rotate_log $GNOME_LOGOUT
    gnome_rotate_log $GNOME_LOGGC
    echo starting $command, logging to $GNOME_LOGOUT
    $thiscmd --config "${GNOME_CONF_DIR}" \
        foreground_start $command $args < /dev/null > ${GNOME_LOGOUT} 2>&1  &
    disown -h -r
    sleep 1; head "${GNOME_LOGOUT}"
  ;;

(foreground_start)
    trap cleanAfterRun SIGHUP SIGINT SIGTERM EXIT
    if [ "$GNOME_NO_REDIRECT_LOG" != "" ]; then
        # NO REDIRECT
        echo "`date` Starting $command on `hostname`"
        echo "`ulimit -a`"
        # in case the parent shell gets the kill make sure to trap signals.
        # Only one will get called. Either the trap or the flow will go through.
        nice -n $GNOME_NICENESS "$GNOME_HOME"/bin/gnome \
            --config "${GNOME_CONF_DIR}" \
            $command "$@" &
            #$command "$@" start &
    else
        echo "`date` Starting $command on `hostname`" >> ${GNOME_LOGLOG}
        echo "`ulimit -a`" >> "$GNOME_LOGLOG" 2>&1
        # in case the parent shell gets the kill make sure to trap signals.
        # Only one will get called. Either the trap or the flow will go through.
        nice -n $GNOME_NICENESS "$GNOME_HOME"/bin/gnome \
            --config "${GNOME_CONF_DIR}" \
            $command "$@" >> ${GNOME_LOGOUT} 2>&1 &
            #$command "$@" start >> ${GNOME_LOGOUT} 2>&1 &
    fi
    # Add to the command log file vital stats on our environment.
    gnome_pid=$!
    echo $gnome_pid > ${GNOME_PID}
    wait $gnome_pid
  ;;

(stop)
    rm -f "$GNOME_START_FILE"
    if [ -f $GNOME_PID ]; then
      pidToKill=`cat $GNOME_PID`
      # kill -0 == see if the PID exists
      if kill -0 $pidToKill > /dev/null 2>&1; then
        echo -n stopping $command
        echo "`date` Terminating $command" >> $GNOME_LOGLOG
        kill $pidToKill > /dev/null 2>&1
        waitForProcessEnd $pidToKill $command
      else
        retval=$?
        echo no $command to stop because kill -0 of pid $pidToKill failed with status $retval
      fi
    else
      echo no $command to stop because no pid file $GNOME_PID
    fi
    rm -f $GNOME_PID
  ;;

(restart)
    # stop the command
    $thiscmd --config "${GNOME_CONF_DIR}" stop $command $args &
    wait_until_done $!
    # wait a user-specified sleep period
    sp=${GNOME_RESTART_SLEEP:-3}
    if [ $sp -gt 0 ]; then
      sleep $sp
    fi
    # start the command
    $thiscmd --config "${GNOME_CONF_DIR}" start $command $args &
    wait_until_done $!
  ;;

(*)
  echo $usage
  exit 1
  ;;
esac
