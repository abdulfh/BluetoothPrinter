package me.hika.bluetoothprinter.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import me.hika.bluetoothprinter.R;

public class DeviceListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private List<BluetoothDevice> data;
    private OnPairButtonClickListener listener;

    static class ViewHolder {
        TextView nameTv,addressTv,pairBtn;
    }
    public DeviceListAdapter( Context context) {
        inflater = LayoutInflater.from(context);
    }
    public void setData(List<BluetoothDevice> bluetoothDevices) {
        data = bluetoothDevices;
    }

    public void setListener(OnPairButtonClickListener pairButtonClickListener) {
        listener = pairButtonClickListener;
    }

    @Override
    public int getCount() {
        return (data == null) ? 0 : data.size();
    }

    @Override
    public Object getItem( int i ) {
        return null;
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public View getView( final int position, View convertView, ViewGroup parent ) {
        ViewHolder holder;

        if (convertView == null) {
            convertView			=  inflater.inflate( R.layout.list_item_device, null);

            holder 				= new ViewHolder();

            holder.nameTv		= (TextView) convertView.findViewById(R.id.tv_name);
            holder.addressTv 	= (TextView) convertView.findViewById(R.id.tv_address);
            holder.pairBtn		= (Button) convertView.findViewById(R.id.btn_pair);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device	= data.get(position);

        holder.nameTv.setText(device.getName());
        holder.addressTv.setText(device.getAddress());

        holder.pairBtn.setText((device.getBondState() == BluetoothDevice.BOND_BONDED) ? "Unpair" : "Pair");
        holder.pairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPairButtonClick(position);
                }
            }
        });

        return convertView;
    }
    public interface OnPairButtonClickListener {
        public abstract void onPairButtonClick(int position);
    }
}
