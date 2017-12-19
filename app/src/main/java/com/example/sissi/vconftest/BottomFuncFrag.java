package com.example.sissi.vconftest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Sissi on 11/21/2016.
 */
public class BottomFuncFrag extends BaseFragment{
    private boolean isOpened = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.conf_bottom, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button switchView = (Button) view.findViewById(R.id.switch_big_small);
        switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changed(FragEvent.VConf_BottomFuncBar_Switch, null);
            }
        });

        Button shareDoc = (Button) view.findViewById(R.id.share_doc);
        shareDoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOpened) {
                    changed(FragEvent.VConf_BottomFuncBar_Open_ShareDoc, null);
                    isOpened = true;
                }else{
                    changed(FragEvent.VConf_BottomFuncBar_Close_ShareDoc, null);
                    isOpened = false;
                }
            }
        });
    }
}
