package com.example.kdt_teamproject_arcore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;

import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final String LOGCAT = "LOG WINDOW";

    GLSurfaceView glSurfaceView;
    MainRenderer mRenderer;
    TextView myTouchView, answerTxtView;

    Session mSession;
    Config mConfig;

    boolean mUserRequestedInstall = true, mTouched = true, isModelInit = false, mCatched = false, isPlaneDetected = false;
    float mCatchX, mCatchY;
    float mCurrentX;
    float mCurrentY;

    // 제스쳐를 감지해 이동, 회전 이벤트를 처리할 객체
    GestureDetector mGesture;

    float[] modelMatrix = new float[16];
    float[] modelMatrix01 = new float[16];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBarAndTitleBar();
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);

        answerTxtView = findViewById(R.id.answerTxtView);

        myTouchView = findViewById(R.id.myTouchView);

        // 제스쳐 이벤트 콜백함수 객체를 생성자 매개변수로 처리 (이벤트핸들러)
        mGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            // 한번 터치 (잡기)
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mCatched = true;
                mCatchX = e.getX();
                mCatchY = e.getY();
                Log.d(LOGCAT + " 클릭", e.getX() + ", " + e.getY());
                return true;
            }

        });

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int i) {
                }

                @Override
                public void onDisplayRemoved(int i) {
                }

                @Override
                public void onDisplayChanged(int i) {
                    synchronized (this) {
                        mRenderer.mViewportChanged = true;
                    }
                }
            }, null);
        }
        mRenderer = new MainRenderer(this, new MainRenderer.RenderCallBack() {
            @Override
            public void preRender() {
                if (mRenderer.mViewportChanged) {
                    Display display = getWindowManager().getDefaultDisplay();
                    int displayRotation = display.getRotation();
                    mRenderer.updateSession(mSession, displayRotation);
                }
                mSession.setCameraTextureName(mRenderer.getTextureID());

                Frame frame = null;
                try {
                    frame = mSession.update();
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

                if (frame.hasDisplayGeometryChanged()) {
                    mRenderer.mCamera.transformDisplayGeometry(frame);
                }

                PointCloud pointCloud = frame.acquirePointCloud();
                mRenderer.mPointCloud.update(pointCloud);
                pointCloud.release();

                float lightIntensity = frame.getLightEstimate().getPixelIntensity();
                float[] colorCorrection = new float[]{1.0f, 1.0f, 1.0f, 0.5f};

                List<HitResult> results = frame.hitTest(mCurrentX, mCurrentY);
                for (HitResult result : results) {
                    Pose pose = result.getHitPose(); // 증강현실에서의 좌표

                    if (!isModelInit) {
                        isModelInit = true;
                        pose.toMatrix(modelMatrix, 0); // 좌표를 가지고 matrix화 시킴
                        pose.toMatrix(modelMatrix01, 0); // 좌표를 가지고 matrix화 시킴
                    }

                    // 증강현실의 좌표에 객체가 있는지 받아옴 (plane이 걸려있는가)
                    Trackable trackable = result.getTrackable();

                    if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(pose)) {

                        // 빛의 세기 값을 넘김
                        mRenderer.mObj.setLightIntensity(lightIntensity);

                        // 빛의 색
                        mRenderer.mObj.setColorCorrection(colorCorrection);

                        mRenderer.mObj.setModelMatrix(modelMatrix);
//                            mRenderer.mCup.setModelMatrix(modelMatrix01);
                    }
                }

                // Session으로부터 증강현실 속에서의 평면이나 점의 객체를 얻을 수 있다.
                // 점이면 point, 평면이면 flat
                Collection<Plane> planes = mSession.getAllTrackables(Plane.class);

                // AR Core 상의 Planes를 얻는다
                for (Plane plane : planes) {
                    // plane이 정상이라면
                    // plane.getSubsumedBy() --> 다른 평면이 존재하는가
                    if (plane.getTrackingState() == TrackingState.TRACKING && plane.getSubsumedBy() == null) {
                        isPlaneDetected = true;
                        // 렌더링에서 plane 정보를 갱신하여 출력
                        mRenderer.mPlane.update(plane);
                    }
                }

//                if (isPlaneDetected) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), "평면~평면~", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                } else {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), "평면을 인식 못하겠쥐????", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }

                if (mCatched) {
                    mCatched = false;
//                    List<HitResult> results = frame.hitTest(mCatchX, mCatchY);
                    String msg = "못잡게쮜??????";
                    for (HitResult result : results) {
                        Pose pose = result.getHitPose(); // 증강현실에서의 좌표

                        if (catchCheck(pose.tx(), pose.ty(), pose.tz())) {
                            msg = "터키터키~";
                            answerTxtView.setText("A");
                            Log.d(LOGCAT + " 잡았다~", pose.tx() + ", " + pose.ty() + ", " + pose.tz());
                            break;
                        }
                    }

                    String finalMsg = msg;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myTouchView.setText(finalMsg);
                        }
                    });
                }

                Camera camera = frame.getCamera();
                float[] projMatrix = new float[16];
                camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f);

                float[] viewMatrix = new float[16];
                camera.getViewMatrix(viewMatrix, 0);

                mRenderer.setProjectionMatrix(projMatrix);
                mRenderer.updateViewMatrix(viewMatrix);

            }
        });

        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.setRenderer(mRenderer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestCameraPermission();

        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, true)) {
                    case INSTALLED:
                        mSession = new Session(this);
                        Log.d("Main", "AR Core Session 생성");
                        break;
                    case INSTALL_REQUESTED:
                        mUserRequestedInstall = false;
                        Log.d("Main", "AR Core 설치가 필요함");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mConfig = new Config(mSession);
        mSession.configure(mConfig);

        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }

        glSurfaceView.onResume();
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();

        glSurfaceView.onPause();
        mSession.pause();
    }

    void hideStatusBarAndTitleBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 터치이벤트를 위임해서 받아옴
        mGesture.onTouchEvent(event);
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            mTouched = true;
//            mCurrentX = event.getX();
//            mCurrentY = event.getY();
//        }
        return true;
    }

    boolean catchCheck(float x, float y, float z) {

        float[][] resAll = mRenderer.mObj.getMinMaxPoint();
        float[] minPoint = resAll[0];
        float[] maxPoint = resAll[1];

        if (x >= minPoint[0] && x <= maxPoint[0] &&
                y >= minPoint[1] && y <= maxPoint[1] &&
                z >= minPoint[2] && z <= maxPoint[2]) {
            return true;
        }
        return false;
    }
}