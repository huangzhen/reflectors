#!/bin/bash
source ~/.bashrc
DATE_YMD=`date '+%Y%m%d'`
Time=`date`
stime=3

a=3
until [ $a -lt 1  ] 
do
     sleep $stime

     if [ -f /gnome/reflectors/gnome-root-sparrow-server.pid ]
     then
        echo "check ok"
        break
     else
        echo "test err:" $a
     fi

     a=$(( $a - 1))
done;

if [ $a  -le  0  ] ; then
    #cd /gnome/sparrow01 && ./bin/gnome-daemon.sh start sparrow-server ./conf/sparrow.conf &

    echo "${Time} reflectors stop ok ." >> /log/reflectors01/reflectors_check.${DATE_YMD}.logs
    echo "reflectors01 status stop ok!"
else
    ps -ef|egrep "reflectors"|grep -v grep|awk '{print $2 }'|xargs kill -9
    rm -f /gnome/reflectors/gnome-root-sparrow-server.pid

    echo "${Time} reflectors stop." >> /log/reflectors/reflectors_check.${DATE_YMD}.logs
        echo "reflectors01 status ok"
fi
