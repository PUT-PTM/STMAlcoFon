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
import android.view.Menu;
import android.view.MenuItem;
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

import static java.lang.Thread.sleep;

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

    // tworzenie wykresu
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
        y1.setAxisMaxValue(4096);
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
        test=(TextView) findViewById((R.id.test));



    }

    // klasa do uzupełniania progressbaru
    class postep extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            wynikPomiaru.setText("");
        }

        @Override
        protected Void doInBackground(Void... params) {
            mActive = true;
            try {
                int waited = 0;
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
        }
    }

    // klasa do wypisywania teksu odiczania
    class oczekiwanie extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            brakPomiaru.setText("               Pomiar za 1 sekundę");
        }

        @Override
        protected Void doInBackground(Void... params) {
            mActive = true;
            try {
                int waited = 0;
                while (mActive && (waited <= 1000)) {
                    Thread.sleep(1000);
                    waited += 1000;
                }
                // Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            brakPomiaru.setText("Dmuchaj aż pasek postępu dojdzie do końca");
            wynikPomiaru.setText(" ‰");
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                            if (isstop == false) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mConnectedThread.start();
                                        addEntry();
                                    }
                                });
                                try {
                                    sleep(1000);
                                } catch (InterruptedException e) {

                                }
                            } else {
                                break;
                            }
                        }
                    }
                }).start();
            }
        };
        pstart.setOnClickListener(pstartclick);
        // listener przycisku pomiar
        View.OnClickListener pomiarclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new oczekiwanie().execute();
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
        // listener przycisku polacz
        View.OnClickListener polaczclick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set up a pointer to the remote node using it's address.
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                try {
                    btSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                }


                // Discovery is resource intensive.  Make sure it isn't going on
                // when you attempt to connect and pass your message.
                btAdapter.cancelDiscovery();


                try {
                    btSocket.connect();
                } catch (IOException e) {
                    try {
                        btSocket.close();
                    } catch (IOException e2) {
                    }
                }
                mConnectedThread = new ConnectedThread(btSocket);

            }
        };
        polacz.setOnClickListener(polaczclick);
    }

    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] {UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {

            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause()
    {
        super.onPause();

        try     {
            btSocket.close();
        } catch (IOException e2) {

        }
    }
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
        }
        else {
            if (btAdapter.isEnabled())
            {

            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }



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
            data.addEntry(new Entry((int) PomiarADC, set.getEntryCount()), 0);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[4];  // buffer store for the stream
        int b1 = 0,b2=0,b3=0,b4=0; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
            try {
                // Read from the InputStream
               b1 = mmInStream.read(buffer);                 // Get number of bytes and message in "buffer";
                test.setText(b1);
                b2=mmInStream.read(buffer);
                test.setText(b2);
                b3=mmInStream.read(buffer);
                test.setText(b3);
                b4=mmInStream.read(buffer);
                test.setText(b4);
            } catch (IOException e) {

            }
        PomiarADC=(((char)b1 -'0')*1000+((char)b2-'0')*100+((char)b3-'0')*10+(char)b4-'0');


    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String wiadomosc) {
        byte[] msgBuffer = wiadomosc.getBytes();
        try {
            mmOutStream.write(msgBuffer); // wysyłanie danych
        } catch (IOException e) {
        }
    }
}
}

