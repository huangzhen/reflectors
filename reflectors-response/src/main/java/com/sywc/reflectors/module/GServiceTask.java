package com.sywc.reflectors.module;

import com.alibaba.fastjson.JSONObject;
import com.sywc.reflectors.SparrowSystem;
import com.iflytek.sparrow.share.Constants;
import com.sywc.reflectors.share.ExceptionConstants;
import com.sywc.reflectors.share.GSessionInfo;
import com.sywc.reflectors.share.SparrowConstants;
import com.sywc.reflectors.share.dto.ExceptionDTO;
import com.sywc.reflectors.share.dto.PlatConfigDTO;
import com.sywc.reflectors.share.task.GMsg;
import com.sywc.reflectors.share.task.GTaskBase;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.httpkit.HeaderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.httpkit.HttpUtils.HttpEncode;

/**
 * @author huangzhen
 * @version 1.0.0
 */
public class GServiceTask extends GTaskBase {
    private static final Logger logger = LoggerFactory.getLogger(GServiceTask.class);
    private static final Integer PREFLIGHT_FOR_AD_MONITOR = 0;
    private static final Integer PREFLIGHT_FOR_AD_REQUEST = 1;

    public GServiceTask(String taskName) {
        super(taskName, 0);
    }

    @Override
    protected void handlerMsg(int msgId, Object objContext) {
        if (null == objContext) {
            logger.error("error: null == objContext");
            return;
        }

        if (!(objContext instanceof GSessionInfo)) {
            logger.error("objContext is not an instance of GSessionInfo");
            return;
        }
        switch (msgId) {
            case SparrowConstants.MSG_ID_SERVICE_ADX_AD_REQ: {
                GSessionInfo sessInfo = (GSessionInfo) objContext;
                if (null == sessInfo) {
                    return;
                }
                sessInfo.nanoGSTaskHdlReqMsgTime = System.currentTimeMillis();
                sessInfo.nanoStartHandlerReq = System.nanoTime();
                Map<String, String> urlParamMap = assemblyUrlParamMap(sessInfo.originReq.getQuery);
                HeaderMap headers = newHeaderMap(sessInfo);
                /** 增加异常处理，当请求参数为空 */
                ExceptionDTO exceptionDTO = null;
                if (null == urlParamMap || urlParamMap.isEmpty()) {
                    exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.pramNameNotExists());
                    sessInfo.callback.run(HttpEncode(200, headers, exceptionDTO.toString()));
                    break;
                }
                if (!urlParamMap.containsKey(SparrowConstants.MUST_PARAM_NAME_NAME) ||
                        StringUtils.isEmpty(urlParamMap.get(SparrowConstants.MUST_PARAM_NAME_NAME))) {
                    exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.paramNameIsEmpty());
                    sessInfo.callback.run(HttpEncode(200, headers, exceptionDTO.toString()));
                    break;
                }
                String platName = urlParamMap.get(SparrowConstants.MUST_PARAM_NAME_NAME);
                sessInfo.platName = platName;
                /**先从缓存去，取不到在加载配置文件*/
                PlatConfigDTO platConfigDTO = SparrowSystem.upplatConfMap.get(platName);

                if (null == platConfigDTO) {
                    logger.debug("UpPlat conf file[{}] has cached,read from cache!,sid={}",platName, sessInfo.sid);
                    StringBuilder fileBuffer = new StringBuilder();
                    fileBuffer.append(SparrowSystem.upplatDirPath).append(File.separator);
                    fileBuffer.append(SparrowConstants.UPPLAT_CONF_DIR_NAME).append(File.separator).append(platName);

                    FileInputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(fileBuffer.toString());
                        String fileValue = IOUtils.toString(inputStream, Charsets.UTF_8);
                        if (StringUtils.isEmpty(fileValue)) {
                            exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.platConfIsEmpty(fileBuffer.toString(), platName));
                        }
                        platConfigDTO = JSONObject.parseObject(fileValue, PlatConfigDTO.class);
                        SparrowSystem.upplatConfMap.put(platName, platConfigDTO);
                    } catch (FileNotFoundException e) {
                        logger.error("Not found file[{}], errorMsg={}, sid={}",platName ,e.getMessage(), sessInfo.sid);
                        exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.platConfNotExists(fileBuffer.toString(), platName));
                    } catch (IOException e) {
                        logger.error("File read[{}] error,errorMsg={},sid={}", platName,e.getMessage(), sessInfo.sid);
                        exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.upplatNotExists(fileBuffer.toString(), platName));
                    } catch (Exception e) {
                        logger.error("File read[{}] error,errorMsg={},sid={}",platName, e.getMessage(), sessInfo.sid);
                        exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.upplatNotExists(fileBuffer.toString(), platName));
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        if (null != exceptionDTO) {
                            sessInfo.callback.run(HttpEncode(200, headers, exceptionDTO.toString()));
                        }
                    }
                }
                sessInfo.setPlatConfigDTO(platConfigDTO);
                GSleepModule.addMsg(new GMsg(-1, sessInfo));
                sessInfo.nanoOverHandlerReq = System.nanoTime();
                break;
            }
            case SparrowConstants.MSG_ID_SERVICE_ADX_AD_RSP: {
                GSessionInfo sessInfo = (GSessionInfo) objContext;
                if (null == sessInfo) {
                    return;
                }
                logger.debug("handler rsp, sid = {}", sessInfo.sid);
                HeaderMap headers = newHeaderMap(sessInfo);
                int randNum = RandomUtils.nextInt(0, 100);
                PlatConfigDTO configDTO = sessInfo.getPlatConfigDTO();

                /**当生产的随机数，小于下发率时，说明应该下发*/
                boolean issuredFlag = randNum <= configDTO.getRatio() ? true : false;

                if (issuredFlag) {
                    StringBuilder fileBuffer = new StringBuilder();
                    fileBuffer.append(SparrowSystem.upplatDirPath).append(File.separator);
                    fileBuffer.append(SparrowConstants.UPPLAT_RES_DIR_NAME).append(File.separator).append(sessInfo.platName);

                    FileInputStream inputStream = null;
                    String fileResValue = StringUtils.EMPTY;
                    if (SparrowSystem.upplatResMap.containsKey(sessInfo.platName)) {
                        logger.debug("Upplat res file[{}] has cached,read from cache!,sid={}",sessInfo.platName, sessInfo.sid);
                        /** 缓存里存在，直接从缓存里读取 */
                        sessInfo.callback.run(HttpEncode(200, headers, SparrowSystem.upplatResMap.get(sessInfo.platName)));
                    } else {
                        try {
                            logger.debug("UpPlat res file not in cache,read from disk!,sid={}", sessInfo.sid);
                            inputStream = new FileInputStream(fileBuffer.toString());
                            fileResValue = IOUtils.toString(inputStream, Charsets.UTF_8);
                            SparrowSystem.upplatResMap.put(sessInfo.platName, fileResValue);
                        } catch (FileNotFoundException e) {
                            logger.error("Not found file[{}], errorMsg={}, sid={}",sessInfo.platName, e.getMessage(), sessInfo.sid);
                        } catch (IOException e) {
                            logger.error("File read[{}] error,errorMsg={},sid={}",sessInfo.platName, e.getMessage(), sessInfo.sid);
                        } finally {
                            IOUtils.closeQuietly(inputStream);
                        }
                    }
                    sessInfo.callback.run(HttpEncode(configDTO.getFillHttpCode(), headers, fileResValue));
                } else {
                    sessInfo.callback.run(HttpEncode(configDTO.getNoFillHttpCode(), headers, null));
                }
                logger.debug("response to client, httpReqId = {}, sid = {}", sessInfo.callback.getHttpReqId(), sessInfo.sid);
                break;
            }
            case SparrowConstants.MSG_ID_SERVICE_ADX_AD_STATIC: {

                GSessionInfo sessInfo = (GSessionInfo) objContext;
                if (null == sessInfo) {
                    return;
                }
                Map<String, String> urlParamMap = assemblyUrlParamMap(sessInfo.originReq.getQuery);

                HeaderMap headers = newHeaderMap(sessInfo);
                /** 增加异常处理，当请求参数为空 */
                ExceptionDTO exceptionDTO = null;
                if (null == urlParamMap || urlParamMap.isEmpty()) {
                    exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.pramNameNotExists());
                    sessInfo.callback.run(HttpEncode(200, headers, exceptionDTO.toString()));
                    return;
                }
                if (!urlParamMap.containsKey(SparrowConstants.MUST_PARAM_NAME_NAME) ||
                        StringUtils.isEmpty(urlParamMap.get(SparrowConstants.MUST_PARAM_NAME_NAME))) {
                    exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.paramNameIsEmpty());
                    sessInfo.callback.run(HttpEncode(200, headers, exceptionDTO.toString()));
                    return;
                }
                String platName = urlParamMap.get(SparrowConstants.MUST_PARAM_NAME_NAME);
                if (SparrowSystem.upplatStaticMap.containsKey(platName)) {
                    logger.debug("Static file[{}] has cached,read from cache!,sid={}",platName, sessInfo.sid);
                    /** 缓存里存在，直接从缓存里读取 */
                    sessInfo.callback.run(HttpEncode(200, headers, SparrowSystem.upplatStaticMap.get(platName)));
                } else {
                    logger.debug("Static file[{}] not in cache,read from disk!,sid={}",platName, sessInfo.sid);
                    String filePath = SparrowSystem.staticDirPath + File.separator + platName;
                    FileInputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(filePath);
                        String fileValue = IOUtils.toString(inputStream, Charsets.UTF_8);
                        sessInfo.callback.run(HttpEncode(200, headers, fileValue));
                        logger.debug("Put static file[{}] to cache!,sid={}", platName,sessInfo.sid);
                        SparrowSystem.upplatStaticMap.put(platName, fileValue);
                    } catch (FileNotFoundException e) {
                        logger.error("Not found file[{}],sid={}",platName, sessInfo.sid);
                        exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.upplatNotExists(filePath, platName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        logger.error("File read[{}] error,errorMsg={},sid={}", platName,e.getMessage(), sessInfo.sid);
                        exceptionDTO = new ExceptionDTO(sessInfo.sid, ExceptionConstants.upplatNotExists(SparrowSystem.staticDirPath, platName));
                    } finally {
                        if (null != exceptionDTO) {
                            sessInfo.callback.run(HttpEncode(200, headers, exceptionDTO.toString()));
                        }
                        IOUtils.closeQuietly(inputStream);
                    }
                }
                break;
            }
            case SparrowConstants.MSG_ID_SERVICE_ADX_404: {
                if (!(objContext instanceof GSessionInfo)) {
                    logger.error("!(objContext instanceof GSessionInfo)");
                    break;
                }
                GSessionInfo sessionInfo = (GSessionInfo) objContext;
                send404Resp(sessionInfo);
                break;
            }
            case SparrowConstants.MSG_ID_SERVICE_ADX_UNSUPPORT_AD_REQUEST_METHOD: {
                if (!(objContext instanceof GSessionInfo)) {
                    logger.error("!(objContext instanceof GSessionInfo)");
                    break;
                }
                GSessionInfo sessionInfo = (GSessionInfo) objContext;
                sendResp(sessionInfo, 405);
                break;
            }
            case SparrowConstants.MSG_ID_SERVICE_ADX_PREFLIGHT_FOR_AD_REQUEST: {
                if (!(objContext instanceof GSessionInfo)) {
                    logger.error("!(objContext instanceof GSessionInfo)");
                    break;
                }
                GSessionInfo sessionInfo = (GSessionInfo) objContext;
                sendPreflightResp(sessionInfo, PREFLIGHT_FOR_AD_REQUEST);
                break;
            }
            default: {
                logger.error("invalid msgId = {}", msgId);
                break;
            }
        }

    }


    @Override
    public boolean startTask() {
        return true;
    }

    @Override
    public boolean closeTask() {
        addMsg(new GMsg(Constants.MSG_ID_SYS_KILL, null));
        return true;
    }


    private void sendPreflightResp(GSessionInfo sessionInfo, Integer type) {
        if (null == sessionInfo || null == sessionInfo.callback) {
            logger.error("sessionInfo == null");
            return;
        }
        logger.info("reqUrl({}), sid = {}", sessionInfo.originReq.url, sessionInfo.sid);
        HeaderMap header = newHeaderMap(sessionInfo);

        if (type.intValue() == PREFLIGHT_FOR_AD_REQUEST) {
            header.put("Access-Control-Allow-Methods", "POST");
        } else if (type.intValue() == PREFLIGHT_FOR_AD_MONITOR) {
            header.put("Access-Control-Allow-Methods", "POST,GET,HEAD");
        } else {
            logger.error("不支持的跨域预检请求类型，既不是针对监控的预检也不是针对广告请求的！sid={}", sessionInfo.sid);
        }

        // 构建预检响应支持的headers
        Object allowHeaders = sessionInfo.originReq.headers.get("access-control-request-headers");
        if (allowHeaders != null) {
            header.put("Access-Control-Allow-Headers", allowHeaders);
        }

        header.put("Access-Control-Max-Age", "86400");

        ByteBuffer[] bytes = HttpEncode(200, header, null);
        sessionInfo.callback.run(bytes);
    }

    private void sendResp(GSessionInfo sessionInfo, int httpCode) {
        if (sessionInfo == null) {
            logger.error("sessionInfo == null");
            return;
        }
        if (sessionInfo.callback == null) {
            logger.error("sessionInfo.callback == null, sid = {}", sessionInfo.sid);
            return;
        }
        logger.info("httpCode = {}, reqUrl({}), sid = {}", httpCode, sessionInfo.originReq.url, sessionInfo.sid);
        HeaderMap header = newHeaderMap(sessionInfo);
        ByteBuffer[] bytes = HttpEncode(httpCode, header, null);
        sessionInfo.callback.run(bytes);
    }

    private void send404Resp(GSessionInfo sessionInfo) {
        if (null == sessionInfo || null == sessionInfo.callback) {
            logger.error("sessionInfo == null");
            return;
        }
        if (sessionInfo.callback == null) {
            logger.error("sessionInfo.callback == null, sid = {}", sessionInfo.sid);
            return;
        }
        logger.info("httpCode = 404, reqUrl({}), sid = {}", sessionInfo.originReq.url, sessionInfo.sid);
        HeaderMap headers = newHeaderMap(sessionInfo);

        ByteBuffer[] bytes = HttpEncode(404, headers, null);
        sessionInfo.callback.run(bytes);
    }

    /**
     * 把下发的 链接组装到 Map 里方面取值
     *
     * @param urlParam 链接的url
     * @return
     */
    private Map<String, String> assemblyUrlParamMap(String urlParam) {
        Map<String, String> resultMap = new HashMap<>(2);
        if (StringUtils.isEmpty(urlParam)) {
            return resultMap;
        }
        String[] params = urlParam.split("&");
        if (params == null || params.length == 0) {
            return resultMap;
        }
        String[] paramValue;
        for (String param : params) {
            if (StringUtils.isEmpty(param) || param.split("=").length != 2) {
                continue;
            }
            paramValue = param.split("=");
            resultMap.put(paramValue[0], paramValue[1]);
        }
        return resultMap;
    }

    private HeaderMap newHeaderMap(GSessionInfo sessionInfo) {
        HeaderMap headers = new HeaderMap();
        // 针对跨域访问做的处理
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Content-Type", "application/json; charset=utf-8");
        return headers;

    }
}
