package com.cdombroski.telecommander;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ProgressBar searchingProgressBar = findViewById(R.id.searchingProgressBar);
        searchingProgressBar.setIndeterminate(true);

        RecyclerView nearbyDevicesList = findViewById(R.id.nearbyDevicesList);
        nearbyDevicesList.setHasFixedSize(true);
        nearbyDevicesList.setLayoutManager(new LinearLayoutManager(this));
        nearbyDevicesList.setAdapter(new DeviceAdapter());
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("started");
    }

    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {
        String[] data = {"hello there", "this is a device", "kinetikos"};
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("clicked");
            }
        };

        public static class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView name;
            public MyViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.deviceName);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.nearby_device_layout, parent, false);
            v.setOnClickListener(mOnClickListener);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.name.setText(data[position]);
        }

        @Override
        public int getItemCount() {
            return data.length;
        }
    }
}
