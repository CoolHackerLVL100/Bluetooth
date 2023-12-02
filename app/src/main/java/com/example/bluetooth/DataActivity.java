package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;

import kotlin.Triple;

public class DataActivity extends AppCompatActivity {
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String mac;
    private BluetoothSocket bluetoothSocket;
    private String messages;
    private boolean isReading = false;
    InputStream inputStream;
    private Button connectButton;
    private Button readButton;
    private TextView timer;
    private TextView amplitude;
    private TextView avgFrequency;
    private TextView avgValue;
    private GraphView graphView;
    DataThread listenInput;
    private String[] values;
    double max;
    private List<DataPoint> dataPoints = new ArrayList<>();
    private boolean isConnected = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        graphView = findViewById(R.id.graph);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setMaxY(4.0);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMaxX(1000);

        graphView.getGridLabelRenderer().setHighlightZeroLines(false);
        graphView.getGridLabelRenderer().setVerticalAxisTitle("Acceleration, m/s^2");
        graphView.getGridLabelRenderer().setLabelVerticalWidth(48);
        graphView.getGridLabelRenderer().setLabelHorizontalHeight(48);
        graphView.getGridLabelRenderer().setHorizontalLabelsAngle(90);
        graphView.getGridLabelRenderer().setPadding(50);
        graphView.getGridLabelRenderer().setLabelsSpace(25);
        graphView.getGridLabelRenderer().setVerticalAxisTitleColor(Color.DKGRAY);
        graphView.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.DKGRAY);
        graphView.getGridLabelRenderer().setVerticalAxisTitleTextSize(32);

        readButton = findViewById(R.id.read);
        connectButton = findViewById(R.id.connect);
        timer = findViewById(R.id.receivedData);
        amplitude = findViewById(R.id.amplitude);
        avgFrequency = findViewById(R.id.avgFrequency);
        avgValue = findViewById(R.id.avgValue);
        readButton.setEnabled(false);
        mac = getIntent().getStringExtra("mac");

        connectButton.setOnClickListener(view -> {
            if (!isConnected){
                new connectBluetooth().execute();
            } else {
                try {
                    bluetoothSocket.close();
                }
                catch (IOException e){

                }
                Toast.makeText(DataActivity.this, "Disconnect...", Toast.LENGTH_SHORT).show();
                isConnected = false;
                readButton.setEnabled(false);
                connectButton.setText("Connect");
            }
        });

        readButton.setOnClickListener(view -> {
            readButton.setEnabled(false);
            isReading = true;
            messages = "";
            CountDownTimer countDownTimer = new CountDownTimer(20000, 20) {
                @SuppressLint("SetTextI18n")
                @Override
                public void onTick(long l) {
                    timer.setText(l / 1000 + "." + l % 1000);
                }

                @Override
                public void onFinish() {
                    timer.setText("");
                    isReading = false;
                    readButton.setEnabled(true);

                    if (dataPoints.size() > 0){
                        dataPoints.clear();
                        graphView.removeAllSeries();
                    }

                    values = messages.split(" ");

                    values[0] = "0.01";
                    values[values.length - 1] = "0.01";

                    graphView.getViewport().setMaxX(values.length);

                    int i = 1;
                    ArrayList<Pair<Integer, Double>> localMaxValues = new ArrayList<>();
                    max = 0;
                    double sum = 0;
                    Pair<Integer, Double> localMax1;
                    Pair<Integer, Double> localMax2 = new Pair<>(1, 0.01);;
                    double tangent = 1;
                    for (String value: values){
                        if (value == ""){
                            continue;
                        }
                        double val = Double.parseDouble(value);
                        if (val > 4.0){
                            continue;
                        }

                        if (val > max){
                            max = val;
                        }

                        if (i > 1){
                            localMax1 = localMax2;
                            localMax2 = new Pair<>(i, val);
                            if (tangent * (localMax2.second - localMax1.second) < 0 && tangent > 0){
                                localMaxValues.add(new Pair<>(i - 1, localMax1.second));
                            }
                            tangent = localMax2.second - localMax1.second;
                        }

                        sum += val;
                        dataPoints.add(new DataPoint(i, val));

                        i++;
                    }

                    DecimalFormat decimalFormat = new DecimalFormat("#0.00");
                    double average = sum / (i - 1);
                    double averageFrequency = 0;
                    for (int j = 0; j < localMaxValues.size() - 1; j++){
                        averageFrequency += 1.0 * (i - 1) / ((localMaxValues.get(j + 1).first - localMaxValues.get(j).first) * 20);
                    }
                    averageFrequency = averageFrequency / (localMaxValues.size() - 1);

                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints.toArray(new DataPoint[0]));
                    series.setColor(Color.RED);
                    graphView.addSeries(series);
                    amplitude.setText(Double.toString(max));
                    avgFrequency.setText(decimalFormat.format(averageFrequency));
                    avgValue.setText(decimalFormat.format(average));
                }
            };
            countDownTimer.start();
        });
    };

    class DataThread extends Thread {

        public void run(){

            try {
                if (bluetoothSocket == null) {
                    System.out.println("XXX");
                } else {
                    inputStream = bluetoothSocket.getInputStream();
                }

                byte[] buffer = new byte[32];
                int bytesRead;

                while (isConnected) {
                    bytesRead = inputStream.read(buffer);

                    if (bytesRead == -1) {
                        break;
                    }

                    if (isReading){
                        messages += new String(buffer, 0, bytesRead).replaceAll("[^\\d. ]", "");
                    }
                }
            } catch (IOException e) {
                isConnected = false;
            }


        }
    };

    private class connectBluetooth extends AsyncTask<Void, Void, Void> {
        private boolean connectSuccess = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(),"Connecting...",Toast.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if (bluetoothSocket == null || !isConnected) {
                    ActivityCompat.requestPermissions(DataActivity.this, new String[]{Manifest.permission.BLUETOOTH}, 1);
                    if (ActivityCompat.checkSelfPermission(DataActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(DataActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    }

                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(mac);
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                }
            } catch (IOException e) {
                connectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);

            if (connectSuccess)
            {
                Toast.makeText(DataActivity.this, "Connected successfully", Toast.LENGTH_SHORT).show();
                connectButton.setText("Disconnect");
                isConnected = true;
                readButton.setEnabled(true);
                listenInput = new DataThread();
                listenInput.start();
            } else {
                Toast.makeText(DataActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}