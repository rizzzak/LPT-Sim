package com.example.lptsim;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.jjoe64.graphview.*;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.graphics.Color;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    GraphView graph = null;
    int pauseInterval = 1;  //длительность такта
    int busBitrate = 8; // разрядность шины
    int dataPacketsMax = 2;
    int timeoutCntrMax = 3;
    int timeoutCntr = 0;
    EditText MLText = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        graph = (GraphView) findViewById(R.id.view);
        MLText = (EditText) findViewById(R.id.editTextTextMultiLine);
        graph.setTitle("LPT Временная диаграмма");
    }
    private String invertString(String str)
    {
        String res = "";
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0') res += '1';
            else if(str.charAt(i) == '1') res += '0';
            else res += str.charAt(i);
        }
        return res;
    }
    private String negString(String str)
    {
        String res = "";
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0') res += '0';
            else if(str.charAt(i) == '1') res += "-1";
            else res += str.charAt(i);
        }
        return res;
    }
    private ArrayList<Integer> stringToSequence(String str)
    {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0'){ arr.add(0); }
            else if (str.charAt(i) == '-'){ arr.add(-1); i++; }
            else arr.add(1);
        }
        return arr;
    }
    private LineGraphSeries<DataPoint> sequenceToSeries(ArrayList<Integer> data)
    {
        //func(seq -> series)
        // input: ArrayList<Integer> data
        // output: LineGraphSeries<DataPoint> series1
        ArrayList<DataPoint> graphPoints = new ArrayList<DataPoint>();
        double yVar = 0.0;
        double xVar = 0.0;
        for(int i = 0; i < data.size(); i++)
        {
            yVar = data.get(i);
            graphPoints.add(new DataPoint(xVar,yVar));
            xVar = xVar + 1;
        }
        //ArrayList -> DataPoint[]
        DataPoint[] xx = new DataPoint[graphPoints.size()];
        for(int i = 0; i < graphPoints.size(); i++)
        {
            xx[i] = graphPoints.get(i);
        }
        LineGraphSeries<DataPoint> series1 = new LineGraphSeries<DataPoint>(xx);
        series1.setColor(Color.BLACK);
        return series1;
    }
    private ArrayList<Integer> moveSeqUpDown(ArrayList<Integer> src, int delta)
    {
        for(int i = 0; i < src.size(); i++)
            src.set(i, src.get(i)+delta);
        return src;
    }
    public void Centronics(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        timeoutCntr = 0;
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        int cntr = ((int) (Math.random()*dataPacketsMax))*busBitrate+busBitrate;
        String readData = "";
        for(int i = 0; i < cntr; i++)
        {
            if(Math.random()<0.5)
                readData += '0';
            else
                readData += '1';
        }
        etData.setText(readData);
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtr = 0;
        int dataLength = readData.length();
        boolean skipIt = false;
        String DataBus = "0";
        String Strobe = "0";
        String Busy = "0";
        String Ack = "0";

        Busy = waitSignal(Busy);
        DataBus = waitSignal(DataBus);
        Strobe = waitSignal(Strobe);
        Ack = waitSignal(Ack);
        MLText.append("Выбран традиционный режим работы LPT-порта:");
        while(dataPtr < dataLength)
        {
            //1. set DB
            DataBus = setSignal(DataBus);
            Busy = waitSignal(Busy);
            Ack = waitSignal(Ack);
            Strobe = waitSignal(Strobe);
            if(!skipIt) MLText.append("\n1. Установлен пакет данных на шину.");
            //2. check busy
            if(Busy.charAt(Busy.length()-1) != '0')
            {
                timeoutCntr++;
                if(timeoutCntr >= timeoutCntrMax)
                {
                    etData.setText("TIMEOUT");
                    return;
                }
                MLText.append("\n2. ПУ занято. Пропуск такта.");
                skipIt = true;
                continue;
            }
            skipIt = false;

            //3. set Data Strobe
            DataBus = waitSignal(DataBus);
            Busy = waitSignal(Busy);
            Ack = waitSignal(Ack);
            Strobe = setSignal(Strobe);
            MLText.append("\n2. Установка строба данных.");
            //4. read Data Strobe
            DataBus = waitSignal(DataBus);
            Busy = setSignal(Busy);
            Ack = waitSignal(Ack);
            Strobe = waitSignal(Strobe);
            MLText.append("\n3. Считывание строба данных.");
            //5. drop Data Strobe
            DataBus = waitSignal(DataBus);
            Busy = waitSignal(Busy);
            Ack = waitSignal(Ack);
            Strobe = dropSignal(Strobe);
            MLText.append("\n4. Снятие строба данных.");
            //6. set Ack
            DataBus = waitSignal(DataBus);
            Busy = dropSignal(Busy);
            Ack = setSignal(Ack);
            Strobe = waitSignal(Strobe);
            MLText.append("\n5. Установка сигнала Ack.");
            //7. drop DB
            DataBus = dropSignal(DataBus);
            Busy = waitSignal(Busy);
            Ack = waitSignal(Ack);
            Strobe = waitSignal(Strobe);
            MLText.append("\n6. Снятие пакета данных с шины,т.к. ПУ подтвердило получение.");
            //8. drop Ack
            DataBus = waitSignal(DataBus);
            Busy = waitSignal(Busy);
            Ack = dropSignal(Ack);
            Strobe = waitSignal(Strobe);
            MLText.append("\n7. Снятие сигнала Ack.");
            String substr = "";
            for(int i = 0; i < busBitrate; i++)
            {
                if(i+dataPtr >= readData.length())
                    break;
                substr += readData.charAt(i+dataPtr);
            }
            MLText.append("\nПередано: "+substr+"\n");
            dataPtr += busBitrate;
        }
        String invertedStrobe = invertString(Strobe);
        String invertedAck = invertString(Ack);
        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedDataBus = negString(DataBus);
        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(DataBus)),"DB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedDataBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedStrobe),2)),"Strobe#",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedAck),4)),"ACK#",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(Busy),6)),"Busy#",Color.CYAN, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(7);
        graph.getLegendRenderer().setVisible(true);
    }
    public void NibbleMode(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        int cntr = ((int) (Math.random()*dataPacketsMax))*busBitrate+busBitrate;
        String readData = "";
        for(int i = 0; i < cntr; i++)
        {
            if(Math.random()<0.5)
                readData += '0';
            else
                readData += '1';
        }
        etData.setText(readData);
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtr = 0;
        int dataLength = readData.length();
        String HostBusy = "1";
        String StateSignalBus = "0";
        String PtrClk = "1";
        MLText.append("Выбран полубайтный режим работы LPT-порта:");
        while(dataPtr < dataLength)
        {
            //1. drop HostBusy
            HostBusy = dropSignal(HostBusy);
            StateSignalBus = waitSignal(StateSignalBus);
            PtrClk = waitSignal(PtrClk);
            MLText.append("\n1. Сигнал готовности приема данных хостом: HostBusy = 0.");
            //2. set StateSign.
            HostBusy = waitSignal(HostBusy);
            StateSignalBus = setSignal(StateSignalBus);
            PtrClk = waitSignal(PtrClk);
            MLText.append("\n2. ПУ помещает данные на шину сигналов состояния.");
            //3. drop PtrClk
            HostBusy = waitSignal(HostBusy);
            StateSignalBus = waitSignal(StateSignalBus);
            PtrClk = dropSignal(PtrClk);
            MLText.append("\n3. ПУ сигнализирует что тетрада готова: PtrClk=0.");
            //4. set HostBusy
            HostBusy = setSignal(HostBusy);
            StateSignalBus = waitSignal(StateSignalBus);
            PtrClk = waitSignal(PtrClk);
            MLText.append("\n4. Хост обрабатывает тетраду: HostBusy=1.");
            //5. set PtrClk
            HostBusy = waitSignal(HostBusy);
            StateSignalBus = waitSignal(StateSignalBus);
            PtrClk = setSignal(PtrClk);
            MLText.append("\n5. ПУ отвечает установкой PtrClk=1.");
            //6. drop StateSign.
            HostBusy = waitSignal(HostBusy);
            StateSignalBus = dropSignal(StateSignalBus);
            PtrClk = waitSignal(PtrClk);
            MLText.append("\n6. ПУ снимает данные с шины состояний.");
            String substr = "";
            for(int i = 0; i < busBitrate/2; i++)
            {
                if(i+dataPtr >= readData.length())
                    break;
                substr += readData.charAt(i+dataPtr);
            }
            MLText.append("\nПередано: "+substr+"\n");
            dataPtr += busBitrate/2;
        }

        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedStateSignalBus = negString(StateSignalBus);

        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(StateSignalBus)),"StateSB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedStateSignalBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(PtrClk),2)),"PtrClk",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(HostBusy),4)),"HostBusy",Color.RED, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(5);
        graph.getLegendRenderer().setVisible(true);
    }
    public void ByteMode(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        int cntr = ((int) (Math.random()*dataPacketsMax))*busBitrate+busBitrate;
        String readData = "";
        for(int i = 0; i < cntr; i++)
        {
            if(Math.random()<0.5)
                readData += '0';
            else
                readData += '1';
        }
        etData.setText(readData);
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtr = 0;
        int dataLength = readData.length();
        String HostBusy = "1";
        String DataBus = "0";
        String HostClk = "1";
        String PtrClk = "1";
        MLText.append("Выбран байтный режим работы LPT-порта:");
        while(dataPtr < dataLength)
        {
            //1. drop HostBusy
            HostBusy = dropSignal(HostBusy);
            HostClk = waitSignal(HostClk);
            PtrClk = waitSignal(PtrClk);
            DataBus = waitSignal(DataBus);
            MLText.append("\n1. Сигнал готовности приема данных хостом: HostBusy=0.");
            //2. set DB
            HostBusy = waitSignal(HostBusy);
            HostClk = waitSignal(HostClk);
            PtrClk = waitSignal(PtrClk);
            DataBus = setSignal(DataBus);
            MLText.append("\n2. ПУ помещает данные на шину данных.");
            //3. drop PtrClk
            HostBusy = waitSignal(HostBusy);
            HostClk = waitSignal(HostClk);
            PtrClk = dropSignal(PtrClk);
            DataBus = waitSignal(DataBus);
            MLText.append("\n3. ПУ сигнализирует о действительности байта: PtrClk=0.");
            //4. set HostBusy
            HostBusy = setSignal(HostBusy);
            HostClk = waitSignal(HostClk);
            PtrClk = waitSignal(PtrClk);
            DataBus = waitSignal(DataBus);
            MLText.append("\n4. Хост обрабатывает байт: HostBusy=1.");
            //5. set PtrClk
            HostBusy = waitSignal(HostBusy);
            HostClk = waitSignal(HostClk);
            PtrClk = setSignal(PtrClk);
            DataBus = waitSignal(DataBus);
            MLText.append("\n5. ПУ отвечает установкой PtrClk=1.");
            //6. drop HostClk
            HostBusy = waitSignal(HostBusy);
            HostClk = dropSignal(HostClk);
            PtrClk = waitSignal(PtrClk);
            DataBus = waitSignal(DataBus);
            MLText.append("\n6. Хост подтверждает прием данных импульсом HostClk=0.");
            //7. drop DB, set HostClk
            HostBusy = waitSignal(HostBusy);
            HostClk = setSignal(HostClk);
            PtrClk = waitSignal(PtrClk);
            DataBus = dropSignal(DataBus);
            MLText.append("\n7. ПУ снимает данные с шины данных.");

            String substr = "";
            for(int i = 0; i < busBitrate; i++)
            {
                if(i+dataPtr >= readData.length())
                    break;
                substr += readData.charAt(i+dataPtr);
            }
            MLText.append("\nПередано: "+substr+"\n");
            dataPtr += busBitrate;
        }
        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedDataBus = negString(DataBus);

        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(DataBus)),"DB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedDataBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(PtrClk),2)),"PtrClk",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(HostClk),4)),"HostClk",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(HostBusy),6)),"HostBusy",Color.CYAN, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(7);
        graph.getLegendRenderer().setVisible(true);
    }

    private void lineSeriesToGraph(LineGraphSeries<DataPoint> series1, String title, int color, int thickness)
    {
        series1.setTitle(title);
        series1.setColor(color);
        series1.setThickness(thickness);
        graph.addSeries(series1);
    }
    private String setSignal(String Signal)
    {
        //поставить 1
        //пауза n-1 тактов - добавить n-1 единиц
        for(int i = 0; i < pauseInterval; i++)
            Signal += "1";
        return Signal;
    }
    private String dropSignal(String Signal)
    {
        //поставить 1
        //пауза n-1 тактов - добавить n-1 единиц
        for(int i = 0; i < pauseInterval; i++)
            Signal += "0";
        return Signal;
    }
    private String waitSignal(String Signal)
    {
        //повтор пред сост n раз
        for(int i = 0; i < pauseInterval; i++)
            Signal += Signal.charAt(Signal.length()-1);
        return Signal;
    }
}