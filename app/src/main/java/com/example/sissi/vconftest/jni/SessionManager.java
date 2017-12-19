package com.example.sissi.vconftest.jni;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.sissi.vconftest.jni.model.ReqRspBeans;
import com.example.sissi.vconftest.PcTrace;
import com.example.sissi.vconftest.jni.model.EmReq;
import com.example.sissi.vconftest.jni.model.EmRsp;
import com.example.sissi.vconftest.jni.model.NativeMethods;

import java.util.ArrayList;

/**
 * 会话管理器
 * Created by Sissi on 1/9/2017.
 */

public final class SessionManager {
    private static SessionManager instance;

    private ArrayList<Session> sessions;  // 正常会话
    private ArrayList<Session> blockedSessions; // 被阻塞的会话

    private static final int MAX_SESSION_NUM = 50; // 正常会话数上限
    private static final int MAX_BLOCKED_SESSION_NUM = 100; // 被阻塞的会话数上限
    public static final int MSG_TIMEOUT = 999;

    private Thread timeoutThread; // 超时线程
    private Handler timeoutHandler;
    private int sessionCnt = 0;

    private SessionManager(){
        PcTrace.p("INIT");
        sessions = new ArrayList<Session>();
        blockedSessions = new ArrayList<Session>();
        initTimeoutThread();
    }

    public synchronized static SessionManager instance() {
        if (null == instance) {
            instance = new SessionManager();
        }

        return instance;
    }


    /**
     * 处理请求。
     * @param requester 请求者.
     * @param reqId 请求ID
     * @param reqPara 请求参数.
     * @param rspIds 请求对应的响应ID.
     * @param timeoutVal 请求超时时长.
     * @param sendreqHandler 用来发送请求的handler.被阻塞的session后续被唤醒时使用该handler发送请求以保证请求均在请求线程执行。
     * @param nativeHandler 模拟的native层对应的handler.仅模拟模式下用到，非模拟模式需传值为null
     * @return 返回真若发送请求成功，假若发送请求失败。
     * */
    public synchronized boolean request(Handler requester, EmReq reqId, String reqPara, EmRsp[] rspIds, Object[] rsps, int timeoutVal,
                                        Handler sendreqHandler, Handler nativeHandler){
        int siz = sessions.size();
        if (siz >= MAX_SESSION_NUM){
            return false;
        }

        // 检查是否存在未完成的同类请求
        for (Session s : sessions){
            if (s.reqId.equals(reqId)) {
                // 存在未完成的同类请求则阻塞当前请求直到同类请求完成。
                if (blockedSessions.size() >= MAX_BLOCKED_SESSION_NUM){
                    return false;
                }
                // 创建阻塞session
                Session sess = new Session(++sessionCnt, requester, reqId, reqPara, rspIds, rsps, timeoutVal, sendreqHandler, nativeHandler);
                blockedSessions.add(sess);
                PcTrace.p("created blocked session id=%s", sessionCnt); // TODO 启动超时？阻塞时间算在超时内？
                return true; // 返回真表示请求成功，不让外部感知请求被阻塞
            }
        }

        // 创建session
        final Session s = new Session(++sessionCnt, requester, reqId, reqPara, rspIds, rsps, timeoutVal, sendreqHandler, nativeHandler);
        sessions.add(s);
        PcTrace.p("created session id=%s", sessionCnt);

        // 发送请求
        sendReq(s);

        return true;
    }

