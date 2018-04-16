package com.dc.arcfacetestdemo;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AlertDialog.OnClickListener {

    private final String TAG = getClass().toString();
    private Uri mPath;
    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_OP = 3;
    private ArrayList<String> mResults = new ArrayList<>();
    private String mName;
    private EditText et_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_name = (EditText) findViewById(R.id.et_name);
        Button sureButton = (Button) findViewById(R.id.sure);
        sureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mName=et_name.getText().toString();
                et_name.setClickable(false);
                et_name.setEnabled(false);
                et_name.setTextColor(Color.RED);
            }
        });
        Button upStrButton = (Button) findViewById(R.id.upStr);
        upStrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("请选择图片")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setItems(new String[]{"打开图片", "拍摄照片"}, MainActivity.this)
                        .show();
            }
        });
        Button registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAllPic();
            }
        });
        Button detecterButton = (Button) findViewById(R.id.detecter);
        detecterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((MyApplication) getApplicationContext()).mFaceDB.mRegister.isEmpty()) {
                    Toast.makeText(MainActivity.this, "没有注册人脸，请先注册！", Toast.LENGTH_SHORT).show();
                } else {
                    Intent it = new Intent(MainActivity.this, DetecterActivity.class);
                    startActivityForResult(it, REQUEST_CODE_OP);
                }
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 1:
                Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE");
                ContentValues values = new ContentValues(1);

                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                mPath = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                getImageByCamera.putExtra(MediaStore.EXTRA_OUTPUT, mPath);

                startActivityForResult(getImageByCamera, REQUEST_CODE_IMAGE_CAMERA);
                break;
            case 0:
                Intent getImageByalbum = new Intent(Intent.ACTION_GET_CONTENT);
                getImageByalbum.addCategory(Intent.CATEGORY_OPENABLE);
                getImageByalbum.setType("image/jpeg");
                startActivityForResult(getImageByalbum, REQUEST_CODE_IMAGE_OP);
                break;
            default:
                ;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMAGE_OP && resultCode == RESULT_OK) {
            mPath = data.getData();
            String file = getPath(mPath);
            Bitmap bmp = ImageUtil.decodeImage(file);
            if (bmp == null || bmp.getWidth() <= 0 || bmp.getHeight() <= 0) {
                Log.e(TAG, "error");
            } else {
                Log.i(TAG, "bmp [" + bmp.getWidth() + "," + bmp.getHeight());
            }

            String encodeString = ImageUtil.GetImageStr(file);
            upString(encodeString,mName);
        } else if (requestCode == REQUEST_CODE_OP) {
            Log.i(TAG, "RESULT =" + resultCode);
            if (data == null) {
                return;
            }
            Bundle bundle = data.getExtras();
            String path = bundle.getString("imagePath");
            Log.i(TAG, "path=" + path);
        } else if (requestCode == REQUEST_CODE_IMAGE_CAMERA && resultCode == RESULT_OK) {
            String file = getPath(mPath);
            //Bitmap bmp = ImageUtil.decodeImage(file);
            String encodeString = ImageUtil.GetImageStr(file);
            upString(encodeString,mName);
        }
        et_name.setText("");
        et_name.setClickable(true);
        et_name.setEnabled(true);
        et_name.setTextColor(Color.BLACK);
    }

    /**
     *
     */
    private void registContactBitmap(ArrayList<Contact> file) {
        Intent it = new Intent(MainActivity.this, RegisterActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("imagePath", file);
        // bundle.putString("imagePath", file);
        it.putExtras(bundle);
        startActivityForResult(it, REQUEST_CODE_OP);
    }

    /**
     * @param uri
     * @return
     */
    private String getPath(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    return null;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    return null;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(this, contentUri, selection, selectionArgs);
            }
        }
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor actualimagecursor = managedQuery(uri, proj, null, null, null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        actualimagecursor.moveToFirst();
        String img_path = actualimagecursor.getString(actual_image_column_index);
        String end = img_path.substring(img_path.length() - 4);
        if (0 != end.compareToIgnoreCase(".jpg") && 0 != end.compareToIgnoreCase(".png")) {
            return null;
        }
        return img_path;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * 向服务器传图片的base64字符串
     * @param upString
     * @param name
     */
    public void upString(String upString, String name) {
        try {
            JSONObject object = new JSONObject();
            object.put("name", name);
            object.put("pic", upString);

            OkGo.<String>post(Urls.URL_TEXT_UPLOAD)//
                    .tag(this)//
                    .headers("Connection", "close")//如果对于部分自签名的https访问不成功，需要加上该控制头
                    .headers("Content-Type", "application/json")//
                    //.headers("token", token)
                    //.params("param1", "paramValue1")/ / 这里不要使用params，upString 与 params 是互斥的，只有 upString 的数据会被上传
                    .upJson(object.toString())
                    //.upString("这是要上传的长文本数据！", MediaType.parse("application/xml"))// 比如上传xml数据，这里就可以自己指定请求头
                    .execute(new StringCallback() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            int code = response.code();
                            Log.i("zsn", "upString--code=" + String.valueOf(code));
                            if(code==200){
                                Toast.makeText(MainActivity.this,"上传图片--成功！！",Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(Response<String> response) {
                            super.onError(response);
                            Log.i("zsn", "error=" + response.message());
                            Toast.makeText(MainActivity.this,"上传图片--失败啦",Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        String desc = event.getmMsg();
        ArrayList<Contact> contactsList = event.getmContactList();
        registContactBitmap(contactsList);
    };

    /**
     * 获取所有的图片数据
     */
    public void getAllPic(){
        OkGo.<String>get(Urls.URL_TEXT_DOWNLOAD)
                .tag(this)
                .headers("Connection", "close")//如果对于部分自签名的https访问不成功，需要加上该控制头
                .headers("Content-Type", "application/json")//
                //.params("param1", "paramValue1")//
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        int code = response.code();
                        Log.i("zsn", "getAllPic--code=" + String.valueOf(code));
                        JsonObject object = new JsonParser().parse(response.body()).getAsJsonObject();
                        int responseCode = object.get("code").getAsInt();
                        String desc = object.get("desc").getAsString();
                        if (responseCode == 200) {
                            JsonArray content = object.get("content").getAsJsonArray();
                            Gson gson = new Gson();
                            ArrayList<Contact> contactList = gson.fromJson(content
                                            .toString(),
                                    new TypeToken<List<Contact>>() {
                                    }.getType());

                            EventBus.getDefault().post(new MessageEvent(desc,contactList));
                        }
                    }
                });
    }

}
