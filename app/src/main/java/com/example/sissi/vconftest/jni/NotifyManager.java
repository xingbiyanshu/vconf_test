package com.example.sissi.vconftest.jni;

import android.os.Handler;
import android.os.Message;

import com.example.sissi.vconftest.PcTrace;
import com.example.sissi.vconftest.jni.model.EmRsp;

import java.util.ArrayList;
import java.util.EnumMap;

/**
 * Native通知消息管理器
 * Created by Sissi on 1/9/2017.
 */
public final class NotifyManager {
    private static NotifyManager instance;
    private EnumMap<EmRsp, ArrayList<Handler>> subscribers;

    private NotifyManager(){
        PcTrace.p("INIT");
        subscribers = new EnumMap<EmRsp, ArrayList<Handler>>(EmRsp.class);
    }

    public synchronized static NotifyManager instance() {
        if (null == instance) {
            instance = new NotifyManager();
        }

        return instance;
    }

    /**
     * 订阅通知。
     * @param subscriber 订阅者.
     * @param ntfId 通知ID.
     * */
    public synchronized void subscribeNtf(Handler subscriber, EmRsp ntfId){
        ArrayList<Handler> subs = subscribers.get(ntfId);
        if (null == subs){
            subs = new ArrayList<Handler>();
        }

        subs.add(subscriber);
        subscribers.put(ntfId, subs);
    }

    /**
     * 取消订阅通知。
     * @param subscriber 订阅者.
     * @param ntfId 通知ID.
     * */
    public synchronized void unsubscribeNtf(Handler subscriber, EmRsp ntfId){
        ArrayList<Handler> subs = subscribers.get(ntfId);
        if (null == subs){
            return;
        }

        subs.remove(subscriber);
    }

    /**
     * 上报通知。
     * @param ntfId 通知ID.
     * @param ntfContent 通知内容.
     * @return 返回真若上报成功，返回假若上报失败。
     * */
    public synchronized boolean notify(EmRsp ntfId, Object ntfContent){
        ArrayList<Handler> subs = subscribers.get(ntfId);
        if (null == subs || 0==subs.size()){
            return false;
        }

        for (Handler sub : subs){
            Message msg = Message.obtain();
            msg.what = ntfId.ordinal();
            msg.obj = ntfContent;
            sub.sendMessage(msg);
        }

        return true;
    }
}