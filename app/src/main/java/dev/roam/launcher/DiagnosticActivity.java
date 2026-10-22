package dev.roam.launcher;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.ArrayDeque;

/** Observes events delivered to this activity, without rewriting key codes. */
public class DiagnosticActivity extends Activity {
    private TextView log;
    private final ArrayDeque<String> lines=new ArrayDeque<>();
    private String header;
    private long lastMotion;
    @Override public void onCreate(Bundle state){
        super.onCreate(state); Ui.immersive(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);int pad=Ui.dp(this,20);root.setPadding(pad,pad,pad,pad);
        root.addView(Ui.text(this,"Remote laboratory",26));
        root.addView(Ui.text(this,"Turn pointer mode OFF. Test ↑ ↓ ← → and OK, then repeat with pointer mode ON. Back exits.",15));
        GridLayout probes=new GridLayout(this);probes.setColumnCount(3);
        Button[] buttons=new Button[6];
        for(int i=0;i<6;i++){
            final int n=i; Button b=new Button(this);buttons[i]=b;b.setText("Tile "+(i+1));b.setId(View.generateViewId());Ui.focusable(b);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=Ui.dp(this,64);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(4,4,4,4);probes.addView(b,lp);
            b.setOnClickListener(v->record("ACTIVATE Tile "+(n+1)));
            b.setOnFocusChangeListener((v,focus)->{if(focus)record("FOCUS Tile "+(n+1));});
        }
        for(int i=0;i<6;i++){
            buttons[i].setNextFocusLeftId(buttons[i%3==0?i:i-1].getId());
            buttons[i].setNextFocusRightId(buttons[i%3==2?i:i+1].getId());
            buttons[i].setNextFocusUpId(buttons[i<3?i:i-3].getId());
            if(i<3)buttons[i].setNextFocusDownId(buttons[i+3].getId());
        }
        root.addView(probes);
        Button copy=new Button(this);copy.setText("Copy diagnostic report");copy.setId(View.generateViewId());root.addView(copy);
        for(int i=3;i<6;i++)buttons[i].setNextFocusDownId(copy.getId());copy.setNextFocusUpId(buttons[3].getId());
        log=Ui.text(this,"",11);log.setTypeface(android.graphics.Typeface.MONOSPACE);
        ScrollView scroll=new ScrollView(this);scroll.setFocusable(false);scroll.addView(log);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        StringBuilder devices=new StringBuilder();
        for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null)devices.append("\nINPUT ").append(id).append(' ').append(d.getName()).append(" sources=0x").append(Integer.toHexString(d.getSources()));}
        header="Roam Launcher 0.1\nModel="+android.os.Build.MODEL+" manufacturer="+android.os.Build.MANUFACTURER+" Android="+android.os.Build.VERSION.RELEASE+" API="+android.os.Build.VERSION.SDK_INT+devices+"\n";
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Remote diagnostics",header+android.text.TextUtils.join("\n",lines)));Toast.makeText(this,"Report copied",Toast.LENGTH_SHORT).show();});
        setContentView(root);record("READY · expected D-pad: UP 19 / DOWN 20 / LEFT 21 / RIGHT 22 / CENTER 23");buttons[0].requestFocus();
    }
    private void record(String value){
        android.util.Log.i("RoamKeys",value);lines.addLast(value);while(lines.size()>65)lines.removeFirst();
        if(log!=null){StringBuilder b=new StringBuilder(header==null?"":header);java.util.Iterator<String> it=lines.descendingIterator();while(it.hasNext())b.append(it.next()).append('\n');log.setText(b.toString());}
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        record(KeyEvent.actionToString(e.getAction())+" "+KeyEvent.keyCodeToString(e.getKeyCode())+" code="+e.getKeyCode()+" scan="+e.getScanCode()+" repeat="+e.getRepeatCount()+" device="+e.getDeviceId()+" source=0x"+Integer.toHexString(e.getSource()));
        return super.dispatchKeyEvent(e);
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){motion(e);return super.dispatchGenericMotionEvent(e);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){motion(e);return super.dispatchTouchEvent(e);}
    private void motion(MotionEvent e){
        long now=android.os.SystemClock.uptimeMillis();if(now-lastMotion<250 && e.getActionMasked()!=MotionEvent.ACTION_DOWN)return;lastMotion=now;
        record("MOTION "+MotionEvent.actionToString(e.getAction())+" device="+e.getDeviceId()+" source=0x"+Integer.toHexString(e.getSource())+" x="+(int)e.getX()+" y="+(int)e.getY()+" scroll="+e.getAxisValue(MotionEvent.AXIS_VSCROLL));
    }
}
