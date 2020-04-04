package com.neosensory.neosensoryblessed;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothCentralCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static com.welie.blessed.BluetoothBytesParser.bytes2String;
import static com.welie.blessed.BluetoothPeripheral.GATT_SUCCESS;

public class NeosensoryBLESSED {

  private final String TAG = NeosensoryBLESSED.class.getSimpleName();

  // UUIDs for Neosensory UART over BLE
  private static final UUID UART_OVER_BLE_SERVICE_UUID =
      UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
  private static final UUID UART_RX_WRITE_UUID =
      UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
  private static final UUID UART_TX_NOTIFY_UUID =
      UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

  // UUIDs for the Device Information service (DIS)
  private static final UUID DIS_SERVICE_UUID =
      UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
  private static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID =
      UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");

  // Local variables
  private BluetoothCentral central;
  private static NeosensoryBLESSED instance = null;
  private Context context;
  private Handler handler = new Handler();
  private static BluetoothPeripheral neoPeripheral = null;
  private static BluetoothGattCharacteristic neoWriteCharacteristic = null;
  private boolean autoReconnectEnabled = false;
  // Local state information
  private boolean neoDeviceConnected = false;
  private boolean neoCLIReady = false;
  private String neoCLIResponse = null;

  // sendCommand encodes the command strings for the CLI in the proper format.
  private boolean sendCommand(String CLICommand) {
    if ((neoDeviceConnected) && (neoCLIReady)) {
      byte[] CLIBytes = CLICommand.getBytes(StandardCharsets.UTF_8);
      neoPeripheral.writeCharacteristic(neoWriteCharacteristic, CLIBytes, WRITE_TYPE_DEFAULT);
      return true;
    } else {
      return false;
    }
  }

  // TODO: Create sync + async modes for awaiting CLI feedback

  /**
   * @brief Request developer authorization. The CLI returns the message “Please type 'accept' and
   *     hit enter to agree to Neosensory Inc's Developer Terms and Conditions, which can be viewed
   *     at https://neosensory.com/legal/dev-terms-service”
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean sendDeveloperAPIAuth() {
    return sendCommand("auth as developer\n");
  }

  /**
   * @brief After successfully calling auth as developer, use the accept command to agree to the
   *     Neosensory Developer API License (https://neosensory.com/legal/dev-terms-service/).
   *     Successfully calling this unlocks the following commands: audio start, audio stop,
   *     motors_clear_queue, motors start, motors_stop, motors vibrate.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean acceptAPIToS() {
    return sendCommand("accept\n");
  }

  /**
   * @brief (Re)starts the device’s microphone audio acquisition. This command requires successful
   *     developer authorization, otherwise, the command will fail.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean startAudio() {
    return sendCommand("audio start\n");
  }

  /**
   * @brief Stop the device’s microphone audio acquisition. This should be called prior to
   *     transmitting motor vibration data. This command requires successful developer
   *     authorization, otherwise, the command will fail.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean stopAudio() {
    return sendCommand("audio stop\n");
  }

  /**
   * @brief Obtain the device’s battery level in %. This command does not require developer
   *     authorization
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean getBatteryLevel() {
    return sendCommand("device battery_soc\n");
  }

  /**
   * @brief Obtain various device and firmware information. This command does not require developer
   *     authorization.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean getDeviceInfo() {
    return sendCommand("device info\n");
  }

  /**
   * @brief Clear any vibration commands sitting the device’s motor FIFO queue. This should be
   *     called prior to streaming control frames using motors vibrate.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean clearMotorQueue() {
    return sendCommand("motors clear_queue\n");
  }

  /**
   * @brief Initialize and start the motors interface. The motors can then accept motors vibrate
   *     commands.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean startMotors() {
    return sendCommand("motors start\n");
  }

  /**
   * @brief Clear the motors command queue and shut down the motor drivers.
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   */
  public boolean stopMotors() {
    return sendCommand("motors stop\n");
  }

  /**
   * @brief Set the actuators amplitudes on a connected Neosensory device
   * @param[in] motorValues byte array of length # of motors of the target device (e.g. should be 4
   *     if a Neosensory Buzz). Element values should between 0 (motor off) and 255 (motor at full
   *     amplitude). Example input format: new byte[] {(byte) 155, (byte) 0, (byte) 0, (byte) 0};
   * @return true if connected to a valid device that is ready to accept CLI commands. TODO: handle
   *     returning JSON response from the device
   * @note actuators will stay vibrating indefinitely on the last frame received until a new control
   *     frame is received
   */
  public boolean vibrateMotors(byte[] motorValues) {
    byte[] b64motorValues = Base64.getEncoder().encode(motorValues);
    String fire_command =
        "motors vibrate " + new String(b64motorValues, StandardCharsets.UTF_8) + "\n";
    return sendCommand(fire_command);
  }

  public String getNeoCLIResponse() {
    return neoCLIResponse;
  }