    /**
     * 处理响应。
     * @param rspId 响应ID
     * @param rspId 响应内容（反序列化后的对象）
     * @return 返回真若该响应已被处理，假若未被处理。
     * */
    public synchronized boolean respond(EmRsp rspId, Object rspContent){
        for (final Session s : sessions){ // 查找正在等待该响应的会话
            if (Session.WAITING != s.state
                    && Session.RECVING != s.state){
                continue;
            }
            for (EmRsp id : s.rspIds){
                if (id.equals(rspId)){ // 该响应是该会话期待的
                    s.state = Session.RECVING; // 已收到响应，继续接收后续响应
                    PcTrace.p("<<<-=- (session %d)%s", s.id, rspId);
                    PcTrace.p(""+ rspContent);
                    // 上报响应给请求者
                    Message rsp = Message.obtain();
                    rsp.what = rspId.ordinal();
                    rsp.obj = rspContent;
                    s.requester.sendMessage(rsp);

                    // 检查该会话是否已结束（获取到所有期待的响应）
                    if (rspId.equals(s.rspIds[s.rspIds.length-1])){
                        PcTrace.p("session %s finished ", s.id);
                        timeoutHandler.removeMessages(MSG_TIMEOUT, s); // 移除定时器
                        s.state = Session.END; // 已获取到所有期待的响应，该session已结束
                        sessions.remove(s);
                        // 驱动被当前session阻塞的Session
                        driveBlockedSession(s.reqId);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 处理超时
     * @param s 已超时的会话。
     * */
    private synchronized void timeout(final Session s){
        PcTrace.p("session %s timeout ", s.id);
        s.state = Session.END; // Session结束

        // 通知用户请求超时
        ReqRspBeans.Timeout to = new ReqRspBeans.Timeout(s.reqId, s.timeoutVal);
        Message rsp = Message.obtain();
        rsp.what = EmRsp.Timeout.ordinal();
        rsp.obj = to;
        s.requester.sendMessage(rsp);

        sessions.remove(s);
        // 驱动被当前session阻塞的Session
        driveBlockedSession(s.reqId);
    }

    /**
     * 驱动可能存在的被阻塞的会话。<p>
     * 相同请求ID的会话同一时间只允许一个处于工作状态，余下的处于阻塞状态。所以当会话结束时需调用此接口驱动可能存在的被阻塞的会话。
     * @param reqId 请求ID
     * */
    private void driveBlockedSession(EmReq reqId){
        Session bs=null;
        for (final Session s : blockedSessions){
            if (reqId.equals(s.reqId)) {
                blockedSessions.remove(s);
                sessions.add(s);
                bs = s;
                break;
            }
        }

        if (null == bs){
            return;
        }

        // 使用发送者发送请求以保证请求均通过请求线程发送
        final Session finalBs = bs;
        bs.sendreqHandler.post(new Runnable() {
            @Override
            public void run() {
                sendReq(finalBs);
            }
        });
    }


    /**
     * 发送请求
     * */
    private void sendReq(Session s){
        if (null != s.nativeHandler){ // 若模拟模式下则请求发给模拟器
            Message req = Message.obtain();
            req.what = s.reqId.ordinal();
            req.obj = s;
            s.nativeHandler.sendMessage(req);
        }else{ // 否则直接调用native接口
            NativeMethods.invoke(s.reqId.toString(), s.reqPara);
        }

        s.state = Session.WAITING; // 请求已发出正在等待响应

        PcTrace.p("-=->>> (session %d)%s ", s.id, s.reqId);
        PcTrace.p("requester=%s, para=%s, timeout=%dms", s.requester, s.reqPara, s.timeoutVal);

        // 启动超时
        Message msg = Message.obtain();
        msg.what = MSG_TIMEOUT;
        msg.obj = s;
        timeoutHandler.sendMessageDelayed(msg, s.timeoutVal);
    }

    /**
     * 初始化超时线程
     * */
    private void initTimeoutThread(){
        timeoutThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                timeoutHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        SessionManager.this.timeout((Session) msg.obj);
                    }
                };
                synchronized (timeoutHandler){ timeoutHandler.notify(); }
                Looper.loop();
            }
        };
        timeoutThread.setName("SessionManager.timeoutThread");
        timeoutThread.start();
        if (!timeoutThread.isAlive()){
            try {
                timeoutHandler.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 会话 */
    public final class Session{
        private final int id;
        private final Handler requester;// 请求者
        private final EmReq reqId;        // 请求Id
        private final String reqPara;   // 请求参数（Json 格式）。可能为null，表示没有请求参数
        private final EmRsp[] rspIds;     // 响应Id。一定不为null，请求必定有响应（即便由此触发的ntf也算）
        private final Object[] rsps;     // 响应。仅用于模拟模式，非模拟模式下必须为null。
        private final int timeoutVal;   // 超时时限，单位：毫秒
        private final ArrayList<EmRsp> recvedRspIds; // 已收到的响应
        private final Handler sendreqHandler;// 用来发送请求的Handler
        private final Handler nativeHandler; // Native模拟器的hanlder。仅用于模拟模式，非模拟模式下必须为null。

        private int state;  // session状态
        private static final int IDLE = 2;  // 空闲。
        private static final int WAITING = 3; // 等待。请求发送以后，收到响应之前。
        private static final int RECVING = 4; // 接收。收到响应后，结束之前。
        private static final int END = 5;   // 结束。session已接收到最后一个响应或者已超时。

        public Session(int id, Handler requester, EmReq reqId, String reqPara, EmRsp[] rspIds, Object[] rsps, int timeoutVal, Handler sendreqHandler, Handler nativeHandler){
            this.id = id;
            this.requester = requester;
            this.reqId = reqId;
            this.reqPara = reqPara;
            this.rspIds = rspIds;
            this.rsps = rsps;
            this.timeoutVal = timeoutVal;
            this.sendreqHandler = sendreqHandler;
            this.nativeHandler = nativeHandler;

            recvedRspIds = new ArrayList<EmRsp>();
            state = IDLE;
        }

        public synchronized EmReq reqId(){
            return reqId;
        }
        public synchronized String reqPara(){
            return reqPara;
        }
        public synchronized EmRsp[] rspIds(){
            return rspIds;
        }
        public synchronized Object[] rsps(){
            return rsps;
        }
    }

}
