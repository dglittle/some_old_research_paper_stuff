package quack;

import java.util.Vector;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;


public class Func {
    public IBinding binding;

    public String key;
    
    public String name;

    public Ident ident;

    public Type returnType;

    public Vector<Type> params;

    public boolean hasThisParam = false;
    
    public boolean method = false;
    
    public boolean literal = false;
    
    public int contextDist = 0;

    // algorithm information
    public Score score;
    
    public Func(String basicType, Type returnType, String regex) {
        this.binding = null;
        this.name = basicType;
        this.returnType = returnType;
        this.params = new Vector<Type>();
        this.ident = new Ident(basicType);
        this.ident.regex = regex;
        this.literal = true;
    }

    public Func(IBinding binding, String name, Type returnType) {
        this.binding = binding;
        if (binding != null) {
            this.key = binding.getKey();
            this.method = binding instanceof IMethodBinding;
        }
        this.name = name;
        this.returnType = returnType;
        this.params = new Vector<Type>();
        ident = new Ident(name);
    }

    public void add(Model model, String group) {
        model.funcGroups.get(group).add(this);
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
}
