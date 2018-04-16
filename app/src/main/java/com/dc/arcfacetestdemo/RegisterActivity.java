package com.dc.arcfacetestdemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.image.ImageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zsn10 on 2017/10/12.
 */

public class RegisterActivity extends Activity implements SurfaceHolder.Callback {
    private final String TAG = this.getClass().toString();
    private Rect src = new Rect();
    private Rect dst = new Rect();
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Thread mView;
    private UIHandler mUIHandler;
    private final static int MSG_CODE = 0x1000;
    private final static int MSG_EVENT_REG = 0x1001;
    private final static int MSG_EVENT_NO_FACE = 0x1002;
    private final static int MSG_EVENT_NO_FEATURE = 0x1003;
    private final static int MSG_EVENT_FD_ERROR = 0x1004;
    private final static int MSG_EVENT_FR_ERROR = 0x1005;
    private ImageView image;
    private AFR_FSDKFace mAFR_FSDKFace;
    private int count;
    private ArrayList<Contact> mFilePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (!getIntentData(getIntent().getExtras())) {
            Log.i(TAG, "getIntentData fail!");
            this.finish();
        }

        mUIHandler = new UIHandler();
        //定义锁对象
        final ReentrantLock lock = new ReentrantLock();
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
        image = (ImageView) findViewById(R.id.image);
        mSurfaceView.getHolder().addCallback(this);
        //实现一次注册多人
        for (final Contact contact : mFilePath) {
            mView = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (mSurfaceHolder == null) {
                            Thread.sleep(100);
                        }
                        // String encodeString = ImageUtil.GetImageStr(path);
                        Bitmap mGenerateImage = ImageUtil.GenerateImage(contact.getPic());
                        //final Bitmap mGenerateImage = ImageUtil.decodeImage(path);
                        src.set(0, 0, mGenerateImage.getWidth(), mGenerateImage.getHeight());
                        //加锁
                        lock.lock();
                        byte[] data = new byte[mGenerateImage.getWidth() * mGenerateImage.getHeight() * 3 / 2];
                        //虹软人脸SDK使用的图像格式是NV21的格式，所以我们需要将获取到的图像转化为对应的格式
                        ImageConverter convert = new ImageConverter();
                        convert.initial(mGenerateImage.getWidth(), mGenerateImage.getHeight(), ImageConverter.CP_PAF_NV21);
                        if (convert.convert(mGenerateImage, data)) {
                            Log.i(TAG, "convert ok!");
                        }
                        convert.destroy();

                        //人脸检测引擎
                        AFD_FSDKEngine engine = new AFD_FSDKEngine();
                        AFD_FSDKVersion version = new AFD_FSDKVersion();
                        List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();
                        AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key,
                                AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
                        Log.i(TAG, "AFD_FSDK_InitialFaceEngine = " + err.getCode());
                        if (err.getCode() != AFD_FSDKError.MOK) {
                            Message reg = Message.obtain();
                            reg.what = MSG_CODE;
                            reg.arg1 = MSG_EVENT_FD_ERROR;  //FD人脸检测引擎初始化失败
                            reg.arg2 = err.getCode();
                            mUIHandler.sendMessage(reg);
                        }
                        err = engine.AFD_FSDK_GetVersion(version);
                        Log.i(TAG, "AFD_FSDK_GetVersion =" + version.toString() + ", " + err.getCode());
                        err = engine.AFD_FSDK_StillImageFaceDetection(data, mGenerateImage.getWidth(), mGenerateImage.getHeight(),
                                AFD_FSDKEngine.CP_PAF_NV21, result);
                        Log.i(TAG, "AFD_FSDK_StillImageFaceDetection =" + err.getCode() + "<" + result.size());

                        //如果人脸检测引擎返回的结果不为空
                        if (!result.isEmpty()) {
                            //人脸识别引擎
                            AFR_FSDKVersion version1 = new AFR_FSDKVersion();
                            AFR_FSDKEngine engine1 = new AFR_FSDKEngine();
                            //定义一个人脸识别结果待存储list中的人脸
                            AFR_FSDKFace result1 = new AFR_FSDKFace();
                            AFR_FSDKError error1 = engine1.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
                            Log.i(TAG, "AFR_FSDK_InitialEngine = " + error1.getCode());
                            if (error1.getCode() != AFD_FSDKError.MOK) {
                                Message reg = Message.obtain();
                                reg.what = MSG_CODE;
                                //FR人脸识别引擎初始化失败
                                reg.arg1 = MSG_EVENT_FR_ERROR;
                                reg.arg2 = error1.getCode();
                                mUIHandler.sendMessage(reg);
                            }
                            error1 = engine1.AFR_FSDK_GetVersion(version1);
                            //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
                            Log.i(TAG, "FR=" + version.toString() + "," + error1.getCode());

//                            while (mSurfaceHolder != null) {
//                                //可传参指定一个dirty区域刷新 其他区域会使用缓存 即为局部重绘
//                                // 同步锁直到调用unlockCanvasAndPost(Canvas canvas)函数才释放该锁，
//                                // 这里的同步机制保证在Surface绘制过程中不会被改变（被摧毁、修改）
//                                Canvas canvas = mSurfaceHolder.lockCanvas();
//                                if (canvas != null) {
//                                    //定义一个画笔
//                                    Paint mPaint = new Paint();
//                                    //当宽度缩放小于高度缩放即适应水平缩放
//                                    boolean fit_horizontal = canvas.getHeight() / (float) src.height() >
//                                            canvas.getWidth() / (float) src.width() ? true : false;
//                                    float scale = 1.0f;
//                                    if (fit_horizontal) {
//                                        //将缩放比例设为更小的宽度
//                                        scale = canvas.getWidth() / (float) src.width();
//                                        dst.left = 0;
//                                        dst.top = (canvas.getHeight() - (int) (src.height() * scale)) / 2;
//                                        dst.right = dst.left + canvas.getWidth();
//                                        dst.bottom = dst.top + (int) (src.height() * scale);
//                                    } else {
//                                        scale = canvas.getHeight() / (float) src.height();
//                                        dst.left = (canvas.getWidth() - (int) (src.width() * scale)) / 2;
//                                        dst.top = 0;
//                                        dst.right = dst.left + (int) (src.width() * scale);
//                                        dst.bottom = dst.top + canvas.getHeight();
//                                    }
//                                    //Bitmap bitmap：要绘制的位图对象
//                                    // Rect src： 是对图片进行裁截，若是空null则显示整个图片
//                                    // RectF dst：是图片在Canvas画布中显示的区域
//                                    canvas.drawBitmap(mGenerateImage, src, dst, mPaint);
//                                    //保存 save和restore配合使用可循环绘制
//                                    // (http://blog.csdn.net/tianjian4592/article/details/45234419)
//                                    canvas.save();
//                                    //缩放
//                                    canvas.scale((float) dst.width() / (float) src.width(), (float) dst.height()
//                                            / (float) src.height());
//                                    //移动canvas
//                                    canvas.translate(dst.left / scale, dst.top / scale);
//                                    for (AFD_FSDKFace face : result) {
//                                        mPaint.setColor(Color.RED);
//                                        mPaint.setStrokeWidth(10.0f);
//                                        //仅描边不填充
//                                        mPaint.setStyle(Paint.Style.STROKE);
//                                        //绘制人脸区域
//                                        canvas.drawRect(face.getRect(), mPaint);
//                                    }
//                                    //画布状态回滚
//                                    canvas.restore();
//                                    //通知系统Surface已经绘制完成 显示出来
//                                    mSurfaceHolder.unlockCanvasAndPost(canvas);
//                                    break;
//                                }
//                            }
                            error1 = engine1.AFR_FSDK_GetVersion(version1);
                            //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
                            Log.i(TAG, "FR=" + version.toString() + "," + error1.getCode());
                            //result1保存了人脸特征信息
                            error1 = engine1.AFR_FSDK_ExtractFRFeature(data, mGenerateImage.getWidth(), mGenerateImage.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(result.get(0).getRect()), result.get(0).getDegree(), result1);
                            Log.i(TAG, "Face=" + result1.getFeatureData()[0] + "," + result1.getFeatureData()[1] + "," + result1.getFeatureData()[2] + "," + error1.getCode());
                            if (error1.getCode() == error1.MOK) {
                                //多个人脸特征时会导致同时操作一个变量出现错误
                                mAFR_FSDKFace = result1.clone();
                                Log.i(TAG, "mAFR_FSDKFace=" + mAFR_FSDKFace);
                                int width = result.get(0).getRect().width();
                                int height = result.get(0).getRect().height();
                                //依据检测出的人脸矩形大小创建一个bitmap
                                Bitmap face_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                                Canvas face_canvas = new Canvas(face_bitmap);
                                face_canvas.drawBitmap(mGenerateImage, result.get(0).getRect(), new Rect(0, 0, width, height), null);  //从原图中取出人脸矩形


                                boolean isAdded = ((MyApplication) RegisterActivity.
                                        this.getApplicationContext()).mFaceDB.addFace(contact.getName(), mAFR_FSDKFace);


                                Message reg = Message.obtain();
                                reg.what = MSG_CODE;
                                //人脸信息正常
                                reg.arg1 = MSG_EVENT_REG;
                                reg.arg2=mFilePath.size();
                                reg.obj = face_bitmap;
                                Bundle bundle=new Bundle();
                                bundle.putBoolean("isAdded",isAdded);
                                bundle.putString("name",contact.getName());
                                reg.setData(bundle);
                                mUIHandler.sendMessage(reg);
                            } else {
                                Message reg = Message.obtain();
                                reg.what = MSG_CODE;
                                //人脸特征无法检测
                                reg.arg1 = MSG_EVENT_NO_FEATURE;
                                mUIHandler.sendMessage(reg);
                            }
                            //销毁人脸识别引擎
                            error1 = engine1.AFR_FSDK_UninitialEngine();
                            Log.i(TAG, "AFR_FSDK_UninitialEngine : " + error1.getCode());
                        } else {
                            Message reg = Message.obtain();
                            reg.what = MSG_CODE;
                            //没有检测到人脸
                            reg.arg1 = MSG_EVENT_NO_FACE;
                            mUIHandler.sendMessage(reg);
                        }
                        //销毁人脸检测引擎
                        err = engine.AFD_FSDK_UninitialFaceEngine();
                        Log.i(TAG, "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
                        if (mGenerateImage != null) {
                            mGenerateImage.recycle();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }
            }
            );
            mView.start();
        }
    }

    /**
     * main thread  used for show image
     */
    class UIHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CODE) {
                if (msg.arg1 == MSG_EVENT_REG) {
                    Bitmap bitmap = (Bitmap) msg.obj;
                    //image.setImageBitmap(bitmap);
                    Log.i(TAG,"count=="+ count++);

                    Bundle data = msg.getData();
                    boolean isAdded = (Boolean) data.get("isAdded");
                    String name=(String)data.get("name");
                    int size = msg.arg2;
                    Log.i("zsn","size="+size+"");
                    if (isAdded) {
                        Toast.makeText(RegisterActivity.this, name+"--->注册成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, name+"--->已注册", Toast.LENGTH_SHORT).show();
                    }

                } else if (msg.arg1 == MSG_EVENT_NO_FEATURE) {
                    Toast.makeText(RegisterActivity.this, "人脸特征无法检测，请换一张图片", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == MSG_EVENT_NO_FACE) {
                    Toast.makeText(RegisterActivity.this, "没有检测到人脸，请换一张图片", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == MSG_EVENT_FD_ERROR) {
                    Toast.makeText(RegisterActivity.this, "FD初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == MSG_EVENT_FR_ERROR) {
                    Toast.makeText(RegisterActivity.this, "FR初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                }
            }
            finish();

        }
    }

    /**
     * @param bundle
     * @note bundle data :
     * String imagePath
     */
    private boolean getIntentData(Bundle bundle) {
        try {
            mFilePath = (ArrayList<Contact>)bundle.getSerializable("imagePath");
            if (mFilePath == null || mFilePath.isEmpty()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
        try {
            mView.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
