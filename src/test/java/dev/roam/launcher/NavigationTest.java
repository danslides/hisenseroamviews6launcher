package dev.roam.launcher;
import org.junit.Test;
import static org.junit.Assert.*;
public class NavigationTest {
    @Test public void edgesDoNotWrapIntoOtherRows(){
        assertEquals(2,Navigation.next(2,10,3,22));
        assertEquals(3,Navigation.next(3,10,3,21));
        assertEquals(1,Navigation.next(1,10,3,19));
        assertEquals(9,Navigation.next(9,10,3,20));
    }
    @Test public void incompleteLastRowRemainsReachable(){
        assertEquals(9,Navigation.next(8,10,3,20));
        assertEquals(6,Navigation.next(9,10,3,19));
    }
    @Test public void everyTileIsReachableByDirections(){
        for(int cols:new int[]{3,5}) for(int count:new int[]{1,2,6,17,1001}) {
            java.util.Set<Integer> seen=new java.util.HashSet<>();java.util.ArrayDeque<Integer> queue=new java.util.ArrayDeque<>();queue.add(0);seen.add(0);
            while(!queue.isEmpty()){int p=queue.remove();for(int key=19;key<=22;key++){int n=Navigation.next(p,count,cols,key);assertTrue(n>=0 && n<count);if(seen.add(n))queue.add(n);}}
            assertEquals(count,seen.size());
        }
    }
    @Test public void emptyAndUnknownInputAreSafe(){assertEquals(0,Navigation.next(0,0,3,22));assertEquals(4,Navigation.next(4,9,3,90));}
}
