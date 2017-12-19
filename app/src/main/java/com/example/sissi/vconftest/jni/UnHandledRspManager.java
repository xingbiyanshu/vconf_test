package com.example.sissi.vconftest.jni;

import com.example.sissi.vconftest.PcTrace;

/**
 * Created by Sissi on 1/19/2017.
 */
public final class UnHandledRspManager {
    private static UnHandledRspManager instance;

    private UnHandledRspManager(){
        PcTrace.p("INIT");
    }

    public synchronized static UnHandledRspManager instance() {
        if (null == instance) {
            instance = new UnHandledRspManager();
        }

        return instance;
    }

    public void handle(String rsp){
        PcTrace.p("handle unhandled rsp="+rsp);
    }
}
