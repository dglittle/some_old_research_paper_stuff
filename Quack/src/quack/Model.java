package quack;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import MyUtil.Bag;
import MyUtil.MyMap;
import MyUtil.UU;

public class Model {

    public IPackageBinding packageBinding;

    // Type stuff

    public Map<String, Type> keyToType = new HashMap<String, Type>();

    public Set<String> processedTypes = new HashSet<String>();

    MyMap<String, Vector<Type>> genericToParameterized;

    public List<Type> types;

    Type objectType;

    Type voidType;

    Type intType;

    // Func stuff

    public Map<String, Vector<Func>> funcGroups = new MyMap(Vector.class);

    public Map<String, Set<Func>> overrideHelper = new MyMap(HashSet.class);

    // Misc stuff

    public Bag<String> functionCallCounts;

    public Type getType(ITypeBinding binding) {
        if (binding == null)
            return null;
        String key = binding.getKey();
        Type t = keyToType.get(key);

        if (t == null) {
            t = new Type(binding);
            keyToType.put(key, t);
        }
        return t;
    }

    public String getTypeNameWithoutParams(ITypeBinding b) {
        UU.match("^(\\w|\\.)*", b.getQualifiedName());
        return UU.m.group(0);
    }

    public void addSuperTypes(Type t) {
        {
            ITypeBinding b = t.binding.getSuperclass();
            if (b != null) {
                Type tt = keyToType.get(b.getKey());
                if (tt != null && tt != t) {
                    t.superTypes.add(tt);
                }
            }
        }
        {
            ITypeBinding b = t.binding.getErasure();
            if (b != null) {
                Type tt = keyToType.get(b.getKey());
                if (tt != null && tt != t) {
                    t.superTypes.add(tt);
                }
            }
        }
        {
            for (ITypeBinding b : t.binding.getInterfaces()) {
                Type tt = keyToType.get(b.getKey());
                if (tt != null && tt != t) {
                    t.superTypes.add(tt);
                }
            }
        }

        if (t.binding.isParameterizedType()) {
            for (Type tt : genericToParameterized
                    .get(getTypeNameWithoutParams(t.binding))) {
                if (tt != t) {
                    if (t.binding.isAssignmentCompatible(tt.binding)
                            && !tt.binding.isAssignmentCompatible(t.binding)) {
                        t.superTypes.add(tt);
                    }
                }
            }
        }

        if (t.superTypes.size() == 0) {
            if (t == voidType) {
            } else if (t == objectType) {
                t.superTypes.add(voidType);
            } else {
                t.superTypes.add(objectType);
            }
        }
    }

    public void createTypeList() {
        try {
            genericToParameterized = new MyMap(Vector.class);
            types = new LinkedList<Type>();

            MyMap<String, Vector<Type>> m = new MyMap(Vector.class);
            for (Type t : keyToType.values()) {
                if (t.binding.isParameterizedType()) {
                    genericToParameterized.get(
                            getTypeNameWithoutParams(t.binding)).add(t);
                }
                types.add(t);
            }
            for (Type t : types) {
                addSuperTypes(t);
                t.subTypeCount = 0;
            }
            for (Type t : types) {
                t.addSubTypeCount();
            }
            Collections.sort(types);
        } catch (Throwable e) {
            Main.getMain().log("Model.java(createTypeList)", e);
            throw new Error(e);
        }
    }

    public void updateTypes(Set<ITypeBinding> bindings) {
        for (ITypeBinding binding : bindings) {
            String key = binding.getKey();
            Vector<Func> funcs = funcGroups.remove(key);
            if (funcs != null) {
                processedTypes.remove(key);
                for (Func f : funcs) {
                    if (f.method) {
                        overrideHelper.get(f.name).remove(f);
                    }
                }
                processType(binding, 0);
            }
        }
    }

    public boolean accessible(int mods, boolean samePackage) {
        if (Modifier.isPublic(mods))
            return true;
        if (Modifier.isProtected(mods))
            return samePackage;
        if (Modifier.isPrivate(mods))
            return false;
        // package-private (no modifiers)
        return samePackage;
    }

