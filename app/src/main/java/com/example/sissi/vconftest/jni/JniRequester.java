package com.example.sissi.vconftest.jni;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import com.example.sissi.vconftest.PcTrace;
import com.example.sissi.vconftest.jni.model.EmReq;
import com.example.sissi.vconftest.jni.model.EmRsp;
import com.example.sissi.vconftest.jni.model.ReqRspBeans;

/**
 * Created by Sissi on 1/9/2017.
 */
public abstract class JniRequester extends Handler {
    protected JniManager jniManager;

    protected JniRequester(){
        jniManager = JniManager.instance();
    }


    /**
     * @see JniManager#request(JniRequester, EmReq, Object) */
    protected void sendReq(EmReq reqId, Object reqPara){
        jniManager.request(this, reqId, reqPara);
    }

    /**
     * @see JniManager#request(JniRequester, EmReq, Object, Object[]) */
    protected void sendReq(EmReq reqId, Object reqPara, Object[] rsps){
        jniManager.request(this, reqId, reqPara, rsps);
    }

    /**
     * @see JniManager#subscribeNtf(JniRequester, EmRsp) */
    protected void subscribeNtf(EmRsp ntfId){
        jniManager.subscribeNtf(this, ntfId);
    }

    /**
     * @see JniManager#unsubscribeNtf(Handler, EmRsp) */
    protected void unsubscribeNtf(EmRsp ntfId){
        jniManager.unsubscribeNtf(this, ntfId);
    }

    /**
     * @see JniManager#ejectNtf(EmRsp, Object)
     * */
    protected void ejectNtf(EmRsp ntfId, @NonNull Object ntf){
        jniManager.ejectNtf(ntfId, ntf);
    }

    protected EmRsp getRsp(int rspId){
        EmRsp[] rsps = EmRsp.values();
        if (rspId<0 || rsps.length<=rspId){
            PcTrace.p(PcTrace.ERROR, "Invalid rsp %d", rspId);
            return null;
        }

        return rsps[rspId];
    }

    @Override
    public void handleMessage(Message msg) {
        int rspId = msg.what;
        if (EmRsp.Error.ordinal() == rspId){
            ReqRspBeans.ErrorRsp errorRsp = (ReqRspBeans.ErrorRsp) msg.obj;
            PcTrace.p(PcTrace.ERROR, errorRsp.description);
        }else if (EmRsp.Timeout.ordinal() == rspId){
            ReqRspBeans.Timeout timeout = (ReqRspBeans.Timeout) msg.obj;
            PcTrace.p(PcTrace.ERROR, "REQ %s timeout(%ds)", timeout.reqId, timeout.timeout);
        }
    }
}
