package com.example.sissi.vconftest.jni;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.sissi.vconftest.PcTrace;
import com.example.sissi.vconftest.jni.model.EmReq;
import com.example.sissi.vconftest.jni.model.EmRsp;
import com.example.sissi.vconftest.jni.model.ReqRspBeans;
import com.example.sissi.vconftest.jni.model.ReqRspMap;

/**
 * Created by Sissi on 1/20/2017.
 * */
/**
 * Native模拟器。<p>
 *
 * 若启用了模拟器则进入了“模拟模式”，模拟模式下模拟器替代了真实的native层，请求会被定向到模拟器而非真实的native层。
 * 模拟器收到请求后会反馈响应。<p>
 * 模拟模式主要有两个用途：<p>
 * 1、便于在Native层没有完成开发的情况下UI层开发仍可以照常进行不受制约以提高开发效率。<p>
 * 2、便于定位问题。比如当联调出现问题时可启用模拟模式跑下程序，若模拟模式下程序正常则问题出在native层，否则问题出在UI层。
 *
 */
public final class NativeEmulator{
    private static NativeEmulator instance;
    private ReqRspMap reqRspMap;
    private JsonManager jsonManager;

    private Thread thread;
    private Handler handler;
    private Callback cb;

    private NativeEmulator() {
        PcTrace.p("INIT");
        reqRspMap = ReqRspMap.instance();
        jsonManager = JsonManager.instance();
        initThread();
    }

    public synchronized static NativeEmulator instance() {
        if (null == instance) {
            instance = new NativeEmulator();
        }

        return instance;
    }

    public Handler getHandler(){
        return handler;
    }

    public void ejectNtf(final EmRsp ntfId, final Object ntf){
        handler.post(new Runnable() {
            @Override
            public void run() {
                ReqRspBeans.Head head= new ReqRspBeans.Head(ntfId.ordinal(), ntfId.toString(), 1);
                ReqRspBeans.Mtapi mtapi= new ReqRspBeans.Mtapi(head, ntf);
                String jsonNtf = jsonManager.toJson(new ReqRspBeans.RspWrapper(mtapi));
                if (null != cb){
                    PcTrace.p("NATIVE REPORT NTF %s: content=%s", ntfId, jsonNtf);
                    cb.callback(jsonNtf);
                }
            }
        });
    }


    private void initThread(){
        thread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        SessionManager.Session s = (SessionManager.Session) msg.obj;
                        EmReq reqId = s.reqId();
                        PcTrace.p("NATIVE RECV REQ %s: reqPara=%s", reqId, s.reqPara());
                        EmRsp[] rspIds = s.rspIds();
                        Object[] rsps = s.rsps();
                        ReqRspBeans.Head head;
                        ReqRspBeans.Mtapi mtapi;
                        for (int i=0; i<rsps.length; ++i){
                            // 构造响应json字符串
                            head= new ReqRspBeans.Head(rspIds[i].ordinal(), rspIds[i].toString(), 1);
                            mtapi= new ReqRspBeans.Mtapi(head, rsps[i]);
                            String jsonRsp = jsonManager.toJson(new ReqRspBeans.RspWrapper(mtapi));
                            if (null != cb){
                                // 上报响应
                                PcTrace.p("NATIVE REPORT RSP %s(for REQ %s): rspContent=%s", rspIds[i], reqId, jsonRsp);
                                cb.callback(jsonRsp);
                            }
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                synchronized (handler) { handler.notify(); }
                Looper.loop();
            }
        };

        thread.setName("NativeEmulator.Thread");

        thread.start();

        if (!thread.isAlive()){
            try {
                handler.wait(); // 保证初始化结束后立即可用。
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface Callback{
        void callback(String jsonRsp);
    }

    public void setCallback(Callback cb){
        this.cb = cb;
    }
}
