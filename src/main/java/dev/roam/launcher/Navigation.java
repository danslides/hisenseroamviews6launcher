package dev.roam.launcher;

/** Pure navigation policy. Android D-pad key values: up=19, down=20, left=21, right=22. */
public final class Navigation {
    public static int next(int position,int count,int columns,int key) {
        if(count<=0 || columns<=0)return 0;
        int p=Math.max(0,Math.min(position,count-1));
        switch(key) {
            case 19: return p>=columns?p-columns:p;
            case 20: return (p/columns+1)*columns<count?Math.min(count-1,p+columns):p;
            case 21: return p%columns>0?p-1:p;
            case 22: return p%columns<columns-1 && p+1<count?p+1:p;
            default: return p;
        }
    }
}
