#!/bin/bash
source ~/.bashrc
DATE_YMD=`date '+%Y%m%d'`
Time=`date`
stime=3

a=3
until [ $a -lt 1  ] 
do
     sleep $stime

     if [ -f /gnome/reflectors01/gnome-root-reflectors-server.pid ]
     then
        echo "check ok"
        break
     else
        echo "test err:" $a
     fi

     a=$(( $a - 1))
done;

if [ $a  -le  0  ] ; then
    cd /gnome/reflectors01 && ./bin/gnome-daemon.sh start reflectors-server ./conf/reflectors.conf &

    echo "${Time} reflectors01  restart ." >> /log/reflectors01/reflectors01_check.${DATE_YMD}.logs
    echo "reflectors01 status restart"
else
    echo "${Time} reflectors01 is ok." >> /log/reflectors01/reflectors01_check.${DATE_YMD}.logs
    echo "reflectors01 status ok"
fi
