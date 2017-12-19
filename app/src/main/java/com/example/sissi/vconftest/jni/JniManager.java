package com.example.sissi.vconftest.jni;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.sissi.vconftest.PcTrace;
import com.example.sissi.vconftest.jni.model.EmReq;
import com.example.sissi.vconftest.jni.model.EmRsp;
import com.example.sissi.vconftest.jni.model.ReqRspBeans;
import com.example.sissi.vconftest.jni.model.ReqRspMap;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Jni管理器（JM）——UI层和Native层交互的中介。
 * Created by Sissi on 1/5/2017.
 */
public final class JniManager {
    private static JniManager instance;

    private boolean reqEnabled; // 是否允许发送请求
    private boolean rspEnabled; // 是否允许接收响应（包括RSP和NTF）
    private Thread reqThread; // 发送请求线程
    private Handler reqHandler; // 请求handler
    private Thread rspThread; // 接收响应线程

    private static final int MAX_RSP_NUM = 1000; // native层响应缓存大小上限。
    private ArrayList<String> rspBuf;   // native层响应缓存。（native线程写入，JM读取）
    private ArrayList<String> localRspBuf; // 本地响应缓存。当JM空闲且native层缓存不为空，JM将响应从native层挪到本地然后处理。
    private boolean isRspThreadBusy = false; // JM是否忙碌中

    private SessionManager sessionManager;  // 会话管理器
    private NotifyManager notifyManager; // 通知管理器
    private UnHandledRspManager unHandledRspManager; // JM未处理消息管理器（既未被会话管理器处理也未被通知管理器处理）
    private JsonManager jsonManager;    // json管理器，负责序列化反序列化
    private ReqRspMap reqRspMap; // 请求-响应映射器，保存有请求响应的映射关系

    private NativeEmulator nativeEmulator; // native模拟器。可模拟native层接收请求及反馈响应，仅用于调试！

    private boolean isWhiteListEnabled;
    private boolean isBlackListEnabled;

    // JM层错误码。（当发生错误时会上报请求者，请求者可捕获以做相应处理）
    private static final int RESULT_CODE_OK = 0;
    private static final int ERROR_REQ_PARA_CLAZZ_NOT_REG = 1;
    private static final int ERROR_REQ_PARA_TO_JSON_FAILED = 2;
    private static final int ERROR_RSP_NOT_REG = 3;
    private static final int ERROR_RSP_CLAZZ_NOT_REG = 4;
    private static final int ERROR_PAIR_RSP_CLAZZ_FAILED = 5;
    private static final int ERROR_TIMEOUT_RSP_NOT_REG = 6;
    private static final int ERROR_INVALID_TIMEOUT_VAL = 7;
    private static final int ERROR_SESSION_NUM_LIMITED = 8;
    private static final int ERROR_INVALID_SESSSION_STATE = 9;
    private static final int ERROR_INVALID_RSP = 10;
    private static final int ERROR_INVALID_PARA = 11;

    private JniManager(){
        PcTrace.p("INIT");
        rspBuf = new ArrayList<String>(MAX_RSP_NUM);
        localRspBuf = new ArrayList<String>();
        reqEnabled = rspEnabled =  true;

        isWhiteListEnabled = false;
        isBlackListEnabled = false;

        sessionManager = SessionManager.instance();
        notifyManager = NotifyManager.instance();
        unHandledRspManager = UnHandledRspManager.instance();

        jsonManager = JsonManager.instance();
        reqRspMap = ReqRspMap.instance();

//        reqRspMap.enableWhiteList(true);
//        reqRspMap.enableBlackList(true);

        if (NativeEmulatorOnOff.on) {
            initEmulator();
        }
        initReqThread();
        initRspThread();
    }

    public synchronized static JniManager instance() {
        if (null == instance) {
            instance = new JniManager();
        }

        return instance;
    }

    /**
     * 向native层发送请求。<p>
     * 该接口是非阻塞的，一方面意味调用该接口不会导致调用者阻塞，另一方面意味着接口返回不代表请求已发送出去仅代表请求已加入请求缓存队列。
     * @param requester 请求者。
     * @param reqId 请求ID。
     * @param reqPara 请求参数。
     * @return 返回真，若发送请求成功，返回假若发送失败。
     * */
    public boolean request(final JniRequester requester, final EmReq reqId, final Object reqPara){
        return request(requester, reqId, reqPara, null);
    }

