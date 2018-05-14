#!/bin/bash
source ~/.bashrc
DATE_YMD=`date '+%Y%m%d'`
Time=`date`
stime=3

a=3
until [ $a -lt 1  ] 
do
     sleep $stime

     if [ -f /gnome/sprrow/gnome-root-sparrow-server.pid ]
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

    echo "${Time} sprrow stop ok ." >> /log/sprrow01/sprrow_check.${DATE_YMD}.logs
    echo "sprrow01 status stop ok!"
else
    ps -ef|egrep "sprrow"|grep -v grep|awk '{print $2 }'|xargs kill -9
    rm -f /gnome/sprrow/gnome-root-sparrow-server.pid

    echo "${Time} sprrow stop." >> /log/sprrow/sprrow_check.${DATE_YMD}.logs
        echo "sprrow01 status ok"
fi
