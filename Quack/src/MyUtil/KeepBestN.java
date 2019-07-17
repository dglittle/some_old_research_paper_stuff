/**
 * 
 */
package MyUtil;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class KeepBestN<T extends Comparable> extends
        PriorityQueue<T> {
    public int n;

    public KeepBestN(int n) {
        this.n = n;
    }

    public boolean add(T item) {
        if (size() < n) {
        } else if (item.compareTo(peek()) > 0) {
            remove();
        } else {
            return false;
        }
        return super.add(item);
    }

    public List<T> sorted() {
        Vector<T> list = new Vector(this);
        Collections.sort(list, Collections.reverseOrder());
        return list;
    }
}