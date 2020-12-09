package com.poutinenerds.networkmonitoring;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class SpeedTestTask extends AsyncTask<Void, Void, Double> {

    double speedResult;
    // Interface used to send [speedResult] to the MainActivity responsible for updating the UI
    OnDataSendToActivity dataSendToActivity;
    // Used to make the asynctask wait for the SpeedTestListener
    boolean completed;

    public SpeedTestTask(Activity activity){
        Log.d("speedtest", "New SpeedTestTask created");
        dataSendToActivity = (OnDataSendToActivity)activity;
    }

    @Override
    protected Double doInBackground(Void... params) {
        SpeedTestSocket speedTestSocket = new SpeedTestSocket();
        Log.d("speedtest", "Running SpeedTestTask");

        completed = false;

        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                speedResult = report.getTransferRateBit().doubleValue() / 100000.0;
                Log.d("speedtest", "SpeedTestTask Listener completed: " + speedResult);
                completed = true;
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                speedResult = 0;
                Log.d("speedtest", "SpeedTestTask Listener completed with error: " + speedResult);
                completed = true;
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                //  Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                //  Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            }
        });

        // HTTP download test with 1mb file. Other options are available
        speedTestSocket.startDownload("http://ipv4.ikoula.testdebit.info/1M.iso" );

        // This is needed to make the asynctask wait for the SpeedTestListener
        // It's not elegant but it works perfectly because asynctask is already running on a sepaated thred from UI
        while(!completed){
            SystemClock.sleep(1);}

        return speedResult;

    }

    @Override
    protected void onPostExecute(Double result) {
       // Async task is finished
        // Now call the callback [sendData]

        super.onPostExecute(result);
        dataSendToActivity.sendData(result);
    }
}
