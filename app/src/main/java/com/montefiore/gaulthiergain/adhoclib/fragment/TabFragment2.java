package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.OnDiscoveryCompleteListener;

public class TabFragment2 extends ListFragment implements AdapterView.OnItemClickListener {

    private BluetoothManager bluetoothManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        bluetoothManager = new BluetoothManager(getContext(), new OnDiscoveryCompleteListener() {
            public void OnDiscoveryComplete(String response) {
                Toast.makeText(getContext(), "REPONSE: " + response, Toast.LENGTH_LONG).show();
            }
        });

        View fragmentView = inflater.inflate(R.layout.fragment_tab_fragment2, container, false);

        Button button = fragmentView.findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(getContext(), "Bluetooth", Toast.LENGTH_LONG).show();

                if(bluetoothManager.isEnabled()){

                    Log.d("[AdHoc]", "Bluetooth is enabled");

                    bluetoothManager.getPairedDevices();
                    bluetoothManager.discovery();
                }else{
                    Log.d("[AdHoc]", "Bluetooth is disabled");
                    bluetoothManager.enable();
                }

            }
        });


       /* final String[] items = new String[bluetoothManager.getHashMapBluetoothDevice().size() + 1];
        for (int i = 0; i < bluetoothManager.getHashMapBluetoothDevice().size(); i++) {
            items[i] = bluetoothManager.getHashMapBluetoothDevice().get(i).getName();
        }*/

        String[] items = {"salut", "test"};

        final ArrayAdapter<String> aa = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, items);
        setListAdapter(aa);


        return fragmentView;
    }

    @Override
    public void onDestroy() {
        Log.d("[AdHoc]", "On Destroy");
        super.onDestroy();
        bluetoothManager.unregisterDiscovery();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        // Cancel discovery
        bluetoothManager.cancelDiscovery();

        // Unregister Discovery
        bluetoothManager.unregisterDiscovery();

        String info = ((TextView) v).getText().toString();
        Toast.makeText(getContext(), "info: " + info, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}