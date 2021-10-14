package no.nordicsemi.android.nrfmesh.node;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.datastore.generated.model.Priority;
import com.amplifyframework.datastore.generated.model.Todo;
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.result.IdentifyEntitiesResult;
import com.bumptech.glide.Glide;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import dagger.hilt.android.AndroidEntryPoint;
import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.models.GenericOnOffServerModel;
import no.nordicsemi.android.mesh.transport.Element;
import no.nordicsemi.android.mesh.transport.GenericOnOffGet;
import no.nordicsemi.android.mesh.transport.GenericOnOffSet;
import no.nordicsemi.android.mesh.transport.GenericOnOffStatus;
import no.nordicsemi.android.mesh.transport.MeshMessage;
import no.nordicsemi.android.mesh.transport.MeshModel;
import no.nordicsemi.android.mesh.transport.ProvisionedMeshNode;
import no.nordicsemi.android.mesh.utils.MeshAddress;
import no.nordicsemi.android.mesh.utils.MeshParserUtils;
import no.nordicsemi.android.nrfmesh.R;
import no.nordicsemi.android.nrfmesh.databinding.LayoutGenericOnOffBinding;

@AndroidEntryPoint
public class GenericOnOffServerActivity extends ModelConfigurationActivity {

    private static final String TAG = GenericOnOffServerActivity.class.getSimpleName();

    private TextView onOffState;
    private TextView remainingTime;
    private Button mActionOnOff;
    protected int mTransitionStepResolution;
    protected int mTransitionSteps;

    //add------------------------------
    private Button btHigh;
    private String mPath = "/storage/emulated/0/DetectFace/yyMMdd.jpg";//設置的照片位址
    public static final int CAMERA_PERMISSION = 100;//檢測相機權限用
    public static final int REQUEST_HIGH_IMAGE = 101;//檢測相機回傳
    byte value_R, value_G, value_B;
    //end-------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSwipe.setOnRefreshListener(this);
        final MeshModel model = mViewModel.getSelectedModel().getValue();
        if (model instanceof GenericOnOffServerModel) {
            final LayoutGenericOnOffBinding nodeControlsContainer = LayoutGenericOnOffBinding.inflate(getLayoutInflater(), binding.nodeControlsContainer, true);
            final TextView time = nodeControlsContainer.transitionTime;
            onOffState = nodeControlsContainer.onOffState;
            remainingTime = nodeControlsContainer.transitionState;
            final Slider transitionTimeSlider = nodeControlsContainer.transitionSlider;
            transitionTimeSlider.setValueFrom(0);
            transitionTimeSlider.setValueTo(230);
            transitionTimeSlider.setValue(0);
            transitionTimeSlider.setStepSize(1);

            final Slider delaySlider = nodeControlsContainer.delaySlider;
            delaySlider.setValueFrom(0);
            delaySlider.setValueTo(255);
            delaySlider.setValue(0);
            delaySlider.setStepSize(1);
            final TextView delayTime = nodeControlsContainer.delayTime;

            mActionOnOff = nodeControlsContainer.actionOn;
            mActionOnOff.setOnClickListener(v -> {
                try {
                    sendGenericOnOff(mActionOnOff.getText().toString().equals(getString(R.string.action_generic_on)), (int) delaySlider.getValue());
                } catch (IllegalArgumentException ex) {
                    mViewModel.displaySnackBar(this, mContainer, ex.getMessage(), Snackbar.LENGTH_LONG);
                }
            });


            //alan_add
            Slider slider1 = (Slider) findViewById(R.id.seekbar_Red);
            Slider slider2 = (Slider) findViewById(R.id.seekbar_Green);
            Slider slider3 = (Slider) findViewById(R.id.seekbar_Blue);

//        slider1.showContextMenu();// 设置推动时显示指示器
//        slider2.showContextMenu();
//        slider3.showContextMenu();

            slider1.setTrackTintList(ColorStateList.valueOf(0xFF881515));// 滑軌底色
            slider2.setTrackTintList(ColorStateList.valueOf(0xFF308014));
            slider3.setTrackTintList(ColorStateList.valueOf(0xFF3D59AB));

            slider1.setValue(0);// 设定初始进度
            slider2.setValue(0);
            slider3.setValue(0);

            slider1.setValueFrom(0);
            slider2.setValueFrom(0);
            slider3.setValueFrom(0);

            slider1.setValueTo(100);// 设定最终进度
            slider2.setValueTo(100);
            slider3.setValueTo(100);

            slider1.setStepSize(1);
            slider2.setStepSize(1);
            slider3.setStepSize(1);

            slider1.setHaloTintList(ColorStateList.valueOf(0xFFFF0000));// 设定光環顔色
            slider2.setHaloTintList(ColorStateList.valueOf(0xFF00FF00));
            slider3.setHaloTintList(ColorStateList.valueOf(0xFF0000FF));

            slider1.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));// 设定滑塊顔色
            slider2.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));
            slider3.setThumbTintList(ColorStateList.valueOf(0xFFFFFAFA));

            slider1.setTrackActiveTintList(ColorStateList.valueOf(0xFFE3170D));//設置軌道活動部分顔色
            slider2.setTrackActiveTintList(ColorStateList.valueOf(0xFF32CD32));
            slider3.setTrackActiveTintList(ColorStateList.valueOf(0xFF1E90FF));

            slider1.setTrackHeight(20);//設置軌道寬度
            slider2.setTrackHeight(20);
            slider3.setTrackHeight(20);
