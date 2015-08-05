/**
 * Cerevo BlueNinja BLE Sample for Android
 * @auther  Cerevo Inc.
 * @since   2015/08/04
 */
/*
Copyright 2015 Cerevo Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.cerevo.blueninja.blueninja_ble_example;

import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    // BLEスキャンタイムアウト(ms)
    private static final int SCAN_TIMEOUT = 10000;
    //デバイス名
    private static final String DEVICE_NAME = "CDP-TZ01B";
    /* UUIDs */
    //BlueNinja Example Service
    private static final String UUID_SERVICE_BNEXAM = "d43a0200-0e5f-4a80-9182-5f82ff67e8f8";
    //ジャイロ
    private static final String UUID_CHARACTERISTIC_GYRO = "d43a0201-0e5f-4a80-9182-5f82ff67e8f8";
    //加速度センサ
    private static final String UUID_CHARACTERISTIC_ACCEL = "d43a0202-0e5f-4a80-9182-5f82ff67e8f8";
    //地磁気センサ
    private static final String UUID_CHARACTERISTIC_MAGM = "d43a0203-0e5f-4a80-9182-5f82ff67e8f8";
    //気圧センサー
    private static final String UUID_CHARACTERISTIC_AIRP = "d43a0212-0e5f-4a80-9182-5f82ff67e8f8";
    //UUIDリスト
    private static final String[] UUIDS_CHARACTERISTIC = {
            UUID_CHARACTERISTIC_ACCEL,
            UUID_CHARACTERISTIC_GYRO,
            UUID_CHARACTERISTIC_MAGM,
            UUID_CHARACTERISTIC_AIRP
    };
    //キャラクタリスティック設定UUID
    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    //ログのTAG
    private static final String TAG = "BNBLE";

    private AppState mAppStat = AppState.INIT;
    private Handler mHandler;

    private BluetoothAdapter mBtAdapter;
    private BluetoothManager mBtManager;
    private BluetoothGatt mBtGatt;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic[] mCharacteristics = new BluetoothGattCharacteristic[UUIDS_CHARACTERISTIC.length];

    private TextView mTextStatus;
    private Button mBtnConn;
    private Button mBtnDiscon;
    private Button mBtnDisable;
    private Button mBtnEnable;
    private TextView mTextAccelX;
    private TextView mTextAccelY;
    private TextView mTextAccelZ;
    private TextView mTextGyroX;
    private TextView mTextGyroY;
    private TextView mTextGyroZ;
    private TextView mTextMagnetoX;
    private TextView mTextMagnetoY;
    private TextView mTextMagnetoZ;
    private TextView mTextAirpressure;
    private TextView mTextAxisAngle;

    private ProgressBar mProgressAccelX;
    private ProgressBar mProgressAccelY;
    private ProgressBar mProgressAccelZ;
    private ProgressBar mProgressGyroX;
    private ProgressBar mProgressGyroY;
    private ProgressBar mProgressGyroZ;
    private ProgressBar mProgressMagnetoX;
    private ProgressBar mProgressMagnetoY;
    private ProgressBar mProgressMagnetoZ;

    private double mAccel[];
    private double mGyro[];
    private double mMagm[];
    private String mAxisAngle;
    private double mAirp;

    /**
     * ボタンクリック時のハンドラ
     */
    public View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnConn:
                    /* 接続ボタン */
                    connectBle();
                    break;
                case R.id.btnDisConn:
                    /* 切断ボタン */
                    disconnectBle();
                    break;
                case R.id.btnDisable:
                    /* Disable Notification ボタン */
                    disableBleNotification();
                    break;
                case R.id.btnEnable:
                    /* Enable Notification ボタン */
                    enableBleNotification();
                    break;
            }
        }
    };

    @Override
    /**
     *
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /* Bluetooth関連の初期化 */
        mBtManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBtAdapter = mBtManager.getAdapter();
        if ((mBtAdapter == null) || !mBtAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetoothが有効ではありません", Toast.LENGTH_SHORT).show();
            finish();
        }

        /* ウィジェットの初期化 */
        //ステータス
        mTextStatus = (TextView)findViewById(R.id.textView1);
        //接続ボタン
        mBtnConn = (Button)findViewById(R.id.btnConn);
        mBtnConn.setOnClickListener(buttonClickListener);
        mBtnConn.setEnabled(true);
        //接続断ボタン
        mBtnDiscon = (Button)findViewById(R.id.btnDisConn);
        mBtnDiscon.setOnClickListener(buttonClickListener);
        mBtnDiscon.setEnabled(false);
        //Notification無効ボタン
        mBtnDisable = (Button)findViewById(R.id.btnDisable);
        mBtnDisable.setOnClickListener(buttonClickListener);
        mBtnDisable.setEnabled(false);
        //Notification有効ボタン
        mBtnEnable = (Button)findViewById(R.id.btnEnable);
        mBtnEnable.setOnClickListener(buttonClickListener);
        mBtnEnable.setEnabled(false);
        /* 加速度 */
        mTextAccelX = (TextView)findViewById(R.id.textAccelX);
        mTextAccelY = (TextView)findViewById(R.id.textAccelY);
        mTextAccelZ = (TextView)findViewById(R.id.textAccelZ);
        mProgressAccelX = (ProgressBar)findViewById(R.id.progressAccelX);
        mProgressAccelY = (ProgressBar)findViewById(R.id.progressAccelY);
        mProgressAccelZ = (ProgressBar)findViewById(R.id.progressAccelZ);
        /* 角速度 */
        mTextGyroX = (TextView)findViewById(R.id.textGyroX);
        mTextGyroY = (TextView)findViewById(R.id.textGyroY);
        mTextGyroZ = (TextView)findViewById(R.id.textGyroZ);
        mProgressGyroX = (ProgressBar)findViewById(R.id.progressGyroX);
        mProgressGyroY = (ProgressBar)findViewById(R.id.progressGyroY);
        mProgressGyroZ = (ProgressBar)findViewById(R.id.progressGyroZ);
        /* 地磁気 */
        mTextMagnetoX = (TextView)findViewById(R.id.textMagnetoX);
        mTextMagnetoY = (TextView)findViewById(R.id.textMagnetoY);
        mTextMagnetoZ = (TextView)findViewById(R.id.textMagnetoZ);
        mProgressMagnetoX = (ProgressBar)findViewById(R.id.progressMagnetoX);
        mProgressMagnetoY = (ProgressBar)findViewById(R.id.progressMagnetoY);
        mProgressMagnetoZ = (ProgressBar)findViewById(R.id.progressMagnetoZ);
        /* 気圧 */
        mTextAirpressure = (TextView)findViewById(R.id.textAirpressure);

        /* UI更新ハンドラ */
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mTextStatus.setText((String)msg.obj);
                AppState state = AppState.values()[msg.what];
                switch (state) {
                    case BLE_READY:
                        mBtnConn.setEnabled(false);
                        mBtnDiscon.setEnabled(true);
                        mBtnDisable.setEnabled(true);
                        mBtnEnable.setEnabled(true);
                        break;
                    case BLE_BUSY:
                        mBtnDisable.setEnabled(false);
                        mBtnEnable.setEnabled(false);
                        break;
                    case BLE_CLOSED:
                        mBtnConn.setEnabled(true);
                        mBtnDiscon.setEnabled(false);
                        mBtnDisable.setEnabled(false);
                        mBtnEnable.setEnabled(false);
                        break;
                    case BLE_DISCONNECTED:
                        mBtnConn.setEnabled(true);
                        mBtnDiscon.setEnabled(false);
                        mBtnDisable.setEnabled(false);
                        mBtnEnable.setEnabled(false);
                        break;
                    case BLE_DATA_UPDATE:
                        ValueType vt = ValueType.values()[msg.arg1];
                        switch (vt) {
                            case VT_ACCEL:  /* 加速度 */
                                mTextAccelX.setText(String.format("X:%5.1fG", mAccel[0]));
                                mProgressAccelX.setProgress((int)(mAccel[0] * 80) + 160);
                                mTextAccelY.setText(String.format("Y:%5.1fG", mAccel[1]));
                                mProgressAccelY.setProgress((int)(mAccel[1] * 80) + 160);
                                mTextAccelZ.setText(String.format("Z:%5.1fG", mAccel[2]));
                                mProgressAccelZ.setProgress((int)(mAccel[2] * 80) + 160);
                                break;
                            case VT_GYRO:   /* 角速度 */
                                mTextGyroX.setText(String.format("X:%7.1fdig/s", mGyro[0]));
                                mProgressGyroX.setProgress((int)(mGyro[0] * 3) + 2000);
                                mTextGyroY.setText(String.format("Y:%7.1fdig/s", mGyro[1]));
                                mProgressGyroY.setProgress((int)(mGyro[1] * 3) + 2000);
                                mTextGyroZ.setText(String.format("Z:%7.1fdig/s", mGyro[2]));
                                mProgressGyroZ.setProgress((int)(mGyro[2] * 3) + 2000);
                                break;
                            case VT_MAGM:   /* 地磁気 */
                                mTextMagnetoX.setText(String.format("X:%7.1fuH", mMagm[0]));
                                mProgressMagnetoX.setProgress((int)(mMagm[0] * 5) + 4000);
                                mTextMagnetoY.setText(String.format("Y:%7.1fuH", mMagm[1]));
                                mProgressMagnetoY.setProgress((int)(mMagm[1] * 5) + 4000);
                                mTextMagnetoZ.setText(String.format("Z:%7.1fuH", mMagm[2]));
                                mProgressMagnetoZ.setProgress((int) (mMagm[2] * 5) + 4000);
                                break;
                            case VT_AIRP:   /* 気圧 */
                                mTextAirpressure.setText(String.format("%7.1fPa", mAirp));
                                break;
                            case VT_ANGL:   /* 姿勢角 */
                                mTextAxisAngle.setText(mAxisAngle);
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        //メンバ変数初期化
        mGyro = new double[3];
        mAccel = new double[3];
        mMagm = new double[3];
    }

    /**
     * 全CharacteristicのNotificationを有効に設定
     */
    private void enableBleNotification()
    {
        for (int i = 0; i < UUIDS_CHARACTERISTIC.length; i++) {
            boolean reg = mGatt.setCharacteristicNotification(mCharacteristics[i], true);
            BluetoothGattDescriptor desc =
                    mCharacteristics[i].getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG));
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mGatt.writeDescriptor(desc);

            if (!reg) {
                setStatus(AppState.BLE_NOTIF_REGISTER_FAILED);
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setStatus(AppState.BLE_NOTIF_REGISTERD);
    }

    /**
     * 全CharacteristicのNotificationを無効に設定
     */
    private void disableBleNotification()
    {
        for (int i = 0; i < UUIDS_CHARACTERISTIC.length; i++) {
            BluetoothGattDescriptor desc =
                    mCharacteristics[i].getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG));
            desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mGatt.writeDescriptor(desc);
            if (!mGatt.setCharacteristicNotification(mCharacteristics[i], false)) {
                setStatus(AppState.BLE_NOTIF_REGISTER_FAILED);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setStatus(AppState.BLE_NOTIF_REGISTERD);
    }

    /**
     * BLEで接続する
     */
    private void connectBle()
    {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtAdapter.stopLeScan(mLeScanCallBack);
                if (AppState.BLE_SCANNING.equals(getStatus())) {
                    setStatus(AppState.BLE_SCAN_FAILED);
                }
            }
        }, SCAN_TIMEOUT);

        mBtAdapter.stopLeScan(mLeScanCallBack);
        mBtAdapter.startLeScan(mLeScanCallBack);
        setStatus(AppState.BLE_SCANNING);
    }

    /**
     * BLE接続断
     */
    private void disconnectBle()
    {
        if (mBtGatt != null) {
            mBtGatt.close();
            mBtGatt = null;
            for (int i = 0; i < UUIDS_CHARACTERISTIC.length; i++) {
                mCharacteristics[i] = null;
            }
            setStatus(AppState.BLE_CLOSED);
        }
    }

    /**
     * BLEスキャンCallback
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, String.format("Device found: %s[%s]", device.getName(), device.getUuids()));
            if (DEVICE_NAME.equals(device.getName())) {
                //BlueNinjaを発見
                setStatus(AppState.BLE_DEV_FOUND);
                mBtAdapter.stopLeScan(this);
                mBtGatt = device.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);
            }
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    setStatus(AppState.BLE_DISCONNECTED);
                    mBtGatt = null;
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(UUID.fromString(UUID_SERVICE_BNEXAM));
            if (service == null) {
                /* サービスが見つからない */
                setStatus(AppState.BLE_SRV_NOT_FOUND);
            } else {
                /* サービス発見 */
                setStatus(AppState.BLE_SRV_FOUND);
                for (int i = 0; i < UUIDS_CHARACTERISTIC.length; i++) {
                    mCharacteristics[i] =
                            service.getCharacteristic(UUID.fromString(UUIDS_CHARACTERISTIC[i]));

                    if (mCharacteristics[i] == null) {
                        /* Characteristicが見つからない */
                        setStatus(AppState.BLE_CHARACTERISTIC_NOT_FOUND);
                        return;
                    }
                }
                mGatt = gatt;
                setStatus(AppState.BLE_READY);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, String.format("onCharacteristicWrite: %d", status));
            setStatus(AppState.BLE_READY);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
            /* 加速度 */
            if (UUID_CHARACTERISTIC_ACCEL.equals(characteristic.getUuid().toString())) {
                byte[] read_data = characteristic.getValue();
                try {
                    JSONObject json = new JSONObject(new String(read_data));
                    mAccel[0] = json.getDouble("ax");
                    mAccel[1] = json.getDouble("ay");
                    mAccel[2] = json.getDouble("az");
                    setStatus(AppState.BLE_DATA_UPDATE, ValueType.VT_ACCEL);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            /* 角速度 */
            if (UUID_CHARACTERISTIC_GYRO.equals(characteristic.getUuid().toString())) {
                byte[] read_data = characteristic.getValue();
                try {
                    JSONObject json = new JSONObject(new String(read_data));
                    mGyro[0] = json.getDouble("gx");
                    mGyro[1] = json.getDouble("gy");
                    mGyro[2] = json.getDouble("gz");
                    setStatus(AppState.BLE_DATA_UPDATE, ValueType.VT_GYRO);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            /* 地磁気 */
            if (UUID_CHARACTERISTIC_MAGM.equals(characteristic.getUuid().toString())) {
                byte[] read_data = characteristic.getValue();
                try {
                    JSONObject json = new JSONObject(new String(read_data));
                    mMagm[0] = json.getDouble("mx");
                    mMagm[1] = json.getDouble("my");
                    mMagm[2] = json.getDouble("mz");
                    setStatus(AppState.BLE_DATA_UPDATE, ValueType.VT_MAGM);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            /* 気圧 */
            if (UUID_CHARACTERISTIC_AIRP.equals(characteristic.getUuid().toString())) {
                byte[] read_data = characteristic.getValue();
                try {
                    JSONObject json = new JSONObject(new String(read_data));
                    mAirp = json.getDouble("ap");
                    setStatus(AppState.BLE_DATA_UPDATE, ValueType.VT_AIRP);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


    };

    private enum ValueType {
        VT_ACCEL,
        VT_GYRO,
        VT_MAGM,
        VT_AIRP,
        VT_ANGL,
    }
    /**
     * アプリケーションの状態
     */
    private enum AppState {
        INIT,
        BLE_SCANNING,
        BLE_SCAN_FAILED,
        BLE_DEV_FOUND,
        BLE_SRV_NOT_FOUND,
        BLE_SRV_FOUND,
        BLE_CHARACTERISTIC_NOT_FOUND,
        BLE_NOTIF_REGISTERD,
        BLE_NOTIF_REGISTER_FAILED,
        BLE_DATA_UPDATE,
        BLE_READY,
        BLE_BUSY,
        BLE_DISCONNECTED,
        BLE_CLOSED
    }

    /**
     * Set application status
     * @param stat Application state.
     */
    private void setStatus(AppState stat) {
        Message msg = new Message();
        msg.what = stat.ordinal();
        msg.obj = stat.name();

        mAppStat = stat;
        mHandler.sendMessage(msg);
    }

    private void setStatus(AppState stat, ValueType vt)
    {
        Message msg = new Message();
        msg.what = stat.ordinal();
        msg.obj = stat.name();
        msg.arg1 = vt.ordinal();

        mAppStat = stat;
        mHandler.sendMessage(msg);
    }

    /**
     * Get application status
     * @return Application state.
     */
    private AppState getStatus()
    {
        return mAppStat;
    }
}