package com.iflytek.sparrow.http;

import org.httpkit.server.HttpServer;
import org.httpkit.server.IHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(GServer.class);

  private String ip;
  private int port;
  private IHandler handler;
  private int nioThreadNum;
  private HttpServer httpServer;

  private volatile boolean running = false;
  public GServer(String ip, int port, IHandler handler) {
    this(ip, port, handler, 4);
  }

  public GServer(String ip, int port, IHandler handler, int nioThreadNum) {
    this.ip = ip;
    this.port = port;
    this.handler = handler;
    this.nioThreadNum = nioThreadNum;
  }

  public boolean start() {
    if (running) {
      LOGGER.warn("server@{}:{} is running.", ip, port);
      return true;
    }
    running = true;

    try {
      httpServer = new HttpServer(this.ip, this.port, this.handler, 20480*1000, 2048*1000, 1024 * 1024 * 4, nioThreadNum);
    } catch (IOException e) {
      running = false;
      LOGGER.error("server@{}:{} failed to start, and exception info is: {}", ip, port, e.getMessage());
      return false;
    }

    try {
      httpServer.start();
    } catch (IOException e) {
      running = false;
      LOGGER.error("server@{}:{} failed to start, and exception info is: {}", ip, port, e.getMessage());
      return false;
    }

    LOGGER.info("server@{}:{} is running.", ip, port);
    return true;
  }

  public void stop() {
    if (!running) {
      LOGGER.warn("server@{}:{} is not running.", ip, port);
      return;
    }
    running = false;
    if (httpServer != null) {
      httpServer.stop();
    }
    LOGGER.info("server@{}:{} is stopped.", ip, port);
  }
}
