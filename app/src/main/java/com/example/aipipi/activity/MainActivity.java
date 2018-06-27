package com.example.aipipi.activity;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.blankj.utilcode.util.LogUtils;
import com.example.aipipi.R;
import com.example.aipipi.base.BaseCallBack;
import com.example.aipipi.base.BaseToolBarActivity;
import com.example.aipipi.ble.BleService;
import com.example.aipipi.ble.observer.BleConnectionObserver;
import com.example.aipipi.ble.observer.BleScanObserver;
import com.example.aipipi.custom.widget.DotMatrixView;
import com.example.aipipi.utils.StringUtil;
import com.example.aipipi.utils.font.FontUtils;
import com.freelink.library.viewHelper.RadioGroupHelper;
import com.freelink.library.widget.ImageTextView;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseToolBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DEFAULT_BLE_DEVICE_ADDR = "98:D3:31:80:1E:9D";

    @BindView(R.id.itv_ble)
    ImageTextView itvBle;

    @BindView(R.id.ll_font)
    LinearLayout llFont;

    @BindView(R.id.editText)
    EditText editText;

    @BindView(R.id.multiDotMatrixView)
    DotMatrixView dotMatrixView;

    private RadioGroupHelper radioGroupHelper;
    final static String[] sFontTyps = {"宋体", "黑体", "仿宋", "楷体"};

    private BleService bleService;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, bleServiceConnection, BIND_AUTO_CREATE);

        showToolBar(false);

        radioGroupHelper = new RadioGroupHelper(llFont, 0);

        radioGroupHelper.setOnCheckedChangeListener(new RadioGroupHelper.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(View v, int index) {
                LogUtils.e(index + "/" + radioGroupHelper.getCheckedRadioIndex());
            }
        });
    }

    ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.InnerBinder binder = (BleService.InnerBinder) service;
            bleService = binder.getService();
            bleService.registerBleScanObserver(true, bleScanObserver);
            bleService.registerBleConnectionObserver(true, bleConnectionObserver);
            if(bleService.isConnected()) {
                itvBle.setText(R.string.ble_connected);
                itvBle.setSelected(true);
            } else {
                itvBle.setText(R.string.ble_disconnect);
                bleService.startDiscovery();
                itvBle.setSelected(false);
            }
            LogUtils.e("bleServiceConnection");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    BleScanObserver bleScanObserver = new BleScanObserver() {
        @Override
        public void onScanStart() {
            showLoadingDialog("搜索设备中", new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if(!bleService.isConnected()
                            && !bleService.isConnecting()) {
                        showToast("未搜索到设备");
                    }
                    bleService.cancelDiscovery();
                }
            });
        }

        @Override
        public void onScanDevice(BluetoothDevice device) {
            if(device.getAddress().equalsIgnoreCase(DEFAULT_BLE_DEVICE_ADDR)) {
                bleService.connect(device);
            }
        }

        @Override
        public void onScanFinished() {
            if(!bleService.isConnected()
                    && !bleService.isConnecting()) {
                hideLoadingDialog();
                showToast("未搜索到设备");
            }
        }
    };

    BleConnectionObserver bleConnectionObserver = new BleConnectionObserver() {
        @Override
        public void onStartConnect() {
            showLoadingDialog("连接设备中");
        }

        @Override
        public void onConnectSuccess(BluetoothDevice device) {
            hideLoadingDialog();
            showToast("连接设备成功");
            itvBle.setText(R.string.ble_connected);
            itvBle.setSelected(true);
        }

        @Override
        public void onConnectFailed(BluetoothDevice device) {
            hideLoadingDialog();
            showToast("连接设备失败");
            itvBle.setText(R.string.ble_disconnect);
            itvBle.setSelected(false);
        }

        @Override
        public void onDisConnected(BluetoothDevice device) {
            hideLoadingDialog();
            showToast("设备断开连接");
            itvBle.setText(R.string.ble_disconnect);
            itvBle.setSelected(false);
        }
    };

    private void makeFontList(final BaseCallBack<List<byte[]>> callBack) {
        final String text = editText.getText().toString();
        if(StringUtil.isEmpty(text)) {
            showToast("请输入文本");
            return;
        }
        int index = radioGroupHelper.getCheckedRadioIndex();
        final String fontType = sFontTyps[index];
        showLoadingDialog("生成字库中");
        new AsyncTask<Void, Void, List<byte[]>>() {
            @Override
            protected List<byte[]> doInBackground(Void... params) {
                return  FontUtils.makeFont24(fontType, text);
            }

            @Override
            protected void onPostExecute(List<byte[]> bytes) {
                hideLoadingDialog();
                callBack.onCallBack(bytes);
            }
        }.execute();
    }

    @OnClick(R.id.tv_preview)
    void onPreview() {
        makeFontList(new BaseCallBack<List<byte[]>>() {
            @Override
            public void onCallBack(List<byte[]> fontList) {
                dotMatrixView.setMatrix(FontUtils.convertMatrix24(fontList));
                dotMatrixView.startScroll(100);
            }
        });
    }

    @OnClick(R.id.tv_send)
    void onSendFont() {
        if(bleService.isConnected()) {
            makeFontList(new BaseCallBack<List<byte[]>>() {
                @Override
                public void onCallBack(List<byte[]> obj) {

                }
            });
//            String text = editText.getText().toString();
//            if(StringUtil.isEmpty(text)) {
//                showToast("请输入文本");
//                return;
//            }
//            try {
//                bleService.send( text.getBytes("UTF-8"));
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
        } else {
            showToast("请先连接蓝牙设备");
        }
    }

    @OnClick(R.id.itv_ble)
    void onConnectBle() {
        if(!bleService.isConnected()
                && !bleService.isConnecting()) {
            bleService.startDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bleService != null) {
            bleService.registerBleScanObserver(false, bleScanObserver);
            bleService.registerBleConnectionObserver(false, bleConnectionObserver);
        }
    }


    private long exitTime = 0;
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            showToast("再按一次退出程序");
            exitTime = System.currentTimeMillis();
        } else {
            // 2s内连续点击back两次可退出程序
            super.onBackPressed();
        }
    }

}