## jnofify 说明

1. linux 和 wins 目录下是 Linux 和 Window 操作系统的库依赖文件
2. 官网下载地址：https://sourceforge.net/projects/jnotify/
3. Linux 操作系统需要把 libjnotify.so 拷贝或软链到 /lib 目录。若是 64 位操作系统需要把 libjnotify_64bit.so 拷贝或软链到 /lib64目录下，并把 libjnotify_64bit.so 改名为 libjnotify.so 
4. Window 操作系统 需要把 jnotify.dll 和 jnotify_64bit.dll 拷贝到 `C:\Windows` 目录下