  /** @brief If connected to a Neosensory device, disconnect it */
  public void disconnectNeoDevice() {
    if ((neoDeviceConnected == true) && (neoPeripheral != null)) {
      central.cancelConnection(neoPeripheral);
    }
  }

  /** @brief Attempt to reconnect to a Neosensory device if disconnected */
  public void attemptNeoReconnect() {
    if ((neoDeviceConnected == false) && (neoPeripheral != null)) {
      handler.postDelayed(
          new Runnable() {
            @Override
            public void run() {
              central.autoConnectPeripheral(neoPeripheral, peripheralCallback);
            }
          },
          5000);
    }
  }

  // Callback for peripherals
  private final BluetoothPeripheralCallback peripheralCallback =
      new BluetoothPeripheralCallback() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
          // Attempt to turn on notifications from the UART
          if (peripheral.getService(UART_OVER_BLE_SERVICE_UUID) != null) {
            BluetoothGattCharacteristic bleNotifyCharacteristic =
                peripheral.getCharacteristic(UART_OVER_BLE_SERVICE_UUID, UART_TX_NOTIFY_UUID);
            peripheral.setNotify(bleNotifyCharacteristic, true);
            neoPeripheral = peripheral;
            neoWriteCharacteristic =
                peripheral.getCharacteristic(UART_OVER_BLE_SERVICE_UUID, UART_RX_WRITE_UUID);
            neoCLIReady = true;
            broadcastCLIReadiness();
            Log.i(TAG, String.format("SUCCESS: CLI ready to accept commands"));

          } else {
            neoCLIReady = false;
            broadcastCLIReadiness();
            Log.i(TAG, String.format("Failure: No services found on UUID"));
          }
        }

        // Log a successful change in notification status for the characteristic
        @Override
        public void onNotificationStateUpdate(
            BluetoothPeripheral peripheral,
            BluetoothGattCharacteristic characteristic,
            int status) {
          if (status == GATT_SUCCESS) {
            if (peripheral.isNotifying(characteristic)) {
              Log.i(
                  TAG,
                  String.format("SUCCESS: Notify set to 'on' for %s", characteristic.getUuid()));
            } else {
              Log.i(
                  TAG,
                  String.format("SUCCESS: Notify set to 'off' for %s", characteristic.getUuid()));
            }
          } else {
            Log.e(
                TAG,
                String.format(
                    "ERROR: Changing notification state failed for %s", characteristic.getUuid()));
          }
        }

        // Log pass/fail upon attempting a a write characteristic
        @Override
        public void onCharacteristicWrite(
            BluetoothPeripheral peripheral,
            byte[] value,
            BluetoothGattCharacteristic characteristic,
            int status) {
          if (status == GATT_SUCCESS) {
            Log.i(
                TAG,
                String.format(
                    "SUCCESS: Writing <%s> to <%s>",
                    bytes2String(value), characteristic.getUuid().toString()));
          } else {
            Log.i(
                TAG,
                String.format(
                    "ERROR: Failed writing <%s> to <%s>",
                    bytes2String(value), characteristic.getUuid().toString()));
          }
        }

        // For now we'll only broadcast UART_TX Notifications (i.e. CLI Output) in our module and
        // send other notifications to logcat
        @Override
        public void onCharacteristicUpdate(
            BluetoothPeripheral peripheral,
            byte[] value,
            BluetoothGattCharacteristic characteristic,
            int status) {
          if (status != GATT_SUCCESS) return;
          UUID characteristicUUID = characteristic.getUuid();
          BluetoothBytesParser parser = new BluetoothBytesParser(value);
          if (characteristicUUID.equals(MANUFACTURER_NAME_CHARACTERISTIC_UUID)) {
            String manufacturer = parser.getStringValue(0);
            Log.i(TAG, String.format("Received manufacturer: %s", manufacturer));
          } else if (characteristicUUID.equals(UART_TX_NOTIFY_UUID)) {
            neoCLIResponse = parser.getStringValue(0);
            Log.i(TAG, String.format("Received notification: %s", neoCLIResponse));
            broadcastCLI(neoCLIResponse);
          } else if (characteristicUUID.equals(UART_RX_WRITE_UUID)) {
            String rx_write_val = parser.getStringValue(0);
            Log.i(TAG, String.format("Received rxwrite: %s", rx_write_val));
          }
        }
      };

  // create an Intent to broadcast if a connected Neosensory device is ready to accept commands
  private void broadcastCLIReadiness() {
    Intent intent = new Intent("CLIAvailable");
    intent.putExtra("CLIReady", neoCLIReady);
    context.sendBroadcast(intent);
  }

  // create an Intent to broadcast Neosensory CLI Output.
  private void broadcastCLI(String CLIResponse) {
    Intent intent = new Intent("CLIOutput");
    intent.putExtra("CLIResponse", CLIResponse);
    context.sendBroadcast(intent);
  }

  // create an Intent to broadcast a message for connection state changes.
  private void broadcastConnectedState() {
    Intent intent = new Intent("ConnectionState");
    intent.putExtra("connectedState", neoDeviceConnected);
    context.sendBroadcast(intent);
  }

  // Example callback processing for BLESSED
  private final BluetoothCentralCallback bluetoothCentralCallback =
      new BluetoothCentralCallback() {
        // Upon connecting to a peripheral, log the output and  broadcast message (e.g. to Main
        // Activity)
        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
          Log.i(TAG, String.format("connected to '%s'", peripheral.getName()));
          neoDeviceConnected = true;
          broadcastConnectedState();
        }

        // Upon a failed connection, log the output
        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, final int status) {
          neoDeviceConnected = false;
          broadcastConnectedState();
          neoCLIReady = false;
          broadcastCLIReadiness();
          Log.e(
              TAG,
              String.format("connection '%s' failed with status %d", peripheral.getName(), status));
        }

        // Upon a disconnect, log the output and attempt to reconnect every 5 seconds.
        @Override
        public void onDisconnectedPeripheral(
            final BluetoothPeripheral peripheral, final int status) {
          neoDeviceConnected = false;
          broadcastConnectedState();
          neoCLIReady = false;
          broadcastCLIReadiness();

          Log.i(
              TAG, String.format("disconnected '%s' with status %d", peripheral.getName(), status));
          if (autoReconnectEnabled) {
            if (neoDeviceConnected == false) {
              handler.postDelayed(
                  new Runnable() {
                    @Override
                    public void run() {
                      central.autoConnectPeripheral(peripheral, peripheralCallback);
                    }
                  },
                  5000);
            }
          }
        }

        // Upon discovering target peripheral, stop scan and initiate connection.
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
          Log.i(TAG, String.format("Found peripheral '%s'", peripheral.getName()));
          central.stopScan();
          central.connectPeripheral(peripheral, peripheralCallback);
        }
      };

  /**
   * @brief Create and return instance using constructor used to connect to first discovered device
   *     containing the name "Buzz"
   * @param[in] context the Android Context * @param[in] autoReconnect boolean for if the Bluetooth
   *     handler should automatically attempt to * reconnect to the device if a connection is lost.
   * @param[in] autoReconnect boolean for if the Bluetooth handler should automatically attempt to *
   *     reconnect to the device if a connection is lost.
   * @return the instance of the NeosensoryBLESSED object
   */
  public static synchronized NeosensoryBLESSED getInstance(Context context, boolean autoReconnect) {
    if (instance == null) {
      instance = new NeosensoryBLESSED(context.getApplicationContext(), autoReconnect);
    }
    return instance;
  }

  /**
   * @brief Create and return instance using constructor used to connect to a device with a specific
   *     address e.g. "EB:CA:85:38:19:1D"
   * @param[in] context the Android Context
   * @param[in] neoAddress string in the format of a desired address e.g. "EB:CA:85:38:19:1D"
   * @param[in] autoReconnect boolean for if the Bluetooth handler should automatically attempt to
   *     reconnect to the device if a connection is lost.
   * @return the instance of the NeosensoryBLESSED object
   */
  public static synchronized NeosensoryBLESSED getInstance(
      Context context, String neoAddress, boolean autoReconnect) {
    if (instance == null) {
      instance = new NeosensoryBLESSED(context.getApplicationContext(), neoAddress, autoReconnect);
    }
    return instance;
  }

  /**
   * @brief Constructor used to connect to a device with a specific address e.g. "EB:CA:85:38:19:1D"
   * @param[in] context the Android Context
   * @param[in] neoAddress string in the format of a desired address e.g. "EB:CA:85:38:19:1D"
   * @param[in] autoReconnect boolean for if the Bluetooth handler should automatically attempt to
   *     reconnect to the device if a connection is lost.
   */
  private NeosensoryBLESSED(Context context, String neoAddress, boolean autoReconnect) {
    this.context = context;
    autoReconnectEnabled = autoReconnect;
    // Create BluetoothCentral
    central = new BluetoothCentral(context, bluetoothCentralCallback, new Handler());
    // Scan for peripherals with a certain service UUIDs
    central.startPairingPopupHack();
    central.scanForPeripheralsWithAddresses(new String[] {neoAddress});
  }

  /**
   * @return the instance of the NeosensoryBLESSED object
   * @brief Constructor used to connect to first discovered device containing the name "Buzz"
   * @param[in] context the Android Context * @param[in] autoReconnect boolean for if the Bluetooth
   *     handler should automatically attempt to * reconnect to the device if a connection is lost.
   * @param[in] autoReconnect boolean for if the Bluetooth handler should automatically attempt to
   *     reconnect to the device if a connection is lost.
   */
  private NeosensoryBLESSED(Context context, boolean autoReconnect) {
    this.context = context;
    autoReconnectEnabled = autoReconnect;
    // Create BluetoothCentral
    central = new BluetoothCentral(context, bluetoothCentralCallback, new Handler());
    // Scan for peripherals with a certain service UUIDs
    central.startPairingPopupHack();
    central.scanForPeripheralsWithNames(new String[] {"Buzz"});
  }
}