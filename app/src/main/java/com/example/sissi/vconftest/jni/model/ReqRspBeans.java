package com.example.sissi.vconftest.jni.model;

/**
 * JNI请求、响应消息体
 * Created by Sissi on 1/12/2017.
 */

/**
 * 此类庞大，建议代码风格尽量紧凑。
 * 类字段名称是跟native层协定的，若更改请加上@SerializedName("native层名称")注释。
 * */
public final class ReqRspBeans {
    private ReqRspBeans(){}

    public static final class Head {
        int eventid;
        String eventname;
        int SessionID;
        public Head(int eventId, String eventName, int sessionID){
            eventid = eventId;
            eventname = eventName;
            SessionID = sessionID;
        }
    }
    public static final class Mtapi {
        Head head;
        Object body;
        public Mtapi(Head head, Object body){
            this.head = head;
            this.body = body;
        }
    }
    public static final class RspWrapper {
        Mtapi mtapi;
        public RspWrapper(Mtapi mtapi){this.mtapi = mtapi;}
    }

    public static final class BaseTypeInt{
        int basetype;
    }
    public static final class BaseTypeBool{
        boolean basetype;
    }
    public static final class BaseTypeString{
        String basetype;
    }

    public static final class Timeout {
        public EmReq reqId;
        public int timeout; // 单位：秒
        public Timeout(EmReq reqId, int timeout){ this.reqId = reqId; this.timeout = timeout;}
    }

    public static final class ErrorRsp {
        public int errorCode;
        public String description;
        public ErrorRsp(int ec, String desc){ errorCode = ec; description=desc;}
    }

    /******************* 顶层类（请求参数类、对应响应json字符串中整个body字段的类） ****************************/
    public static final class AllReqEnumsReq {
        public int reqId;
        public AllReqEnumsReq(int id){ reqId = id; }
    }
    public static final class AllReqEnumsRsp {
        public EmReq[] reqList;
    }
    public static final class Rsp2 {
        public String rspId;
    }
    public static final class TestNtf {
        public String ntfId;
    }
    /**协议栈初始化响应。
    * (建议使用响应消息名命名响应“顶层”类）*/
    public static final class StackInitResNtf {
        BaseTypeInt MainParam;
        BaseTypeInt AssParam;
    }

}
