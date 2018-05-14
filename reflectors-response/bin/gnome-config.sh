# included in all the gnome scripts with source command
# should not be executable directly
# also should not be passed any arguments, since we need original $*

# resolve links - "${BASH_SOURCE-$0}" may be a softlink

this="${BASH_SOURCE-$0}"

while [ -h "$this" ]; do
  ls=`ls -ld "$this"`
  link=`expr "$ls" : '.*-> \(.*\)$'`

  if expr "$link" : '.*/.*' > /dev/null; then
    this="$link"
  else
    this=`dirname "$this"`/"$link"
  fi

done

# convert relative path to absolute path
bin=$(dirname "$this")
script=$(basename "$this")
bin=$(cd "$bin">/dev/null; pwd)
this="$bin/$script"

# the root of the gnome installation
if [ -z "$GNOME_HOME" ]; then
  export GNOME_HOME=$(dirname "$this")/..
fi

#check to see if the conf dir or gnome home are given as an optional arguments
while [ $# -gt 1 ]
do
  if [ "--config" = "$1" ]
  then
    shift
    confdir=$1
    shift
    GNOME_CONF_DIR=$confdir
  else
    # Presume we are at end of options and break
    break
  fi
done
 
# Allow alternate gnome conf dir location.
GNOME_CONF_DIR="${GNOME_CONF_DIR:-$GNOME_HOME/conf}"

# Source the gnome-env.sh.  Will have JAVA_HOME defined.
if [ -z "$GNOME_ENV_INIT" ] && [ -f "${GNOME_CONF_DIR}/gnome-env.sh" ]; then
  . "${GNOME_CONF_DIR}/gnome-env.sh"
  export GNOME_ENV_INIT="true"
fi

# Newer versions of glibc use an arena memory allocator that causes virtual
# memory usage to explode. Tune the variable down to prevent vmem explosion.
export MALLOC_ARENA_MAX=${MALLOC_ARENA_MAX:-4}

# Now having JAVA_HOME defined is required 
if [ -z "$JAVA_HOME" ]; then
    cat 1>&2 <<EOF
+======================================================================+
|                    Error: JAVA_HOME is not set                       |
+----------------------------------------------------------------------+
| Please download the latest Sun JDK from the Sun Java web site        |
|     > http://www.oracle.com/technetwork/java/javase/downloads        |
|                                                                      |
| Gnome requires Java 1.7 or later.                                    |
+======================================================================+
EOF
    exit 1
fi
