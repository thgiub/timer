package ru.kamaz.itis.timer;


import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.time.Instant;

import ru.kamaz.itis.timer.interfaces.OnSurfaceCreatedListener;

import static android.content.ContentValues.TAG;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private OnSurfaceCreatedListener surfaceListener;

    public CameraPreview(Context context, Camera camera, OnSurfaceCreatedListener listener) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceListener = listener;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

    }

    public void surfaceCreated(SurfaceHolder holder) {

        try {
            if (mCamera!=null){
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                surfaceListener.onSurfaceCreated();
            }else {
               //mCamera.stopPreview();
                surfaceListener.onCreateSurfaceFailed();
            }

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            surfaceListener.onCreateSurfaceFailed();
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e){

        }
        try {

            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}
