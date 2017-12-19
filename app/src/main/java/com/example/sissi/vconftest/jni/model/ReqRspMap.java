package com.example.sissi.vconftest.jni.model;

import com.example.sissi.vconftest.PcTrace;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Created by Sissi on 1/9/2017.
 */
public final class ReqRspMap {
    private static ReqRspMap instance;

    private final EnumMap<EmReq, EmRsp[]> reqRsps ;     // 请求——响应
    private final EnumMap<EmReq, Integer> reqTimeouts;   // 请求——超时时限
    private final EnumMap<EmRsp, Class<?>> rspClazzs;       // 响应——响应类类型

    private final EnumSet<EmRsp> whiteList; //  白名单
    private final EnumSet<EmRsp> blackList; // 黑名单

    private static final int DEFAULT_TIMEOUT = 10; // 默认超时时长。单位: 秒

    private ReqRspMap(){
        PcTrace.p("INIT");
        reqRsps = new EnumMap<EmReq, EmRsp[]>(EmReq.class);
        rspClazzs = new EnumMap<EmRsp, Class<?>>(EmRsp.class);
        reqTimeouts = new EnumMap<EmReq, Integer>(EmReq.class);

        whiteList = EnumSet.noneOf(EmRsp.class);
        blackList = EnumSet.noneOf(EmRsp.class);

        initMap();
        initWhiteList();
        initBlackList();
    }

    public synchronized static ReqRspMap instance() {
        if (null == instance) {
            instance = new ReqRspMap();
        }

        return instance;
    }

    public int getTimeout(EmReq req){
        Integer timeoutVal = reqTimeouts.get(req);
        if (null == timeoutVal) {
            return DEFAULT_TIMEOUT;
        }

        return timeoutVal;
    }

    public EmRsp[] getRsps(EmReq req){
        return reqRsps.get(req);
    }

    public Class<?> getRspClazz(EmRsp rsp){
        return rspClazzs.get(rsp);
    }

    public Class<?>[] getRspClazzs(EmReq req){
        EmRsp[] rsps = getRsps(req);
        if (null == rsps){return null;}
        ArrayList<Class<?>> clazzs = new ArrayList<Class<?>>();
        Class<?> claz = null;
        for (int i=0; i<rsps.length; ++i){
            claz = getRspClazz(rsps[i]);
            if (null != claz){
                clazzs.add(claz);
            }
        }

        return clazzs.toArray(new Class<?>[clazzs.size()]);
    }


    public EmRsp getRsp(String rspName){
        return EmRsp.valueOf(rspName);
    }

    public boolean isInWhiteList(EmRsp rsp){
        return whiteList.contains(rsp);
    }

    public boolean isInBlackList(EmRsp rsp){
        return blackList.contains(rsp);
    }


    private void initMap(){
        initReqMap();
        initRspMap();
    }

    private void map(EmReq req, EmRsp[] rsps){
        map(req, rsps, DEFAULT_TIMEOUT);
    }

    private void map(EmReq req, EmRsp[] rsps, int timeoutVal){
        reqRsps.put(req, rsps);
        reqTimeouts.put(req, timeoutVal);
    }

    private void map(EmRsp rsp, Class<?> rspClazz){
        rspClazzs.put(rsp, rspClazz);
    }


    private void initWhiteList(){
        whiteList.add(EmRsp.StackInitResNtf);
        whiteList.add(EmRsp.TestNtf);
//        whiteList.add(EmRsp.AllReqEnumsRsp);
//        whiteList.add(EmRsp.Rsp2);
    }

    private void initBlackList(){
    }

    /*注册映射关系：响应——响应对应的类*/
    private void initRspMap(){
        map(EmRsp.AllReqEnumsRsp, ReqRspBeans.AllReqEnumsRsp.class);
        map(EmRsp.Rsp2, ReqRspBeans.Rsp2.class);
        map(EmRsp.TestNtf, ReqRspBeans.TestNtf.class);
        map(EmRsp.StackInitResNtf, ReqRspBeans.StackInitResNtf.class);
    }

    /*注册映射关系：请求——响应列表——超时时长*/
    private void initReqMap(){
        map(EmReq.AllReqEnumsReq, new EmRsp[]{EmRsp.AllReqEnumsRsp, EmRsp.Rsp2});
        map(EmReq.Req2, new EmRsp[]{EmRsp.Rsp2});
    }

}

