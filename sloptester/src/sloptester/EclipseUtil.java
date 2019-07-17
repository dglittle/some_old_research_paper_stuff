package sloptester;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import MyUtil.u;

public class EclipseUtil {

    public static void test() {
        u.debug("blah!");
    }
    
    public static int loc(ICompilationUnit unit) throws Exception {
    	String s = unit.getBuffer().getContents();
    	Matcher m = Pattern.compile("(?m)\\S.*$").matcher(s);
    	int count = 0;
    	while (m.find()) {
    		count++;
    	}
    	return count;
    }

    public static Set<ITypeBinding>
    getNamedTypes(CompilationUnit n) {
        class MyASTVisitor extends ASTVisitor {
            public Set<ITypeBinding> typeBindings = new HashSet<ITypeBinding>();

            public void preVisit(ASTNode node) {
                if (node instanceof SimpleName) {
                    IBinding b = ((SimpleName) node).resolveBinding();
                    if (b instanceof ITypeBinding) {
                        typeBindings.add((ITypeBinding) b);
                    }
                }
                if (node instanceof Type) {
                    ITypeBinding b = ((Type) node).resolveBinding();
                    typeBindings.add((ITypeBinding) b);
                }
            }
        }
        MyASTVisitor v = new MyASTVisitor();
        n.accept(v);
        return v.typeBindings;
    }

    public static String getKeyConsistentWithAutoComplete(ITypeBinding binding) {
        String key = binding.getKey();
        key = key.replaceAll("/", ".");
        key = key.replaceAll("\\w+~", "");
        return key;
    }

    public static boolean isBadMethod(IMethodBinding b) {
        if (b.isGenericMethod() || b.isVarargs()) {
            return true;
        }
        return false;
    }

    public static CompilationUnit compile(ICompilationUnit unit, int offset) {
        ASTParser p = ASTParser.newParser(AST.JLS3);
        p.setResolveBindings(true);
        p.setStatementsRecovery(true);
        p.setSource(unit);
        if (offset > 0) {
            p.setFocalPosition(offset);
        }
        return (CompilationUnit) p.createAST(null);
    }

    public static boolean hasErrors(CompilationUnit n) {
        for (IProblem p : n.getProblems()) {
            if (p.isError()) {
                return true;
            }
        }
        return false;
    }

    public static ITypeBinding getHoleType(ASTNode node) {
        ASTNode parent = node.getParent();
        if (parent instanceof WhileStatement) {
            WhileStatement a = (WhileStatement) parent;
            if (a.getExpression() == node) {
                return a.getAST().resolveWellKnownType("boolean");
            }
        }
        if (parent instanceof ForStatement) {
            ForStatement a = (ForStatement) parent;
            if (a.getExpression() == node) {
                return a.getAST().resolveWellKnownType("boolean");
            }
        }
        if (parent instanceof MethodInvocation) {
            MethodInvocation a = (MethodInvocation) parent;
            for (int i = 0; i < a.arguments().size(); i++) {
                if (node == a.arguments().get(i)) {
                    return a.resolveMethodBinding().getParameterTypes()[i];
                }
            }
        }
        if (parent instanceof ClassInstanceCreation) {
            ClassInstanceCreation a = (ClassInstanceCreation) parent;
            for (int i = 0; i < a.arguments().size(); i++) {
                if (node == a.arguments().get(i)) {
                    return a.resolveConstructorBinding()
                            .getParameterTypes()[i];
                }
            }
        }
        if (parent instanceof VariableDeclarationFragment) {
            VariableDeclarationFragment a = (VariableDeclarationFragment) parent;
            if (a.getInitializer() == node) {
                return a.resolveBinding().getType();
            }
        }
        if (parent instanceof Assignment) {
            Assignment a = (Assignment) parent;
            String op = a.getOperator().toString();
            if (op.equals("=")) {
                if (a.getRightHandSide() == node) {
                    return a.resolveTypeBinding();
                }
            }
        }
        if (parent instanceof PostfixExpression) {
            PostfixExpression a = (PostfixExpression) parent;
            return a.getAST().resolveWellKnownType("long");
        }
        if (parent instanceof PrefixExpression) {
            PrefixExpression a = (PrefixExpression) parent;
            String op = a.getOperator().toString();
            if (op.equals("!")) {
                return a.getAST().resolveWellKnownType("boolean");
            } else if (op.equals("~")) {
                return a.getAST().resolveWellKnownType("long");
            } else {
                return a.getAST().resolveWellKnownType("double");
            }
        }
        if (parent instanceof InfixExpression) {
            InfixExpression a = (InfixExpression) parent;
            String op = a.getOperator().toString();
            if (op.equals("+")) {
            } else {
                String type = a.resolveTypeBinding().getQualifiedName();
                if (op.matches("\\^|\\&|<<|>>|>>>|\\|")) {
                    return a.getAST().resolveWellKnownType("long");
                }
                if (op.matches("\\*|/|\\%|\\-|<|>|<=|>=")) {
                    return a.getAST().resolveWellKnownType("double");
                }
                if (op.matches("&&|\\|\\|")) {
                    return a.getAST().resolveWellKnownType("boolean");
                }
            }
        }

        return null;
    }

    public static CompilationUnit compile(ICompilationUnit unit) {
        return compile(unit, -1);
    }

    public static String getName(ICompilationUnit cu) {
        return cu.getResource().getName();
    }
}
