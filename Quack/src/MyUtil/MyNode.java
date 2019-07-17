package MyUtil;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.regex.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.nio.channels.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.jar.*;

public class MyNode<T> implements Comparable<MyNode<T>> {
    public T data;

    public MyNode<T> parent;

    public MyNode[] children;

    public MyNode(T data, int numChildren) {
        this.data = data;
        children = new MyNode[numChildren];
    }

    public void setChild(int i, MyNode<T> child) {
        children[i] = child;
        child.parent = this;
    }

    public MyNode<T> getRoot() {
        if (parent == null) {
            return this;
        } else {
            return parent.getRoot();
        }
    }

    public MyNode<T> clone(Map<MyNode<T>, MyNode<T>> map) {
        MyNode<T> c = new MyNode<T>(data, children.length);
        for (int i = 0; i < children.length; i++) {
            MyNode<T> child = children[i];
            if (child != null) {
                c.children[i] = child.clone(map);
            }
        }
        map.put(this, c);
        return c;
    }

    public int compareTo(MyNode<T> that) {
        if (data != null && data instanceof Comparable) {
            return ((Comparable) data).compareTo(that.data);
        }
        return 0;
    }

    public boolean equals(Object o) {
        return equals((MyNode<T>) o);
    }

    public boolean equals(MyNode<T> that) {
        if ((this.data == null && that.data == null)
                || this.data.equals(that.data)) {
            if (children.length != that.children.length)
                return false;
            for (int i = 0; i < children.length; i++) {
                if (!this.children[i].equals(that.children[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int getSize() {
        int size = 1;
        for (MyNode<T> child : children) {
            size += child.getSize();
        }
        return size;
    }

    public int getHeight() {
        int maxChildHeight = 0;
        for (MyNode<T> child : children) {
            int childHeight = child.getHeight() + 1;
            if (childHeight > maxChildHeight) {
                maxChildHeight = childHeight;
            }
        }
        return maxChildHeight;
    }

    public int getWidth() {
        int maxChildWidth = 1;
        for (MyNode<T> child : children) {
            int childWidth = child.getWidth();
            if (childWidth > maxChildWidth) {
                maxChildWidth = childWidth;
            }
        }
        return Math.max(maxChildWidth, children.length);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        toStringHelper(0, buf);
        return buf.toString();
    }

    public void toStringHelper(int indent, StringBuffer buf) {
        for (int i = 0; i < indent; i++) {
            buf.append("    ");
        }
        buf.append("(" + hashCode() + ") " + data);
        buf.append("\n");

        for (MyNode<T> child : children) {
            if (child != null) {
                child.toStringHelper(indent + 1, buf);
            } else {
                for (int i = 0; i < indent + 1; i++) {
                    buf.append("    ");
                }
                buf.append("<null>\n");
            }
        }
    }
}
