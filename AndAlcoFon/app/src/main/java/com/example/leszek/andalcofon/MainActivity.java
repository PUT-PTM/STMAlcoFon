package com.example.leszek.andalcofon;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private LineChart mChart;
    private RelativeLayout mLayout;
    Button pstart,stop,pomiar,kalibracja,polacz;
    Spinner listaBT;
    boolean isstop=false;
    protected boolean mActive;
    protected static final int TIMER_RUNTIME=5000;
    protected ProgressBar mProgress;
    protected TextView wynikPomiaru, brakPomiaru;
    /*Handler h;
    final int RECIEVE_MESSAGE = 1;        // Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;*/
    public void createChart()
    {
        mLayout=(RelativeLayout) findViewById(R.id.wykres);
        //crate line chart
        mChart=new LineChart(this);
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
        mChart.setBackgroundColor(Color.rgb(255,140,0));

        //now we work with data
        LineData data= new LineData();
        data.setValueTextColor(Color.WHITE);

        // add data to line chart
        mChart.setData(data);

        //get legend object
        Legend l= mChart.getLegend();
        //Customize legend
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis x1 = mChart.getXAxis();
        x1.setTextColor(Color.WHITE);
        x1.setDrawGridLines(false);
        x1.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart.getAxisLeft();
        y1.setTextColor(Color.WHITE);
        y1.setAxisMaxValue(140f);
        y1.setDrawGridLines(true);

        YAxis y12 = mChart.getAxisRight();
        y12.setEnabled(false);
    }
    public void initializeElements()
    {
        mProgress =(ProgressBar) findViewById(R.id.postep);
        // przyciski związane z activity_main
        pstart=(Button) findViewById(R.id.pstart);
        stop=(Button) findViewById(R.id.stop);
        pomiar=(Button) findViewById(R.id.pomiar);
        kalibracja=(Button) findViewById(R.id.kalibracja);
        polacz=(Button) findViewById(R.id.polacz);
        wynikPomiaru=(TextView) findViewById(R.id.wynikPomiaru);
        brakPomiaru=(TextView) findViewById(R.id.brakPomiaru);


    }

        class postep extends AsyncTask<Void,Void,Void> {
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
    class oczekiwanie extends AsyncTask<Void,Void,Void> {
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
    public void updateProgress(final int timePassed)
    {
        if(null!=mProgress)
        {
            final int progress=mProgress.getMax()*timePassed/TIMER_RUNTIME;
            mProgress.setProgress(progress);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createChart();
        initializeElements();

        View.OnClickListener stopclick= new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isstop=true;
            }
        };
        stop.setOnClickListener(stopclick);
        View.OnClickListener pstartclick = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                isstop=false;
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        for(int i=0;i<100;i++)
                        {
                            if(isstop==false){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addEntry();
                                }
                            });
                            try
                            {
                                sleep(1000);
                            }
                            catch (InterruptedException e)
                            {

                            }
                        }
                            else{
                                break;
                            }
                        }
                    }
                }).start();
            }
        };
        pstart.setOnClickListener(pstartclick);

        View.OnClickListener pomiarclick=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new oczekiwanie().execute();
                new postep().execute();
            }
        };
        pomiar.setOnClickListener(pomiarclick);
        View.OnClickListener kalibracjaclick= new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new postep().execute();
                brakPomiaru.setText("Kalibracja czujnika");
            }
        };
        kalibracja.setOnClickListener(kalibracjaclick);
        View.OnClickListener polaczclick=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        };
        polacz.setOnClickListener(polaczclick);
    }

    private void addEntry()
    {
        LineData data = mChart.getData();
        if (data!=null)
        {
            LineDataSet set=data.getDataSetByIndex(0);
            if(set==null)
            {
                set= createSet();
                data.addDataSet(set);
            }
            // add a new random value
            data.addXValue("");
            data.addEntry(new Entry((float) (Math.random() * 75) + 60f, set.getEntryCount()), 0);

            mChart.notifyDataSetChanged();
            mChart.setVisibleXRange(6);
            mChart.moveViewToX(data.getXValCount()-7);
        }
    }

    //method to create set
    private LineDataSet createSet()
    {
        LineDataSet set = new LineDataSet(null,"Aktualna wartosc");
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
}
