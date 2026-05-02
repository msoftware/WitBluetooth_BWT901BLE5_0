package com.wit.example;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import com.wit.example.UI.CustomAdapter;
import com.wit.example.UI.ListItem;
import com.wit.witsdk.Bluetooth.WitBluetoothManager;
import com.wit.witsdk.Device.DeviceManager;
import com.wit.witsdk.Device.DeviceModel;
import com.wit.witsdk.Device.Interface.DeviceDataListener;
import com.wit.witsdk.Device.Interface.DeviceFindListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 示例主界面，展示搜索列表和传感器数据
 * Example main interface, displaying search list and sensor data
 * */
public class MainActivity extends AppCompatActivity implements DeviceDataListener, DeviceFindListener {

    private static final String TAG = "WitLOG";

    private final List<ListItem> findList = new ArrayList<>();

    private Timer timer;

    private CustomAdapter customAdapter;

    private LineChartView lineChartViewAcc;
    private LineChartView lineChartViewGyro;

    private LineChartData lineChartDataAcc;
    private LineChartData lineChartDataGyro;

    private final List<PointValue> pointValuesAcc1 = new ArrayList<>();
    private final List<PointValue> pointValuesAcc2 = new ArrayList<>();
    private final List<PointValue> pointValuesAcc3 = new ArrayList<>();

    private final List<PointValue> pointValuesGyro1 = new ArrayList<>();
    private final List<PointValue> pointValuesGyro2 = new ArrayList<>();
    private final List<PointValue> pointValuesGyro3 = new ArrayList<>();

    private int pointIndexAcc1 = 0;
    private int pointIndexAcc2 = 0;
    private int pointIndexAcc3 = 0;

    private int pointIndexGyro1 = 0;
    private int pointIndexGyro2 = 0;
    private int pointIndexGyro3 = 0;

