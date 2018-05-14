package com.sywc.reflectors.handler;

import com.sywc.reflectors.SparrowSystem;
import com.iflytek.sparrow.share.Constants;
import com.sywc.reflectors.share.GSessionInfo;
import com.iflytek.sparrow.share.OriginRequest;
import com.sywc.reflectors.share.SparrowConstants;
import org.apache.commons.lang3.StringUtils;
import org.httpkit.HttpMethod;
import org.httpkit.server.AsyncChannel;
import org.httpkit.server.Frame;
import org.httpkit.server.HttpRequest;
import org.httpkit.server.IHandler;
import org.httpkit.server.RespCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GServiceServerHandler implements IHandler {
    private static final Logger log = LoggerFactory.getLogger(GServiceServerHandler.class);

    public GServiceServerHandler() {
    }

    @Override
    public void close(int timeoutMs) {
        log.debug("close.");
    }

    @Override
    public void handle(AsyncChannel channel, Frame frame) {
        log.error("handle channel and frame: not support");
    }

    @Override
    public void handle(HttpRequest request, final RespCallback callback) {
        OriginRequest originReq = new OriginRequest();
        if (request.method.KEY == HttpMethod.GET.KEY) {
            originReq.type = Constants.HTTP_REQ_TYPE_GET;
            originReq.getQuery = request.queryString;
        } else if (request.method.KEY == HttpMethod.POST.KEY) {
            originReq.type = Constants.HTTP_REQ_TYPE_POST;
            originReq.postBody = request.getPostBody();
            /**无论是 GET 还是 POST 都给 getQuery 赋值 */
            originReq.getQuery = request.queryString;
        } else if (request.method.KEY == HttpMethod.OPTIONS.KEY) {
            originReq.type = Constants.HTTP_REQ_TYPE_OPTIONS;
        } else {
            originReq.type = Constants.HTTP_REQ_TYPE_OTHER;
        }

        originReq.url = request.uri;
        originReq.headers = request.getHeaders();

        log.debug("the requested url is: {}", originReq.url);
        log.debug("the header is:{}", originReq.headers);

        GSessionInfo sessInfo = GSessionInfo.getNewSession(originReq);

        sessInfo.callback = callback;

        if (originReq.type == Constants.HTTP_REQ_TYPE_OPTIONS) {
            log.debug("PreFlight request!");
            SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_PREFLIGHT_FOR_AD_REQUEST, sessInfo);
            return;
        }

        if (originReq.type != Constants.HTTP_REQ_TYPE_POST && originReq.type != Constants.HTTP_REQ_TYPE_GET) {
            log.debug("not support method = {}, <=[0,get;1,post;2,other], url = {}, query = {}, headers:[{}], sid = {}",
                    originReq.type,
                    originReq.url,
                    originReq.getQuery,
                    originReq.headers,
                    sessInfo.sid);
            SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_UNSUPPORT_AD_REQUEST_METHOD, sessInfo);
            return;
        }

        if (StringUtils.containsIgnoreCase(originReq.url, "/api/upplat")) {
            SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_AD_REQ, sessInfo);
        } else if (StringUtils.containsIgnoreCase(originReq.url, "/api/static")) {
            SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_AD_STATIC, sessInfo);
        } else {
            log.debug("Not found,url = {}, query = {}, headers:[{}], sid = {}", originReq.url,
                    originReq.getQuery, originReq.headers, sessInfo.sid);
            SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_404, sessInfo);
            return;
        }
        sessInfo.nanoAddToGServerTaskTime = System.nanoTime();
    }

    public void handle(AsyncChannel channel, Frame.TextFrame frame) {
        log.debug("handle channel and TextFrame frame");
    }

    @Override
    public void clientClose(AsyncChannel channel, int status) {
        log.debug("handle channel and status: client has closed the channel.");
    }

}