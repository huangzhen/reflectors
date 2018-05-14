# Set environment variables here.

# This script sets variables multiple times over the course of starting an gnome process,
# so try to keep things idempotent unless you want to take an even deeper look
# into the startup scripts (bin/gnome, etc.)

# The java implementation to use.  Java 1.7+ required.
# export JAVA_HOME=/usr/java/jdk1.7.0/

# Extra Java CLASSPATH elements.  Optional.
export GNOME_CLASSPATH=${GNOME_HOME}

export GNOME_MIN_MAX_HEAPSZIE=1g

# The maximum amount of heap to use. Default is left to JVM default.
export GNOME_HEAPSIZE=${GNOME_MIN_MAX_HEAPSZIE}

# Uncomment below if you intend to use off heap cache. For example, to allocate 8G of 
# offheap, set the value to "8G".
# export GNOME_OFFHEAPSIZE=1G

# Extra Java runtime options.
# Below are what we set by default.  May only work with SUN JVM.
export GNOME_OPTS="-server -Xms${GNOME_HEAPSIZE} -XX:NewRatio=3"

# Uncomment one of the below three options to enable java garbage collection logging for the server-side processes.

# This enables basic gc logging to the .out file.
export SERVER_GC_OPTS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps"

# This enables basic gc logging to its own file.
# If FILE-PATH is not replaced, the log file(.gc) would still be generated in the GNOME_LOG_DIR .
# export SERVER_GC_OPTS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:<FILE-PATH>"

# This enables basic GC logging to its own file with automatic log rolling. Only applies to jdk 1.6.0_34+ and 1.7.0_2+.
# If FILE-PATH is not replaced, the log file(.gc) would still be generated in the GNOME_LOG_DIR .
# export SERVER_GC_OPTS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:<FILE-PATH> -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=1 -XX:GCLogFileSize=512M"

# Uncomment one of the below three options to enable java garbage collection logging for the client processes.

# This enables basic gc logging to the .out file.
# export CLIENT_GC_OPTS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps"

# This enables basic gc logging to its own file.
# If FILE-PATH is not replaced, the log file(.gc) would still be generated in the GNOME_LOG_DIR .
# export CLIENT_GC_OPTS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:<FILE-PATH>"

# This enables basic GC logging to its own file with automatic log rolling. Only applies to jdk 1.6.0_34+ and 1.7.0_2+.
# If FILE-PATH is not replaced, the log file(.gc) would still be generated in the GNOME_LOG_DIR .
# export CLIENT_GC_OPTS="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:<FILE-PATH> -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=1 -XX:GCLogFileSize=512M"

# Uncomment and adjust to enable JMX exporting
# See jmxremote.password and jmxremote.access in $JRE_HOME/lib/management to configure remote password access.
# More details at: http://java.sun.com/javase/6/docs/technotes/guides/management/agent.html
# export GNOME_JMX_BASE="-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"

# Where log files are stored.  $GNOME_HOME/logs by default.
# export GNOME_LOG_DIR=${GNOME_HOME}/logs
export GNOME_LOG_DIR=${GNOME_HOME}/logs

# Enable remote JDWP debugging of major GNOME processes. Meant for Core Developers 
# export GNOME_OPTS="$GNOME_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8070"

# A string representing this instance of gnome. $USER by default.
# export GNOME_IDENT_STRING=$USER

# The scheduling priority for daemon processes.  See 'man nice'.
# export GNOME_NICENESS=10

# The directory where pid files are stored. /tmp by default.
# export GNOME_PID_DIR=/var/gnome/pids
export GNOME_PID_DIR=${GNOME_HOME}

