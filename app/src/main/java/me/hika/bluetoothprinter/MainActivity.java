package me.hika.bluetoothprinter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.hika.bluetoothprinter.activity.DeviceListActivity;


public class MainActivity extends AppCompatActivity {
    private Button btnPair,btnPrint,btnEnable;
    private TextView tv_status;
    private ProgressDialog progressDialog;
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;

    public static final int PERMISSION_BLUETOOTH = 1;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        btnEnable = findViewById( R.id.btnEnable );
        btnPair = findViewById( R.id.btnPair );
        btnPrint = findViewById( R.id.btnPrint );
        tv_status = findViewById( R.id.tv_status );

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        progressDialog = new ProgressDialog( MainActivity.this );

        progressDialog.setMessage( "Scanning.." );
        progressDialog.setCancelable( false );
        progressDialog.setButton( DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialogInterface, int i ) {
                dialogInterface.dismiss();
                bluetoothAdapter.cancelDiscovery();
            }
        } );

        if (bluetoothAdapter == null) {
            showUnsupported();
        } else {
            btnPair.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view ) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices == null || pairedDevices.size() == 0) {
                            bluetoothAdapter.startDiscovery();
//                        showToast("No Bluetooth Connection");
                    } else {
                        ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();

                        list.addAll(pairedDevices);

                        Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                        intent.putParcelableArrayListExtra("device.list", list);

                        startActivity(intent);
                    }
                }
            } );
            btnEnable.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view ) {
                    if(bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.disable();
                        showDisabled();
                    }else {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, 1000);
                    }
                }
            } );
            btnPrint.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view ) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
                    } else {
                        printIt(BluetoothPrintersConnections.selectFirstPaired());
                    }
                }
            } );
            if (bluetoothAdapter.isEnabled()) {
                showEnabled();
            } else {
                showDisabled();
            }
        }


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(bluetoothReceiver, intentFilter);
    }
    private void showEnabled() {
        tv_status.setText("Bluetooth is On");
        tv_status.setTextColor( Color.BLUE);

        btnEnable.setText("Disable");
        btnEnable.setEnabled(true);

        btnPair.setEnabled(true);
        btnPrint.setEnabled(true);
    }
    private void showDisabled() {
        tv_status.setText("Bluetooth is Off");
        tv_status.setTextColor( Color.RED);

        btnEnable.setText("Enable");
        btnEnable.setEnabled(true);

        btnPair.setEnabled(false);
        btnPrint.setEnabled(false);
    }
    private void showUnsupported() {
        tv_status.setText("Bluetooth is unsupported by this device");

        btnEnable.setText("Enable");
        btnEnable.setEnabled(false);

        btnPair.setEnabled(false);
        btnPrint.setEnabled(false);
    }
    private void showToast( String message ) {
        Toast.makeText( this,message,Toast.LENGTH_SHORT ).show();
    }
    public void printIt( DeviceConnection printerConnection) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
            final EscPosPrinter printer = new EscPosPrinter(printerConnection, 203, 58f, 32);
            List<Bitmap> segments = new ArrayList<Bitmap>();
            final StringBuilder textToPrint = new StringBuilder();
            Picasso.get().load("https://dummyimage.com/600x1000/000/fff.png").into( new Target() {
                @Override
                public void onBitmapLoaded(Bitmap ImgBitmap, Picasso.LoadedFrom from) {
                    int width = ImgBitmap.getWidth();
                    int height = ImgBitmap.getHeight();
                    int pixel = 254;

                    for(int y = 0; y < height; y += pixel) {
                        Bitmap newBitmap = Bitmap.createBitmap(ImgBitmap, 0, y, width, (y + pixel >= height) ? height - y : pixel);
                        textToPrint.append("[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, newBitmap) + "</img>\n");
                    }
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });

            Log.d("PT", "getAsyncEscPosPrinter: " + textToPrint);
            printer.printFormattedText(textToPrint.toString());
        }  catch (EscPosConnectionException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle("Broken connection")
                    .setMessage(e.getMessage())
                    .show();
        } catch (EscPosParserException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle("Invalid formatted text")
                    .setMessage(e.getMessage())
                    .show();
        } catch (EscPosEncodingException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle("Bad selected encoding")
                    .setMessage(e.getMessage())
                    .show();
        } catch (EscPosBarcodeException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle("Invalid barcode")
                    .setMessage(e.getMessage())
                    .show();
        }
    }
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    showToast("Enabled");

                    showEnabled();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                bluetoothDevices = new ArrayList<BluetoothDevice>();

                progressDialog.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressDialog.dismiss();

                Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);

                newIntent.putParcelableArrayListExtra("device.list", bluetoothDevices);

                startActivity(newIntent);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!bluetoothDevices.contains( device )) {
                    bluetoothDevices.add(device);
                    showToast("Found device " + device.getName());
                }
            }
        }
    };


}