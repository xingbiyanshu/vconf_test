package com.example.sissi.vconftest;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by Sissi on 11/21/2016.
 */
public abstract class BaseFragment extends Fragment {
    protected BaseActivity ctx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ctx = (BaseActivity) getActivity();
    }

    protected void changed(FragEvent id, Object content){
        ctx.fragChanged(id, content);
    }

/*    public enum Event {
        *//*Event id fragment report to activity.
        * compose: Moduleid_fragid_eventname
        * *//*

        *//*Conference*//*
        VConf_JoinConf_Succeed,
        VConf_BottomFuncBar_Switch,
        VConf_BottomFuncBar_Open_ShareDoc,
        VConf_BottomFuncBar_Close_ShareDoc,
    }*/
}
