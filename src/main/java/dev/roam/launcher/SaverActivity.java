package dev.roam.launcher;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.util.ArrayList;

/** Local video playlist; no Apple footage, network access, or storage permissions required. */
public class SaverActivity extends Activity {
    private VideoView video;
    private ArrayList<String> playlist;
    private int index,position,failures;
    static ArrayList<String> readVideos(Context c){
        ArrayList<String> result=new ArrayList<>();try{JSONArray a=new JSONArray(c.getSharedPreferences("saver",0).getString("videos","[]"));for(int i=0;i<a.length();i++)result.add(a.getString(i));}catch(Exception ignored){}return result;
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);Ui.immersive(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        playlist=readVideos(this);if(playlist.isEmpty()){finish();return;}
        if(saved!=null){index=saved.getInt("index")%playlist.size();position=saved.getInt("position");}
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff000000);
        video=new VideoView(this);FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);root.addView(video,vp);
        TextView caption=Ui.text(this,"ROAM  ·  Press any key or touch to return",14);caption.setPadding(24,24,24,24);root.addView(caption,new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START));
        setContentView(root);
        video.setOnPreparedListener(mp->{mp.setVolume(0,0);if(position>0)video.seekTo(position);position=0;video.start();});
        video.setOnCompletionListener(mp->{failures=0;index=(index+1)%playlist.size();play();});
        video.setOnErrorListener((mp,what,extra)->{
            if(++failures>=playlist.size()){Toast.makeText(this,"Unable to play selected videos. Try a local H.264 MP4 file.",Toast.LENGTH_LONG).show();finish();}
            else {index=(index+1)%playlist.size();video.post(this::play);}return true;
        });
    }
    private void play(){if(!isFinishing())video.setVideoURI(Uri.parse(playlist.get(index)));}
    @Override protected void onResume(){super.onResume();if(video!=null)play();}
    @Override protected void onPause(){if(video!=null){position=video.getCurrentPosition();video.stopPlayback();}super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("index",index);out.putInt("position",video==null?position:video.getCurrentPosition());super.onSaveInstanceState(out);}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN)finish();return true;}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)finish();return true;}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_SCROLL || e.getActionMasked()==MotionEvent.ACTION_HOVER_MOVE)finish();return true;}
}
