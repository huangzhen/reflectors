package org.httpkit.server;

import com.sywc.reflectors.share.GThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

public class HttpServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    private final String ip;
    private final int port;
    private int maxBody;
    private int maxLine;
    private int maxWs;
    private final IHandler handler;
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final String name;
    private final int nioThreadNum;
    private List<IOWorker> iOWorkerList = new ArrayList<IOWorker>();
    private long recvSocketNum;
    private AtomicLong recvReqSeqno = new AtomicLong(1L);
    private ExecutorService acceptor;
    private ExecutorService iOWorkerPool;
    private volatile boolean running = false;

    public HttpServer(String ip, int port, IHandler handler) throws IOException {
        this(ip, port, handler, 10);
    }

    public HttpServer(String ip, int port, IHandler handler, int ioWorkerNum) throws IOException {
        this(ip,
                port,
                handler,
                20480,
                2048,
                1024 * 1024 * 4,
                ioWorkerNum);
    }

    public HttpServer(String ip,
                      int port,
                      IHandler handler,
                      int maxBody,
                      int maxLine,
                      int maxWs,
                      int workThreadNum) throws IOException {
        this.ip = ip;
        this.port = port;
        this.handler = handler;

        this.maxBody = maxBody;
        this.maxLine = maxLine;
        this.maxWs = maxWs;

        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(this.ip, this.port), 10240);
        serverChannel.register(selector, OP_ACCEPT);

        name = "server" + this.port;
        recvSocketNum = 0;
        acceptor = Executors.newSingleThreadExecutor(new GThreadFactory(name + "-acceptor"));
        iOWorkerPool = Executors.newFixedThreadPool(workThreadNum, new GThreadFactory(name + "-pool"));

        nioThreadNum = workThreadNum;
    }

    public String getReqId() {
        return "HttpServer" + port + "-" + recvReqSeqno.getAndDecrement();
    }

    private void accept(SelectionKey key) {
        ServerSocketChannel ch = (ServerSocketChannel) key.channel();
        SocketChannel s;
        try {
            while ((s = ch.accept()) != null) {
                /* 接收到http请求链路后, 将链路绑定到一个Work线程 */
                SocketAddress remoteAddress = s.getRemoteAddress();

                int idxNioThread = (int) (recvSocketNum % nioThreadNum);
                if (!iOWorkerList.get(idxNioThread).addMsg(IOWorker.NIOTHREAD_MSGID_001_BINDING_SOCKET, s)) {
                    LOGGER.error("socket({}) binding failed!", remoteAddress);
                } else {
                    LOGGER.debug("socket({}) binding succeeded!", remoteAddress);
                }

                recvSocketNum++;
            }
        } catch (Exception e) {
            LOGGER.error("accept coming but exception, info is: ", e);
        }
    }

    public void start() throws IOException {
        if (running) {
            LOGGER.warn("{} has been started, and it is running.", name);
            return;
        }

        running = true;

        for (int i = 0; i < this.nioThreadNum; ++i) {
            IOWorker ioWorker = new IOWorker(this, name + "worker" + String.format("%02d", i), handler, maxBody, maxLine, maxWs);
            iOWorkerList.add(ioWorker);
            iOWorkerPool.submit(ioWorker);
        }
        acceptor.submit(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        if (selector.select() <= 0) {
                            continue;
                        }
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();
                        for (SelectionKey key : selectedKeys) {
                            // TODO I do not know if this is needed
                            // if !valid, isAcceptable, isReadable.. will Exception
                            // run hours happily after commented, but not sure.
                            if (!key.isValid()) {
                                continue;
                            }
                            if (key.isAcceptable()) {
                                accept(key);
                            } else if (key.isReadable()) {
                                /* 异常 */
                                LOGGER.error("recv read msg");
                            } else if (key.isWritable()) {
                                /* 异常 */
                                LOGGER.error("recv write msg");
                            }
                        }
                        selectedKeys.clear();
                    } catch (ClosedSelectorException ignore) {
                        return; // stopped
                        // do not exits the while IO event loop. if exits, then will not
                        // process any IO event
                        // jvm can catch any exception, including OOM
                    } catch (Throwable e) {
                        // catch any exception(including OOM), print it
                        LOGGER.error("http server loop error, should not happen, exception info is: ", e);
                    }
                }
            }
        });
        LOGGER.info("{} has been started.", name);
    }

    public void stop() {
        stop(100, TimeUnit.MILLISECONDS);
    }

    public void stop(long timeout, TimeUnit timeUnit) {
        if (!running) {
            LOGGER.info("{} has been stopped, and no need to stop it again.", name);
            return;
        }
        running = false;
        long start = System.nanoTime();
        try {
            LOGGER.info("shutting down the serverChannel ...");
            serverChannel.close(); // stop accept any request
            LOGGER.info("shutting down the serverChannel successfully.");
        } catch (IOException e) {
            LOGGER.error("shutting down the serverChannel failed: ", e);
        }

        LOGGER.info("stopping {} ...", name);
        if (acceptor != null) {
            LOGGER.info("shutting down the acceptor ...");
            acceptor.shutdown();
            boolean isTermited = false;
            try {
                long rest = timeUnit.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                isTermited = acceptor.awaitTermination(rest, timeUnit);
            } catch (InterruptedException e) {
                LOGGER.error("shutting down the acceptor failed: ", e);
            }
            if (isTermited) {
                LOGGER.info("shutting down the acceptor successfully.");
            }
        }

        // TODO: 2017/3/21 这样关闭可能会导致 IOWorker 还未执行关闭消息就被干掉了
        for (IOWorker ioWorker : iOWorkerList) {
            ioWorker.addMsg(IOWorker.NIOTHREAD_MSGID_002_CLOSE_THREAD, null);
        }

        if (iOWorkerPool != null) {
            LOGGER.info("shutting down the iOWorkerPool ...");
            iOWorkerPool.shutdown();
            boolean isTermited = false;
            try {
                long rest = timeUnit.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                isTermited = iOWorkerPool.awaitTermination(rest, timeUnit);
            } catch (InterruptedException e) {
                LOGGER.error("shutting down the iOWorkerPool failed: ", e);
            }
            if (isTermited) {
                LOGGER.info("shutting down the iOWorkerPool successfully.");
            }
        }

        try {
            LOGGER.info("shutting down the selector ...");
            selector.close();
            LOGGER.info("closing the selector successfully.");
        } catch (IOException e) {
            LOGGER.error("closing the selector failed: ", e);
        }

        LOGGER.info("stopping {} successfully.", name);
    }

    public int getPort() {
        return this.serverChannel.socket().getLocalPort();
    }
}
