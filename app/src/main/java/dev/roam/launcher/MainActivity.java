package dev.roam.launcher;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;

public class MainActivity extends Activity {
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final Handler timer = new Handler(Looper.getMainLooper());
    private GridView grid;
    private int columns, selected;
    private boolean active;
    private String restoreId;
    private final Runnable idle = () -> { if (active && !videos().isEmpty()) startActivity(new Intent(this, SaverActivity.class)); };
    private static final int PICK_VIDEO = 10;
    private static class Entry {
        final String id, title; final Drawable icon; final Runnable action;
        Entry(String id, String title, Drawable icon, Runnable action) { this.id=id; this.title=title; this.icon=icon; this.action=action; }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); Ui.immersive(this);
        if (state != null) restoreId = state.getString("selected");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int pad=Ui.dp(this,24); root.setPadding(pad,pad,pad,pad);
        root.setBackground(new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0xff182f49,0xff0c101b}));
        root.addView(Ui.text(this,"ROAM  /  HOME",14));
        TextView title=Ui.text(this,"Your screen. Your space.",30); title.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,8)); root.addView(title);
        TextView hint=Ui.text(this,"Apps & controls  ·  Swipe or use the arrow keys",14); hint.setPadding(0,0,0,Ui.dp(this,16)); root.addView(hint);
        grid=new GridView(this); grid.setId(View.generateViewId());
        columns=getResources().getConfiguration().orientation==2?5:3;
        grid.setNumColumns(columns); grid.setVerticalSpacing(Ui.dp(this,16)); grid.setHorizontalSpacing(Ui.dp(this,16));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH); grid.setClipToPadding(false);
        grid.setPadding(4,4,4,Ui.dp(this,20)); grid.setSelector(android.R.color.transparent);
        root.addView(grid,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        grid.setOnItemClickListener((p,v,pos,id)-> { selected=pos; launch(pos); });
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id) { selected=pos; }
        });
        grid.setOnKeyListener((v,key,event)-> {
            boolean arrow=key>=KeyEvent.KEYCODE_DPAD_UP && key<=KeyEvent.KEYCODE_DPAD_RIGHT;
            boolean accept=key==KeyEvent.KEYCODE_DPAD_CENTER || key==KeyEvent.KEYCODE_ENTER || key==KeyEvent.KEYCODE_NUMPAD_ENTER || key==KeyEvent.KEYCODE_BUTTON_A;
            if (!arrow && !accept) return false;
            if (event.getAction()!=KeyEvent.ACTION_DOWN) return true;
            if(accept) { if(event.getRepeatCount()==0) launch(selected); return true; }
            selected=Navigation.next(selected,entries.size(),columns,key);
            grid.setSelection(selected); grid.smoothScrollToPosition(selected); return true;
        });
    }
    private void launch(int pos) { if(pos>=0 && pos<entries.size()) { try { entries.get(pos).action.run(); } catch(Exception e) { toast("Could not open this item: "+e.getClass().getSimpleName()); } } }
    private void add(String id,String title,Runnable action){ entries.add(new Entry(id,title,null,action)); }
    private void refresh() {
        String keep=restoreId!=null?restoreId:(selected<entries.size()?entries.get(selected).id:null); restoreId=null;
        entries.clear();
        add("diagnostics","Remote test",()->startActivity(new Intent(this,DiagnosticActivity.class)));
        add("videos","Choose aerial videos",this::pickVideos);
        add("preview","Start screensaver",()->{ if(videos().isEmpty()) toast("Choose one or more aerial videos first."); else startActivity(new Intent(this,SaverActivity.class)); });
        add("idle","Idle timer: "+(getPreferences(0).getBoolean("idle",true)?"2 minutes":"off"),()->{
            getPreferences(0).edit().putBoolean("idle",!getPreferences(0).getBoolean("idle",true)).apply(); refresh(); resetTimer(); });
        add("home","Default home app",()->startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)));
        add("settings","Device settings",()->startActivity(new Intent(Settings.ACTION_SETTINGS)));
        PackageManager pm=getPackageManager(); TreeMap<String,ResolveInfo> unique=new TreeMap<>();
        // Prefer TV entry points when an app advertises both mobile and TV launch activities.
        for(String cat:new String[]{Intent.CATEGORY_LAUNCHER,Intent.CATEGORY_LEANBACK_LAUNCHER}) {
            for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat),0))
                if(!r.activityInfo.packageName.equals(getPackageName())) unique.put(r.activityInfo.packageName,r);
        }
        ArrayList<ResolveInfo> apps=new ArrayList<>(unique.values()); Collections.sort(apps,(a,b)->a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        for(ResolveInfo r:apps) {
            ComponentName c=new ComponentName(r.activityInfo.packageName,r.activityInfo.name);
            entries.add(new Entry(c.flattenToString(),r.loadLabel(pm).toString(),r.loadIcon(pm),()->startActivity(new Intent(Intent.ACTION_MAIN).setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))));
        }
        selected=0; for(int i=0;i<entries.size();i++) if(entries.get(i).id.equals(keep)) selected=i;
        grid.setAdapter(new BaseAdapter(){
            public int getCount(){return entries.size();} public Object getItem(int p){return entries.get(p);} public long getItemId(int p){return p;}
            public View getView(int p,View reuse,android.view.ViewGroup parent){
                LinearLayout tile;
                if(reuse instanceof LinearLayout) tile=(LinearLayout)reuse;
                else {
                    tile=new LinearLayout(MainActivity.this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
                    tile.setPadding(8,12,8,12); Ui.focusable(tile); tile.setFocusable(false);
                    ImageView icon=new ImageView(MainActivity.this); icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    tile.addView(icon,new LinearLayout.LayoutParams(Ui.dp(MainActivity.this,52),Ui.dp(MainActivity.this,52)));
                    TextView label=Ui.text(MainActivity.this,"",14); label.setGravity(Gravity.CENTER); label.setMaxLines(2); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    tile.addView(label,new LinearLayout.LayoutParams(-1,-2));
                }
                tile.setLayoutParams(new AbsListView.LayoutParams(-1,Ui.dp(MainActivity.this,124)));
                Entry e=entries.get(p); ((ImageView)tile.getChildAt(0)).setImageDrawable(e.icon);
                tile.getChildAt(0).setVisibility(e.icon==null?View.GONE:View.VISIBLE);
                ((TextView)tile.getChildAt(1)).setText(e.title); tile.setContentDescription(e.title); return tile;
            }
        });
        grid.requestFocus(); grid.setSelection(selected);
    }
    private void pickVideos(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(i,PICK_VIDEO); } catch(ActivityNotFoundException e){toast("No file picker is installed. Install a DocumentsProvider-compatible file manager.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data); if(request!=PICK_VIDEO || result!=RESULT_OK || data==null)return;
        ArrayList<Uri> picked=new ArrayList<>();
        if(data.getClipData()!=null) for(int i=0;i<data.getClipData().getItemCount();i++) picked.add(data.getClipData().getItemAt(i).getUri());
        else if(data.getData()!=null)picked.add(data.getData());
        JSONArray saved=new JSONArray();
        for(Uri u:picked) { try { getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION); saved.put(u.toString()); } catch(SecurityException e){toast("This file provider cannot keep video access. Choose a local file.");} }
        if(saved.length()>0) {
            // Release grants no longer used by the replacement playlist.
            for(String old:videos()) if(!picked.contains(Uri.parse(old))) try {getContentResolver().releasePersistableUriPermission(Uri.parse(old),Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
            getSharedPreferences("saver",0).edit().putString("videos",saved.toString()).apply(); toast("Saved "+saved.length()+" video(s).");
        }
    }
    ArrayList<String> videos(){return SaverActivity.readVideos(this);}
    private void toast(String t){Toast.makeText(this,t,Toast.LENGTH_LONG).show();}
    private void resetTimer(){timer.removeCallbacks(idle);if(active && getPreferences(0).getBoolean("idle",true))timer.postDelayed(idle,120000);}
    @Override public void onUserInteraction(){super.onUserInteraction();resetTimer();}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e){resetTimer();return super.dispatchGenericMotionEvent(e);}
    @Override protected void onResume(){super.onResume();active=true;refresh();resetTimer();Ui.immersive(this);}
    @Override protected void onPause(){active=false;timer.removeCallbacks(idle);super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){if(selected<entries.size())out.putString("selected",entries.get(selected).id);super.onSaveInstanceState(out);}
    @Override public void onBackPressed(){grid.requestFocus();grid.setSelection(0);selected=0;}
}