    public void processType(ITypeBinding binding, int recurs) {
        if (binding == null)
            return;

        if (binding.isArray()) {
            String key = binding.getKey();
            if (!processedTypes.add(key))
                return;
            Type thisType = getType(binding);
            Func func = new Func(null, "#array accessor", getType(binding
                    .getElementType()));
            func.params.add(thisType);
            func.hasThisParam = true;
            func.params.add(intType);
            func.add(this, key);
            return;
        }

        if (!(binding.isClass() || binding.isInterface() || binding.isEnum()))
            return;

        String key = binding.getKey();
        if (!processedTypes.add(key))
            return;
        Type thisType = getType(binding);
        String thisName = EclipseUtil.getExternalClassName(binding);
        boolean samePackage = packageBinding.isEqualTo(binding.getPackage());

        // super classes
        processType(binding.getSuperclass(), recurs);
        for (ITypeBinding i : binding.getInterfaces()) {
            processType(i, recurs);
        }

        // add fields
        for (IVariableBinding f : binding.getDeclaredFields()) {
            if (accessible(f.getModifiers(), samePackage)) {
                {
                    Func func = new Func(f, f.getName(), getType(f.getType()));
                    func.params.add(thisType);
                    func.hasThisParam = true;
                    func.add(this, key);

                    // recurs
                    if (recurs > 0) {
                        processType(f.getType(), recurs - 1);
                    }
                }

                if (Modifier.isStatic(f.getModifiers())) {
                    Func func = new Func(f, thisName + "." + f.getName(),
                            getType(f.getType()));
                    func.add(this, key);
                }
            }
        }

        // add methods
        for (IMethodBinding m : binding.getDeclaredMethods()) {
            if (accessible(m.getModifiers(), samePackage)) {

                // if this is a constructor,
                // then make sure we are either in the same package,
                // or it is for a public class
                if (m.isConstructor()) {
                    if (!samePackage
                            && !Modifier.isPublic(binding.getModifiers())) {
                        continue;
                    }
                    // also, don't add constructors for classes inside methods,
                    // (we'll add them when we walk)
                    if (binding.getDeclaringMethod() != null) {
                        continue;
                    }
                    // also also, don't add constructors for
                    // non-static-non-top-level classes
                    if (binding.getDeclaringClass() != null
                            && !Modifier.isStatic(binding.getModifiers())) {
                        continue;
                    }
                }

                ITypeBinding returnType = m.getReturnType();
                if (m.isConstructor()) {
                    returnType = binding;
                }

                if (!m.isConstructor()) {

                    boolean overrides = false;
                    Set<Func> otherFuncs = overrideHelper.get(m.getName());
                    for (Func other : otherFuncs) {
                        if (m.overrides((IMethodBinding) other.binding)) {
                            overrides = true;
                            break;
                        }
                    }
                    if (overrides) {
                        continue;
                    }

                    Func func = new Func(m, m.getName(), getType(returnType));
                    func.params.add(thisType);
                    func.hasThisParam = true;
                    for (ITypeBinding param : m.getParameterTypes()) {
                        func.params.add(getType(param));
                    }
                    func.add(this, key);

                    otherFuncs.add(func);
                }

                if (m.isConstructor() || Modifier.isStatic(m.getModifiers())) {
                    String name = null;

                    if (m.isConstructor()) {
                        name = "new " + thisName;
                    } else if (Modifier.isStatic(m.getModifiers())) {
                        name = thisName + "." + m.getName();
                    }
                    Func func = new Func(m, name, getType(returnType));
                    for (ITypeBinding param : m.getParameterTypes()) {
                        func.params.add(getType(param));
                    }
                    func.add(this, key);
                }

                // recurs
                if (recurs > 0) {
                    processType(returnType, recurs - 1);
                }
            }
        }
    }

    public void processType_local_addConstructors(ITypeBinding binding,
            String funcGroupKey) {
        if (binding == null)
            return;
        if (!(binding.isClass() || binding.isInterface() || binding.isEnum()))
            return;

        Type thisType = getType(binding);
        String thisName = EclipseUtil.getExternalClassName(binding);
        boolean samePackage = packageBinding.isEqualTo(binding.getPackage());

        // add constructors
        for (IMethodBinding m : binding.getDeclaredMethods()) {
            if (m.isConstructor() && accessible(m.getModifiers(), samePackage)) {
                String name = "new " + thisName;
                Func func = new Func(m, name, thisType);
                for (ITypeBinding param : m.getParameterTypes()) {
                    func.params.add(getType(param));
                }
                func.add(this, funcGroupKey);
            }
        }
    }

