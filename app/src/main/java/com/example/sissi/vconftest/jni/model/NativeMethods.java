package com.example.sissi.vconftest.jni.model;

import com.example.sissi.vconftest.PcTrace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Sissi on 1/20/2017.
 */
public final class NativeMethods {
    private NativeMethods(){}

    public static synchronized boolean invoke(String methodName, String jsonPara){
        Method method = null;
        try {
            if (null != jsonPara) {
                method = NativeMethods.class.getDeclaredMethod(methodName, String.class);
                method.invoke(null, jsonPara);
                PcTrace.p("invoke method %s para %s", method, jsonPara);
            }else{
                method = NativeMethods.class.getDeclaredMethod(methodName);
                method.invoke(null);
                PcTrace.p("invoke method %s para %s", method, jsonPara);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return true;
    }

    private static void AllReqEnumsReq(){
    }
    private static void AllReqEnumsReq(String para){
    }
    private static void Req2(){
    }
}
