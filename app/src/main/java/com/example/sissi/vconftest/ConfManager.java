package com.example.sissi.vconftest;

import android.os.Message;

import com.example.sissi.vconftest.jni.model.EmReq;
import com.example.sissi.vconftest.jni.model.EmRsp;
import com.example.sissi.vconftest.jni.model.ReqRspBeans;
import com.example.sissi.vconftest.jni.JniRequester;

/**
 * Created by Sissi on 1/10/2017.
 */
public class ConfManager extends JniRequester{

    private static ConfManager instance;

    private ConfManager(){}

    public synchronized static ConfManager instance() {
        if (null == instance) {
            instance = new ConfManager();
        }

        return instance;
    }

    public void makeCall(String e164){
        PcTrace.p("UI send request");
        ReqRspBeans.AllReqEnumsRsp rsp1 = new ReqRspBeans.AllReqEnumsRsp();
        rsp1.reqList = new EmReq[]{EmReq.AllReqEnumsReq, EmReq.Req2};
        ReqRspBeans.Rsp2 rsp2 = new ReqRspBeans.Rsp2();
        rsp2.rspId = "Rsp2";
//        ReqRspBeans.AllReqEnumsReq req = new ReqRspBeans.AllReqEnumsReq(123);

        // 发送请求（模拟模式，请求走模拟器，自己指定期望的响应内容）
        sendReq(EmReq.AllReqEnumsReq, null, new Object[]{rsp1, rsp2});
        sendReq(EmReq.Req2, null, new Object[]{rsp2});

        // 发送请求（请求走真实的native层）
        sendReq(EmReq.AllReqEnumsReq, null);
        sendReq(EmReq.Req2, null);

        // 订阅通知，发射通知（模拟模式，自己指定期望的通知内容）
        subscribeNtf(EmRsp.TestNtf);
        ReqRspBeans.TestNtf testNtf = new ReqRspBeans.TestNtf();
        testNtf.ntfId = "TestNtf";
        ejectNtf(EmRsp.TestNtf, testNtf);
    }

    @Override
    public void handleMessage(Message msg) {
        EmRsp rsp = getRsp(msg.what);
        if (null == rsp){ return; }

        if (EmRsp.AllReqEnumsRsp.equals(rsp)){
            ReqRspBeans.AllReqEnumsRsp allReqEnumsRsp = (ReqRspBeans.AllReqEnumsRsp) msg.obj;
            PcTrace.p("UI recv rsp %s, content=%s", rsp, msg.obj.toString());
        }else if (EmRsp.Rsp2.equals(rsp)){
            ReqRspBeans.Rsp2 rsp2 = (ReqRspBeans.Rsp2) msg.obj;
            PcTrace.p("UI recv rsp %s, content=%s", rsp, msg.obj.toString());
        }else if (EmRsp.TestNtf.equals(rsp)){
            ReqRspBeans.TestNtf testNtf = (ReqRspBeans.TestNtf) msg.obj;
            PcTrace.p("UI recv rsp %s, content=%s", rsp, msg.obj.toString());
        }else{
            super.handleMessage(msg);
        }
    }
}
