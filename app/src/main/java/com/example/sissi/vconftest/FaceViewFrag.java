package com.example.sissi.vconftest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Sissi on 11/21/2016.
 */
public class FaceViewFrag extends BaseStreamFragment{

//    private SurfaceView faceView;

//    private BaseStrategy strategy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        PcTrace.p("=>");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        PcTrace.p("=>");
        return inflater.inflate(R.layout.conf_faceview, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        PcTrace.p("=>");
        super.onViewCreated(view, savedInstanceState);

//        faceView = (SurfaceView) view.findViewById(R.id.face_view);

        strategy = new FaceViewTextureViewStrategy(this); //new FaceViewSurfaceViewStrategy(this); //new FaceViewTextureViewStrategy(this);
    }

    @Override
    public void onStart() {
        PcTrace.p("=>");
        super.onStart();
        strategy.start();
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
        strategy.stop();
    }

    @Override
    public void onDestroyView() {
        PcTrace.p("=>");
        super.onDestroyView();
        strategy = null;
    }

}
