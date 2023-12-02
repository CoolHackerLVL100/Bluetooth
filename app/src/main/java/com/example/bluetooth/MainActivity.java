package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ListView devicesListView;
    private static BluetoothAdapter bluetoothAdapter;
    private static ArrayList<Pair<String, String>> devices = new ArrayList<>();
    private static ArrayAdapter<Pair<String, String>> devicesArrayAdapter;
    private ImageButton searchButton;

    public static void log(String st){
        System.out.println(st);
    }
    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return super.checkPermission(permission, pid, uid);

    }
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("6th");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    return;
                }

                log("1st");

                if (!devices.contains(new Pair<>(device.getName(), device.getAddress()))){
                    devices.add(new Pair<>(device.getName() != null ? device.getName(): "Unknown device", device.getAddress()));
                    devicesArrayAdapter = new ArrayAdapter<Pair<String, String>>(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, (List) devices){

                        @Override
                        public View getView(int position, View convertView, ViewGroup parent){
                            View row = super.getView(position, convertView, parent);

                            TextView text1 = (TextView) row.findViewById(android.R.id.text1);
                            text1.setTextSize(18);
                            text1.setTypeface(null, Typeface.BOLD);
                            TextView text2 = (TextView) row.findViewById(android.R.id.text2);
                            text2.setTextSize(14);

                            Pair<String, String> item = devices.get(position);
                            text1.setText(item.first);
                            text2.setText(item.second);
                            return row;
                        }
                    };
                    devicesListView.setAdapter(devicesArrayAdapter);
                }
            }
        }
    };

    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchButton = findViewById(R.id.search);
        devicesListView = findViewById(R.id.bluetoothList);

        searchButton.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.BLUETOOTH_SCAN}, 0);
                return;
            }
            log("5th");
            if (bluetoothAdapter.isDiscovering()){
                bluetoothAdapter.cancelDiscovery();
            }

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},2);
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},3);

            bluetoothAdapter.startDiscovery();

            IntentFilter discoverIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver, discoverIntentFilter);
        });

        devicesListView.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent dataActivityIntent = new Intent(MainActivity.this, DataActivity.class);
            dataActivityIntent.putExtra("mac", ((TwoLineListItem) view).getText2().getText());
            startActivity(dataActivityIntent);
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                log("3rd");
                Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(bluetoothIntent);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                log("1st");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                return;
            }
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {

                    devices.add(new Pair<>(device.getName() != null ? device.getName(): "Unknown device", device.getAddress()));
                }
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 0);
            }

            devicesArrayAdapter = new ArrayAdapter<Pair<String, String>>(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, (List) devices){

                @Override
                public View getView(int position, View convertView, ViewGroup parent){
                    View row = super.getView(position, convertView, parent);

                    TextView text1 = (TextView) row.findViewById(android.R.id.text1);
                    text1.setTextSize(18);
                    text1.setTypeface(null, Typeface.BOLD);
                    TextView text2 = (TextView) row.findViewById(android.R.id.text2);
                    text2.setTextSize(14);

                    Pair<String, String> item = devices.get(position);
                    text1.setText(item.first);
                    text2.setText(item.second);
                    return row;
                }
            };
            devicesListView.setAdapter(devicesArrayAdapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult();
//        switch (requestCode) {
//            case 1:
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // permission granted
//                    readContacts();
//                } else {
//                    // permission denied
//                }
//                return;
//        }
//    }
}