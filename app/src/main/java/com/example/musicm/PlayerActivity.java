package com.example.musicm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.gauravk.audiovisualizer.visualizer.BarVisualizer;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.File;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {

    Button btnPlay, btnNext, btnPrev, btnFastF, btnFastR, btnRepeat;
    TextView txtSongName, txtSongStart, txtSongStop;
    ImageView playerImg;
    SeekBar seekBar;
    BarVisualizer visualizer;

    String sName;
    public static final String EXTRA_NAME = "song_name";
    static MediaPlayer mediaPlayer;
    int position;
    ArrayList<File> mySongs;
    Thread updateSeekBar;

    AnimatorSet animatorSet = new AnimatorSet();

    private static boolean repeat = false;

    private InterstitialAd mInterstitialAd ;
    private boolean adSt = false;
    private boolean homeSt = false;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            stopAnimation();
            btnPlay.setBackgroundResource(R.drawable.ic_play);
//            mediaPlayer.pause();
//            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if(visualizer != null){
            visualizer.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getSupportActionBar().setTitle("Now Playing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        btnPlay = findViewById(R.id.btn_play);
        btnNext = findViewById(R.id.btn_next);
        btnPrev = findViewById(R.id.btn_prev);
        btnFastF = findViewById(R.id.btn_fast_f);
        btnFastR = findViewById(R.id.btn_fast_r);

        btnRepeat=  findViewById(R.id.btn_repeat);
        txtSongName = findViewById(R.id.txt_player_song_name);
        txtSongStart = findViewById(R.id.txt_song_start);
        txtSongStop = findViewById(R.id.txt_song_stop);

        playerImg = findViewById(R.id.image_view);

        seekBar = findViewById(R.id.seekbar);
        visualizer = findViewById(R.id.visualizer);

        if (mediaPlayer != null) {
            //release already played media player
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        mySongs = (ArrayList) bundle.getParcelableArrayList("songs");
        String songName = i.getStringExtra("songName");
        position = bundle.getInt("pos", 0);

        txtSongName.setSelected(true);
        Uri uri = Uri.parse(mySongs.get(position).toString());
        sName = mySongs.get(position).getName();
        txtSongName.setText(sName);


        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        mediaPlayer.start();

        updateSeekBar = new Thread() {
            @Override
            public void run() {
                int totalDuration = mediaPlayer.getDuration();
                int currentPosition = 0;

                while (currentPosition < totalDuration) {
                    try {
                        sleep(400);
                        currentPosition = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPosition);


                    } catch (InterruptedException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        seekBar.setMax(mediaPlayer.getDuration());
        updateSeekBar.start();
        seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        seekBar.getThumb().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);


        //change on user seekBar moving
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        String endTime = createTime(mediaPlayer.getDuration());
        txtSongStop.setText(endTime);

        //handle the updating of current time in every second
        final Handler handler = new Handler();
        final int delay = 1000;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentTime = createTime(mediaPlayer.getCurrentPosition());
                txtSongStart.setText(currentTime);
                handler.postDelayed(this, delay);
            }
        }, delay);


        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mInterstitialAd != null && adSt == false){
                    mInterstitialAd.show(PlayerActivity.this);
                }
                adSt= false;
                if (mediaPlayer.isPlaying()) {
                    btnPlay.setBackgroundResource(R.drawable.ic_play);
                    mediaPlayer.pause();
                    stopAnimation();
                } else {
                    btnPlay.setBackgroundResource(R.drawable.ic_pause);
                    mediaPlayer.start();
                    startAnimation(playerImg);
                }


            }
        });
        // on song complete listener
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(repeat){
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    Uri u = Uri.parse(mySongs.get(position).toString());
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                    sName = mySongs.get(position).getName();
                    txtSongName.setText(sName);
                    mediaPlayer.start();
                    btnPlay.setBackgroundResource(R.drawable.ic_pause);
                    startAnimation(playerImg);
                    int audioSessionId = mediaPlayer.getAudioSessionId();
                    if(audioSessionId != -1){
                        visualizer.setAudioSessionId(audioSessionId);
                    }
                }else{
                    btnNext.performClick();
                }
            }
        });

        int audioSessionId = mediaPlayer.getAudioSessionId();
        if(audioSessionId != -1){
            visualizer.setAudioSessionId(audioSessionId);
        }
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position = ((position + 1) % mySongs.size());
                Uri u = Uri.parse(mySongs.get(position).toString());
                mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                sName = mySongs.get(position).getName();
                txtSongName.setText(sName);
                mediaPlayer.start();
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                startAnimation(playerImg);
                int audioSessionId = mediaPlayer.getAudioSessionId();
                if(audioSessionId != -1){
                    visualizer.setAudioSessionId(audioSessionId);
                }

            }
        });
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                mediaPlayer.release();
                position = ((position - 1) < 0) ? (mySongs.size() - 1) : (position - 1);
                Uri u = Uri.parse(mySongs.get(position).toString());
                mediaPlayer = MediaPlayer.create(getApplicationContext(), u);
                sName = mySongs.get(position).getName();
                txtSongName.setText(sName);
                mediaPlayer.start();
                btnPlay.setBackgroundResource(R.drawable.ic_pause);
                startAnimation(playerImg);
                int audioSessionId = mediaPlayer.getAudioSessionId();
                if(audioSessionId != -1){
                    visualizer.setAudioSessionId(audioSessionId);
                }

            }
        });
        btnFastF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {

                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 10000);

                }
            }
        });
        btnFastR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {

                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 10000);

                }
            }
        });
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeat = !repeat;
                System.out.println(repeat);

                if ((repeat == true)) {
                    btnRepeat.setBackgroundResource(R.drawable.ic_repeat);
                } else {
                    btnRepeat.setBackgroundResource(R.drawable.ic_repeat_n_enabled);
                }


            }
        });

        AdRequest interstatialAdRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", interstatialAdRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        //handle interstatialAd events
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed
                                PlayerActivity.this.mInterstitialAd = null;
                                Log.e("TAG", "The ad was dismissed.");

                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when fullscreen content failed to show.
                                mInterstitialAd = null;
                                Log.e("TAG", "The ad failed to show.");

                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                                mInterstitialAd = null;

                                Log.e("TAG", "The ad was shown.");
                            }
                        });

                        Log.e("ad load", "onAdLoaded");

                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.e("ad error", loadAdError.getMessage());

                        mInterstitialAd = null;

                    }
                });
        System.out.println("=========================="+mInterstitialAd);

    }

    public void startAnimation(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(playerImg, "rotation", 0f, 360f);
        animator.setDuration(1000);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animatorSet.playTogether(animator);
        animatorSet.start();


    }

    public void stopAnimation(){
        animatorSet.end();
        animatorSet.cancel();
    }
    public String createTime(int duration) {
        String time = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        time += min + ":";
        if (sec < 10) {
            time += "0";
        }
        time += sec;
        return time;
    }

    @Override
    protected void onPause() {
        adSt = true;
        if (mediaPlayer.isPlaying()) {
            btnPlay.setBackgroundResource(R.drawable.ic_play);
            mediaPlayer.pause();
            stopAnimation();
        } else {
            btnPlay.setBackgroundResource(R.drawable.ic_pause);
            mediaPlayer.start();
            startAnimation(playerImg);
        }
        super.onPause();
    }
}