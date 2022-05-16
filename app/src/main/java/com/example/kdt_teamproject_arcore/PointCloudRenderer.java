package com.example.kdt_teamproject_arcore;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.PointCloud;

public class PointCloudRenderer {

    float[] mViewMatrix = new float[16];
    float[] mProjMatrix = new float[16];

    // 점. 고정되어있으므로 그대로 써야한다
    // GPU 를 이용하여 고속 계산하여 화면 처리하기 위한 코드
    String vertexShaderCode =
            "uniform mat4 uMvpMatrix;"
                    + "uniform vec4 uColor;"
                    + "uniform float uPointSize;"
                    + "attribute vec4 vPosition;" // vec4 -> 3차원 좌표
                    + "varying vec4 vColor;" // 2 진형태

                    + "void main () {"

                    + "vColor = uColor;"
                    + "gl_Position = uMvpMatrix * vec4(vPosition.xyz, 1.0);" // 좌표
                    // gl_Position : OpenGL에 있는 변수 이용 > 계산식 uMVPMatrix * vPosition

                    + "gl_PointSize = uPointSize;"

                    + "}";

    // 화면에 어떻게 그려지는지
    String fragmentShaderCode =
            // 정밀도 중간
            "precision mediump float;"
                    + "varying vec4 vColor;" // 4 개 (점들) 컬러를 받겠다

                    + "void main() {"

                    + "gl_FragColor = vColor;"

                    + "}";


    int[] mVbo;

    int mProgram;

    // 점 개수
    int mNumPoints = 0;

    PointCloudRenderer() {


    }

    // 초기화
    void init() {

        // 점 생성
        mVbo = new int[1];

        // 한군데, 넣는곳, 시작번지
        GLES20.glGenBuffers(1, mVbo, 0);

        // 사용할 것, 시작위치를 연결
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);

        // 버퍼 데이터를 읽어온다
        // 사용할 것, 데이터 크기, 버퍼, int 2 (모름)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 1000 * 16, null, GLES20.GL_DYNAMIC_DRAW);

        // 사용이 끝난 후 원위치로
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


        // 기존에 GPU로 연산하던 코드를 가져다가 사용
        // 점 쉐이더 생성
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexShaderCode);

        // 컴파일
        GLES20.glCompileShader(vShader);


        // 텍스처
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragmentShaderCode);


        // 컴파일
        GLES20.glCompileShader(fShader);


        // mProgram = vShader + fShader
        mProgram = GLES20.glCreateProgram();
        // 점위치 계산식 합치기
        GLES20.glAttachShader(mProgram, vShader);
        // 색상 계산식 합치기
        GLES20.glAttachShader(mProgram, fShader);

        GLES20.glLinkProgram(mProgram); // 도형 렌더링 계산식 정보를 넣는다.
    }

    // 3차원 좌표 값을 갱신할 메소드
    void update(PointCloud pointCloud) {

        // 점 위치 정보 받기 위한 바인딩
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);

        // 점의 갯수가 몇개인지 갱신
        // 점 정보. 점들. 점들중 안그리고 남아있는 정보(remainig) (덩치가 크니가 4개로 자른다)
        mNumPoints = pointCloud.getPoints().remaining() / 4;

        // 점위치 정보 Buffer로 갱신
        // 사용할 것, 데이터 크기, 정보를 받아오는 곳 (16개), int 2 (모름)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * 16, pointCloud.getPoints());

        // 점 위치 정보 바인딩 해제
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


    }

    // 카메라로 그리기
    void draw() {

        float[] mMVPMatrix = new float[16];

        // 사용할 것, 시작번지, 계산할것 1 , 시작번지, 계산할것 2, 시작번지
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);

        //계산된 렌더링 정보 사용한다.
        GLES20.glUseProgram(mProgram);


        // 핸들러

        // vPosition
        // mProgram == > vertexShader
        int position = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int color = GLES20.glGetUniformLocation(mProgram, "uColor");
        int mvp = GLES20.glGetUniformLocation(mProgram, "uMvpMatrix");
        int size = GLES20.glGetUniformLocation(mProgram, "uPointSize");

        // GPU 활성화
        GLES20.glEnableVertexAttribArray(position);

        // 바인딩
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);

        // 점 포인터
        GLES20.glVertexAttribPointer(
                position,                  // 정점 속성의 인덱스 지정
                4,                    // 점속성 - 좌표계 (16개 넣었으므로 4)
                GLES20.GL_FLOAT,          // 점의 자료형 float
                false,                // 노멀라이즈. 정규화 true, 직접변환 false
                16,                  // 점 속성의 stride(간격)
                0                    // 점 정보
        );


        // 4개 정보:  사용할것, RGBA
        GLES20.glUniform4f(color, 1.0f, 1.0f, 1.0f, 0.5f);

        // 그려지는 곳에 위치, 보이는 정보를 적용한다.
        GLES20.glUniformMatrix4fv(mvp, 1, false, mMVPMatrix, 0);

        // 점이므로 1개정보: 사용할것, 점 크기
        GLES20.glUniform1f(size, 10.0f);

        // 점들을 그린다, ?, (점 갯수) 10개
        GLES20.glDrawArrays(
                GLES20.GL_POINTS,
                0,
                mNumPoints // 점 갯수
        );

        // 닫는다
        GLES20.glDisableVertexAttribArray(position);

        // 바인딩 해제
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    }


//    void updateMatrix(float [] mViewMatrix, float [] mProjMatrix){
//        // 배열 복제
//        // 원본 , 원본의 시작위치, 복사될 배열, 복사배열 시작위치, 개수
//        System.arraycopy(mViewMatrix,0,this.mViewMatrix,0,16);
//        System.arraycopy(mProjMatrix,0,this.mProjMatrix,0,16);
//    }


    void updateProjMatrix(float[] projMatrix) {
        System.arraycopy(projMatrix, 0, this.mProjMatrix, 0, 16);
    }

    void updateViewMatrix(float[] viewMatrix) {
        System.arraycopy(viewMatrix, 0, this.mViewMatrix, 0, 16);
    }
}