    /**
     * 向native层发送请求。<p>
     * 该接口是非阻塞的，一方面意味调用该接口不会导致调用者阻塞，另一方面意味着接口返回不代表请求已发送出去仅代表请求已加入请求缓存队列。
     * @param requester 请求者。
     * @param reqId 请求ID。
     * @param reqPara 请求参数。
     * @param rsps 请求对应的响应。<b>若该值不为null则请求将发往native模拟器而非真实的native层。<b/>
     * @return 返回真，若发送请求成功，返回假若发送失败。
     * */
    public boolean request(final JniRequester requester, final EmReq reqId, final Object reqPara, final Object[] rsps){
        if (null==requester || null==reqId){
            PcTrace.p(PcTrace.ERROR, "Invalid para");
            return false;
        }
        if (null!=rsps && null==nativeEmulator){
            PcTrace.p(PcTrace.WARN, "Emulator not enabled");
            return false;
        }
        reqHandler.post(new Runnable() {
            @Override
            public void run() {
                String jsonReqPara = jsonManager.toJson(reqPara);
                jsonReqPara = "null".equalsIgnoreCase(jsonReqPara) ? null : jsonReqPara;
                EmRsp[] rspIds = reqRspMap.getRsps(reqId);
                Class<?>[] rspClazzs = reqRspMap.getRspClazzs(reqId);
                int timeoutVal = reqRspMap.getTimeout(reqId)*1000;
                // 注册信息检查
                if (null==rspIds || 0==rspIds.length){
                    reportError(requester, reqId, ERROR_RSP_NOT_REG, "No register of rsps");
                    return;
                }if (null==rspClazzs || 0==rspClazzs.length){
                    reportError(requester, reqId, ERROR_RSP_CLAZZ_NOT_REG, "No register of rsp clazzs");
                    return;
                }if (rspIds.length != rspClazzs.length){
                    reportError(requester, reqId, ERROR_PAIR_RSP_CLAZZ_FAILED, "Failed to pair rsps and clazzs");
                    return;
                }if (timeoutVal <= 0){
                    reportError(requester, reqId, ERROR_INVALID_TIMEOUT_VAL, "Invalid timeout value "+timeoutVal);
                    return;
                }
                Handler nativeHandler = null;
                if (null != rsps){
                    if (rspClazzs.length != rsps.length){
                        reportError(requester, reqId, ERROR_INVALID_RSP, "Invalid rsps num, need "+rspClazzs.length+" got "+rsps.length);
                        return;
                    }
                    for (int i=0; i<rsps.length; ++i){
                        if (!rsps[i].getClass().equals(rspClazzs[i])){
                            reportError(requester, reqId, ERROR_INVALID_RSP, "Invalid rsp "+rsps[i]+" ,should be "+rspClazzs[i]);
                            return;
                        }
                    }
                    nativeHandler = nativeEmulator.getHandler();
                }
                PcTrace.p("-~->>> %s", reqId);
                PcTrace.p("requester=%s, para=%s", requester, jsonReqPara);
                // 发送请求
                if (!sessionManager.request(requester, reqId, jsonReqPara, rspIds, rsps, timeoutVal, reqHandler, nativeHandler)){
                    reportError(requester, reqId, ERROR_SESSION_NUM_LIMITED, "Session number limited");
                    return;
                }
            }
        });

        return true;
    }

    /**
     * 取消请求。
     * */
    public void cancelRequest(JniRequester requester, EmReq reqId){

    }


    /**
     * native层响应。<p>
     * 该接口是非阻塞的，不会阻塞native线程。
     * @param jsonRsp json格式的响应。
     * */
    public void respond(String jsonRsp){
        synchronized (rspBuf) {
            if (MAX_RSP_NUM <= rspBuf.size()) {
                return;
            }

            rspBuf.add(jsonRsp); // 消息写入缓存
            if (!isRspThreadBusy) {
                rspBuf.notify(); // 通知有新消息到达
            }
        }
    }

    /**
     * 订阅通知。
     * @param subscriber 订阅者。
     * @param ntfId 订阅的通知。
     * @return 返回真若订阅成功，返回假若订阅失败。
     * */
    public boolean subscribeNtf(JniRequester subscriber, EmRsp ntfId){
        Class<?> ntfClazz = reqRspMap.getRspClazz(ntfId);
        if (null == ntfClazz){
            PcTrace.p(PcTrace.ERROR, "No register of clazz of "+ntfId);
            return false;
        }
        notifyManager.subscribeNtf(subscriber, ntfId);
        return true;
    }

    /**
     * 取消订阅通知。
     * @param subscriber 订阅者。
     * @param ntfId 订阅的通知。
     * */
    public void unsubscribeNtf(Handler subscriber, EmRsp ntfId){
        notifyManager.unsubscribeNtf(subscriber, ntfId);
    }

    /**
     * 发射通知。仅用于模拟模式。
     * */
    public boolean ejectNtf(final EmRsp ntfId, Object ntf){
        if (null == nativeEmulator){
            return false;
        }
        nativeEmulator.ejectNtf(ntfId, ntf);
        return true;
    }

