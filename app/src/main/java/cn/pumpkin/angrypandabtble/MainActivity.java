package cn.pumpkin.angrypandabtble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;

    private List<ScanResult> mExistedDevices;

    /**
     * GAP协议 广播
     */
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    //服务和特征值
    private UUID writeServiceUuid;
    private UUID writeCharaUuid;
    private UUID readServiceUuid;
    private UUID readCharaUuid;
    private UUID notifyServiceUuid;
    private UUID notifyCharaUuid;
    private UUID indicateServiceUuid;
    private UUID indicateCharaUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatas();

    }

    public void initDatas() {

        mExistedDevices = new ArrayList<>();
        checkPermissions();
        checkSupportBLE();

    }

    /**
     * 检查当前设备是否支持BLE4.0协议
     */
    public void checkSupportBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(MainActivity.this, "Can not support BLE4.0 device !", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            // 用户已经同意该权限
                            startScanDevice();
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』

                        }
                    }
                });
    }

    public void enableBluetoothBle() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, "the device not support peripheral", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    /**
     * *****************************************************************************************************
     * GAP 广播的一些基本设置,在没有设备连接的状态的时候,通过可以向周围的设备广播数据,这个和UDP的多播类似
     * 这个地方要注意,广播是被扫描者进行发送的,不是扫描者发送的,比如:A 打开进行扫描附近B设备,B设备在没有
     * 连接的时候可以不断通过广播发送数据,那么A端就可以不断收到B端过来的数据.
     **/
    public AdvertiseSettings createAdvSettings(boolean connectAble, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(connectAble);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings mAdvertiseSettings = builder.build();
        if (mAdvertiseSettings == null) {
            Toast.makeText(this, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseSettings;
    }

    private void stopAdvertise() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    // 广播数据,广播的数据在中心端ScanRecord中
    public AdvertiseData createAdvertiseData() {
        AdvertiseData.Builder mDataBuilder = new AdvertiseData.Builder();
        mDataBuilder.setIncludeDeviceName(true); //广播名称也需要字节长度
        mDataBuilder.setIncludeTxPowerLevel(true);
        mDataBuilder.addServiceData(ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb"),new byte[]{1,2});
        AdvertiseData mAdvertiseData = mDataBuilder.build();
        if (mAdvertiseData == null) {
            Toast.makeText(MainActivity.this, "mAdvertiseSettings == null", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseData;
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (settingsInEffect != null) {
                Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode()
                        + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.e(TAG, "onStartSuccess, settingInEffect is null");
            }
            Log.e(TAG, "onStartSuccess settingsInEffect" + settingsInEffect);

        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "onStartFailure errorCode" + errorCode);

            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_data_too_large", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_too_many_advertises", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising because no advertising instance is available.");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_already_started", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to start advertising as the advertising is already started");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_internal_error", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Operation failed due to an internal error");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Toast.makeText(MainActivity.this, "R.string.advertise_failed_feature_unsupported", Toast.LENGTH_LONG).show();
                Log.e(TAG, "This feature is not supported on this platform");
            }
        }
    };

    /**
     * ******************************************************************************************************
     */
    /**
     * 开始扫描结果
     * 间断性扫描,扫描一次维持10s
     */
    private static final long SCAN_TIME_LIMIT = 10000;
    private BluetoothLeScanner mScanner;

    private void startScanDevice() {

        if (mBluetoothAdapter != null) {
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanner.startScan(mScanCallBack);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //结束扫描
                    stopScanDevice();
                }
            }, SCAN_TIME_LIMIT);
        }

    }

    /**
     * 停止扫描设备
     */
    private void stopScanDevice() {
        if (mScanner != null) {
            mScanner.stopScan(mScanCallBack);
        }
    }

    /**
     * 扫描反馈结果
     */
    private ScanCallBack mScanCallBack = new ScanCallBack();

    private class ScanCallBack extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (mExistedDevices != null) {
                mExistedDevices.add(result);
                /**
                 * GAP广播数据
                 */
                ScanRecord record = result.getScanRecord();
                record.getTxPowerLevel();
                // addServiceData
                byte[] datas = record.getServiceData(ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb"));
                // 上面datas数据应该是上面广播处理的{1,2}
                // ...
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "scan failed code : " + errorCode);
        }

    }

    /**
     * 连接设备
     *
     * @param context 上下报文
     * @param index   选择第index个设备进行连接
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void connectDevice(Context context, int index) {

        if (mExistedDevices != null && mExistedDevices.size() > index) {
            ScanResult result = mExistedDevices.get(index);
            BluetoothDevice mDevice = result.getDevice();
            mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallback, TRANSPORT_LE);
        }

    }

    /**
     * 连接设备过程中的各种状态回调
     */
    private GattCallback mGattCallback = new GattCallback();

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            /**
             * 获取连接过程中,对方设备的相关信息
             */
            getServiceAndCharaUUID();

            /**
             * 订阅消息通知
             */
            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt
                    .getService(notifyServiceUuid).getCharacteristic(notifyCharaUuid), true);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    /**
     * 连接发现的设备时,可以获取对方设备的特征值和服务的UUID地址信息
     */
    private void getServiceAndCharaUUID() {
        List<BluetoothGattService> bluetoothGattServices = mBluetoothGatt.getServices();
        for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
            List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                int charaProp = characteristic.getProperties();
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    readCharaUuid = characteristic.getUuid();
                    readServiceUuid = bluetoothGattService.getUuid();
                    Log.e(TAG, "read_chara=" + readCharaUuid + "----read_service=" + readServiceUuid);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    writeCharaUuid = characteristic.getUuid();
                    writeServiceUuid = bluetoothGattService.getUuid();
                    Log.e(TAG, "write_chara=" + writeCharaUuid + "----write_service=" + writeServiceUuid);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    writeCharaUuid = characteristic.getUuid();
                    writeServiceUuid = bluetoothGattService.getUuid();
                    Log.e(TAG, "write_chara=" + writeCharaUuid + "----write_service=" + writeServiceUuid);

                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    notifyCharaUuid = characteristic.getUuid();
                    notifyServiceUuid = bluetoothGattService.getUuid();
                    Log.e(TAG, "notify_chara=" + notifyCharaUuid + "----notify_service=" + notifyServiceUuid);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    indicateCharaUuid = characteristic.getUuid();
                    indicateServiceUuid = bluetoothGattService.getUuid();
                    Log.e(TAG, "indicate_chara=" + indicateCharaUuid + "----indicate_service=" + indicateServiceUuid);

                }
            }
        }
    }

    /**
     * 连接成功以后,发送数据信息给对方
     *
     * @param indatas 发送的数据
     */
    public void send(byte[] indatas) {

        BluetoothGattService service = mBluetoothGatt.getService(writeServiceUuid);
        BluetoothGattCharacteristic charaWrite = service.getCharacteristic(writeCharaUuid);
        charaWrite.setValue(indatas);
        mBluetoothGatt.writeCharacteristic(charaWrite);

    }

    /**
     * 读取/获取数据信息
     *
     * @param outdatas 读取/接受到的数据
     */
    public void read(byte[] outdatas) {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(readServiceUuid)
                .getCharacteristic(readCharaUuid);
        mBluetoothGatt.readCharacteristic(characteristic);
        byte[] results = characteristic.getValue();
        System.arraycopy(results, results.length, outdatas, 0, results.length);
    }



}
