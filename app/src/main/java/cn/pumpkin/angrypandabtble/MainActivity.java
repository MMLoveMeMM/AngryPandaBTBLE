package cn.pumpkin.angrypandabtble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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
    }

    /**
     * 开始扫描结果
     */
    private BluetoothLeScanner mScanner;

    private void startScanDevice() {

        if (mBluetoothAdapter != null) {
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanner.startScan(mScanCallBack);
        }

    }

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
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "scan failed code : " + errorCode);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void connectDevice(Context context, int index) {

        if (mExistedDevices != null && mExistedDevices.size() > 0) {
            ScanResult result = mExistedDevices.get(index);
            BluetoothDevice mDevice = result.getDevice();

            mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallback, TRANSPORT_LE);

        }

    }

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
                    .getService(notifyServiceUuid).getCharacteristic(notifyCharaUuid),true);
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
     * @param indatas
     */
    public void send(byte[] indatas){

        BluetoothGattService service=mBluetoothGatt.getService(writeServiceUuid);
        BluetoothGattCharacteristic charaWrite=service.getCharacteristic(writeCharaUuid);
        charaWrite.setValue(indatas);
        mBluetoothGatt.writeCharacteristic(charaWrite);

    }

    /**
     * 读取/获取数据信息
     * @param outdatas
     */
    public void read(byte[] outdatas){
        BluetoothGattCharacteristic characteristic=mBluetoothGatt.getService(readServiceUuid)
                .getCharacteristic(readCharaUuid);
        mBluetoothGatt.readCharacteristic(characteristic);
        byte[] results = characteristic.getValue();
        System.arraycopy(results,results.length,outdatas,0,results.length);
    }

    private void readData() {

    }

}