//        slider1.setBackgroundColor(getResources().getColor(R.color.red));// 背景颜色// 监听进度
//        slider1.setOnValueChangedListener(new OnValueChangedListener() {
//
//            @Override
//            public void onValueChanged(int value) {
//                // TODO 自动生成的方法存根
//                System.out.println("now value = "+ value);
//            }
//        });

            slider1.setThumbElevation(30);// 设置滑块的影子大小
            slider2.setThumbElevation(30);// 设置滑块的影子大小
            slider3.setThumbElevation(30);// 设置滑块的影子大小
            //add
            final byte[] b = new byte[101];
            for(int count = 0;count <= 100;count++){
                b[count] = (byte)(count);
            };
            //end

            mActionRead = nodeControlsContainer.actionRead;
            mActionRead.setOnClickListener(v -> sendGenericOnOffGet());

            transitionTimeSlider.addOnChangeListener(new Slider.OnChangeListener() {
                int lastValue = 0;
                double res = 0.0;

                @Override
                public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                    //add
                    slider1.addOnChangeListener(new Slider.OnChangeListener() {
                        int lastValue = 0;
//                double res = 0.0;

                        @Override
                        public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                            final int progress1 = (int) value;
                            lastValue = progress1;
                            mTransitionStepResolution = progress1-1;
                            mTransitionSteps = progress1;
                            value_R=b[progress1];
                        }
                    });

                    slider2.addOnChangeListener(new Slider.OnChangeListener() {
                        int lastValue2 = 0;
//                double res = 0.0;

                        @Override
                        public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                            final int progress2 = (int) value;
                            lastValue2 = progress2;
                            mTransitionStepResolution = progress2-1;
                            mTransitionSteps = progress2;
                            value_G=b[progress2];
                        }
                    });

                    slider3.addOnChangeListener(new Slider.OnChangeListener() {
                        int lastValue3 = 0;
//                double res = 0.0;

                        @Override
                        public void onValueChange(@NonNull final Slider slider, final float value, final boolean fromUser) {
                            final int progress3 = (int) value;
                            lastValue3 = progress3;
                            mTransitionStepResolution = progress3-1;
                            mTransitionSteps = progress3;
                            value_B=b[progress3];
                        }
                    });
                    //end
                }
            });
            delaySlider.addOnChangeListener((slider, value, fromUser) -> delayTime.setText(getString(R.string.transition_time_interval, String.valueOf((int) value * MeshParserUtils.GENERIC_ON_OFF_5_MS), "ms")));

            mViewModel.getSelectedModel().observe(this, meshModel -> {
                if (meshModel != null) {
                    updateAppStatusUi(meshModel);
                    updatePublicationUi(meshModel);
                    updateSubscriptionUi(meshModel);
                }
            });


            //相機
            //setContentView(R.layout.layout_generic_on_off);
            //Button btHigh = findViewById(R.id.buttonHigh);
            //取得相機權限
            if (checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
            /**按下照相之拍攝按鈕*/
            btHigh = findViewById(R.id.buttonHigh);
            btHigh.setOnClickListener(v->{
                Intent highIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //檢查是否已取得權限
                if (highIntent.resolveActivity(getPackageManager()) == null) return;
                //取得相片檔案的URI位址及設定檔案名稱
                File imageFile = getImageFile();
                if (imageFile == null) return;
                //取得相片檔案的URI位址
                Uri imageUri = FileProvider.getUriForFile(
                        this,
                        "no.nordicsemi.android.nrfmesh",//記得要跟AndroidManifest.xml中的authorities 一致
                        imageFile
                );
                highIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(highIntent,REQUEST_HIGH_IMAGE);//開啟相機
//                        registerForActivityResult(highIntent,REQUEST_HIGH_IMAGE);
            });

        }


        //add by myself


        try {
            //Amplify.addPlugin(new AWSApiPlugin());
            //Amplify.addPlugin(new AWSDataStorePlugin());
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSPredictionsPlugin());
            Amplify.configure(getApplicationContext());
            Log.i("Tutorial", "Initialized Amplify");


        } catch (AmplifyException failure) {
            Log.e("Tutorial", "Could not initialize Amplify", failure);
        }