    public void processType_local(ITypeBinding binding, boolean onlyStatic,
            Set<String> processed, Set<String> onlyTheseFields,
            ITypeBinding rootBinding, String funcGroupKey) {
        if (binding == null)
            return;
        if (!(binding.isClass() || binding.isInterface() || binding.isEnum()))
            return;

        if (!processed.add(binding.getKey())) {
            return;
        }
        Type thisType = getType(binding);
        String thisName = EclipseUtil.getExternalClassName(binding);
        Type rootType = getType(rootBinding);
        boolean samePackage = packageBinding.isEqualTo(binding.getPackage());
        boolean sameClass = thisType == rootType;

        // super classes
        processType_local(binding.getSuperclass(), onlyStatic, processed,
                onlyTheseFields, rootBinding, funcGroupKey);
        for (ITypeBinding i : binding.getInterfaces()) {
            processType_local(i, onlyStatic, processed, onlyTheseFields,
                    rootBinding, funcGroupKey);
        }

        // add fields
        for (IVariableBinding f : binding.getDeclaredFields()) {
            if (onlyTheseFields == null
                    || onlyTheseFields.contains(f.getName())) {
                if (!onlyStatic || Modifier.isStatic(f.getModifiers())) {
                    Func func = new Func(f, f.getName(), getType(f.getType()));
                    func.add(this, funcGroupKey);
                }
            }

            // add accessors for
            // - protected (but in different package)
            // - private (but in same class)
            if ((Modifier.isProtected(f.getModifiers()) && !samePackage)
                    || (Modifier.isPrivate(f.getModifiers()) && sameClass)) {
                Func func = new Func(f, f.getName(), getType(f.getType()));
                func.params.add(rootType);
                func.hasThisParam = true;
                func.add(this, funcGroupKey);
            }
        }

        // add methods
        for (IMethodBinding m : binding.getDeclaredMethods()) {
            if (!m.isConstructor()
                    && (!onlyStatic || Modifier.isStatic(m.getModifiers()))) {
                Func func = new Func(m, m.getName(), getType(m.getReturnType()));
                for (ITypeBinding param : m.getParameterTypes()) {
                    func.params.add(getType(param));
                }
                func.add(this, funcGroupKey);
            }

            // add accessors for
            // - protected (but in different package)
            // - private (but in same class)
            if ((Modifier.isProtected(m.getModifiers()) && !samePackage)
                    || (Modifier.isPrivate(m.getModifiers()) && sameClass)) {
                ITypeBinding returnType = m.getReturnType();
                if (m.isConstructor()) {
                    returnType = binding;
                }

                if (!m.isConstructor()) {
                    Func func = new Func(m, m.getName(), getType(returnType));
                    func.params.add(rootType);
                    func.hasThisParam = true;
                    for (ITypeBinding param : m.getParameterTypes()) {
                        func.params.add(getType(param));
                    }
                    func.add(this, funcGroupKey);
                } else {
                    Func func = new Func(m, "new " + thisName,
                            getType(returnType));
                    for (ITypeBinding param : m.getParameterTypes()) {
                        func.params.add(getType(param));
                    }
                    func.add(this, funcGroupKey);
                }
            }
        }
    }

