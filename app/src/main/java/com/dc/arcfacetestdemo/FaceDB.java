package com.dc.arcfacetestdemo;

import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.java.ExtInputStream;
import com.guo.android_extend.java.ExtOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zsn10 on 2017/7/11.
 */

public class FaceDB {
    private final String TAG = this.getClass().toString();

    public static String appid = "自己申请的appid";
    public static String ft_key = "自己申请的key";
    public static String fd_key = "自己申请的key";
    public static String fr_key = "自己申请的key";


    String mDBPath;
    List<FaceRegist> mRegister;
    AFR_FSDKEngine mFREngine;
    AFR_FSDKVersion mFRVersion;
    boolean mUpgrade;

    class FaceRegist {
        String mName;
        List<AFR_FSDKFace> mFaceList;

        public FaceRegist(String name) {
            mName = name;
            mFaceList = new ArrayList<>();
        }
    }

    public FaceDB(String path) {
        mDBPath = path;
        mRegister = new ArrayList<>();
        mFRVersion = new AFR_FSDKVersion();
        mUpgrade = false;
        mFREngine = new AFR_FSDKEngine();
        AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
        if (error.getCode() != AFR_FSDKError.MOK) {
            Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
        } else {
            mFREngine.AFR_FSDK_GetVersion(mFRVersion);
            Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
        }
    }

    public void destroy() {
        if (mFREngine != null) {
            mFREngine.AFR_FSDK_UninitialEngine();
        }
    }

    private boolean saveInfo() {
        try {
            FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt");
            ExtOutputStream bos = new ExtOutputStream(fs);
            Log.d(TAG, "保存的信息=" + mFRVersion.toString() + "," + mFRVersion.getFeatureLevel());
            bos.writeString(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel());
            bos.close();
            fs.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean loadInfo() {
        try {
            FileInputStream fs = new FileInputStream(mDBPath + "/face.txt");
            ExtInputStream bos = new ExtInputStream(fs);
            //load version
            String version_saved = bos.readString();
            Log.i(TAG, "version_saved=" + version_saved);
            if (version_saved.equals(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel())) {
                mUpgrade = true;
            }
            //load all regist name.
            if (version_saved != null) {
                for (String name = bos.readString(); name != null; name = bos.readString()) {
                    mRegister.add(new FaceRegist(new String(name)));
                    Log.i(TAG, "FaceRegist=" + name);
                }
            }
            bos.close();
            fs.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadFaces() {
        if (loadInfo()) {
            try {
                for (FaceRegist face : mRegister) {
                    Log.i(TAG, "load name:" + face.mName + "'s face feature data.");
                    Log.i(TAG, "mDBPath=" + mDBPath);
                    FileInputStream fs = new FileInputStream(mDBPath + "/" + face.mName + ".data");
                    ExtInputStream bos = new ExtInputStream(fs);
                    AFR_FSDKFace afr = null;
                    do {
                        if (afr != null) {
                            if (mUpgrade) {
                                //upgrade data.
                            }
                            face.mFaceList.add(afr);
                        }
                        afr = new AFR_FSDKFace();
                    } while (bos.readBytes(afr.getFeatureData()));
                    bos.close();
                    fs.close();
                }
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!saveInfo()) {
                Log.e(TAG, "save fail!");
            }
        }
        return false;
    }

    // TODO: 2017/10/13  待完善注册时先对比人脸库中是否含有同一个人 而非使用名字将特征放在一起
    public boolean addFace(String registerName, AFR_FSDKFace result) {
        try {
            //check if already registered.
            AFR_FSDKMatching score = new AFR_FSDKMatching();

            float max = 0.0f;
            String name = null;
            for (FaceDB.FaceRegist fr : mRegister) {
                for (AFR_FSDKFace face : fr.mFaceList) {
                    AFR_FSDKError e = mFREngine.AFR_FSDK_FacePairMatching(result, face, score);
                    Log.i(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + e.getCode());
                    if (max < score.getScore()) {
                        max = score.getScore();
                        name = fr.mName;
                    }
                }
            }
            Log.i(TAG, "max==" + max);
            if (max > 0.6f) {
                //fr success.
                return false;
            } else {
                final String mNameShow = "未识别";
                FaceRegist frface = new FaceRegist(registerName);
                frface.mFaceList.add(result);
                mRegister.add(frface);
            }

//-----------------------2--------------------------------
//            boolean add = true;
//            boolean isSame = false;
//            for (FaceRegist frface : mRegister) {
//                //判断数据库中是否存在该人名字 有则将脸谱加入list中
//                if (frface.mName.equals(registerName)) {
//                    for (AFR_FSDKFace afr_fsdkFace : frface.mFaceList) {
//                        if (new String(result.getFeatureData()).equals(new String(afr_fsdkFace.getFeatureData()))) {  //byte[]转换为string后对比
//                            isSame = true;  //传入的与list中已存在的任意一个特征相同即为重复
//                        }
//                    }
//                    if (!isSame)  //不重复则添加进list
//                        frface.mFaceList.add(result);
//                    add = false;
//                    break;
//                }
//            }
//
//            if (add) { // not registered.
//                FaceRegist frface = new FaceRegist(registerName);
//                frface.mFaceList.add(result);
//                mRegister.add(frface);
//            }
//
//            if (!new File(mDBPath + "/face.txt").exists()) {
//                if (!saveInfo()) {
//                    Log.e(TAG, "save fail!");
//                }
//            }
//
//            if (!isSame) {  //重复特征则不保存信息
//                if (saveInfo()) {
//                    //update all names
//                    FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
//                    ExtOutputStream bos = new ExtOutputStream(fs);
//                    for (FaceRegist frface : mRegister) {
//                        bos.writeString(frface.mName);
//                    }
//                    bos.close();
//                    fs.close();
//
//                    //save new feature
//                    fs = new FileOutputStream(mDBPath + "/" + registerName + ".data", true);
//                    bos = new ExtOutputStream(fs);
//                    bos.writeBytes(result.getFeatureData());
//                    bos.close();
//                    fs.close();
//                }
//                return true;
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return false;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//        return true;


//----------3-----------------

//			for (FaceRegist frface : mRegister) {
//				if (frface.mName.equals(name)) {
//					frface.mFaceList.add(face);
//					add = false;
//					break;
//				}
//			}
//			if (add) { // not registered.
//				FaceRegist frface = new FaceRegist(name);
//				frface.mFaceList.add(result);
//				mRegister.add(frface);
//			}

            if (!new File(mDBPath + "/face.txt").exists()) {
                if (!saveInfo()) {
                    Log.i(TAG, "save fail!");
                }
            }
            Log.i(TAG, "save name --mDBPath=" + mDBPath);
            //save name
            FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
            ExtOutputStream bos = new ExtOutputStream(fs);
            bos.writeString(registerName);
            bos.close();
            fs.close();

            Log.i(TAG, "save feature --mDBPath=" + mDBPath);
            //save feature
            fs = new FileOutputStream(mDBPath + "/" + registerName + ".data", true);
            bos = new ExtOutputStream(fs);
            bos.writeBytes(result.getFeatureData());
            bos.close();
            fs.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean upgrade() {
        return false;
    }

}
