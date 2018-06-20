package org.httpkit.server;

import org.httpkit.HeaderMap;
import org.httpkit.LineTooLargeException;
import org.httpkit.ProtocolException;
import org.httpkit.RequestTooLargeException;
import org.httpkit.server.Frame.BinaryFrame;
import org.httpkit.server.Frame.CloseFrame;
import org.httpkit.server.Frame.PingFrame;
import org.httpkit.server.Frame.PongFrame;
import org.httpkit.server.Frame.TextFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static org.httpkit.HttpUtils.HttpEncode;
import static org.httpkit.HttpUtils.WsEncode;
import static org.httpkit.server.Frame.CloseFrame.CLOSE_AWAY;
import static org.httpkit.server.Frame.CloseFrame.CLOSE_MESG_BIG;
import static org.httpkit.server.Frame.CloseFrame.CLOSE_NORMAL;

class PendingKey {
    public static final int OP_WRITE = -1;
    public final SelectionKey key;
    // operation: can be register for write or close the selectionkey
    public final int Op;

    PendingKey(SelectionKey key, int op) {
        this.key = key;
        Op = op;
    }
}


class IOMsg {
    int msgId;
    Object objMsg;

    IOMsg(int msgId, Object objMsg) {
        this.msgId = msgId;
        this.objMsg = objMsg;
    }
}


class IOWorker implements Runnable {
    public static final int NIOTHREAD_MSGID_001_BINDING_SOCKET = 1;
    public static final int NIOTHREAD_MSGID_002_CLOSE_THREAD = 2;
    public static final int NIOTHREAD_MSGID_003_PENDING_OPR = 3;
    private final Logger LOG = LoggerFactory.getLogger(IOWorker.class);
    private final IHandler handler;

    /* max http body size */
    private final int maxBody;

    /* max header line size */
    private final int maxLine;

    /* websocket max messagesize */
    private final int maxWs;
    private final Selector selector;

    private final String name;

    private final HttpServer owner;

    /*
     * 异步线程操作队列, 里面存放着需要处理的非实时网络I/O事件, 主要有三类: 1 异步线程关闭事件 2 异步线程绑定http链路事件 3
     * 异步线程在已建链路上的主动操作事件(PendingKey中的各种事件: 延迟写、非keep-alive链路断开等...)
     */
    private final ConcurrentLinkedQueue<IOMsg> pending = new ConcurrentLinkedQueue<IOMsg>();

