package quack;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import MyUtil.Bag;
import MyUtil.UU;

public class EclipseUtil {

	public static boolean isMethodClass(ITypeBinding b) {
		IMethodBinding parentMethod = b.getDeclaringMethod();
		ITypeBinding parentType = b.getDeclaringClass();
		return parentMethod != null
				&& parentMethod.getDeclaringClass().isEqualTo(parentType);
	}

	public static void getExternalClassName_helper(ITypeBinding b,
			IMethodBinding root, StringBuffer buf) {

		ITypeBinding parentClass = b.getDeclaringClass();
		IMethodBinding parentMethod = b.getDeclaringMethod();
		if (parentClass != null) {
			if (root == null
					|| root.isEqualTo(parentClass.getDeclaringMethod())) {
				getExternalClassName_helper(parentClass, root, buf);
				buf.append(".");
			}
		}
		buf.append(b.getName());
	}

	public static String getExternalClassName(ITypeBinding b) {
		StringBuffer buf = new StringBuffer();
		getExternalClassName_helper(b, b.getDeclaringMethod(), buf);
		return buf.toString();
	}

	public static int determineKeywordsStart(String s) throws Exception {
		int start = 0;
		String regex = Ident.keywordRegex;
		l1: for (int i = 0; i < s.length(); i++) {
			String quack = s.substring(i);
			int lastIndex = 0;
			boolean firstTime = true;
			start = -1;

			Matcher m = Pattern.compile(regex).matcher(quack);
			while (m.find()) {
				if (m.start(0) != lastIndex) {
					continue l1;
				}
				lastIndex = m.end(0);

				String token = m.group(1);
				if (firstTime) {
					firstTime = false;
					if (token.matches("return|throw")) {
						continue;
					}
				}
				if (start == -1) {
					start = i + m.start(0);
				}
			}

			if (lastIndex != quack.length()) {
				continue l1;
			}
			return start;
		}
		return -1;
	}

	public static IPackageBinding getPackage(CompilationUnit ast) {
		for (Object o : ast.types()) {
			if (o instanceof TypeDeclaration) {
				return ((TypeDeclaration) o).resolveBinding().getPackage();
			}
			if (o instanceof EnumDeclaration) {
				return ((EnumDeclaration) o).resolveBinding().getPackage();
			}
		}
		throw new Error("no types found in java file: "
				+ ast.getJavaElement().getElementName());
	}

	public static Set<ITypeBinding> getDeclaredTypesIncludingMethodClasses(
			CompilationUnit ast) {
		final Set<ITypeBinding> types = new HashSet();
		class MyASTVisitor extends ASTVisitor {
			public boolean visit(EnumDeclaration node) {
				types.add(node.resolveBinding());
				return true;
			}

			public boolean visit(TypeDeclaration node) {
				types.add(node.resolveBinding());
				return true;
			}
		}
		ast.accept(new MyASTVisitor());
		return types;
	}

	public static IJavaElement getIJavaElement(ITextEditor editor) {
		return JavaCore.create(((IFileEditorInput) editor.getEditorInput())
				.getFile());
	}

	public static String getProjectAndPackage(IJavaElement j) {
		String packageName = ((IPackageFragment) j.getParent())
				.getElementName();
		String projectName = j.getJavaProject().getProject().getName();
		return projectName + ":" + packageName;
	}

	public static String getProjectAndPackage(CompilationUnit ast) {
		return getProjectAndPackage(ast.getJavaElement());
	}

	public static void countCallsToDifferentMethodsAndFields(
			CompilationUnit ast, final Bag<String> counts) throws Exception {
		class MyASTVisitor extends ASTVisitor {
			public boolean isFieldLike(IVariableBinding vb) {
				return vb != null && (vb.isField() || vb.isEnumConstant());
			}

			public boolean visit(VariableDeclarationFragment node) {
				IVariableBinding vb = node.resolveBinding();
				if (isFieldLike(vb)) {
					counts.add(vb.getKey(), -1);
				}
				return true;
			}

			public boolean visit(SimpleName node) {
				IBinding b = node.resolveBinding();
				if (b instanceof IVariableBinding) {
					IVariableBinding vb = (IVariableBinding) b;
					if (isFieldLike(vb)) {
						counts.add(vb.getKey());
					}
				}
				return true;
			}

			public boolean visit(MethodInvocation node) {
				IMethodBinding b = node.resolveMethodBinding();
				if (b != null) {
					counts.add(b.getKey());
				}
				return true;
			}
		}
		MyASTVisitor v = new MyASTVisitor();
		ast.accept(v);
	}

