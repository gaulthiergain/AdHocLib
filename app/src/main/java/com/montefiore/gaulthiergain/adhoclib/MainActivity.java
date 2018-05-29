package com.montefiore.gaulthiergain.adhoclib;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.TransferManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[AdHoc][Main]";
    private static final int NORMAL = 0;
    private static final int WARNING = 1;
    private static final int ERROR = 2;
    private static final int REQUEST_CODE_LOC = 4;

    private TransferManager transferManager;

    private CustomAdapter dataAdapter;

    private ListenerApp listenerApp = new ListenerApp() {
        @Override
        public void onReceivedData(AdHocDevice adHocDevice, Object pdu) {
            updateLog(NORMAL, "Data receive " + pdu.toString() + " from " + adHocDevice.getDeviceName());
        }

        @Override
        public void onForwardData(AdHocDevice adHocDevice, Object pdu) {

        }

        @Override
        public void onConnection(AdHocDevice adHocDevice) {
            if (adHocDevice.isDirectedConnected()) {
                updateLog(NORMAL, "Connected with device " + adHocDevice.getDeviceName() + " - " + adHocDevice.getLabel());
            } else {
                updateLog(NORMAL, "Device " + adHocDevice.getDeviceName() + " - " + adHocDevice.getLabel() + " has joined");
            }
            updateGui();
        }

        @Override
        public void onConnectionFailed(Exception e) {
            updateLog(ERROR, "Exception during opening connection: " + e.getMessage());
            e.printStackTrace();
        }

        @Override
        public void onConnectionClosed(AdHocDevice adHocDevice) {
            if (adHocDevice.isDirectedConnected()) {
                updateLog(NORMAL, "Close connection with device " + adHocDevice.getDeviceName()
                        + " - " + adHocDevice.getLabel());
            } else {
                updateLog(NORMAL, "Device " + adHocDevice.getDeviceName() + " - " + adHocDevice.getLabel() + " has left");
            }
        }

        @Override
        public void onConnectionClosedFailed(Exception e) {
            updateLog(ERROR, "Exception during closing connection: " + e.getMessage());
            e.printStackTrace();
        }

        @Override
        public void processMsgException(Exception e) {
            updateLog(ERROR, "Exception during processing message: " + e.getMessage());
            e.printStackTrace();
        }
    };

    private DiscoveryListener listenerDiscovery = new DiscoveryListener() {
        @Override
        public void onDeviceDiscovered(AdHocDevice device) {
            updateLog(NORMAL, "Device discovered: " + device.getDeviceName() + " " + device.getMacAddress());
        }

        @Override
        public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {
            updateLog(NORMAL, "On discovery completed");

            updateListView(mapNameDevice);
        }

        @Override
        public void onDiscoveryStarted() {
            updateLog(NORMAL, "Discovery started");
        }

        @Override
        public void onDiscoveryFailed(Exception e) {
            updateLog(ERROR, "Exception during discovery: " + e.getMessage());
        }
    };

    private void updateListView(HashMap<String, AdHocDevice> mapNameDevice) {
        ArrayList<SelectedDevice> listOfDevices = new ArrayList<>();

        if (mapNameDevice.size() > 0) {
            for (AdHocDevice device : mapNameDevice.values()) {
                listOfDevices.add(new SelectedDevice(device));
            }

            // Create an ArrayAdaptar from the String Array
            dataAdapter = new CustomAdapter(MainActivity.this,
                    R.layout.row, listOfDevices);
            ListView listView = findViewById(R.id.listViewDiscoveredDevice);
            listView.setVisibility(View.VISIBLE);

            // Assign adapter to ListView
            listView.setAdapter(dataAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {

                }
            });

            Button btnConnect = findViewById(R.id.button_connect);
            btnConnect.setVisibility(View.VISIBLE);
        } else {
            Button btnConnect = findViewById(R.id.button_connect);
            btnConnect.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.enable_wifi:
                if (!transferManager.isWifiEnabled()) {
                    transferManager.enableWifi(getApplicationContext(), initListenerAdapter());
                } else {
                    updateLog(WARNING, "Wifi is already enabled");
                }
                return true;
            case R.id.disable_wifi:
                if (transferManager.isWifiEnabled()) {
                    try {
                        transferManager.disableWifi();
                        updateLog(NORMAL, "Disable Wifi");
                    } catch (IOException e) {
                        updateLog(ERROR, "Error while disabling wifi " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    updateLog(WARNING, "Wifi is already disabled");
                }
                return true;
            case R.id.enable_bt:
                if (!transferManager.isBluetoothEnabled()) {
                    try {
                        transferManager.enableBluetooth(0, getApplicationContext(), initListenerAdapter());
                    } catch (BluetoothBadDuration e) {
                        updateLog(ERROR, "Error while enabling Bluetooth " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    updateLog(WARNING, "Bluetooth is already enabled");
                }
                return true;
            case R.id.disable_bt:
                if (transferManager.isBluetoothEnabled()) {
                    try {
                        transferManager.disableBluetooth();
                        updateLog(NORMAL, "Disable Bluetooth");
                    } catch (IOException e) {
                        updateLog(ERROR, "Error while disabling Bluetooth " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    updateLog(WARNING, "Bluetooth is already disabled");
                }
                return true;
            case R.id.remove_group:

                try {
                    transferManager.removeWifiGroup(new ListenerAction() {
                        @Override
                        public void onSuccess() {
                            updateLog(NORMAL, "Group are removed");
                        }

                        @Override
                        public void onFailure(Exception e) {
                            updateLog(ERROR, "Error while removing group " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } catch (DeviceException e) {
                    updateLog(ERROR, "DeviceException: " + e.getMessage());
                    e.printStackTrace();
                }

                break;
            case R.id.is_groupOwner:
                boolean groupOwner;
                try {
                    groupOwner = transferManager.isWifiGroupOwner();
                    updateLog(NORMAL, "Is group OWner" + groupOwner);
                } catch (DeviceException e) {
                    updateLog(ERROR, "DeviceException: " + e.getMessage());
                }

                break;
            case R.id.get_neighbors:
                ArrayList<AdHocDevice> adHocDevices = transferManager.getDirectNeighbors();
                if (adHocDevices.size() > 0) {
                    for (AdHocDevice adHocDevice : adHocDevices) {
                        updateLog(NORMAL, "Neighbors: " + adHocDevice);
                    }
                } else {
                    updateLog(WARNING, "No Neighbors");
                }

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private ListenerAdapter initListenerAdapter() {
        return new ListenerAdapter() {
            @Override
            public void onEnableBluetooth(boolean success) {
                if (success) {
                    updateLog(NORMAL, "Bluetooth is now enabled");
                } else {
                    updateLog(WARNING, "Bluetooth is not enabled");
                }
            }

            @Override
            public void onEnableWifi(boolean success) {
                if (success) {
                    updateLog(NORMAL, "Wifi is now enabled");
                } else {
                    updateLog(WARNING, "Wifi is not enabled");
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Config config = new Config();
        Log.d(TAG, config.toString());

        config.setConnectionFlooding(true);
        config.setReliableTransportWifi(true);
        config.setJson(true);

        transferManager = new TransferManager(true, config, listenerApp);
        updateLog(NORMAL, "Own address is " + transferManager.getOwnAddress());

        try {
            transferManager.start(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
            updateLog(ERROR, "IO exception: " + e.getMessage());
        }

        updateLog(NORMAL, "Transfer manager is initiated");


        // Initialize the button to perform device discovery
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setVisibility(View.VISIBLE);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Reset listView
                ListView listView = findViewById(R.id.listViewDiscoveredDevice);
                dataAdapter = new CustomAdapter(MainActivity.this,
                        R.layout.row, new ArrayList<SelectedDevice>());
                listView.setAdapter(dataAdapter);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    accessLocationPermission();
                } else {
                    try {
                        transferManager.discovery(listenerDiscovery);
                    } catch (DeviceException e) {
                        e.printStackTrace();
                        updateLog(ERROR, "Device exception: " + e.getMessage());
                    }
                }
            }
        });

        Button pairedBtn = findViewById(R.id.button_paired);
        pairedBtn.setVisibility(View.VISIBLE);
        pairedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (transferManager.isBluetoothEnabled()) {
                    // Reset listView
                    ListView listView = findViewById(R.id.listViewDiscoveredDevice);
                    dataAdapter = new CustomAdapter(MainActivity.this,
                            R.layout.row, new ArrayList<SelectedDevice>());
                    listView.setAdapter(dataAdapter);

                    updateListView(transferManager.getPairedBluetoothDevices());

                    updateLog(NORMAL, "Get paired bluetooth device");
                } else {
                    updateLog(ERROR, "Bluetooth is not enabled");
                }
            }
        });


        checkButtonClick();

    }

    private void updateGui() {

        final EditText editTextDest = findViewById(R.id.editTextDest);
        editTextDest.setVisibility(View.VISIBLE);

        final EditText editTextSend = findViewById(R.id.editTextSend);
        editTextSend.setVisibility(View.VISIBLE);

        Button btnSend = findViewById(R.id.button_send);
        btnSend.setVisibility(View.VISIBLE);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editTextSend.getText().toString();
                String remoteDest = editTextDest.getText().toString();

                try {
                    updateLog(NORMAL, "Send to : " + remoteDest);
                    transferManager.sendMessageTo(msg, new AdHocDevice(remoteDest));
                } catch (IOException e) {
                    updateLog(ERROR, "IOException: " + e.getMessage());
                    e.printStackTrace();
                } catch (DeviceException e) {
                    updateLog(ERROR, "DeviceException: " + e.getMessage());
                    e.printStackTrace();
                }

            }
        });

        Button btnBroadcast = findViewById(R.id.button_broadcast);
        btnBroadcast.setVisibility(View.VISIBLE);

        btnBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editTextSend.getText().toString();
                String remoteDest = editTextDest.getText().toString();

                if (remoteDest.length() == 0) {
                    try {

                        updateLog(NORMAL, "Broadcast msg: " + transferManager.broadcast(msg));
                    } catch (IOException e) {
                        updateLog(ERROR, "IOException: " + e.getMessage());
                        e.printStackTrace();
                    } catch (DeviceException e) {
                        updateLog(ERROR, "DeviceException: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        updateLog(NORMAL, "Broadcast msg except: " + remoteDest + " -> " +
                                transferManager.broadcastExcept(msg, new AdHocDevice(remoteDest)));
                    } catch (IOException e) {
                        updateLog(ERROR, "IOException: " + e.getMessage());
                        e.printStackTrace();
                    } catch (DeviceException e) {
                        updateLog(ERROR, "DeviceException: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private void updateLog(int type, String msg) {
        TextView textView = findViewById(R.id.text_log);
        SpannableString contentText = new SpannableString(textView.getText());
        String htmlText = Html.toHtml(contentText);
        switch (type) {
            case NORMAL:
                textView.setText(Html.fromHtml(htmlText + msg), TextView.BufferType.EDITABLE);
                break;
            case WARNING:
                textView.setText(Html.fromHtml(htmlText + "<font color='blue'>" + msg + "</font>"), TextView.BufferType.EDITABLE);
                break;
            case ERROR:
            default:
                textView.setText(Html.fromHtml(htmlText + "<font color='red'>" + msg + "</font>"), TextView.BufferType.EDITABLE);
                break;
        }
    }

    private void checkButtonClick() {

        Button myButton = findViewById(R.id.button_connect);
        myButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                ArrayList<SelectedDevice> devices = dataAdapter.getDeviceList();
                for (int i = 0; i < devices.size(); i++) {
                    SelectedDevice device = devices.get(i);
                    if (device.isSelected()) {
                        try {
                            transferManager.connect(2, device);
                        } catch (DeviceException e) {
                            e.printStackTrace();
                            updateLog(ERROR, "DeviceException: " + e.getMessage());
                        }
                    }
                }
            }
        });

    }

    @Override
    protected void onStop() {
        try {
            transferManager.stopListening();
        } catch (IOException e) {
            e.printStackTrace();
            updateLog(ERROR, "IOException exception" + e.getMessage());
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            transferManager.stopListening();
        } catch (IOException e) {
            e.printStackTrace();
            updateLog(ERROR, "IOException exception" + e.getMessage());
        }
        super.onDestroy();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void accessLocationPermission() {
        int accessCoarseLocation = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int accessFineLocation = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);

        List<String> listRequestPermission = new ArrayList<String>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            requestPermissions(strRequestPermission, REQUEST_CODE_LOC);
        } else {
            try {
                transferManager.discovery(listenerDiscovery);
            } catch (DeviceException e) {
                updateLog(ERROR, "Device exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOC:
                if (grantResults.length > 0) {
                    for (int gr : grantResults) {
                        // Check if request is granted or not
                        if (gr != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }

                    try {
                        transferManager.discovery(listenerDiscovery);
                    } catch (DeviceException e) {
                        updateLog(ERROR, "Device exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                break;
            default:
                return;
        }
    }

}