    /* shared, single thread */
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 64);

    IOWorker(HttpServer httpServer,
             String name,
             IHandler handler,
             int maxBody,
             int maxLine,
             int maxWs) throws IOException {
        this.owner = httpServer;
        this.handler = handler;
        this.maxLine = maxLine;
        this.maxBody = maxBody;
        this.maxWs = maxWs;

        this.selector = Selector.open();
        this.name = name;
    }

    /**
     * test by xmzheng for protobuffer, this method should not be used for others
     *
     * @param content
     */
    private static void wirteRequest2File(byte[] content) {
        try {

//      System.out.println(content.length);
            FileOutputStream out = new FileOutputStream("wmk_request_content_21");
            out.write(content, 0, content.length);
            out.flush();
            out.getFD().sync();
            out.close();
        } catch (Exception e) {

        }
    }

    public boolean addMsg(int msgId, Object objMsg) {
        if (pending.add(new IOMsg(msgId, objMsg))) {
            selector.wakeup();
            return true;
        }

        return false;
    }

    private void closeKey(final SelectionKey key, int status) {
        try {
            key.channel().close();
        } catch (Exception ignore) {
        }

        ServerAtta att = (ServerAtta) key.attachment();
        if (att instanceof HttpAtta) {
            handler.clientClose(att.channel, -1);
        } else if (att != null) {
            handler.clientClose(att.channel, status);
        }
    }

    private void decodeHttp(HttpAtta atta, SelectionKey key, SocketChannel ch) {
        try {
            do {
                AsyncChannel channel = atta.channel;
                HttpRequest request = atta.decoder.decode(buffer);

                if (request != null) {
                    channel.reset(request);
                    if (request.isWebSocket) {
                        key.attach(new WsAtta(channel, maxWs));
                    } else {
                        atta.keepalive = request.isKeepAlive;
                    }
                    request.channel = channel;
                    request.remoteAddr = (InetSocketAddress) ch.socket().getRemoteSocketAddress();
                    request.localAddr = (InetSocketAddress) ch.socket().getLocalSocketAddress();
                    String httpReqId = owner.getReqId();
                    handler.handle(request, new RespCallback(key, this, httpReqId));
                    // pipelining not supported : need queue to ensure order
                    atta.decoder.reset();
                }
            } while (buffer.hasRemaining()); // consume all
        } catch (ProtocolException e) {
            closeKey(key, -1);
        } catch (RequestTooLargeException e) {
            LOG.warn("", e);
            atta.keepalive = false;
            tryWrite(key, "httpcode413", HttpEncode(413, new HeaderMap(), e.getMessage()));
        } catch (LineTooLargeException e) {
            atta.keepalive = false; // close after write
            tryWrite(key, "httpcode414", HttpEncode(414, new HeaderMap(), e.getMessage()));
        }
    }

    private void decodeWs(WsAtta atta, SelectionKey key) {
        try {
            do {
                Frame frame = atta.decoder.decode(buffer);
                if (frame instanceof TextFrame || frame instanceof BinaryFrame) {
                    handler.handle(atta.channel, frame);
                    atta.decoder.reset();
                } else if (frame instanceof PingFrame) {
                    atta.decoder.reset();
                    tryWrite(key, "ws", WsEncode(WSDecoder.OPCODE_PONG, frame.data));
                } else if (frame instanceof PongFrame) {
                    atta.decoder.reset();
                    tryWrite(key, "ws", WsEncode(WSDecoder.OPCODE_PING, frame.data));
                } else if (frame instanceof CloseFrame) {
                    handler.clientClose(atta.channel, ((CloseFrame) frame).getStatus());
                    // close the TCP connection after sent
                    atta.keepalive = false;
                    tryWrite(key, "ws", WsEncode(WSDecoder.OPCODE_CLOSE, frame.data));
                }
            } while (buffer.hasRemaining()); // consume all
        } catch (ProtocolException e) {
            LOG.error("ProtocolException: ", e);
            closeKey(key, CLOSE_MESG_BIG); // TODO more specific error
        }
    }

    private void doRead(final SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        try {
            buffer.clear(); // clear for read
            int read = ch.read(buffer);
            if (read == -1) {
                // remote entity shut the socket down cleanly.
                closeKey(key, CLOSE_AWAY);
            } else if (read > 0) {
                buffer.flip(); // flip for read
                final ServerAtta atta = (ServerAtta) key.attachment();
                if (atta instanceof HttpAtta) {
                    decodeHttp((HttpAtta) atta, key, ch);
                } else {
                    decodeWs((WsAtta) atta, key);
                }
            }
        } catch (IOException e) { // the remote forcibly closed the connection
            closeKey(key, CLOSE_AWAY);
        }
    }

    private void doWrite(SelectionKey key) {
        ServerAtta atta = (ServerAtta) key.attachment();
        SocketChannel ch = (SocketChannel) key.channel();
        try {
            // the sync is per socket (per client). virtually, no contention
            // 1. keep byte data order, 2. ensure visibility
            synchronized (atta) {
                LinkedList<ByteBuffer> toWrites = atta.toWrites;
                {
                    // todo The following if block is just for show the response to the Tester.***
//          if (toWrites != null) {
//            DynamicBytes dynamicBytes = new DynamicBytes(4096);
//            int respLen;
//            int size = toWrites.size();
//            if (size < 1) {
//              LOG.debug("buffers.length < 1");
//              dynamicBytes.append("null");
//            } else {
//              ByteBuffer buffer = toWrites.get(size - 1);
//              while (buffer.hasRemaining()) {
//                dynamicBytes.append(buffer.get());
//              }
//              buffer.flip();
//            }
//            respLen = dynamicBytes.length();
//
//            try {
//              LOG.debug("$$$$pending response to the client is: " + new String(dynamicBytes.get(), 0, respLen, "utf-8"));
//            } catch (UnsupportedEncodingException e) {
//              LOG.debug(e.getMessage());
//            }
//          }
                }
                int size = toWrites.size();
//        LOG.trace("pending write: size = " + size);
                if (size == 1) {
                    ch.write(toWrites.get(0));
                    // TODO investigate why needed.
                    // ws request for write, but has no data?
                } else if (size > 0) {
                    ByteBuffer buffers[] = new ByteBuffer[size];
                    toWrites.toArray(buffers);
                    ch.write(buffers, 0, buffers.length);
                }
                Iterator<ByteBuffer> ite = toWrites.iterator();
                while (ite.hasNext()) {
                    if (!ite.next().hasRemaining()) {
                        ite.remove();
                    }
                }
                // all done
                if (toWrites.size() == 0) {
                    if (atta.isKeepAlive()) {
                        key.interestOps(OP_READ);
                    } else {
                        closeKey(key, CLOSE_NORMAL);
                    }
                }
            }
        } catch (IOException e) { // the remote forcibly closed the connection
            closeKey(key, CLOSE_AWAY);
        }
    }

    public void tryWrite(final SelectionKey key, String httpReqSeqId, ByteBuffer... buffers) {
        // todo The following if block is just for show the response to the Tester.***
//    if (buffers != null) {
//      DynamicBytes dynamicBytes = new DynamicBytes(4096);
//      int respLen;
//      int size = buffers.length;
//      if (size < 1) {
//        LOG.debug("buffers.length < 1");
//        dynamicBytes.append("null");
//      } else {
//        ByteBuffer buffer = buffers[size - 1];
//        while (buffer.hasRemaining()) {
//          dynamicBytes.append(buffer.get());
//        }
//        buffer.flip();
//      }
//      respLen = dynamicBytes.length();
//
//      try {
//        LOG.debug("$$$$response to the client is: " + new String(dynamicBytes.get(), 0, respLen, "utf-8"));
//      } catch (UnsupportedEncodingException e) {
//        LOG.debug(e.getMessage());
//      }
//    }
        tryWrite(key, httpReqSeqId, false, buffers);
    }

    public void tryWrite(final SelectionKey key,
                         String httpReqSeqId,
                         boolean chunkInprogress,
                         ByteBuffer... buffers) {
        ServerAtta atta = (ServerAtta) key.attachment();
        synchronized (atta) {
            SocketChannel ch = (SocketChannel) key.channel();
            atta.chunkedResponseInprogress(chunkInprogress);
            if (atta.toWrites.isEmpty()) {
                try {
                    // TCP buffer most of time is empty, writable(8K ~ 256k)
                    // One IO thread => One thread reading + Many thread writing
                    // Save 2 system call
                    ch.write(buffers, 0, buffers.length);
                    LOG.debug("send http-rsp({}) to SocketChannel", httpReqSeqId);

                    if (buffers[buffers.length - 1].hasRemaining()) {
                        /* 异常处理: 这个情况什么时候出现呢？？？？ 后面留意 */
                        for (ByteBuffer b : buffers) {
//              LOG.trace("tw...");
                            if (b.hasRemaining()) {
                                atta.toWrites.add(b);
                            }
                        }

                        addMsg(NIOTHREAD_MSGID_003_PENDING_OPR, new PendingKey(key, PendingKey.OP_WRITE));
                        selector.wakeup();
                    } else if (!atta.isKeepAlive()) {
                        LOG.debug("http link({}) not keep-alive, and close it.", ch.getRemoteAddress());
                        addMsg(NIOTHREAD_MSGID_003_PENDING_OPR, new PendingKey(key, CLOSE_NORMAL));
                    } else {
//            LOG.trace("tw has sent all data to client.");
                    }
                } catch (IOException e) {
                    addMsg(NIOTHREAD_MSGID_003_PENDING_OPR, new PendingKey(key, CLOSE_AWAY));
                }
            } else {
                /* 异常处理: 这个情况什么时候出现呢？？？？ 后面留意 */
                LOG.debug("put http-rsp({}) to SocketChannel's WriteBuffer", httpReqSeqId);

                // If has pending write, order should be maintained. (WebSocket)
                Collections.addAll(atta.toWrites, buffers);
                addMsg(NIOTHREAD_MSGID_003_PENDING_OPR, new PendingKey(key, PendingKey.OP_WRITE));
                selector.wakeup();
            }
        }
    }

    private void closeThread() {
        /* 将异步线程维护的所有链路关闭掉 */
        if (selector.isOpen()) {
            Set<SelectionKey> t = selector.keys();
            SelectionKey[] keys = t.toArray(new SelectionKey[t.size()]);
            for (SelectionKey k : keys) {
                /**
                 * 1. t.toArray will fill null if given array is larger. 2. compute t.size(), then try to
                 * fill the array, if in the mean time, another thread close one SelectionKey, will result a
                 * NPE
                 *
                 * https://github.com/http-kit/http-kit/issues/125
                 */
                if (k != null) {
                    closeKey(k, 0); // 0 => close by server
                }
            }

            try {
                selector.close();
            } catch (IOException ignore) {
            }
        }
    }

    public String getName() {
        return name;
    }

    public void run() {
        while (true) {
            try {
                /* 先判断有木有待处理的非网路IO事件 */
                IOMsg iOMsg;
                while ((iOMsg = pending.poll()) != null) {
                    switch (iOMsg.msgId) {
                        case NIOTHREAD_MSGID_001_BINDING_SOCKET: {
                            if (!(iOMsg.objMsg instanceof SocketChannel)) {
                                LOG.error("iOMsg.objMsg's class type({}) not SocketChannel", iOMsg.objMsg.getClass().getName());
                                break;
                            }

                            try {
                                SocketChannel httpSocket = (SocketChannel) iOMsg.objMsg;
                                httpSocket.configureBlocking(false);
                                HttpAtta atta = new HttpAtta(maxBody, maxLine);
                                SelectionKey k = httpSocket.register(selector, OP_READ, atta);
                                atta.channel = new AsyncChannel(k, this);

                                LOG.debug("recv BINDING_SOCKET msg and binding http link");
                            } catch (Exception e) {
                                // eg: too many open files. do not quit
                                LOG.error("handle BINDING_SOCKET msg exception info is: {}", e.getMessage());
                            }

                            break;
                        }

                        case NIOTHREAD_MSGID_002_CLOSE_THREAD: {
                            LOG.info("recv CLOSE_THREAD msg and closeThread");
                            closeThread();
                            LOG.info("{} exit", name);
                            return;
                        }

                        case NIOTHREAD_MSGID_003_PENDING_OPR: {
//              LOG.trace("pending write...");
                            if (!(iOMsg.objMsg instanceof PendingKey)) {
                                LOG.error("iOMsg.objMsg's class type({}) not PendingKey", iOMsg.objMsg.getClass().getName());
                                break;
                            }

                            PendingKey k = (PendingKey) iOMsg.objMsg;
                            if (k.Op == PendingKey.OP_WRITE) {
                                if (k.key.isValid()) {
                                    k.key.interestOps(OP_WRITE);
                                }
                            } else {
                                LOG.debug("****** close the channel ******");
                                closeKey(k.key, k.Op);
                            }

                            break;
                        }
                    }
                }

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
                        LOG.error("recv accept msg.");
                    } else if (key.isReadable()) {
                        doRead(key);
                    } else if (key.isWritable()) {
                        doWrite(key);
                    }
                }
                selectedKeys.clear();
            } catch (ClosedSelectorException ignore) {
                return;
                // stopped
                // do not exits the while IO event loop. if exits, then will not
                // process any IO event
                // jvm can catch any exception, including OOM
            } catch (Throwable e) {
                // catch any exception(including OOM), print it
//        StringBuilder sb = new StringBuilder(1024);
//        sb.append(e.getClass().getName()).append(":").append(e.getMessage()).append("\n");
//        for (StackTraceElement ele : e.getStackTrace()) {
//          sb.append(ele.getClassName()).append("::").append(ele.getMethodName()).append("::")
//            .append(ele.getLineNumber()).append("\n");
//        }
                LOG.error("error should not happen, exception info is: ", e);
            }
        }
    }
}
