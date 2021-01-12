package com.example.wifiawarep2p;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class UsersAdapter extends ArrayAdapter<Chat> {
    public UsersAdapter(Context context, ArrayList<Chat> users) {
        super(context, 0, users);
    }

    //inner class to hold views for each row
    public class ViewHolder{
        TextView name;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Chat user = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.chat_list_elements, parent, false);
        }


        // viewholder object
        final ViewHolder holder = new ViewHolder();
        holder.name = convertView.findViewById(R.id.tvUserName);
        holder.name.setText(user.name);

        // Lookup view for data population
       // TextView tvName = (TextView) convertView.findViewById(R.id.tvUserName);
        // Populate the data into the template view using the data object
        //tvName.setText(user.name);
        // Return the completed view to render on screen
        return convertView;
    }


}