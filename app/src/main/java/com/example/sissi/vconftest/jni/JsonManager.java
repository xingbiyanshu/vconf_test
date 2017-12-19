package com.example.sissi.vconftest.jni;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.example.sissi.vconftest.PcTrace;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.json.JSONException;
import org.json.JSONObject;

public final class JsonManager {

    private static JsonManager instance;

    private GsonBuilder gsonBuilder;
    private Gson gson;

    private static ArrayList<Class<? extends Enum>> enumClazzs ;

    private static final String KEY_MTAPI = "mtapi";
    private static final String KEY_HEAD = "head";
    private static final String KEY_BODY = "body";
    private static final String KEY_EVENTNAME = "eventname";
    private static final String KEY_BASE_TYPE = "basetype";

    private JsonManager() {
        PcTrace.p("INIT");
        gsonBuilder = new GsonBuilder();

        initEnumClazzs();

        // 注册enum类型
        for (Class<? extends Enum> c : enumClazzs) {
            regEnumType(c);
        }

        gson = gsonBuilder.create();
    }

    public synchronized static JsonManager instance() {
        if (null == instance) {
            instance = new JsonManager();
        }

        return instance;
    }


    private  <T extends Enum<T>> void regEnumType(final Class<T> t) {
        gsonBuilder.registerTypeAdapter(t, new JsonSerializer<T>() {

            @Override
            public JsonElement serialize(T paramT, Type paramType,
                    JsonSerializationContext paramJsonSerializationContext) {
                return new JsonPrimitive(paramT.ordinal());
            }
        });

        gsonBuilder.registerTypeAdapter(t, new JsonDeserializer<T>() {

            @Override
            public T deserialize(JsonElement paramJsonElement, Type paramType,
                    JsonDeserializationContext paramJsonDeserializationContext) throws JsonParseException {
                T[] enumConstants = t.getEnumConstants();
                int enumOrder = paramJsonElement.getAsInt();
                if (enumOrder < enumConstants.length)
                    return enumConstants[enumOrder];

                return null;
            }

        });
    }


    public Gson obtainGson() {
        return gson;
    }

    public String toJson(Object obj){
        return gson.toJson(obj);
    }

    public <T> T fromJson(String json, Class<T> classOfT){
        return gson.fromJson(json, classOfT);
    }

    public String getRspName(String jsonRsp) throws JSONException {
        return new JSONObject(jsonRsp).getJSONObject(KEY_MTAPI).getJSONObject(KEY_HEAD).getString(KEY_EVENTNAME);
    }

    public String getRspBody(String jsonRsp) throws JSONException {
        return new JSONObject(jsonRsp).getJSONObject(KEY_MTAPI).getString(KEY_BODY);
    }

    private void initEnumClazzs(){
        enumClazzs = new ArrayList<Class<? extends Enum>>();
//        enumClazzs.add(EmReq.class);
//        enumClazzs.add(EmRsp.class);
    }

}
