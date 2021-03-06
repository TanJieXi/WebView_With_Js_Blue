package com.example.bluesanfang;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by TanJieXi on 2018/4/2.
 */

public class BlueUtils {
    private static boolean isInit = false;
    private static BleManager mBleManager;
    private static List<BleDevice> mBleDevices;
    private static String[] mDecicesTyes;
    public volatile static BlueUtils sBlueUtils;
    private String service_uuid;
    private String[] c_uuid;   //可能需要对多个uuid进行通知监听
    private String write_command;  // 命令
    private String write_c_uuid;
    private ConnectBlueListener mConnectBlueListener;
    private String name = "";
    private boolean isRepet = false;
    private boolean isWrite = false;

    private BlueUtils() {

    }

    public static BlueUtils getInstance() {
        if (sBlueUtils == null) {
            synchronized (BlueUtils.class) {
                if (sBlueUtils == null) {
                    sBlueUtils = new BlueUtils();
                }
            }
        }

        if (!isInit) {
            init();
        }
        return sBlueUtils;
    }

    private static void init() {
        mBleManager = BleManager.getInstance();
        if (mBleManager.isSupportBle()) {
            mBleManager.enableBluetooth();
        }
        mBleDevices = new ArrayList<>();
        mDecicesTyes = new String[1];
    }

    protected void stopBlue() {
        mBleManager.cancelScan();
        mBleManager.disconnectAllDevice();
        // mBleManager.disableBluetooth();
        mBleManager.destroy();
        isInit = false;
    }

    protected void startScan(String name, String service_uuid, String[] c_uuid, ConnectBlueListener connectBlueListener) {
        startScan(name, service_uuid, c_uuid, false, null, null, connectBlueListener);
    }


    protected void startScan(final String name, String service_uuid, String[] c_uuid, final boolean isWrite, String write_c_uuid, String write_command, ConnectBlueListener connectBlueListener) {
        if (!isInit) {
            init();
        }
        this.isWrite = isWrite;
        this.name = name;
        this.c_uuid = c_uuid;
        this.service_uuid = service_uuid;
        if (isWrite) {
            this.write_command = write_command;
            this.write_c_uuid = write_c_uuid;
        }
        this.mConnectBlueListener = connectBlueListener;
        mBleManager.scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                if (!isRepet) {
                    mConnectBlueListener.onChangeText("扫描设备中，请稍后");
                } else {
                    mConnectBlueListener.onChangeText("请将设备靠近后进行重连");
                }
            }

