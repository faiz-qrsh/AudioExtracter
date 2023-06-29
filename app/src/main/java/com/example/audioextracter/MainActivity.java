package com.example.audioextracter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    Button selectVideoBtn, convertBtn;
    VideoView videoView;
    ProgressDialog progressDialog;
    String videoPath, audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectVideoBtn=findViewById(R.id.selectVideoBtn);
        convertBtn=findViewById(R.id.convertBtn);
        videoView=findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        // creating the progress dialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        selectVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                startActivityForResult(intent,123);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 123) {
                if (data != null) {
                    Uri video = data.getData();
                    videoPath=getPath(video);
                    try{
                        videoView.setVideoURI(video);
                        videoView.setZOrderOnTop(true);
                        videoView.start();
                    }catch(Exception e){
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    selectVideoBtn.setVisibility(GONE);
                    convertBtn.setVisibility(VISIBLE);

                    convertBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            progressDialog.show();
                            File musicDir = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_MUSIC
                            );

                            String[] array = videoPath.split("/",0);
                            String filePrefix = array[array.length-1].replace("mp4","");
                            String fileExtn = ".mp3";
                            String yourRealPath = getPath(video);
                            File dest = new File(musicDir, filePrefix + fileExtn);

                            //add number to same names file
                            int fileNo = 0;
                            while (dest.exists()) {
                                fileNo++;
                                dest = new File(musicDir, filePrefix + fileNo + fileExtn);
                            }

                            audioPath = dest.getAbsolutePath();
                            String[] command = {"-y", "-i", yourRealPath, "-vn", "-ar", "44100", "-ac", "2", "-b:a", "256k", "-f", "mp3", audioPath};
                            executeFFmpeg(command);
                        }
                    });
                }
            }
        }
    }

    private void executeFFmpeg(String[] complexCommand) {
        try{
            FFmpeg.executeAsync(complexCommand, new ExecuteCallback() {
                @Override
                public void apply(long executionId,  int returnCode) {
                    if(returnCode==RETURN_CODE_SUCCESS){
                        progressDialog.dismiss();
                        AlertDialog.Builder alert=new AlertDialog.Builder(MainActivity.this);
                        alert.setIcon(R.drawable.ic_baseline_file_download_done_24);
                        alert.setTitle("Audio Extracted!").setMessage("Your audio file is saved to "+audioPath);
                        alert.setCancelable(false);
                        alert.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                videoView.setVideoURI(null);
                                selectVideoBtn.setVisibility(VISIBLE);
                                convertBtn.setVisibility(GONE);
                                dialog.dismiss();
                            }
                        });
                        alert.show();
                    }else{
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }catch (Exception e){
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        @SuppressLint("Recycle")
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }
}