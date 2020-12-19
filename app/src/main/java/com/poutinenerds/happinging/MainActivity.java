package com.poutinenerds.happinging;

import androidx.appcompat.app.AppCompatActivity;


import android.app.Activity;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.github.pwittchen.reactivewifi.ReactiveWifi;
import com.neosensory.neosensoryblessed.NeosensoryBlessed;

import com.potterhsu.Pinger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import us.mulb.jeff.NeoVibe;

import static android.os.SystemClock.uptimeMillis;

public class MainActivity extends AppCompatActivity implements OnDataSendToActivity {
    // set string for filtering output for this activity in Logcat
    private final String TAG = MainActivity.class.getSimpleName();

    // UI Components
    private TextView neoCliOutput;
    private TextView neoCliHeader;
    private Button neoConnectButton;
    private Button neoVibrateButton;
    // Network monitor UI components
    private TextView resultInternetConnectivity;
    private TextView resultNetworkConnectivity;
    private TextView resultWifiSignal;
    private TextView resultBandwidth;
    //Network simulator UI components
    private Switch switchSimulateNetwork;
    private SeekBar seekBarBandwidth;
    private SeekBar seekBarWifiStrength;
    private Switch switchNetworkWorking;
    private Switch switchIspWorking;

    //Needed to receive the async task SpeedTestTask response
    private Activity mActivity;

    // Constants`
    private static final int ACCESS_LOCATION_REQUEST = 2;

    // Access the library to leverage the Neosensory API
    private NeosensoryBlessed blessedNeo = null;
    private NeoVibe neoVibe = null;

    // Variable to track whether or not the wristband should be vibrating
    private static boolean vibrating = false;
    private static boolean disconnectRequested = false; // used for requesting a disconnect within our thread
    Runnable vibratingPattern;
    Thread vibratingPatternThread;

    // Subscriptions to the reactive network/wifi libraries
    private Disposable signalLevelSubscription;
    private Disposable internetConnectivitySubscription;
    private Disposable networkConnectivitySubscription;

    // Network Monitor variables
    // These are updated automatically by the reactive libraries
    // Haptic designers should consult their values inside a particular mapping strategy run()
    private int wifiStrength = 50; // 0-100 . The initial value is needed because the reactive library will update it only after there's a change in the wifi strength.
    private boolean localRouterWorking;
    private boolean ispWorking;
    // Bandwidth is updated after running the async task SpeedTestTask
    private int bandwidth; // mbps

    // Holds the simulated network states read from the UI
    private boolean simulateNetwork = false;
    private int simulatedWifiStrength;
    private boolean simulatedIspWorking;
    private boolean simulatedLocalRouterWorking;
    private int simulatedBandwidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        // Get a lock on on the UI components
        setContentView(R.layout.activity_main);
        neoCliOutput = (TextView) findViewById(R.id.cli_response);
        neoCliHeader = (TextView) findViewById(R.id.cli_header);
        neoVibrateButton = (Button) findViewById(R.id.pattern_button);
        neoConnectButton = (Button) findViewById(R.id.connection_button);

        resultWifiSignal = (TextView) findViewById(R.id.resultWifiSignal);
        resultWifiSignal.setText(String.valueOf(wifiStrength));
        resultBandwidth = (TextView) findViewById(R.id.resultBandwidth);
        resultInternetConnectivity = (TextView) findViewById(R.id.resultInternet);
        resultNetworkConnectivity = (TextView) findViewById(R.id.resultNetwork);

        switchSimulateNetwork = (Switch) findViewById(R.id.switchSimulate);
        seekBarBandwidth = (SeekBar) findViewById(R.id.seekBar_bandwidth);
        seekBarWifiStrength = (SeekBar) findViewById(R.id.seekBar_wifi);
        switchNetworkWorking = (Switch) findViewById(R.id.switchNetworkOK);
        switchIspWorking = (Switch) findViewById(R.id.switchIspOk);

        displayInitialUI();
        NeosensoryBlessed.requestBluetoothOn(this);
        if (checkLocationPermissions()) {
            displayInitConnectButton();
        } // Else, this function will have the system request permissions and handle displaying the
        // button in the callback onRequestPermissionsResult

        neoVibe = new NeoVibe(null, (Vibrator) getSystemService(VIBRATOR_SERVICE));

        // Create the vibrating pattern thread (but don't start it yet)
        vibratingPattern = new VibratingPatternMauricio();
        //  vibratingPattern = new VibratingPatternPing(); // Original Jeff's basic ping sweep

