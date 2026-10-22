package dev.roam.launcher;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.ArrayDeque;

/** Shows complete gestures, including short synthetic swipes from vendor remotes. */
public class DiagnosticActivity extends Activity {
    private TextView log;
    private final ArrayDeque<String> lines=new ArrayDeque<>();
    private String header="";
    private float startX,startY,lastX,lastY;
    private long started,lastHover;
    private int moves,device,source;
    private boolean tracking;
    @Override public void onCreate(Bundle state){
        super.onCreate(state); Ui.immersive(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        int pad=Ui.dp(this,16);root.setPadding(pad,pad,pad,pad);
        root.addView(Ui.text(this,"Remote test 0.2 — complete gestures",24));
        root.addView(Ui.text(this,"Cursor OFF: press Right, Left, Up, Down, OK slowly, in that order. Latest result is at the top.",14));
        LinearLayout controls=new LinearLayout(this);
        Button clear=new Button(this);clear.setText("Clear results");
        Button copy=new Button(this);copy.setText("Copy report");
        controls.addView(clear,new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(copy,new LinearLayout.LayoutParams(0,-2,1));root.addView(controls);
        GridLayout probes=new GridLayout(this);probes.setColumnCount(3);
        Button[] buttons=new Button[6];
        for(int i=0;i<6;i++){
            final int n=i; Button b=new Button(this);buttons[i]=b;b.setText("Tile "+(i+1));b.setId(View.generateViewId());Ui.focusable(b);
            b.setFocusableInTouchMode(true);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=Ui.dp(this,48);
            lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(4,4,4,4);probes.addView(b,lp);
            b.setOnClickListener(v->record("ACTIVATE Tile "+(n+1)));
            b.setOnFocusChangeListener((v,focus)->{if(focus)record("FOCUS Tile "+(n+1));});
        }
        for(int i=0;i<6;i++){
            buttons[i].setNextFocusLeftId(buttons[i%3==0?i:i-1].getId());
            buttons[i].setNextFocusRightId(buttons[i%3==2?i:i+1].getId());
            buttons[i].setNextFocusUpId(buttons[i<3?i:i-3].getId());
            buttons[i].setNextFocusDownId(buttons[i>=3?i:i+3].getId());
        }
        root.addView(probes);
        header="Model="+android.os.Build.MODEL+" Android="+android.os.Build.VERSION.RELEASE+" API="+android.os.Build.VERSION.SDK_INT;
        root.addView(Ui.text(this,header,12));
        log=Ui.text(this,"",14);log.setTypeface(android.graphics.Typeface.MONOSPACE);
        ScrollView scroll=new ScrollView(this);scroll.setFocusable(false);scroll.addView(log);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        clear.setOnClickListener(v->{lines.clear();tracking=false;record("READY — Right, Left, Up, Down, OK");});
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Remote diagnostics",header+"\n"+android.text.TextUtils.join("\n",lines)));Toast.makeText(this,"Report copied",Toast.LENGTH_SHORT).show();});
        setContentView(root);buttons[0].requestFocus();record("READY — Right, Left, Up, Down, OK");
    }
    private void record(String value){
        android.util.Log.i("RoamKeys",value);lines.addFirst(value);while(lines.size()>50)lines.removeLast();
        if(log!=null)log.setText(android.text.TextUtils.join("\n",lines));
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        String action=e.getAction()==KeyEvent.ACTION_DOWN?"DOWN":e.getAction()==KeyEvent.ACTION_UP?"UP":"MULTIPLE";
        record("KEY "+action+" "+KeyEvent.keyCodeToString(e.getKeyCode())+" repeat="+e.getRepeatCount()+" device="+e.getDeviceId());
        return super.dispatchKeyEvent(e);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            if(tracking)finishGesture("INCOMPLETE",lastX,lastY,e.getEventTime());
            startX=lastX=e.getX();startY=lastY=e.getY();started=e.getEventTime();moves=0;device=e.getDeviceId();source=e.getSource();tracking=true;
            record("START device="+device+" at "+(int)startX+","+(int)startY);
        }else if(tracking){
            lastX=e.getX();lastY=e.getY();
            if(action==MotionEvent.ACTION_MOVE){
                moves+=1+e.getHistorySize();
                // Keep the newest movement visible even if firmware never sends UP.
                String live="MOVE device="+device+" dx="+(int)(lastX-startX)+" dy="+(int)(lastY-startY);
                if(!lines.isEmpty() && lines.peekFirst().startsWith("MOVE "))lines.removeFirst();
                record(live);
            }else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL){
                finishGesture(action==MotionEvent.ACTION_UP?"END":"CANCEL",lastX,lastY,e.getEventTime());
            }
        }else record("TOUCH "+MotionEvent.actionToString(action)+" device="+e.getDeviceId());
        return super.dispatchTouchEvent(e);
    }
    private void finishGesture(String status,float x,float y,long time){
        record(status+" device="+device+" source=0x"+Integer.toHexString(source)
            +" from "+(int)startX+","+(int)startY+" to "+(int)x+","+(int)y
            +" dx="+(int)(x-startX)+" dy="+(int)(y-startY)+" moves="+moves+" ms="+(time-started));
        tracking=false;
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){
        long now=android.os.SystemClock.uptimeMillis();
        if(e.getActionMasked()==MotionEvent.ACTION_SCROLL || now-lastHover>1000){
            lastHover=now;record("POINTER "+MotionEvent.actionToString(e.getAction())+" device="+e.getDeviceId()+" scroll="+e.getAxisValue(MotionEvent.AXIS_VSCROLL));
        }
        return super.dispatchGenericMotionEvent(e);
    }
}
