package dev.roam.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.TextView;

final class Ui {
    static int dp(Activity a, float n) { return (int)(a.getResources().getDisplayMetrics().density*n+.5f); }
    static GradientDrawable surface(int color, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(22);
        if (stroke != 0) d.setStroke(4, stroke); return d;
    }
    static void focusable(View v) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, surface(0xff365b80, Color.WHITE));
        s.addState(new int[]{android.R.attr.state_pressed}, surface(0xff365b80, Color.WHITE));
        s.addState(new int[]{android.R.attr.state_selected}, surface(0xff365b80, Color.WHITE));
        s.addState(new int[]{}, surface(0xff233349, 0)); v.setBackground(s); v.setFocusable(true);
    }
    static TextView text(Activity a, String value, int size) {
        TextView t = new TextView(a); t.setText(value); t.setTextSize(size); t.setTextColor(Color.WHITE); return t;
    }
    static void immersive(Activity a) {
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