        // ADD OTHER MAPPINGS HERE
        // .
        // .
    }

    ////////////////////////////////////////
    // VibratingPattern Implementations
    /////////////////////////////////////

    // Add the implementation of new mappings here

    enum SweepType {DISCRETE, PSYCHOMETRIC};

    class VibratingPatternMauricio implements Runnable {
        final String TAG = VibratingPatternMauricio.class.getSimpleName();

        ////////////////
        // SETTINGS
        /////////////////

        // Choose one type
      //  final SweepType sweepType = SweepType.DISCRETE;
        final SweepType sweepType = SweepType.PSYCHOMETRIC;

        final int PRESENTATION_PERIOD = 6 * 1000;

        final int MAX_INTENSITY = 150; // User's preference
        final int MIN_INTENSITY = 30; // User's preference

        final int LOWEST_WIFI_STRENGTH = 10; // this maps to intensity = MAX_INTENSITY
        final int HIGHEST_WIFI_STRENGTH = 100; // this maps to intensity = MIN_INTENSITY
        final int LOWEST_BANDWIDTH = 2; // this maps to pause = MAX_PAUSE in the discrete sweep, and to SLOWEST_SWIPE in the psychometric sweep
        final int HIGHEST_BANDWIDTH = 50; // this maps to pause = 0 ms in the discrete sweep, and to FASTEST_SWIPE in the psychometric sweep

        // Used in the discrete sweep
        final int MAX_PAUSE = 400;
        // MIN_PAUSE is 0
        final int DURATION_VIB = 100; // has to be >= than NeoVibe.MIN_MS_BETWEEN_COMMANDS

        // Used in the psychometric sweepes
        final int SLOWEST_SWIPE = 4000;
        final int FASTEST_SWIPE = 800;

        ////////////////////

        private int convertBandwidthToPause(int bandwidth) {
            int pauseRepresentingBandwidth = MAX_PAUSE - MAX_PAUSE * (bandwidth - LOWEST_BANDWIDTH) / (HIGHEST_BANDWIDTH - LOWEST_BANDWIDTH);

            if (pauseRepresentingBandwidth < 0) {
                pauseRepresentingBandwidth = 0;
            } else if (pauseRepresentingBandwidth > MAX_PAUSE) {
                pauseRepresentingBandwidth = MAX_PAUSE;
            }
            Log.d(TAG, " Conversion bandwith to pause between vibrations:  " + bandwidth + "mbps -> " + String.valueOf(pauseRepresentingBandwidth) + " ms");

            return pauseRepresentingBandwidth;
        }

        private int convertWifiToIntensity(int wifiStrength) {
            int intensityRepresentingWifi = MAX_INTENSITY - (MAX_INTENSITY - MIN_INTENSITY) * (wifiStrength - LOWEST_WIFI_STRENGTH) / (HIGHEST_WIFI_STRENGTH - LOWEST_WIFI_STRENGTH);

            if (intensityRepresentingWifi < MIN_INTENSITY) {
                intensityRepresentingWifi = MIN_INTENSITY;
            } else if (intensityRepresentingWifi > MAX_INTENSITY) {
                intensityRepresentingWifi = MAX_INTENSITY;
            }

            Log.d(TAG, " Conversion wifi strength to intensity:  " + wifiStrength + " % -> " + String.valueOf(intensityRepresentingWifi) + " [0-255]");

            return intensityRepresentingWifi;

        }

        @Override
        public void run() {
            int renderedBandwidth;
            int renderedWifiStrength;
            boolean renderedLocalRouterWorking;
            boolean renderedIspWorking;

            // loop until the thread is interrupted
            while (!Thread.currentThread().isInterrupted() && vibrating) {
                try {
                    //  Perform the async speedTest in a separated thread every cicle
                    // The results probably won't be available in this cycle, but it doesn't matter
                    new SpeedTestTask(mActivity).execute();

                    if (simulateNetwork) {
                        Log.d(TAG, "Simulating Network. Ignoring real status and considering those:");
                        renderedBandwidth = simulatedBandwidth;
                        renderedWifiStrength = simulatedWifiStrength;
                        renderedLocalRouterWorking = simulatedLocalRouterWorking;
                        renderedIspWorking = simulatedIspWorking;

                    } else {
                        // Using real network states
                        Log.d(TAG, "Using real network info:");
                        renderedBandwidth = bandwidth;
                        renderedWifiStrength = wifiStrength;
                        renderedLocalRouterWorking = localRouterWorking;
                        renderedIspWorking = ispWorking;
                    }

                    Log.d(TAG, "Bandwidth " + renderedBandwidth);
                    Log.d(TAG, "Wifi " + renderedWifiStrength);
                    Log.d(TAG, "Local Router working " + renderedLocalRouterWorking);
                    Log.d(TAG, "ISP working " + renderedIspWorking);
                    Log.d(TAG, "------------------------------");

                    //////////////
                    // Mauricio's Mapping Logic
                    /////////////

                    int intensity = convertWifiToIntensity(renderedWifiStrength);

                    if (!renderedLocalRouterWorking) {
                        neoVibe.sweepDiscrete(0, 0, MAX_INTENSITY, DURATION_VIB, false, 0);
                    } else if (!renderedIspWorking) {
                        neoVibe.sweepBounceDiscrete(0, 1, MAX_INTENSITY, DURATION_VIB, false, 0);
                    }
                    // No major problem. Now render wifi strength and bandwidth
                    else {
                        int pause = convertBandwidthToPause(renderedBandwidth);
                        // Two options for sweeping
                        switch (sweepType){
                            case DISCRETE:
                                neoVibe.sweepBounceDiscrete(0, 3, intensity, DURATION_VIB, false, pause);
                                break;
                            case PSYCHOMETRIC:
                                int sweepDuration =  FASTEST_SWIPE + pause *(SLOWEST_SWIPE - FASTEST_SWIPE)/MAX_PAUSE; // converts  pause used in discrete to duration
                                neoVibe.sweepBounce(0.0F, 1.0F, (float)intensity/255, sweepDuration);
                                break;
                        }
                    }

                    Thread.sleep(PRESENTATION_PERIOD);

                } catch (InterruptedException e) {
                    if (blessedNeo != null) blessedNeo.stopMotors();
                    if (blessedNeo != null) blessedNeo.resumeDeviceAlgorithm();
                    Log.i(TAG, "Interrupted thread");
                    e.printStackTrace();
                }
            }
            if (disconnectRequested) {
                Log.i(TAG, "Disconnect requested while thread active");
                blessedNeo.stopMotors();
                blessedNeo.resumeDeviceAlgorithm();
                // When disconnecting: it is possible for the device to process the disconnection request
                // prior to processing the request to resume the onboard algorithm, which causes the last
                // sent motor command to "stick"
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                blessedNeo.disconnectNeoDevice();
                disconnectRequested = false;
            }
        }
    }


    // Create a Runnable (thread) to send a repeating vibrating pattern. Should terminate if
    // the variable `vibrating` is False
    // Jeff's original implementation - sweep if ping is ok
    class VibratingPatternPing implements Runnable {
        private Pinger pinger = new Pinger();


        public void run() {
            // loop until the thread is interrupted
            int motorID = 0;
            while (!Thread.currentThread().isInterrupted() && vibrating) {
                try {

                    long uptimeStart = uptimeMillis();
                    boolean pingRet = pinger.ping("8.8.8.8", 2);
                    Log.d("Main", "Ping returned in this many ms:  " + (uptimeMillis() - uptimeStart));
                    if (pingRet == true) {  //if ping succeeds before timeout
                        neoVibe.sweepBounce(0.0F, 1.0F, 255, 2000);
                        //neoVibe.randomVibes(500, 255, 2000);
                    }
                    Thread.sleep(4 * 1000);
                } catch (InterruptedException e) {
                    if (blessedNeo != null) blessedNeo.stopMotors();
                    if (blessedNeo != null) blessedNeo.resumeDeviceAlgorithm();
                    Log.i(TAG, "Interrupted thread");
                    e.printStackTrace();
                }
            }
            if (disconnectRequested) {
                Log.i(TAG, "Disconnect requested while thread active");
                blessedNeo.stopMotors();
                blessedNeo.resumeDeviceAlgorithm();
                // When disconnecting: it is possible for the device to process the disconnection request
                // prior to processing the request to resume the onboard algorithm, which causes the last
                // sent motor command to "stick"
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                blessedNeo.disconnectNeoDevice();
                disconnectRequested = false;
            }
        }
    }


    //////////////////////////
    // Cleanup on shutdown //
    /////////////////////////

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(BlessedReceiver);
        if (vibrating) {
            vibrating = false;
            disconnectRequested = true;
        }
        blessedNeo = null;
        neoVibe.setBlessedNeo(blessedNeo);
        vibratingPatternThread = null;
        //Dispose subscriptions to the reactive libraries
        safelyDispose(networkConnectivitySubscription, internetConnectivitySubscription, signalLevelSubscription);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unsubscribe all listeners
        safelyDispose(networkConnectivitySubscription, internetConnectivitySubscription, signalLevelSubscription);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Subscribe to listen changes in the wifi signal, internet connectivity, and network connectivity
        startWifiSignalLevelSubscription();
        startInternetConnectivitySubscription();
        startNetworkConnectivitySubscription();
    }

    ////////////////////////////////////
    // SDK state change functionality //
    ////////////////////////////////////

    // A Broadcast Receiver is responsible for conveying important messages/information from our
    // NeosensoryBlessed instance. There are 3 types of messages we can receive:
    //
    // 1. "com.neosensory.neosensoryblessed.CliReadiness": conveys a change in state for whether or
    // not a connected Buzz is ready to accept commands over its command line interface. Note: If the
    // CLI is ready, then it is currently a prerequisite that a compliant device is connected.
    //
    // 2. "com.neosensory.neosensoryblessed.ConnectedState": conveys a change in state for whether or
    // not we're connected to a device. True == connected, False == not connected. In this example, we
    // don't actually need this, because we can use the CLI's readiness by proxy.
    //
    // 3. "com.neosensory.neosensoryblessed.CliMessage": conveys a message sent to Android from a
    // connected Neosensory device's command line interface
    private final BroadcastReceiver BlessedReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra("com.neosensory.neosensoryblessed.CliReadiness")) {
                        // Check the message from NeosensoryBlessed to see if a Neosensory Command Line
                        // Interface
                        // has become ready to accept commands
                        // Prior to calling other API commands we need to accept the Neosensory API ToS
                        if (intent.getBooleanExtra("com.neosensory.neosensoryblessed.CliReadiness", false)) {
                            // request developer level access to the connected Neosensory device
                            blessedNeo.sendDeveloperAPIAuth();
                            // sendDeveloperAPIAuth() will then transmit a message back requiring an explicit
                            // acceptance of Neosensory's Terms of Service located at
                            // https://neosensory.com/legal/dev-terms-service/
                            blessedNeo.acceptApiTerms();
                            Log.i(TAG, String.format("state message: %s", blessedNeo.getNeoCliResponse()));
                            // Assuming successful authorization, set up a button to run the vibrating pattern
                            // thread above
                            displayVibrateButton();
                            displayDisconnectUI();
                        } else {
                            displayReconnectUI();
                        }
                    }

                    if (intent.hasExtra("com.neosensory.neosensoryblessed.CliMessage")) {
                        String notification_value =
                                intent.getStringExtra("com.neosensory.neosensoryblessed.CliMessage");
                        neoCliOutput.setText(notification_value);
                    }

                    if (intent.hasExtra("com.neosensory.neosensoryblessed.ConnectedState")) {
                        if (intent.getBooleanExtra("com.neosensory.neosensoryblessed.ConnectedState", false)) {
                            Log.i(TAG, "Connected to Buzz");
                        } else {
                            Log.i(TAG, "Disconnected from Buzz");
                        }
                    }
                }
            };

    ///////////////////////////////////
    // User interface functionality //
    //////////////////////////////////

    private void displayInitialUI() {
        displayReconnectUI();
        neoVibrateButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!vibrating) {
                            if (blessedNeo != null) blessedNeo.pauseDeviceAlgorithm();
                            neoVibrateButton.setText("Stop Vibration Pattern");
                            vibrating = true;
                            // run the vibrating pattern loop
                            vibratingPatternThread = new Thread(vibratingPattern);
                            vibratingPatternThread.start();

                        } else {
                            neoVibrateButton.setText("Start Vibration Pattern");
                            vibrating = false;
                            if (blessedNeo != null) blessedNeo.resumeDeviceAlgorithm();
                        }
                    }
                });

        switchSimulateNetwork.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                simulateNetwork = isChecked;

            }
        });

        switchNetworkWorking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                simulatedLocalRouterWorking = isChecked;
            }
        });

        switchIspWorking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                simulatedIspWorking = isChecked;
            }
        });

        seekBarWifiStrength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                simulatedWifiStrength = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBarBandwidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                simulatedBandwidth = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void displayReconnectUI() {
        //neoCliOutput.setVisibility(View.INVISIBLE);
        //neoCliHeader.setVisibility(View.INVISIBLE);
        //neoVibrateButton.setVisibility(View.INVISIBLE);
        neoVibrateButton.setClickable(false);
        neoVibrateButton.setText(
                "Start Vibration Pattern"); // Vibration stops on disconnect so reset the button text
        neoConnectButton.setText("Scan and Connect to Neosensory Buzz");
        neoConnectButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        blessedNeo.attemptNeoReconnect();
                        toastMessage("Attempting to reconnect. This may take a few seconds.");
                    }
                });
    }

    private void displayDisconnectUI() {
        neoCliOutput.setVisibility(View.VISIBLE);
        neoCliHeader.setVisibility(View.VISIBLE);
        neoConnectButton.setText("Disconnect");
        neoConnectButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!vibrating) {
                            blessedNeo.disconnectNeoDevice();
                        } else {
                            // If motors are vibrating (in the VibratingPattern thread in this case) and we want
                            // to stop them on disconnect, we need to add a sleep/delay as it's possible for the
                            // disconnect to be processed prior to stopping the motors. See the VibratingPattern
                            // definition.
                            disconnectRequested = true;
                            vibrating = false;
                        }
                    }
                });
    }

    private void displayInitConnectButton() {
        // Display the connect button and create the Bluetooth Handler if so
        neoConnectButton.setClickable(true);
        neoConnectButton.setVisibility(View.VISIBLE);
        neoConnectButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        if (vibrating) {  //don't allow connecting to band if vibing already started TODO: just grey out button rather than ignore it...
                            Log.w(TAG, "Preventing buzz connection because vibration is already running, and this causes weirdness.");
                            return;
                        }
                        initBluetoothHandler();
                    }
                });
    }

    public void displayVibrateButton() {
        neoVibrateButton.setVisibility(View.VISIBLE);
        neoVibrateButton.setClickable(true);
    }

    private void toastMessage(String message) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }
    //////////////////////////////////////////////
    // Bluetooth and permissions initialization //
    //////////////////////////////////////////////

    private void initBluetoothHandler() {
        // Create an instance of the Bluetooth handler. This uses the constructor that will search for
        // and connect to the first available device with "Buzz" in its name. To connect to a specific
        // device with a specific address, you can use the following pattern:  blessedNeo =
        // NeosensoryBlessed.getInstance(getApplicationContext(), <address> e.g."EB:CA:85:38:19:1D",
        // false);
        blessedNeo =
                NeosensoryBlessed.getInstance(getApplicationContext(), new String[]{"Buzz"}, false);
        neoVibe.setBlessedNeo(blessedNeo);
        // register receivers so that NeosensoryBlessed can pass relevant messages and state changes to MainActivity
        registerReceiver(BlessedReceiver, new IntentFilter("BlessedBroadcast"));
    }

    private boolean checkLocationPermissions() {
        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && targetSdkVersion >= Build.VERSION_CODES.Q) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST);
                return false;
            } else {
                return true;
            }
        } else {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_LOCATION_REQUEST);
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ((requestCode == ACCESS_LOCATION_REQUEST)
                && (grantResults.length > 0)
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            displayInitConnectButton();
        } else {
            toastMessage("Unable to obtain location permissions, which are required to use Bluetooth.");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    //////////////////////////////////////////////
    //   NETWORK MONITOR STUFF
    //////////////////////////////////////////////////

    private void safelyDispose(Disposable... disposables) {
        for (Disposable subscription : disposables) {
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        }
    }

    // These are responsible for subscribing and reacting to the changes in network
    // When a new value is observed, the callbacks codes below are run

    private void startWifiSignalLevelSubscription() {

        signalLevelSubscription = ReactiveWifi.observeWifiSignalLevel(getApplicationContext(), 100)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(level -> {
                    //Callback
                    wifiStrength = level; // updates the level
                    resultWifiSignal.setText(level.toString()); // updates the UI
                });
    }

    private void startInternetConnectivitySubscription() {

        internetConnectivitySubscription = ReactiveNetwork
                .observeInternetConnectivity()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(isConnectedToInternet -> {
                    //Callback
                    ispWorking = isConnectedToInternet;
                    resultInternetConnectivity.setText(String.valueOf(ispWorking));
                });
    }

    private void startNetworkConnectivitySubscription() {
        networkConnectivitySubscription = ReactiveNetwork
                .observeNetworkConnectivity(getApplicationContext())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectivity -> {
                    //Callback
                    if (connectivity.state().toString().equals(NetworkInfo.State.CONNECTED.toString())) {
                        localRouterWorking = true;
                    } else {
                        localRouterWorking = false;
                    }
                    // update  UI
                    resultNetworkConnectivity.setText(String.valueOf(localRouterWorking));
                });
    }

    // Callback function to receive data from the Async SpeedTestTask
    @Override
    public void sendData(Double bandwidth) {
        Log.d(TAG, "Bandwidth Result received in main : " + bandwidth);
        this.bandwidth = (int) bandwidth.doubleValue();
        //Updates the UI
        resultBandwidth.setText(String.valueOf(this.bandwidth));
    }


}
