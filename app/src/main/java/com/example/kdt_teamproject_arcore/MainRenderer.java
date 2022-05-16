package com.example.kdt_teamproject_arcore;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.google.ar.core.Session;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer {

    CameraPreview mCamera;
    PointCloudRenderer mPointCloud;
    boolean mViewportChanged;
    int mViewportWidth, mViewportHeight;
    RenderCallBack mRenderCallBack;
    PlaneRenderer mPlane;
    ObjRenderer mObj;
    ObjRenderer mCup;

    String[] objs = {"android.obj", "aircraft.obj", "craft.obj", "cup.obj"};
    String[] jpgs = {"android.png", "aircraft.jpg", "craft.png", "cup.png"};

    MainRenderer(Context context, RenderCallBack callBack) {
        mRenderCallBack = callBack;
        mCamera = new CameraPreview();
        mPointCloud = new PointCloudRenderer();
        mPlane = new PlaneRenderer(Color.rgb(0.3f,0.3f,0.8f), 0.5f);

        // Obj 생성
        mObj = new ObjRenderer(context, objs[0], jpgs[0]);
//        mCup = new ObjRenderer(context, objs[3], jpgs[3]);

    }

    interface RenderCallBack {
        void preRender();
    }

    // Surface가 생성될 때 호출
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // 3차원 좌표
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        // 색상
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.2f, 0.3f, 0.4f, 1.0f);

        mCamera.init();
        mPointCloud.init();
        mPlane.init();
        mObj.init();
//        mCup.init();
    }

    // Surface Size가 변경될 때 호출, onSurfaceCreated()가 호출될 때마다 호출
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mViewportChanged = true;
        mViewportWidth = width;
        mViewportHeight = height;
    }

    // Rendering 수행
    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mRenderCallBack.preRender();
        GLES20.glDepthMask(false);
        mCamera.draw();
        GLES20.glDepthMask(true);
        mPointCloud.draw();

        mPlane.draw();
        mObj.draw();
//        mCup.draw();
    }

    void updateSession(Session session, int displayRotation) {
        if (mViewportChanged) {
            session.setDisplayGeometry(displayRotation, mViewportWidth, mViewportHeight);
            mViewportChanged = false;
        }
    }

    void setProjectionMatrix(float[] matrix) {
        mPointCloud.updateProjMatrix(matrix);
        mPlane.setProjectionMatrix(matrix);
        mObj.setProjectionMatrix(matrix);
//        mCup.setProjectionMatrix(matrix);
    }

    void updateViewMatrix(float[] matrix) {
        mPointCloud.updateViewMatrix(matrix);
        mPlane.setViewMatrix(matrix);
        mObj.setViewMatrix(matrix);
//        mCup.setViewMatrix(matrix);
    }

    int getTextureID() {
        return mCamera == null ? -1 : mCamera.mTextures[0];
    }

}
