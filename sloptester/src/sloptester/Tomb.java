package sloptester;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import MyUtil.Bag;
import MyUtil.Pair;
import MyUtil.u;

public class Tomb {

    public IPackageBinding packageBinding;

    public Map<String, Type> keyToType = new HashMap<String, Type>();

    public Vector<Func> funcs = new Vector<Func>();

    public Map<String, Func> keyToFunc = new HashMap<String, Func>();

    public Set<String> processedTypes = new HashSet<String>();

    public Set<String> processedTypes_Static = new HashSet<String>();
    
    public Bag<String> wordFreqs = new Bag<String>();
    
    public void calcWordFreqs() {
    	for (Func f : funcs) {
    		for (String word : f.ident.words) {
    			wordFreqs.add(word);
    		}
    	}
    }
    
    public String mostUniqueWord(Collection<String> words) {
    	int best = Integer.MAX_VALUE;
    	String bestWord = null;
    	for (String word : words) {
    		int freq = wordFreqs.get(word);
    		if (bestWord == null || freq < best) {
    			best = freq;
    			bestWord = word;
    		}
    	}
    	return bestWord;
    }
    
    public Vector<String> sortByFreq(List<String> words) {
    	Vector<Pair<Integer, String>> v = new Vector<Pair<Integer, String>>();
    	for (String word : words) {
    		v.add(new Pair<Integer, String>(wordFreqs.get(word), word));
    	}
    	Collections.sort(v);
    	Vector<String> v2 = new Vector<String>();
    	for (Pair<Integer, String> p : v) {
    		v2.add(p.right);
    	}
    	return v2;
    }

    public Type getType(ITypeBinding binding) {
        String key = binding.getKey();
        Type t = keyToType.get(key);
        if (t == null) {
            t = new Type(binding);
            keyToType.put(key, t);
        }
        return t;
    }

    public void printKeywordFreqs() {
        Bag<String> bag = new Bag<String>();
        for (Func f : funcs) {
            for (String s : f.ident.words) {
                bag.add(s);
            }
        }
        u.print(bag.getSortedPairs());
    }

    public Type getTypeWeak(ITypeBinding binding) {
        return keyToType.get(binding.getKey());
    }

    public void calcSubTypes() {
        for (Type t1 : keyToType.values()) {
            t1.subTypes = new HashSet<Type>();
            for (Type t2 : keyToType.values()) {
                if (t2.binding.isAssignmentCompatible(t1.binding)) {
                    t1.subTypes.add(t2);
                }
            }
        }
    }

    public Type addType_andUpdate(ITypeBinding binding) {
        if (binding == null)
            return null;
        String key = binding.getKey();
        Type t = keyToType.get(key);
        if (t == null) {
            t = new Type(binding);
            keyToType.put(key, t);

            t.subTypes = new HashSet<Type>();
            for (Type t2 : keyToType.values()) {
                if (t2.binding.isAssignmentCompatible(t.binding)) {
                    t.subTypes.add(t2);
                }
                if (t.binding.isAssignmentCompatible(t2.binding)) {
                    t2.subTypes.add(t);
                }
            }
        }
        return t;
    }