    /**
     * native回调。<p>
     * 方法名称是固定的，要修改需和native层协商一致。
     * */
    public void callback(String jsonRsp){
        respond(jsonRsp);
    }
    public void Callback(String strMsg) {
        callback(strMsg);
    }

    /**
     * 报告错误给请求者。
     * */
    private void reportError(JniRequester requester, EmReq request, int errorId, String errorDescription){
        String errorStr = "Request "+request+" failed: "+errorDescription;
        PcTrace.p(PcTrace.ERROR, errorStr);
        ReqRspBeans.ErrorRsp errorRsp = new ReqRspBeans.ErrorRsp(errorId, errorStr);
        Message msg = Message.obtain();
        msg.what = EmRsp.Error.ordinal();
        msg.obj = errorRsp;
        requester.sendMessage(msg);
    }

    /**
     * 初始化发送请求线程
     * */
    private void initReqThread(){
        reqThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();

                reqHandler = new Handler();
                synchronized (reqHandler){reqHandler.notify();}

                Looper.loop();
            }
        };

        reqThread.setName("JniManager.reqThread");

        reqThread.start();

        if (!reqThread.isAlive()){
            try {
                reqHandler.wait(); // 保证thread初始化结束后handler立即可用。
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化接收响应线程
     * */
    private void initRspThread(){
        rspThread = new Thread(){
            @Override
            public void run() {
                String rsp = null;
                String rspName = null;
                String rspBody = null;
                EmRsp rspId = null;
                Class<?> clz = null;
                Object rspObj = null;
                SessionManager.Session session = null;
                while(true){
                    synchronized (rspBuf){
                        if (!rspBuf.isEmpty()){
                            // 拷贝消息到本地缓存
                            isRspThreadBusy = true;
                            localRspBuf.clear();
                            localRspBuf.addAll(rspBuf);
                            rspBuf.clear();
                        }else{
                            // 等待消息到来
                            try {
                                isRspThreadBusy = false;
                                rspBuf.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                PcTrace.p(PcTrace.ERROR, "JniManager.rspThread was interrupted !");
                                return;
                            }
                        }
                    }

                    // 处理native消息
                    while (!localRspBuf.isEmpty()){
                        rsp = localRspBuf.get(0);
                        localRspBuf.remove(rsp);
                        try {
                            rspName = jsonManager.getRspName(rsp);
                            rspBody = jsonManager.getRspBody(rsp);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (null == rspName || null == rspBody){
                            PcTrace.p(PcTrace.ERROR, "Invalid rsp: "+ rsp);
                            continue;
                        }
                        PcTrace.p("<<<-~- "+rspName);
                        PcTrace.p(rsp);

                        rspId = reqRspMap.getRsp(rspName);
                        if (null == rspId){
                            PcTrace.p(PcTrace.ERROR, "Invalid rsp name: "+ rspName);
                            continue;
                        }

//                        if (isWhiteListEnabled
//                                && !reqRspMap.isInWhiteList(emRsp)){
//                            PcTrace.p(PcTrace.ERROR, " rsp %s not in white list", rspName);
//                            continue;
//                        }else if (isBlackListEnabled
//                                && reqRspMap.isInBlackList(emRsp)){
//                            PcTrace.p(PcTrace.ERROR, " rsp %s in black list", rspName);
//                            continue;
//                        }

                        clz = reqRspMap.getRspClazz(rspId);
                        if (null == clz){
                            PcTrace.p(PcTrace.WARN, "Failed to find clazz corresponding "+rspName);
                            unHandledRspManager.handle(rsp); // 用户可能不想通过JM处理该响应，交给其自由发挥。
                            continue;
                        }

                        rspObj = jsonManager.fromJson(rspBody, clz);
                        if (null == rspObj){
                            PcTrace.p(PcTrace.ERROR, "Failed to convert json str to object, json str: "+ rspBody);
                            continue;
                        }

                        if (sessionManager.respond(rspId, rspObj)){
                            PcTrace.p("JM REPORT RSP %s content=%s", rspId, rspObj);
                        }else if(notifyManager.notify(rspId, rspObj)){
                            PcTrace.p("JM REPORT NTF %s content=%s", rspId, rspObj);
                        }else {
                            PcTrace.p(PcTrace.WARN, "JM unhandled rsp %s content=%s", rspId, rspObj);
                            unHandledRspManager.handle(rsp);  // 用户可能不想通过JM处理该响应，交给其自由发挥。
                        }
                    }
                }
            }
        };

        rspThread.setName("JniManager.rspThread");

        rspThread.start();
    }

    /**
     * 初始化Native模拟器
     * */
    private void initEmulator(){
        nativeEmulator = NativeEmulator.instance();
        nativeEmulator.setCallback(new NativeEmulator.Callback(){
            @Override
            public void callback(String jsonRsp) {
                respond(jsonRsp);
            }
        });
    }
}
