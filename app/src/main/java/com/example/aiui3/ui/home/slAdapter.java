package com.example.aiui3.ui.home;


import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.aiui3.R;
import com.example.aiui3.ui.dashboard.DashboardFragment;

import java.util.List;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

public class slAdapter extends RecyclerView.Adapter<slAdapter.MyViewHolder> {
    private List<Pair<Pair<Bitmap, Bitmap>, Bitmap>> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View cardView;
        public MyViewHolder(View v) {
            super(v);
            cardView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public slAdapter(List<Pair<Pair<Bitmap, Bitmap>, Bitmap>> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public slAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.savedcard, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int p = vh.getAdapterPosition();
                if (p!= RecyclerView.NO_POSITION) {
                    DashboardFragment.bitmap =  mDataset.get(p).first.first;
                    DashboardFragment.linesBitMap = mDataset.get(p).first.second;
                    DashboardFragment.onlyLinesBitMap = mDataset.get(p).second;
                    Toast.makeText(v.getContext(), "Image selected!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ((ImageView)holder.cardView.findViewWithTag("img")).setImageBitmap(mDataset.get(position).first.first);
        ((ImageView)holder.cardView.findViewWithTag("img2")).setImageBitmap(mDataset.get(position).first.second);


    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }


}