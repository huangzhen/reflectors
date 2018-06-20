package org.httpkit.server;

public class HttpAtta extends ServerAtta {

    public final HttpDecoder decoder;

    public HttpAtta(int maxBody, int maxLine) {
        decoder = new HttpDecoder(maxBody, maxLine);
    }
}