	public static void test() {
		UU.debug("blah!");
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

	public static Set<ITypeBinding> getNamedTypes(CompilationUnit n) {
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

	public static CompilationUnit compile(ICompilationUnit unit,
			IJavaProject proj, char[] source, int offset) {
		ASTParser p = ASTParser.newParser(AST.JLS3);
		p.setResolveBindings(true);
		p.setStatementsRecovery(true);
		p.setSource(source);
		p.setProject(proj);
		p.setUnitName(unit.getElementName());
		if (offset > 0) {
			p.setFocalPosition(offset);
		}
		return (CompilationUnit) p.createAST(null);
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

	public static CompilationUnit compile(ICompilationUnit unit) {
		return compile(unit, -1);
	}

	public static boolean hasErrors(CompilationUnit n) {
		for (IProblem p : n.getProblems()) {
			if (p.isError()) {
				return true;
			}
		}
		return false;
	}

	public static ITypeBinding getEnclosingClass(ASTNode n) {
		if (n instanceof AnonymousClassDeclaration) {
			return ((AnonymousClassDeclaration) n).resolveBinding();
		} else if (n instanceof EnumDeclaration) {
			return ((EnumDeclaration) n).resolveBinding();
		} else if (n instanceof TypeDeclaration) {
			return ((TypeDeclaration) n).resolveBinding();
		} else if (n.getParent() != null) {
			return getEnclosingClass(n.getParent());
		} else {
			return null;
		}
	}

	public static ITypeBinding getClassForMethodInvocation(MethodInvocation mi) {
		Expression e = mi.getExpression();
		if (e != null) {
			return e.resolveTypeBinding();
		} else {
			return getEnclosingClass(mi);
		}
	}

	public static Vector<IMethodBinding> getMethodsOfName(ITypeBinding b,
			String name) {
		Vector<IMethodBinding> v = new Vector();
		getMethodsOfName_helper(b, name, v);
		return v;
	}

	public static void getMethodsOfName_helper(ITypeBinding b, String name,
			Vector<IMethodBinding> v) {
		if (b == null)
			return;
		for (IMethodBinding m : b.getDeclaredMethods()) {
			if (m.getName().equals(name)) {
				v.add(m);
			}
		}
		getMethodsOfName_helper(b.getSuperclass(), name, v);
		for (ITypeBinding i : b.getInterfaces()) {
			getMethodsOfName_helper(i, name, v);
		}
	}

	public static ITypeBinding getHoleType(ASTNode node) {
		try {
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

				ITypeBinding b = getClassForMethodInvocation(a);
				Vector<IMethodBinding> ms = getMethodsOfName(b, a.getName()
						.toString());

				// for now, if there is more than one, bail
				if (ms.size() > 1)
					return null;

				for (int i = 0; i < a.arguments().size(); i++) {
					if (node == a.arguments().get(i)) {
						IMethodBinding ab = a.resolveMethodBinding();
						if (!ab.isVarargs() || (i < a.arguments().size() - 1)) {
							return ab.getParameterTypes()[i];
						} else {
							// ... since we can't return both, we return null
							return null;
						}
					}
				}
			}
			if (parent instanceof ClassInstanceCreation) {
				ClassInstanceCreation a = (ClassInstanceCreation) parent;

				// if there is more than one constructor, bail
				int count = 0;
				ITypeBinding b = a.resolveTypeBinding();
				for (IMethodBinding m : b.getDeclaredMethods()) {
					if (m.isConstructor())
						count++;
				}
				if (count > 1)
					return null;

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
		} catch (Exception e) {
			// oh well, we tried
		}

		return null;
	}
}
