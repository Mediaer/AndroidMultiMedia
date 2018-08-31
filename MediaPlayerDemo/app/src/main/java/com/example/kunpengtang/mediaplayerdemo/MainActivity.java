package com.example.kunpengtang.mediaplayerdemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, MediaPlayer.OnCompletionListener{

    private boolean isStopUpdateingProgress = false;
    private EditText etPath;
    private MediaPlayer mMediaPlayer;
    private SeekBar mSeekbar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

    private final int NORMAL = 0;//闲置
    private final int PLAYING = 1;//播放中
    private final int PAUSING = 2;//暂停
    private final int STOPPING = 3;//停止

    private int currentstate = NORMAL;//播放器当前状态，默认是空闲状态
    private SurfaceHolder holder;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Checks if the app has permission to write to device storage
         * If the app does not has permission then the user will be prompted to
         * grant permissions
         * @param activity
         */
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }

        etPath = (EditText)findViewById(R.id.et_path);
        mSeekbar = (SeekBar)findViewById(R.id.sb_progress);
        tvCurrentTime = (TextView)findViewById(R.id.tv_current_time);
        tvTotalTime = (TextView)findViewById(R.id.tv_total_time);

        mSeekbar.setOnSeekBarChangeListener(this);

        SurfaceView mSurfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        holder = mSurfaceView.getHolder();//surfaceview帮助类对象

        //不是采用自己内部的双缓冲区，而是等待别人推送数据
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


    }

    public void start(View v) {
        if (mMediaPlayer != null) {
            if (currentstate == STOPPING) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
            } else if (currentstate != PAUSING) {
                mMediaPlayer.start();
                currentstate = PLAYING;
                isStopUpdateingProgress = false;//每次在调用刷新线程时，设置为false
                return;
            }
        }
        play();
    }

    public void stop(View v) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }

    private void play() {
        String path = etPath.getText().toString().trim();
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDisplay(holder);//设置播放器显示位置

            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
            mMediaPlayer.start();

            mMediaPlayer.setOnCompletionListener(this);
            currentstate = PLAYING;

            int duration = mMediaPlayer.getDuration();//总时长
            mSeekbar.setMax(duration);
            //把总时长显示在textView上
            int m = duration/1000/60;
            int s = duration/1000%60;
            tvTotalTime.setText("/"+m+":"+s);
            tvCurrentTime.setText("00:00");

            isStopUpdateingProgress = false;
            new Thread(new UpdateProgressRunnable()).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停
     */
    public void pause(View v) {
        if (mMediaPlayer != null && currentstate == PLAYING) {
            mMediaPlayer.pause();
            currentstate = PAUSING;
            isStopUpdateingProgress = true;//停止刷新主线程
        }
    }

    /**
     * 重播
     */
    public void restart(View v) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            play();
        }
    }

    /**
     * 播放完成时回调此方法
     * @param mediaPlayer
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Toast.makeText(this, "播放完毕，重新播放", 0).show();
        mediaPlayer.start();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isStopUpdateingProgress = true;//当开始拖动时，就开始停止刷新线程
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        //播放器切换到指定的进度位置上
        mMediaPlayer.seekTo(progress);
        isStopUpdateingProgress = false;
        new Thread(new UpdateProgressRunnable()).start();
    }
    //刷新进度和时间任务
    private class UpdateProgressRunnable implements Runnable {

        @Override
        public void run() {
            //每隔1秒读取一下当前正在播放的进度，设置给seekbar
            while (!isStopUpdateingProgress) {
                //得到当前进度
                int currentPosition = mMediaPlayer.getCurrentPosition();
                mSeekbar.setProgress(currentPosition);
                final int m = currentPosition/1000/60;
                final int s = currentPosition/1000%60;

                //此方法给定的runable对象，会执行主线程(UI线程中)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCurrentTime.setText(m+":"+s);
                    }
                });
                SystemClock.sleep(1000);
            }
        }
    }
}
