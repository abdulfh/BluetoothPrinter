package me.hika.bluetoothprinter.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

import me.hika.bluetoothprinter.R;
import me.hika.bluetoothprinter.adapter.DeviceListAdapter;

public class DeviceListActivity extends AppCompatActivity {
    private ListView listView;
    private DeviceListAdapter deviceListAdapter;
    private ArrayList<BluetoothDevice> deviceList;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_device_list );

        deviceList = getIntent().getExtras().getParcelableArrayList( "device.list" );
        listView = findViewById( R.id.lv_paired );

        deviceListAdapter = new DeviceListAdapter( this );
        deviceListAdapter.setData( deviceList );
        deviceListAdapter.setListener( new DeviceListAdapter.OnPairButtonClickListener() {
            @Override
            public void onPairButtonClick( int position ) {
                BluetoothDevice device = deviceList.get( position );
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    unpairDevice(device);
                } else {
                    showToast("Pairing...");

                    pairDevice(device);
                }
            }
        } );

        listView.setAdapter( deviceListAdapter );
        registerReceiver( pairReceiver,new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED) );
    }
    private void pairDevice( BluetoothDevice device ) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showToast( String message ) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void unpairDevice( BluetoothDevice device ) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        public void onReceive( Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state 		= intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState	= intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    showToast("Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    showToast("Unpaired");
                }

                deviceListAdapter.notifyDataSetChanged();
            }
        }
    };
}