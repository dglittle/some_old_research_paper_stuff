package quack;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class Type implements Comparable<Type> {
    public ITypeBinding binding;

    public Set<Type> superTypes = new HashSet();

    public boolean isJavaLangObject;

    public int subTypeCount = 0;

    // algorithm information
    public Score[][] score;

    public Type(ITypeBinding binding) {
        this.binding = binding;
    }

    public String toString() {
        String s = binding.getQualifiedName();
        if ((s == null) || (s.length() == 0))
            s = binding.getKey();
        return s;
    }

    public int compareTo(Type that) {
        return this.subTypeCount - that.subTypeCount;
    }

    public void addSubTypeCount() {
        subTypeCount++;
        for (Type t : superTypes) {
            t.addSubTypeCount();
        }
    }
}