    public void processType(ITypeBinding binding, boolean doLiquid,
            boolean doStatic) {
        if (binding == null)
            return;
        String key = binding.getKey();
        if (doLiquid && !processedTypes.add(key))
            doLiquid = false;
        if (doStatic && !processedTypes_Static.add(key))
            doStatic = false;
        if (!doLiquid && !doStatic)
            return;

        Type thisType = getType(binding);
        String thisName = binding.getName();

        processType(binding.getSuperclass(), doLiquid, doStatic);
        for (ITypeBinding i : binding.getInterfaces()) {
            processType(i, doLiquid, false);
        }

        if (binding.isClass() || binding.isInterface() || binding.isEnum()) {

            boolean samePackage = packageBinding
                    .isEqualTo(binding.getPackage());

            // add fields
            for (IVariableBinding f : binding.getDeclaredFields()) {
                if (Modifier.isPublic(f.getModifiers())
                        || (samePackage && !Modifier
                                .isPrivate(f.getModifiers()))) {
                    if (doLiquid) {
                        Func func = new Func(f, f.getName(), getType(f
                                .getType()), false);
                        func.params.add(thisType);
                        func.hasThisParam = true;
                        func.add(this);
                    }

                    if (doStatic && Modifier.isStatic(f.getModifiers())) {
                        Func func = new Func(f, thisName + "." + f.getName(),
                                getType(f.getType()), false);
                        func.add(this);
                    }
                }
            }

            // add methods
            for (IMethodBinding m : binding.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())
                        || (samePackage && !Modifier
                                .isPrivate(m.getModifiers()))) {

                    ITypeBinding returnType = m.getReturnType();
                    if (m.isConstructor()) {
                        returnType = binding;
                    }

                    if (doLiquid && !m.isConstructor()) {
                        Func func = new Func(m, m.getName(),
                                getType(returnType), false);
                        func.params.add(thisType);
                        func.hasThisParam = true;
                        for (ITypeBinding param : m.getParameterTypes()) {
                            func.params.add(getType(param));
                        }
                        func.add(this);
                    }

                    if (doStatic
                            && (m.isConstructor() || Modifier.isStatic(m
                                    .getModifiers()))) {
                        String name = null;
                        
                        // we don't want constructors for inner classes,
                        // because they are difficult to deal with for now
                        if (m.isConstructor() && (m.getDeclaringClass().getDeclaringClass() != null)) {
                        	continue;
                        }
                        
                        if (m.isConstructor()) {
                            name = "new " + thisName;
                        } else if (Modifier.isStatic(m.getModifiers())) {
                            name = thisName + "." + m.getName();
                        }
                        Func func = new Func(m, name, getType(returnType),
                                false);
                        for (ITypeBinding param : m.getParameterTypes()) {
                            func.params.add(getType(param));
                        }
                        func.add(this);
                    }
                }
            }
        }
    }

    public void processType_local(ITypeBinding binding, boolean doLiquid,
            boolean doStatic, Set<String> processed) {
        if (binding == null)
            return;

        Type thisType = getType(binding);
        String thisName = binding.getName();
        String key = binding.getKey();
        if (!processed.add(key)) {
        	return;
        }

        processType_local(binding.getSuperclass(), doLiquid, doStatic, processed);
        for (ITypeBinding i : binding.getInterfaces()) {
            processType_local(i, doLiquid, false, processed);
        }
        if (binding.isClass() || binding.isInterface() || binding.isEnum()) {

            boolean samePackage = packageBinding
                    .isEqualTo(binding.getPackage());

            // add fields
            for (IVariableBinding f : binding.getDeclaredFields()) {
                if (Modifier.isPublic(f.getModifiers())
                        || (samePackage && !Modifier
                                .isPrivate(f.getModifiers()))) {
                    if (doLiquid
                            || (doStatic && Modifier.isStatic(f.getModifiers()))) {
                        Func func = new Func(f, f.getName(), getType(f
                                .getType()), true);
                        func.bonus = 0.001;
                        func.add(this);
                    }
                }
            }

            // add methods
            for (IMethodBinding m : binding.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers())
                        || (samePackage && !Modifier
                                .isPrivate(m.getModifiers()))) {
                    if (!m.isConstructor()
                            && (doLiquid || (doStatic && Modifier.isStatic(m
                                    .getModifiers())))) {
                        Func func = new Func(m, m.getName(), getType(m
                                .getReturnType()), true);
                        for (ITypeBinding param : m.getParameterTypes()) {
                            func.params.add(getType(param));
                        }
                        func.bonus = 0.001;
                        func.add(this);
                    }
                }
            }
        }
    }

    public Tomb(CompilationUnit n) throws Exception {
        for (Object o : n.types()) {
            TypeDeclaration td = (TypeDeclaration) o;
            ITypeBinding b = td.resolveBinding();
            if (packageBinding == null) {
                packageBinding = b.getPackage();
            }
            processType(b, true, true);
        }

        Set<ITypeBinding> namedTypes = EclipseUtil.getNamedTypes(n);

        for (ITypeBinding b : namedTypes) {
            processType(b, false, true);
        }

        for (int i = funcs.size() - 1; i >= 0; i--) {
            Func f = funcs.get(i);
            processType(f.returnType.binding, true, false);
        }

        for (ITypeBinding b : namedTypes) {
            processType(b, true, false);
        }

        for (int i = funcs.size() - 1; i >= 0; i--) {
            Func f = funcs.get(i);
            processType(f.returnType.binding, true, false);
        }

        calcSubTypes();

        Type t = keyToType.get("Ljava/lang/Object;");
        if (t != null) {
            t.isJavaLangObject = true;
        }
        
        calcWordFreqs();
    }

    public void debugPrintToFiles() throws Exception {
        if (true) {
            {
                PrintWriter out = new PrintWriter("c:/Home/logTypes.txt");
                out.println("types...");
                for (Type t : keyToType.values()) {
                    out.println("T: " + t);
                    for (Type subT : t.subTypes) {
                        out.println("\t" + subT);
                    }
                }
                out.close();
            }

            // ... write the funcs out to a file
            {
                PrintWriter out = new PrintWriter("c:/Home/logFuncs.txt");
                out.println("funcs...");
                for (Func f : funcs) {
                    out.println(f);
                    out.println("\t" + f.binding.getKey() + "\n");
                }
                out.close();
            }
        }
    }
}
