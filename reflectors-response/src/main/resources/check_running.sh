#!/bin/bash
source ~/.bashrc
DATE_YMD=`date '+%Y%m%d'`
Time=`date`
stime=3

a=3
until [ $a -lt 1  ] 
do
     sleep $stime

     if [ -f /gnome/adx01/gnome-root-adx-server.pid ]
     then
        echo "check ok"
        break
     else
        echo "test err:" $a
     fi

     a=$(( $a - 1))
done;

if [ $a  -le  0  ] ; then
    cd /gnome/adx01 && ./bin/gnome-daemon.sh start adx-server ./conf/adx.conf &

    echo "${Time} adx01  restart ." >> /log/adx01/adx01_check.${DATE_YMD}.logs
    echo "adx01 status restart"
else
    echo "${Time} adx01 is ok." >> /log/adx01/adx01_check.${DATE_YMD}.logs
    echo "adx01 status ok"
fi
