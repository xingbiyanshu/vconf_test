package com.example.sissi.vconftest;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Sissi on 11/21/2016.
 */
public class JoinConfFrag extends BaseFragment {

    @Override
    public void onAttach(Context context) {
        PcTrace.p("=>");
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        PcTrace.p("=>");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PcTrace.p("=>");
//        return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.conf_join, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        PcTrace.p("=>");
        super.onViewCreated(view, savedInstanceState);

        Button bt = (Button) view.findViewById(R.id.enter_conf);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                ConfManager.instance().makeCall("e164");
                changed(FragEvent.VConf_JoinConf_Succeed, null);
            }
        });
    }

    @Override
    public void onStart() {
        PcTrace.p("=>");
        super.onStart();
    }

    @Override
    public void onResume() {
        PcTrace.p("=>");
        super.onResume();
    }

    @Override
    public void onPause() {
        PcTrace.p("=>");
        super.onPause();
    }

    @Override
    public void onStop() {
        PcTrace.p("=>");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        PcTrace.p("=>");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        PcTrace.p("=>");
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        PcTrace.p("=>");
        super.onDestroyView();
    }
}
