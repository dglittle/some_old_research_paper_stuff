package sloptester;

import java.util.Vector;

import org.eclipse.jdt.core.dom.IBinding;

import deslopper2.Score;

public class Func {
    public IBinding binding;

    public String name;

    public Type returnType;

    public Vector<Type> params;

    public boolean hasThisParam = false;
    public boolean local;

    public String key;

    // algorithm information
    public Score score;

    public double bonus;

    public Ident ident;

    public Func(IBinding binding, String name, Type returnType, boolean local) {
        this.binding = binding;
        this.name = name;
        this.returnType = returnType;
        this.params = new Vector<Type>();
        
        this.local = local;

        ident = new Ident(name);
    }

    public void add(Tomb tomb) {
        tomb.funcs.add(this);
        register(tomb);
    }
    
    public void register(Tomb tomb) {
        makeKey();
        if (tomb.keyToFunc.put(key, this) != null) {
            throw new IllegalArgumentException("already registered!: " + this + "\nkey: " + key);
        }
    }

    public void unregister(Tomb tomb) {
        if (key == null) {
            throw new IllegalArgumentException("no key made for: " + this);
        }
        tomb.keyToFunc.remove(key);
    }
    
    public boolean isRegistered(Tomb tomb) {
        makeKey();
        return isRegistered(tomb, key);
    }
    
    public static boolean isRegistered(Tomb tomb, String key) {
        return tomb.keyToFunc.get(key) != null;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(returnType);
        sb.append(" ");
        sb.append(name);
        sb.append("(");
        boolean first = true;
        for (Type t : params) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(t);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    public void makeKey() {
        if (key != null) return;
        key = makeKey(binding, hasThisParam, local);
    }
    
    public static String makeKey(IBinding binding, boolean hasThis, boolean local) {
        StringBuffer sb = new StringBuffer();
        if (hasThis) {
            sb.append(".");
        } else {
            sb.append("_");
        }
        if (local) {
            sb.append("$");
        } else {
            sb.append("_");
        }
        sb.append(binding.getKey());
        return sb.toString();
    }
}
