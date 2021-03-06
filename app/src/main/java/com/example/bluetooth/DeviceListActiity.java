package com.example.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class DeviceListActiity extends AppCompatActivity {

    public static final String TAG = "device";
    private BluetoothAdapter mBtAdapter;
    //已配对设备列表
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //未配对设备列表
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";  //Mac地址
    //定义广播接收者，用于处理扫描蓝牙设备后的结果
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    /*if (device.getBluetoothClass().getMajorDeviceClass()
                            == android.bluetooth.BluetoothClass.Device.Major.PHONE){
                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }*/
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        //在被调用活动里，设置返回结果码
        setResult(Activity.RESULT_CANCELED);
        init();  //活动界面
    }

    private void init() {
        Log.d(TAG,"跳转搜索蓝牙页面");
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(v -> {
            Toast.makeText(DeviceListActiity.this, R.string.scanning, Toast.LENGTH_LONG).show();
            doDiscovery();  //搜索蓝牙设备
        });
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_list);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_list);
//        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
//        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        //已配对蓝牙设备列表
        ListView pairedListView =findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mPaireDeviceClickListener);
        //未配对蓝牙设备列表
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mNewDeviceClickListener);
        //动态注册广播接收者
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        //开始获取已配对蓝牙设备列表
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                /*if (device.getBluetoothClass().getMajorDeviceClass()
                        == android.bluetooth.BluetoothClass.Device.Major.PHONE){
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }*/
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }
    private void doDiscovery() {
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();  //开始搜索蓝牙设备并产生广播
        //startDiscovery是一个异步方法
        //找到一个设备时就发送一个BluetoothDevice.ACTION_FOUND的广播
    }
    private AdapterView.OnItemClickListener mPaireDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);  //Mac地址
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
    private AdapterView.OnItemClickListener mNewDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            mBtAdapter.cancelDiscovery();
            Toast.makeText(DeviceListActiity.this, "请在蓝牙设置界面手动连接设备",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivityForResult(intent,1);
        }
    };
    //回调方法：进入蓝牙配对设置界面返回后执行
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        init();  //刷新好友列表
    }
}