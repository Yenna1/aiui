package com.example.aiui3.ui.home;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aiui3.ImageSaver;
import com.example.aiui3.MainActivity;
import com.example.aiui3.R;
import com.example.aiui3.ui.dashboard.DashboardFragment;
import com.example.aiui3.ui.notifications.NotificationsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;

public class HomeFragment extends Fragment {
    ProgressBar pb;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.savedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setNestedScrollingEnabled(false);
        RecyclerView[] views = {recyclerView};

        new tTask().execute(views);
        return root;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        BottomNavigationView navView = getActivity().findViewById(R.id.nav_view);
        navView.getMenu().getItem(0).setEnabled(false);
        navView.getMenu().getItem(1).setEnabled(true);
        navView.getMenu().getItem(2).setEnabled(true);
        pb = getActivity().findViewById(R.id.pbBar);
        pb.setVisibility(View.VISIBLE);
    }
    private class tTask extends AsyncTask<RecyclerView, Void, Void> {
        RecyclerView recyclerView;
        slAdapter sl;
        tTask(){
        }
        protected Void doInBackground(RecyclerView... views) {
            recyclerView = views[0];
            ArrayList<Pair<Pair<Bitmap, Bitmap>, Bitmap>> bList = new ArrayList<>();
            File directory = new File(Environment.getExternalStorageDirectory() + "/aiuiImages");
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    Bitmap bitmap = new ImageSaver(getContext()).
                            setFileName("myImage.png").
                            setDirectoryName(f.getName()).
                            load();
                    Bitmap bitmap2 = new ImageSaver(getContext()).
                            setFileName("myImage2.png").
                            setDirectoryName(f.getName()).
                            load();
                    Bitmap bitmap3 = new ImageSaver(getContext()).
                            setFileName("myImage3.png").
                            setDirectoryName(f.getName()).
                            load();
                    bList.add(new Pair<Pair<Bitmap, Bitmap>, Bitmap>(new Pair<Bitmap, Bitmap>(bitmap, bitmap2), bitmap3));
                }
            }
            sl = new slAdapter(bList);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            recyclerView.setAdapter(sl);
            pb.setVisibility(View.INVISIBLE);
        }
    }
}
