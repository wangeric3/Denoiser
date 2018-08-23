package com.wangeric3.denoiser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FileChooser extends AppCompatActivity {

    private static final String EXTRA_CHOSEN_FILE = "com.wangeric3.android.ModelTest.chosen_file";

    private Button backButton;

    File saveDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Denoiser");
    File originalDir = new File(saveDir.getAbsolutePath() + "/original");
    File cleanDir = new File(saveDir.getAbsolutePath() + "/clean");
    ArrayList<File> files = new ArrayList<>(Arrays.asList(originalDir.listFiles()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);

        backButton = (Button) findViewById(R.id.file_back);

        if (!saveDir.exists()) saveDir.mkdirs();
        if (!originalDir.exists()) originalDir.mkdirs();
        if (!cleanDir.exists()) cleanDir.mkdirs();

        ListView storedFiles = (ListView)findViewById(R.id.list);

        FileAdapter adapter = new FileAdapter(this, files);
        storedFiles.setAdapter(adapter);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                setResult(Activity.RESULT_CANCELED,data);
                finish();
            }
        });
    }

    public static String decodeIntent(Intent result){
        return result.getStringExtra(EXTRA_CHOSEN_FILE);
    }

    private class FileAdapter extends ArrayAdapter<File>{
        private ArrayList<File> list;
        private Context context;

        TextView fileName;
        ImageButton delete_item;

        public FileAdapter(Context context, ArrayList<File> list) {
            super(context, 0, files);
            this.list = list;
            this.context = context;
        }


        public View getView(final int position, View convertView, ViewGroup parent){
            View listItemView = convertView;
            if(listItemView == null){
                listItemView = LayoutInflater.from(getContext()).inflate(
                        R.layout.row_layout,parent,false
                );
            }

            final File currentFile = getItem(position);

            fileName = (TextView)listItemView.findViewById(R.id.list_file_name);
            delete_item = (ImageButton)listItemView.findViewById(R.id.delete_file);

            String[] splitFile = currentFile.getAbsolutePath().split("/");
            final String name = splitFile[splitFile.length-1];
            fileName.setText(name);

            listItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_CHOSEN_FILE, list.get(position).getAbsolutePath());
                    setResult(Activity.RESULT_OK,data);
                    finish();
                }
            });

            delete_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String cleanName = name.substring(0,name.length()-4) + "-clean.wav";
                    File cleanFile = new File(cleanName);
                    if (cleanFile.exists()) cleanFile.delete();
                    currentFile.delete();
                    remove(getItem(position));
                    notifyDataSetChanged();
                }
            });

            return listItemView;
        }

    }


}
