package com.example.leszek.andalcofon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {


    private LineChart mChart;
    private RelativeLayout mLayout;
    Button pstart, stop, pomiar, kalibracja, polacz;
    boolean isstop = false;
    protected boolean mActive;
    protected static final int TIMER_RUNTIME = 5000;
    protected ProgressBar mProgress;
    protected TextView wynikPomiaru, brakPomiaru, test;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;
    public int PomiarADC;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "20:16:01:06:92:27";


    public void createChart() {
        mLayout = (RelativeLayout) findViewById(R.id.wykres);

        //crate line chart
        mChart = new LineChart(this);
        //add to layout
        mLayout.addView(mChart);
        //customize line chart
        mChart.setDescription("");
        mChart.setNoDataTextDescription("brak danych do wyswietlenia");
        //enable values
        mChart.setHighlightEnabled(true);
        //enable touch gestures
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        //enable pinch zoom to avoid scaling x and y axis separately
        mChart.setPinchZoom(true);

        // alternative background color
        mChart.setBackgroundColor(Color.rgb(255, 140, 0));

        //now we work with data
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add data to line chart
        mChart.setData(data);

        //get legend object
        Legend l = mChart.getLegend();
        //Customize legend
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMaxValue(150);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);
    }

    // inicjalizowanie przycisków
    public void initializeElements() {
        mProgress = (ProgressBar) findViewById(R.id.postep);
        // przyciski związane z activity_main
        pstart = (Button) findViewById(R.id.pstart);
        stop = (Button) findViewById(R.id.stop);
        pomiar = (Button) findViewById(R.id.pomiar);
        kalibracja = (Button) findViewById(R.id.kalibracja);
        polacz = (Button) findViewById(R.id.polacz);
        wynikPomiaru = (TextView) findViewById(R.id.wynikPomiaru);
        brakPomiaru = (TextView) findViewById(R.id.brakPomiaru);

    }

    class OdczytCiagly extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {

            // mConnectedThread.start();

        }

        @Override
        protected Void doInBackground(Void... params) {
            while (isstop != true) {
                addEntry();
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {

                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

        }

    }

    // klasa do uzupełniania progressbaru
    class postep extends AsyncTask<Void, Void, Void> {
        int waited = 0;

        @Override
        protected void onPreExecute() {
            wynikPomiaru.setText("");
        }

        @Override
        protected Void doInBackground(Void... params) {
            mActive = true;
            try {
                while (mActive && (waited <= TIMER_RUNTIME)) {
                    Thread.sleep(250);
                    if (mActive) {
                        waited += 250;
                        updateProgress(waited);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            brakPomiaru.setText("koniec odliczania");
        }

    }

    // metoda do wypełniania progressbaru
    public void updateProgress(final int timePassed) {
        if (null != mProgress) {
            final int progress = mProgress.getMax() * timePassed / TIMER_RUNTIME;
            mProgress.setProgress(progress);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createChart();
        initializeElements();

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
        // Listener przycisku stop
        View.OnClickListener stopclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isstop = true;
            }
        };
        stop.setOnClickListener(stopclick);
        // Listener przycisku start
        View.OnClickListener pstartclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isstop = false;
                mConnectedThread = new ConnectedThread(btSocket);
                try {
                    mConnectedThread.write("C");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new OdczytCiagly().execute();
            }
        };
        pstart.setOnClickListener(pstartclick);
        // listener przycisku pomiar
        View.OnClickListener pomiarclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new postep().execute();
            }
        };
        pomiar.setOnClickListener(pomiarclick);
        // listener przycisku kalibracja
        View.OnClickListener kalibracjaclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new postep().execute();
                brakPomiaru.setText("Kalibracja czujnika");
            }
        };
        kalibracja.setOnClickListener(kalibracjaclick);
        View.OnClickListener polaczclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set up a pointer to the remote node using it's address.
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                try {
                    btSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                }

                try {
                    btSocket.connect();
                } catch (IOException e) {
                    try {
                        btSocket.close();
                    } catch (IOException e2) {
                    }
                }
            }
        };
        polacz.setOnClickListener(polaczclick);
    }


    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {

            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }


    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
        } else {
            if (btAdapter.isEnabled()) {

            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    // wysyłanie danych do wykresu
    // wysyłanie danych do wykresu
    private void addEntry() {
        LineData data = mChart.getData();
        if (data != null) {
            LineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            // add a new random value
            data.addXValue("");
            data.addEntry(new Entry((float) PomiarADC, set.getEntryCount()), 0);

            mChart.notifyDataSetChanged();
            mChart.setVisibleXRange(6);
            mChart.moveViewToX(data.getXValCount() - 7);
        }
    }

    //method to create set
    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Aktualna wartosc");
        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 177));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        return set;
    }


    public void OnPause() {
        try {
            btSocket.close();
        } catch (IOException e2) {
        }
    }


    public class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[4];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer, 0, 4);                 // Get number of bytes and message in "buffer";

            } catch (IOException e) {

            }
                PomiarADC = ((buffer[0] - '0' )* 1000)+((buffer[1] - '0' )* 100)+((buffer[2] - '0' )* 10)+(buffer[3] - '0' );

        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String wiadomosc) throws IOException {
            String msg = wiadomosc;
            mmOutStream.write(msg.getBytes()); // wysyłanie danych
        }
    }
}


