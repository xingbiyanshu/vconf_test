package com.example.sissi.vconftest;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Created by Sissi on 11/24/2016.
 */
public class FaceViewTextureViewStrategy extends BaseStrategy implements TextureView.SurfaceTextureListener{
    private Camera camera;
    private TextureView textureView;
    private BaseStreamFragment context;

    public FaceViewTextureViewStrategy(BaseStreamFragment context){
        textureView = new TextureView(context.getActivity());
        ((ViewGroup)context.getView()).addView(textureView);
        this.context = context;
    }

    @Override
    public void start() {
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void stop() {
        textureView.setSurfaceTextureListener(null);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        PcTrace.p("=>"+surface+" w="+width+" h="+height);

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

        Camera.Size lSize = getOptimalPreviewSize(width, height,camera.getParameters().getSupportedPreviewSizes());
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(lSize.width, lSize.height);
        camera.setParameters(parameters);
        camera.setDisplayOrientation(270);

        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            camera.release();
            camera = null;
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        PcTrace.p("=>");
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        PcTrace.p("=>"+surface);
        camera.stopPreview();
        camera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    private Size getOptimalPreviewSize(int w, int h, List<Size> sizes) {
        if (sizes == null) return null;

        Size optimalSize = null;

        int w0, h0, w1, h1, maxW = 0;

		/* 先尝试选用“宽高比”完全匹配的  */

        /** 考虑 相机的“高-宽”定义和android系统的“高-宽”定义相反的情况（实测此为多数情况）**/
        w0 = h;
        h0 = w;
        for (Size size : sizes) {
            w1 = size.width;
            h1 = size.height;
            if (w1 * h0 == w0 * h1) {
                if (maxW < w1) { // “宽高比”完全匹配的前提下找分辨率最大的
                    maxW = w1;
                    optimalSize = size;
                }
            }
        }

        /** 若没找到，则考虑 相机的“高-宽”定义和android系统的“高-宽”定义相同的情况  **/
        if (null == optimalSize) {
            w0 = w;
            h0 = h;
            maxW = 0;
            for (Size size : sizes) {
                w1 = size.width;
                h1 = size.height;
                if (w1 * h0 == w0 * h1) {
                    if (maxW < w1) { // “宽高比”完全匹配的前提下找分辨率最大的
                        maxW = w1;
                        optimalSize = size;
                    }
                }
            }
        }

		/* 若没有“宽高比”完全匹配的, 则选用“宽高比”最接近的 */

        if (null == optimalSize) {
            double targetRatio, ratioDelta, minDelta = Double.MAX_VALUE;

            /** 相机的“高-宽”定义和android系统的“高-宽”定义相反的情况  **/
            targetRatio = (double) h / w;
            for (Size size : sizes) {
                w1 = size.width;
                h1 = size.height;
                ratioDelta = Math.abs((double) w1 / h1 - targetRatio);
                if (ratioDelta < minDelta) {
                    minDelta = ratioDelta;
                    optimalSize = size;
                }
            }

            /** 相机的“高-宽”定义和android系统的“高-宽”定义相同的情况 **/
            targetRatio = (double) w / h;
            for (Size size : sizes) {
                w1 = size.width;
                h1 = size.height;
                ratioDelta = Math.abs((double) w1 / h1 - targetRatio);
                if (ratioDelta < minDelta) {
                    minDelta = ratioDelta;
                    optimalSize = size;
                }
            }

            /** 在最接近“宽高比”的前提下再找最大分辨率 **/
            w0 = optimalSize.width;
            h0 = optimalSize.height;
            maxW = 0;
            for (Camera.Size size : sizes) {
                w1 = size.width;
                h1 = size.height;
                if (w1 * h0 == w0 * h1) {
                    if (maxW < w1) {
                        maxW = w1;
                        optimalSize = size;
                    }
                }
            }
        }

        return optimalSize;
    }

}