            @Override
            public void onScanning(BleDevice result) {
                if (!StringUtil.isEmpty(result.getName())) {
                    Log.i("dsfdasgasdf", result.getName());
                    String scanRecord = Utils.convertHexToString(Utils.bytes2HexString(result.getScanRecord()));
                    if (scanRecord.contains("iChoice")) {
                        mDecicesTyes[0] = "iChoice";
                    } else if (scanRecord.contains("JKFR")) {
                        mDecicesTyes[0] = "JKFR";
                    } else if (scanRecord.contains("JK_FR")) {
                        mDecicesTyes[0] = "JK_FR";
                    }

                    mBleDevices.add(result);
                }
            }


            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                mConnectBlueListener.onChangeText("扫描成功，连接中，请稍后");
                connectDevice();
            }
        });
    }

    private void connectDevice() {
        if (mBleDevices.size() <= 0) {
            mConnectBlueListener.onChangeText("没有扫描到设备");
            Toast.makeText(App.getContext(), "没有扫描到设备", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i("dsfdasgasdf", mBleDevices.toString());
        for (BleDevice s : mBleDevices) {
            if (mDecicesTyes[0].equals(s.getName())) {
                this.name = mDecicesTyes[0];
                connect(s);
            }
        }
    }

    private void connect(final BleDevice result) {
        mBleManager.connect(result, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                mConnectBlueListener.onChangeText("温度计连接中");
                isRepet = false;
            }

            @Override
            public void onConnectFail(BleException exception) {
                mConnectBlueListener.onChangeText("连接失败");
                isRepet = false;
            }


            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onConnectSuccess(final BleDevice bleDevice, BluetoothGatt gatt, int status) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService s : services) {
                    Log.i("dsfdsfgd", service_uuid);
                    Log.i("dsfdsfgd", s.getUuid().toString() + "");
                    if ("iChoice".equals(name)) {
                        if (s.getUuid().toString().contains("ba11f08c-5f14-0b0d-1080")) {
                            service_uuid = s.getUuid().toString();
                            c_uuid = new String[]{
                                    "0000cd01-0000-1000-8000-00805f9b34fb",
                                    "0000cd02-0000-1000-8000-00805f9b34fb",
                                    "0000cd03-0000-1000-8000-00805f9b34fb",
                                    "0000cd04-0000-1000-8000-00805f9b34fb",};

                            isWrite = true;
                            write_c_uuid = "0000cd20-0000-1000-8000-00805f9b34fb";
                            write_command = "AA5504B10000B5";
                        }
                    } else if("JK_FR".equals(name)||"JKFR".equals(name)) {
                        if(s.getUuid().toString().contains("0000fff0-0000-1000-8000-00805f9b34fb")){
                            service_uuid = s.getUuid().toString();
                            c_uuid = new String[]{"0000fff2-0000-1000-8000-00805f9b34fb"};
                            isWrite = false;
                            write_c_uuid = "";
                            write_command = "";
                        }
                    }
                }

                mConnectBlueListener.onChangeText("连接设备成功");
                isRepet = false;
                for (int i = 0, len = c_uuid.length; i < len; i++) {
                    try {
                        Thread.sleep(400);
                        setNo(bleDevice, i);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                mConnectBlueListener.onChangeText("断开连接");
                Log.i("dsfdasfgdsf", isActiveDisConnected + "");
                if (!isActiveDisConnected) {
                    Observable.timer(5000, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) throws Exception {
                                    isRepet = true;
                                    startScan(name, service_uuid, c_uuid, isWrite, write_c_uuid, write_command, mConnectBlueListener);
                                }
                            });
                }
            }
        });
    }


    private void write(BleDevice bleDevice) {
        Log.i("dsfdasfgdsf", "-----write_c_uuid-------->" + write_c_uuid);
        Log.i("dsfdasfgdsf", "-----write_command-------->" + write_command);
        mBleManager.write(bleDevice, service_uuid, write_c_uuid, DealDataUtils.getInstance().getHexBytes(write_command), new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                Log.i("dsfdasfgdsf", "-----onWriteSuccess-------->");
            }

            @Override
            public void onWriteFailure(BleException exception) {
                Log.i("dsfdasfgdsf", "-----onWriteFailure-------->" + exception.toString());
            }
        });
    }


    private void setNo(final BleDevice bleDevice, final int i) {
        isRepet = false;
        Log.i("dsfdsgdfg", service_uuid);
        Log.i("dsfdsgdfg", c_uuid[i]);
        mBleManager.notify(bleDevice,
                service_uuid,
                c_uuid[i],
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        if (isWrite && !StringUtil.isEmpty(write_command) && i == (c_uuid.length - 1)) {
                            write(bleDevice);
                        }
                        Log.i("dsfdasfgdsf", "-----onNotifySuccess-------->" + i);

                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        Log.i("dsfdasfgdsf", "-----onNotifyFailure-------->" + exception.toString());
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        switch (name) {
                            case "JKFR":
                            case "JK_FR":
                                DealDataUtils.getInstance().dealTemData(
                                        DealDataUtils.getInstance().bytes2HexString(data)
                                        , new DealDataListener() {
                                            @Override
                                            public void onFetch(int code, String message) {
                                                mConnectBlueListener.onChangeText(message);
                                            }
                                        });
                                break;
                            case "iChoice":
                                Log.i("dsfdasfgdsf", "--->message-" + DealDataUtils.getInstance().bytes2HexString(data));
                                DealDataUtils.getInstance().dealOxiData(
                                        DealDataUtils.getInstance().bytes2HexString(data)
                                        , new DealDataListener() {
                                            @Override
                                            public void onFetch(int code, String message) {
                                                mConnectBlueListener.onChangeText(message);
                                            }
                                        });
                                break;
                        }
                    }
                });

    }

}