    public Model(CompilationUnit n) throws Exception {
        packageBinding = EclipseUtil.getPackage(n);

        // add basic types
        if (true) {
            AST ast = n.getAST();

            objectType = getType(ast.resolveWellKnownType("java.lang.Object"));
            objectType.isJavaLangObject = true;
            voidType = getType(ast.resolveWellKnownType("void"));

            keyToType.put(ast.resolveWellKnownType("int").getKey(), getType(ast
                    .resolveWellKnownType("java.lang.Integer")));
            keyToType.put(ast.resolveWellKnownType("long").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Long")));
            keyToType.put(ast.resolveWellKnownType("short").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Short")));
            keyToType.put(ast.resolveWellKnownType("byte").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Byte")));
            keyToType.put(ast.resolveWellKnownType("char").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Character")));
            keyToType.put(ast.resolveWellKnownType("double").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Double")));
            keyToType.put(ast.resolveWellKnownType("float").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Float")));
            keyToType.put(ast.resolveWellKnownType("boolean").getKey(),
                    getType(ast.resolveWellKnownType("java.lang.Boolean")));

            String[] basics = new String[] { "int", "long", "short", "byte",
                    "char", "double", "float", "boolean" };
            for (String basicA : basics) {
                for (String basicB : basics) {
                    ITypeBinding typeA = ast.resolveWellKnownType(basicA);
                    ITypeBinding typeB = ast.resolveWellKnownType(basicB);
                    if (typeA.isAssignmentCompatible(typeB)
                            && !typeB.isAssignmentCompatible(typeA)) {
                        getType(typeA).superTypes.add(getType(typeB));
                    }
                }
            }

            intType = getType(ast.resolveWellKnownType("int"));

            Func f;
            f = new Func("int", getType(ast.resolveWellKnownType("int")),
                    "[-+]?(0[xX][0-9a-fA-F]+|\\d+)");
            f.add(this, "#basic");
            f = new Func("long", getType(ast.resolveWellKnownType("long")),
                    "[-+]?(0[xX][0-9a-fA-F]+|\\d+)[lL]?");
            f.add(this, "#basic");
            f = new Func("short", getType(ast.resolveWellKnownType("short")),
                    "[-+]?(0[xX][0-9a-fA-F]+|\\d+)");
            f.add(this, "#basic");
            f = new Func("byte", getType(ast.resolveWellKnownType("byte")),
                    "[-+]?(0[xX][0-9a-fA-F]+|\\d+)");
            f.add(this, "#basic");
            f = new Func("char", getType(ast.resolveWellKnownType("char")),
                    "'\\\\?.'");
            f.add(this, "#basic");
            f = new Func("double", getType(ast.resolveWellKnownType("double")),
                    "[-+]?(0[xX][0-9a-fA-F]+|(\\d+(\\.(\\d+)?)?|\\.\\d+)(?:e[-+]?\\d+)?)[dD]?");
            f.add(this, "#basic");
            f = new Func("float", getType(ast.resolveWellKnownType("float")),
                    "[-+]?(0[xX][0-9a-fA-F]+|(\\d+(\\.(\\d+)?)?|\\.\\d+)(?:e[-+]?\\d+)?)[fF]?");
            f.add(this, "#basic");
            f = new Func("boolean",
                    getType(ast.resolveWellKnownType("boolean")), "true|false");
            f.add(this, "#basic");

            f = new Func("String", getType(ast
                    .resolveWellKnownType("java.lang.String")), "\".*?\"");
            f.add(this, "#basic");
            
            f = new Func(null, "0", getType(ast.resolveWellKnownType("byte")));
            f.add(this, "#basic");            
            f = new Func(null, "'c'", getType(ast.resolveWellKnownType("char")));
            f.add(this, "#basic");            
            f = new Func(null, "0.0", getType(ast.resolveWellKnownType("float")));
            f.add(this, "#basic");
            f = new Func(null, "true", getType(ast.resolveWellKnownType("boolean")));
            f.add(this, "#basic");            
            f = new Func(null, "\"Hello\"", getType(ast.resolveWellKnownType("java.lang.String")));
            f.add(this, "#basic");            
        }
    }

    public void processTypesForAST(CompilationUnit n) {
        for (ITypeBinding b : EclipseUtil.getNamedTypes(n)) {
            processType(b, 2);
        }
        createTypeList();
    }
    
    public int getFuncCount() {
        int sum = 0;
        for (Vector<Func> fs : funcGroups.values()) {
            sum += fs.size();
        }
        return sum;
    }

    public void debugPrintToFiles() throws Exception {
        if (false) {
            {
                PrintWriter out = new PrintWriter("c:/Home/logTypes.txt");
                out.println("types...");
                for (Type t : types) {
                    out.println("T: " + t);
                    for (Type subT : t.superTypes) {
                        out.println("\t" + subT);
                    }
                }
                out.close();
            }

            {
                PrintWriter out = new PrintWriter("c:/Home/logFuncs.txt");
                out.println("funcs...");
                for (Map.Entry<String, Vector<Func>> e : funcGroups.entrySet()) {
                    out.println("----------------------");
                    out.println("group: " + e.getKey());
                    for (Func f : e.getValue()) {
                        out.println("\tT: " + f);
                    }
                }
                out.close();
            }
        }
    }

    public Bag<String> debug_getFuncNames() {
        Bag<String> bag = new Bag();
        for (Vector<Func> funcs : funcGroups.values()) {
            for (Func func : funcs) {
                bag.add(func.name);
            }
        }
        return bag;
    }
}
