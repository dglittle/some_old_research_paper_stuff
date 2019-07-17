package sloptester;

import java.util.Set;
import java.util.Vector;

import org.eclipse.jdt.core.dom.ITypeBinding;

import deslopper2.Score;

public class Type implements Comparable {
    public ITypeBinding binding;
    public Set<Type> subTypes;
    public boolean isJavaLangObject;
    
    // algorithm information
    public Score[][] score;
    public Vector<Score> nextScore;
    
    public Type(ITypeBinding binding) {
        this.binding = binding;
    }
    
    public String toString() {
        return binding.getQualifiedName();
    }
    
    public int compareTo(Object o) {
    	Type t = (Type)o;
    	return new Integer(subTypes.size()).compareTo(t.subTypes.size());
    }
}