//        Amplify.DataStore.observe(Todo.class,
//                started -> Log.i("Tutorial", "Observation began."),
//                change -> Log.i("Tutorial", change.item().toString()),
//                failure -> Log.e("Tutorial", "Observation failed.", failure),
//                () -> Log.i("Tutorial", "Observation complete.")
//        );

//        Amplify.Auth.fetchAuthSession(
//                result -> Log.i("AmplifyQuickstart", result.toString()),
//                error -> Log.e("AmplifyQuickstart", error.toString())
//        );


//        AuthSignUpOptions options = AuthSignUpOptions.builder()
//                .userAttribute(AuthUserAttributeKey.email(), "joe.hung@ecomm.com.tw")
//                .build();
//        Amplify.Auth.signIn(
//                "mood1013",
//                "mood1013",
//                result -> Log.i("AuthQuickstart", result.isSignInComplete() ? "Sign in succeeded" : "Sign in not complete"),
//                error -> Log.e("AuthQuickstart", error.toString())
//        );



        //create DetectFace folder in android for picture
        String DetectFacedir = "/DetectFace/";
        File PrimaryStorage = Environment.getExternalStorageDirectory();
        File PICDir = new File("/storage/emulated/0/DetectFace/");
        File ReadyPath = new File("/storage/emulated/0/DetectFace/" + "Ready.txt");
        Log.e("str", String.valueOf(PrimaryStorage));
        try {
            Log.i("test", "delete CMD");
            String deleteCmd = "rm -r " + ReadyPath;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(deleteCmd);


        } catch (FileNotFoundException e) {
            Log.e("NOTFOUND", "file notfound");
        } catch (IOException e) {
            Log.e("IOERROR", "some IO error");
        }

        Task task = new Task();

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(0);
        executor.scheduleWithFixedDelay(task, 1, 300, TimeUnit.SECONDS);
        //end----------------
    }

    /**取得相片檔案的URI位址及設定檔案名稱*/
    private File getImageFile()  {
        Date date = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String time = sdf.format(date);
        String fileName = time;
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File f = new File(mPath);
//        try {
//            //給予檔案命名及檔案格式
//            File imageFile = File.createTempFile(fileName,".jpg", f);
//            //給予全域變數中的照片檔案位置，方便後面取得
//            Log.e("aa", "imageFile = " + imageFile.getAbsolutePath());
//            mPath = imageFile.getAbsolutePath();
//            return imageFile;
//        } catch (IOException e) {
//            return null;
//        }

        try {
            //給予檔案命名及檔案格式
//             File imageFile = File.createTempFile(fileName,".jpg", f);
//             File imageFile = new File("/storage/emulated/0/Detectface/yyMMdd.jpeg");
//             imageFile.mkdir();
            f.createNewFile();
            //給予全域變數中的照片檔案位置，方便後面取得
            Log.e("aa", "imageFile = " + f.getAbsolutePath());
            mPath = f.getAbsolutePath();
            return f;
        } catch (IOException e) {
            return null;
        }
    }

    /**取得照片回傳*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**可在此檢視回傳為哪個相片，requestCode為上述自定義，resultCode為-1就是有拍照，0則是使用者沒拍照*/
        Log.d(TAG, "onActivityResult: requestCode: "+requestCode+", resultCode "+resultCode);
        /**如果是的相片回傳*/
        if (requestCode == REQUEST_HIGH_IMAGE && resultCode == -1) {
            ImageView imageHigh = findViewById(R.id.imageViewHigh);
            new Thread(() -> {
                //在BitmapFactory中以檔案URI路徑取得相片檔案，並處理為AtomicReference<Bitmap>，方便後續旋轉圖片
                AtomicReference<Bitmap> getHighImage = new AtomicReference<>(BitmapFactory.decodeFile(mPath));
                Matrix matrix = new Matrix();
                matrix.setRotate(90f);//轉90度
                getHighImage.set(Bitmap.createBitmap(getHighImage.get()
                        , 0, 0
                        , getHighImage.get().getWidth()
                        , getHighImage.get().getHeight()
                        , matrix, true));
                runOnUiThread(() -> {
                    //以Glide設置圖片(因為旋轉圖片屬於耗時處理，故會LAG一下，且必須使用Thread執行緒)
                    Glide.with(this)
                            .load(getHighImage.get())
                            .centerCrop()
                            .into(imageHigh);
                });
            }).start();
        }
        else{
            Toast.makeText(this, "未作任何拍攝", Toast.LENGTH_SHORT).show();
        }

    }

    class Task implements Runnable {
        public void run() {
            Log.i("test", "run started");
//             File PrimaryStorage = Environment.getExternalStorageDirectory();
//             //Log.e("str", String.valueOf(PrimaryStorage));
//             String Facedir = "/DetectFace/";
//             String ReadyFil = "READY.txt";
//           File imageFile = new File("/storage/emulated/0/Detectface/yyMMdd.jpeg");
//            imageFile.mkdir();
//             //Log.i("test","create file");
//             //File imageFile = new File(System.currentTimeMillis() + ".jpg");
//             File ReadyPath = new File("/storage/emulated/0/Detectface2/" + ReadyFil);


//            File file = new File(ReadyPath, ReadyFil);
//            FileOutputStream outputStream = null;
//            try {
//                outputStream = new FileOutputStream(ReadyFil);
//                outputStream.write("0".getBytes());
//                outputStream.close();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            //if (ReadyPath.exists()) {
            //Log.e("try","ReadyPath exists");
//                try {
//                    String deleteCmd = "rm -r " + ReadyPath;
//                    Runtime runtime = Runtime.getRuntime();
//                    runtime.exec(deleteCmd);
//
//                } catch (FileNotFoundException e) {
//                    Log.e("NOTFOUND", "file notfound");
//                } catch (IOException e) {
//                    Log.e("IOERROR", "some IO error");
//                }


            try {
                Log.i("try", "DetectEntities");
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap image = BitmapFactory.decodeFile(String.valueOf(mPath), bmOptions); //將照片轉為jpg
                DetectEntities(image); //將照片上傳至AWS
            } catch (Exception e) {
                Log.e("DETECT", "detect error" + e.getMessage());
            }
            //}
        }
    }


    private void DetectEntities(Bitmap image) {
        if (mPath == "/storage/emulated/0/Detectface/yyMMdd.jpg") {
            try {
                Log.i("DETECTENTITIES", "started");

                Amplify.Predictions.identify(  //啟用臉部辨識系統
                        IdentifyActionType.DETECT_ENTITIES,
                        image,
                        result -> LabelDataHold((IdentifyEntitiesResult) result, image),

                        error -> Log.e("AmplifyQuickstart", "Identify failed ", error)// + error.getMessage())
                );

                Log.i("DETECTENTITIES", "finished");

            } catch (Exception e) {
                Log.e("DETECT", "DetectEntities error " + e.getMessage()); //+ e.getMessage());
            }
        }
    }


    private void LabelDataHold(IdentifyEntitiesResult result, Bitmap image) {
        final String[] printout = new String[result.getEntities().size()];
        double[][] Xnumber = new double[result.getEntities().size()][];
        int max = result.getEntities().size();

        for (int m = 0; m < max; m++) {
            printout[m] = String.valueOf(result.getEntities().get(m).getEmotions().get(m).getValue());
            printout[m] = String.valueOf(result.getEntities().get(m).getBox());
            printout[m] = String.valueOf(result.getEntities().get(m).getAgeRange());
            printout[m] = String.valueOf(result.getEntities().get(m).getGender());
            printout[m] = String.valueOf(result.getEntities().get(m).getLandmarks());
            printout[m] = String.valueOf(result.getEntities().get(m).getPolygon());
            printout[m] = String.valueOf(result.getEntities().get(m).getPose());


            //result.getEntities().get(0).getAgeRange().getLow();

            //Log.i("result", result.toString());
            Log.i("Emotions  Result", result.getEntities().get(m).getEmotions().get(m).getValue()
                    + ", Confidence: " + result.getEntities().get(m).getEmotions().get(m).getConfidence());

            Log.i("AgeRange  Result", "Age: " + result.getEntities().get(0).getAgeRange().getLow()
                    + " - " + result.getEntities().get(0).getAgeRange().getHigh());

            Log.i("Gender    Result", result.getEntities().get(0).getGender().getValue()
                    + ", Confidence: " + result.getEntities().get(0).getGender().getConfidence());

//         Log.i("Try           Result", result.getEntities().get(0).
//                 + ", Confidence: " + result.getEntities().get(0).getEmotions().get(0).getConfidence());

            //Log.i("Landmarks Result", String.valueOf(result.getEntities().get(0).getLandmarks()));
        }

    }

    //end






    @Override
    protected void enableClickableViews() {
        super.enableClickableViews();
        if (mActionOnOff != null && !mActionOnOff.isEnabled())
            mActionOnOff.setEnabled(true);
    }

    @Override
    protected void disableClickableViews() {
        super.disableClickableViews();
        if (mActionOnOff != null)
            mActionOnOff.setEnabled(false);
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
    }

    @Override
    protected void updateMeshMessage(final MeshMessage meshMessage) {
        super.updateMeshMessage(meshMessage);
        mSwipe.setOnRefreshListener(this);
        if (meshMessage instanceof GenericOnOffStatus) {
            final GenericOnOffStatus status = (GenericOnOffStatus) meshMessage;
            final boolean presentState = status.getPresentState();
            final Boolean targetOnOff = status.getTargetState();
            final int steps = status.getTransitionSteps();
            final int resolution = status.getTransitionResolution();
            if (targetOnOff == null) {
                if (presentState) {
                    onOffState.setText(R.string.generic_state_on);
                    mActionOnOff.setText(R.string.action_generic_off);
                } else {
                    onOffState.setText(R.string.generic_state_off);
                    mActionOnOff.setText(R.string.action_generic_on);
                }
                remainingTime.setVisibility(View.GONE);
            } else {
                if (!targetOnOff) {
                    onOffState.setText(R.string.generic_state_on);
                    mActionOnOff.setText(R.string.action_generic_off);
                } else {
                    onOffState.setText(R.string.generic_state_off);
                    mActionOnOff.setText(R.string.action_generic_on);
                }
                remainingTime.setText(getString(R.string.remaining_time, MeshParserUtils.getRemainingTransitionTime(resolution, steps)));
                remainingTime.setVisibility(View.VISIBLE);
            }
        }
        hideProgressBar();
    }

    /**
     * Send generic on off get to mesh node
     */
    public void sendGenericOnOffGet() {
        if (!checkConnectivity(mContainer)) return;
        final Element element = mViewModel.getSelectedElement().getValue();
        if (element != null) {
            final MeshModel model = mViewModel.getSelectedModel().getValue();
            if (model != null) {
                if (!model.getBoundAppKeyIndexes().isEmpty()) {
                    final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
                    final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);

                    final int address = element.getElementAddress();
                    Log.v(TAG, "Sending message to element's unicast address: " + MeshAddress.formatAddress(address, true));

                    final GenericOnOffGet genericOnOffSet = new GenericOnOffGet(appKey);
                    sendMessage(address, genericOnOffSet);
                } else {
                    mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    /**
     * Send generic on off set to mesh node
     *
     * @param state true to turn on and false to turn off
     * @param delay message execution delay in 5ms steps. After this delay milliseconds the model will execute the required behaviour.
     */
    public void sendGenericOnOff(final boolean state, final Integer delay) {
        if (!checkConnectivity(mContainer)) return;
        final ProvisionedMeshNode node = mViewModel.getSelectedMeshNode().getValue();
        if (node != null) {
            final Element element = mViewModel.getSelectedElement().getValue();
            if (element != null) {
                final MeshModel model = mViewModel.getSelectedModel().getValue();
                if (model != null) {
                    if (!model.getBoundAppKeyIndexes().isEmpty()) {
                        final int appKeyIndex = model.getBoundAppKeyIndexes().get(0);
                        final ApplicationKey appKey = mViewModel.getNetworkLiveData().getMeshNetwork().getAppKey(appKeyIndex);
                        final int address = element.getElementAddress();
                        final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(appKey, state,
                                new Random().nextInt(), mTransitionSteps, mTransitionStepResolution, delay);
                        sendMessage(address, genericOnOffSet);
                    } else {
                        mViewModel.displaySnackBar(this, mContainer, getString(R.string.error_no_app_keys_bound), Snackbar.LENGTH_LONG);
                    }
                }
            }
        }
    }
}