    // 设备管理器 Device Manager
    private final DeviceManager deviceManager = DeviceManager.getInstance();
    // endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WitBluetoothManager.requestPermissions(this);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch swi = findViewById(R.id.switch1);
        swi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                StartScan();
            } else {
                StopScan();
            }
        });

        ListView listView = findViewById(R.id.scanlist);
        customAdapter = new CustomAdapter(this, findList);
        listView.setAdapter(customAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ListItem clickedItem = (ListItem) parent.getItemAtPosition(position);
            ShowDevice(clickedItem.getTitle());
        });

        deviceManager.AddDeviceListener(this);
        deviceManager.AddDeviceFindListener(this);
        initChart();
    }

    private void initChart() {

        lineChartViewAcc = findViewById(R.id.line_chart_view_a);
        lineChartViewGyro= findViewById(R.id.line_chart_view_g);

        lineChartDataAcc = new LineChartData();
        Line lineAcc1 = new Line(pointValuesAcc1).setColor(0xFF4081FF).setCubic(false);
        Line lineAcc2 = new Line(pointValuesAcc2).setColor(0x40FF81FF).setCubic(false);
        Line lineAcc3 = new Line(pointValuesAcc3).setColor(0xFF8140FF).setCubic(false);

        lineChartDataGyro = new LineChartData();

        Line lineGyro1 = new Line(pointValuesGyro1).setColor(0xFF4081FF).setCubic(false);
        Line lineGyro2 = new Line(pointValuesGyro2).setColor(0x40FF81FF).setCubic(false);
        Line lineGyro3 = new Line(pointValuesGyro3).setColor(0xFF8140FF).setCubic(false);

        List<Line> linesGyro = new ArrayList<>();
        linesGyro.add(lineGyro1);
        linesGyro.add(lineGyro2);
        linesGyro.add(lineGyro3);
        lineChartDataGyro.setLines(linesGyro);
        lineChartViewGyro.setLineChartData(lineChartDataGyro);

        List<Line> linesAcc = new ArrayList<>();
        linesAcc.add(lineAcc1);
        linesAcc.add(lineAcc2);
        linesAcc.add(lineAcc3);
        lineChartDataAcc.setLines(linesAcc);
        lineChartViewAcc.setLineChartData(lineChartDataAcc);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(timer!=null){
            timer.cancel();
            timer = null;
        }
        startUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer!=null){
            timer.cancel();
            timer = null;
        }

        deviceManager.RemoveDeviceListener(this);
        deviceManager.RemoveDeviceFindListener(this);
    }

    private void startUpdate(){
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (ListItem lis : findList){
                    DeviceModel deviceModel = DeviceManager.getInstance().GetDevice(lis.getTitle());
                    lis.setData(deviceModel.GetDataDisplayLine());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        customAdapter.notifyDataSetChanged();
                    }
                });
            }
        }, 1000, 100);
    }

    private void StartScan(){
        findList.clear();
        ListView listView = findViewById(R.id.scanlist);
        CustomAdapter adapter = (CustomAdapter) listView.getAdapter();
        adapter.notifyDataSetChanged();
        DeviceManager.getInstance().CleanAllDevice();

        try {
            WitBluetoothManager witBluetoothManager = WitBluetoothManager.getInstance(this);
            witBluetoothManager.startScan();
        } catch (Exception e) {
            Log.e(TAG, "Start searching for anomalies：" + e.getMessage());
        }
    }

    private void StopScan(){
        try {
            WitBluetoothManager witBluetoothManager = WitBluetoothManager.getInstance(this);
            witBluetoothManager.stopScan();
        } catch (Exception e) {
            Log.e(TAG, "Search Error："+ e.getMessage());
        }
    }

    private void ShowDevice(String deviceName){
        // 跳转数据页面
        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra("DeviceName", deviceName);
        startActivity(intent);
    }

    /**
     * 找到设备时回调此方法
     * Call back this method when the device is found
     * */
    @SuppressLint("MissingPermission")
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        String deviceName = device.getName();
        if(deviceName != null && deviceName.startsWith("WT")){
            String name = deviceName + "(" + device.getAddress() +")";
            for(ListItem item : findList){
                if(Objects.equals(item.getTitle(), name)){
                    return;
                }
            }
            DeviceModel deviceModel = new DeviceModel(name, device);
            deviceManager.AddDevice(name, deviceModel);
            findList.add(new ListItem(name, "data"));
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ListView listView = findViewById(R.id.scanlist);
                    CustomAdapter adapter = (CustomAdapter) listView.getAdapter();
                    adapter.notifyDataSetChanged();
                }
            });

            try {
                deviceModel.Connect(MainActivity.this);
            } catch (Exception e) {
                Log.e(TAG, "Connect Error：" + e.getMessage());
            }
        }
    }


    @Override
    public void OnReceive(String deviceName, String displayData) {
        // Get the device model from the device manager and extract specific data (e.g. AngX) for plotting
        DeviceModel deviceModel = deviceManager.GetDevice(deviceName);
        if (deviceModel != null) {
            float valueAcc1 = deviceModel.GetData("AccX").floatValue();
            float valueAcc2 = deviceModel.GetData("AccY").floatValue();
            float valueAcc3 = deviceModel.GetData("AccZ").floatValue();
            runOnUiThread(() -> updateChartAcc(valueAcc1, valueAcc2, valueAcc3));
            float valueGyro1 = deviceModel.GetData("AsX").floatValue();
            float valueGyro2 = deviceModel.GetData("AsY").floatValue();
            float valueGyro3 = deviceModel.GetData("AsZ").floatValue();
            runOnUiThread(() -> updateChartGyro(valueGyro1, valueGyro2, valueGyro3));
        }
    }

    private void updateChartAcc(float value1, float value2, float value3) {
        pointValuesAcc1.add(new PointValue(pointIndexAcc1++, value1));
        if (pointValuesAcc1.size() > 50) {
            pointValuesAcc1.remove(0);
        }

        pointValuesAcc2.add(new PointValue(pointIndexAcc2++, value2));
        if (pointValuesAcc2.size() > 50) {
            pointValuesAcc2.remove(0);
        }

        pointValuesAcc3.add(new PointValue(pointIndexAcc3++, value3));
        if (pointValuesAcc3.size() > 50) {
            pointValuesAcc3.remove(0);
        }
        lineChartViewAcc.setLineChartData(lineChartDataAcc);
    }

    private void updateChartGyro(float value1, float value2, float value3) {
        pointValuesGyro1.add(new PointValue(pointIndexGyro1++, value1));
        if (pointValuesGyro1.size() > 50) {
            pointValuesGyro1.remove(0);
        }

        pointValuesGyro2.add(new PointValue(pointIndexGyro2++, value2));
        if (pointValuesGyro2.size() > 50) {
            pointValuesGyro2.remove(0);
        }

        pointValuesGyro3.add(new PointValue(pointIndexGyro3++, value3));
        if (pointValuesGyro3.size() > 50) {
            pointValuesGyro3.remove(0);
        }
        lineChartViewGyro.setLineChartData(lineChartDataGyro);
    }

    /**
     * 设备状态改变时
     * When the device status changes
     * */
    @Override
    public void OnStatusChange(String deviceName, boolean status) {
        if(status){
            Toast.makeText(MainActivity.this, deviceName + "  Connected", Toast.LENGTH_SHORT);
        }
        else {
            Toast.makeText(MainActivity.this, deviceName + "  Disconnect", Toast.LENGTH_SHORT);
        }
    }
}