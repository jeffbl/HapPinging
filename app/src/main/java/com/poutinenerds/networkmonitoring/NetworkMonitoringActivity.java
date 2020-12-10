package com.poutinenerds.networkmonitoring;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.github.pwittchen.reactivewifi.ReactiveWifi;

import com.poutinenerds.happinging.R;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

// Basic Network Monitoring Activity

// Uses  reactivenetwork and reactivewifi libraries
// Callbacks for changes in the a) wifi signal, b) internet connectivity, and c) network connectivity
// Also implements an async network bandwidth test. To call this method, rotate the device.

public class NetworkMonitoringActivity extends AppCompatActivity implements OnDataSendToActivity {

    private TextView resultInternetConnectivity;
    private TextView resultNetworkConnectivity;
    private TextView resultWifiSignal;
    private TextView resultBandwidth;

    // These are the subscriptions to the reactive network/wifi libraries
    private Disposable signalLevelSubscription;
    private Disposable internetConnectivitySubscription;
    private Disposable networkConnectivitySubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_monitoring);

        resultWifiSignal = findViewById(R.id.resultWifiSignal);
        resultBandwidth = findViewById(R.id.resultBandwidth);
        resultInternetConnectivity =  findViewById(R.id.resultInternet);
        resultNetworkConnectivity =  findViewById(R.id.resultNetwork);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("speedtest", "RESUMING");

        //  Perform the async speedTest in a separated thread
        new SpeedTestTask(this).execute();

        // Subscribe to listen changes in the wifi signal, internet connectivity, and network connectivity
        startWifiSignalLevelSubscription();
        startInternetConnectivitySubscription();
        startNetworkConnectivitySubscription();
    }

    @Override
    protected void onPause() {
        super.onPause();
        safelyDispose(networkConnectivitySubscription, internetConnectivitySubscription, signalLevelSubscription);
    }

    private void startWifiSignalLevelSubscription() {

        signalLevelSubscription = ReactiveWifi.observeWifiSignalLevel(getApplicationContext(), 100)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(level -> {
                    //Callback goes here. For now just update the UI
                    resultWifiSignal.setText(level.toString());
                });
    }

    private void startInternetConnectivitySubscription() {

        internetConnectivitySubscription = ReactiveNetwork
                .observeInternetConnectivity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isConnectedToInternet -> {
                    //Callback goes here. For now just update the UI
                    resultInternetConnectivity.setText(isConnectedToInternet.toString());
                });

    }

    private void startNetworkConnectivitySubscription() {
        networkConnectivitySubscription = ReactiveNetwork
                .observeNetworkConnectivity(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectivity -> {
                    //Callback goes here. For now just update the UI
                    resultNetworkConnectivity.setText(connectivity.state().toString());
                });
    }


    // Callback function to receive data from the Async SpeedTestTask

    @Override
    public void sendData(Double bandwidth) {
        Log.d("speedtest", "Result received in main : " + bandwidth);
        //Updates the UI
        resultBandwidth.setText(String.format("%.0f", bandwidth));
    }

    // Add other callbacks here
    //
    //
    //


    private void safelyDispose(Disposable... disposables) {
        for (Disposable subscription : disposables) {
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }
}