package com.example.sissi.vconftest;

import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;

/**
 * Created by Sissi on 11/24/2016.
 */
public class FaceViewSurfaceViewStrategy extends BaseStrategy implements SurfaceHolder.Callback{
    private SurfaceView surfaceView;
    private Camera camera;
    private BaseStreamFragment context;

    public FaceViewSurfaceViewStrategy(BaseStreamFragment context){
        surfaceView = new SurfaceView(context.getActivity());
        ((ViewGroup)context.getView()).addView(surfaceView);
        this.context = context;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        PcTrace.p("=>");
        int cameraNum = Camera.getNumberOfCameras();
        if (cameraNum <= 0){
            return;
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraId=0;
        for (; cameraId < cameraNum; cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                break;
            }
        }

        if (cameraId < cameraNum){
            camera = Camera.open(cameraId);
        }else{
            camera = Camera.open();
        }

        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        PcTrace.p("=>");
        if (null == camera){
            return;
        }

//        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        PcTrace.p("=>");
        if (null != camera){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void start() {
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void stop() {
        surfaceView.getHolder().removeCallback(this);
    }
}
