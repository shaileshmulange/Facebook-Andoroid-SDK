package mb.com.facebooksharingwithlogin;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.Sharer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.this.getClass().getName();


    private static final int PICK_GALLERY_VIDEO = 1;
    private static final int PICK_GALLERY_IMAGE = 2;

    private CallbackManager callbackManager;
    private AccessToken accessToken;

    private ShareButton shareButton;
    private LoginButton loginButton;
    private RelativeLayout rlProfileArea;
    private TextView tvName;
    private Button btnShareVideos;
    private Button btnShareImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initParameters();
        initViews();


//        // Add code to print out the key hash
//        try {
//            PackageInfo info = getPackageManager().getPackageInfo(
//                    "mb.com.facebooksharingwithlogin", PackageManager.GET_SIGNATURES);
//            for (Signature signature : info.signatures) {
//                MessageDigest md = MessageDigest.getInstance("SHA");
//                md.update(signature.toByteArray());
//                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//            }
//        } catch (PackageManager.NameNotFoundException e) {
//
//        } catch (NoSuchAlgorithmException e) {
//
//        }

        AccessTokenTracker accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {

                if (currentAccessToken == null) {
                    Log.d(TAG, "User logged out successfully");
                    tvName.setVisibility(View.INVISIBLE);
                }
            }
        };

    }

    public void initParameters() {
        accessToken = AccessToken.getCurrentAccessToken();
        callbackManager = CallbackManager.Factory.create();
    }

    public void initViews() {

        shareButton = new ShareButton(MainActivity.this);
        loginButton = (LoginButton) findViewById(R.id.activity_main_btn_login);
        rlProfileArea = (RelativeLayout) findViewById(R.id.activity_main_rl_profile_area);
        tvName = (TextView) findViewById(R.id.activity_main_tv_name);
        btnShareVideos = (Button) findViewById(R.id.activity_main_btn_share_videos);
        btnShareImages = (Button) findViewById(R.id.activity_main_btn_share_images);


        loginButton.setReadPermissions(Arrays.asList(new String[]{"email", "user_birthday", "user_hometown"}));

        if (accessToken != null) {
            getProfileData();
        } else {
            tvName.setVisibility(View.INVISIBLE);
        }

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "User login successfully");
                getProfileData();
            }

            @Override
            public void onCancel() {
                // App code
                Log.d(TAG, "User cancel login");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.d(TAG, "Problem for login");
            }
        });

        shareButton.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Log.d(TAG, "Media file share successfully");
                Toast.makeText(MainActivity.this, "Media file share successfully", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Media Share Cancel");
                Toast.makeText(MainActivity.this, "Media Share Cancel", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(FacebookException e) {
                Log.d(TAG, "Error for sharing media");
                Toast.makeText(MainActivity.this, "Error for sharing media", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        btnShareVideos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("video/*");
                startActivityForResult(intent, PICK_GALLERY_VIDEO);
            }
        });

        btnShareImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_GALLERY_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_GALLERY_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uriPhoto = data.getData();
                    Log.d(TAG, "Selected image path :" + uriPhoto.toString());


                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uriPhoto));
                        SharePhoto photo = new SharePhoto.Builder().setBitmap(bitmap)
                                .build();
                        SharePhotoContent content = new SharePhotoContent.Builder()
                                .addPhoto(photo).build();

                        shareButton.setShareContent(content);
                        shareButton.performClick();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
                break;
            case PICK_GALLERY_VIDEO:
                if (resultCode == RESULT_OK) {
                    Uri uriVideo = data.getData();

                    try {
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(uriVideo,
                                filePathColumn, null, null, null);
                        if (cursor != null) {
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String videoPath = cursor.getString(columnIndex);
                            cursor.close();
                            Log.d(TAG, "Selected video path :" + videoPath);

                            ShareVideo video = new ShareVideo.Builder()
                                    .setLocalUrl(uriVideo).build();

                            ShareVideoContent content = new ShareVideoContent.Builder()
                                    .setVideo(video).build();

                            shareButton.setShareContent(content);
                            shareButton.performClick();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
                break;
            default:
                callbackManager.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public void getProfileData() {
        try {
            accessToken = AccessToken.getCurrentAccessToken();
            tvName.setVisibility(View.VISIBLE);
            GraphRequest request = GraphRequest.newMeRequest(
                    accessToken,
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject object,
                                GraphResponse response) {
                            Log.d(TAG, "Graph Object :" + object);
                            try {
                                String name = object.getString("name");
                                tvName.setText("Welcome, " + name);

                                Log.d(TAG, "Name :" + name);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id,name,link,birthday,gender,email");
            request.setParameters(parameters);
            request.executeAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
