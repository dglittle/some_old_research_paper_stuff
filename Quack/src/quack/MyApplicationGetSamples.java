package quack;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Stack;
import java.util.Vector;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

import MyUtil.UU;

public class MyApplicationGetSamples {

	public Vector<String> keysNoLits;
	public Vector<String> keysWithLits;

	public void processExpression(ICompilationUnit unit, ASTNode node,
			MyASTVisitorSmall v) throws Exception {

		// if (v.hasLiterals) {
		// return;
		// }
		// if (v.maxDepth <= 1) {
		// return;
		// }

		try {

			int nodeStart = node.getStartPosition();
			int nodeEnd = nodeStart + node.getLength();

			String key = unit.getPath() + ":" + nodeStart + ":" + nodeEnd + ":"
					+ node.toString().replaceAll("[\r\n]", "");

			// make sure this isn't followed by a '.',
			// as in "System.out" from "System.out.println()",
			// since that will conflict with the return type sensor
			// if (unit.getSource().charAt(nodeEnd) == '.') {
			// return;
			// }

			// we don't want ones that are too long (> 13)
			// Ident ident = new Ident(node.toString());
			// if (ident.words.size() < 2)
			// return;
			// if (ident.words.size() > 13)
			// return;
			// if (v.numLiterals > 4)
			// return;

			// if (v.numLiterals > 0) {
			// keysWithLits.add(key);
			// } else {
			keysNoLits.add(key);
			// }
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(0);
		}
	}

	class MyASTVisitorSmall extends ASTVisitor {
		public boolean good = true;
		public int currentDepth = 0;
		public int maxDepth = 0;
		public int numLiterals = 0;
		public int numStringLiterals = 0;

		public boolean influenceDepth(ASTNode node) {
			if ((node instanceof ArrayAccess)
					|| (node instanceof BooleanLiteral)
					|| (node instanceof CharacterLiteral)
					|| (node instanceof ConstructorInvocation)
					|| (node instanceof FieldAccess)
					|| (node instanceof MethodInvocation)
					|| (node instanceof NumberLiteral)
					|| (node instanceof StringLiteral)
					|| (node instanceof ThisExpression)) {
				return true;
			} else if (node instanceof Name) {
				Name name = (Name) node;
				IBinding b = name.resolveBinding();
				if ((b instanceof IMethodBinding)
						|| (b instanceof IVariableBinding)) {
					return true;
				}
			}
			return false;
		}

		public void preVisit(ASTNode node) {
			if (!((node instanceof ArrayAccess)
					|| (node instanceof BooleanLiteral)
					|| (node instanceof CharacterLiteral)
					|| (node instanceof ConstructorInvocation)
					|| (node instanceof FieldAccess)
					|| (node instanceof MethodInvocation)
					|| (node instanceof NumberLiteral)
					|| (node instanceof ParameterizedType)
					|| (node instanceof QualifiedName)
					|| (node instanceof QualifiedType)
					|| (node instanceof SimpleName)
					|| (node instanceof SimpleType)
					|| (node instanceof StringLiteral)
					|| (node instanceof ThisExpression)
					|| (node instanceof TypeParameter) || (node instanceof WildcardType))) {
				good = false;
			}
			if ((node instanceof BooleanLiteral)
					|| (node instanceof CharacterLiteral)
					|| (node instanceof NumberLiteral)
					|| (node instanceof StringLiteral)) {
				numLiterals++;
			}
			if (node instanceof StringLiteral) {
				numStringLiterals++;
			}
			if (influenceDepth(node)) {
				currentDepth++;
				if (currentDepth > maxDepth) {
					maxDepth = currentDepth;
				}
			}
		}

		public void postVisit(ASTNode node) {
			if (influenceDepth(node)) {
				currentDepth--;
			}
		}
	}

	public void processCompilationUnit(final ICompilationUnit unit)
			throws Exception {

		class MyASTVisitorBig extends ASTVisitor {
			public void preVisit(ASTNode node) {
				if ((node instanceof ArrayAccess)
						|| (node instanceof BooleanLiteral)
						|| (node instanceof CharacterLiteral)
						|| (node instanceof ConstructorInvocation)
						|| (node instanceof FieldAccess)
						|| (node instanceof MethodInvocation)
						|| (node instanceof NumberLiteral)
						|| (node instanceof StringLiteral)
						|| (node instanceof ThisExpression)) {
					MyASTVisitorSmall v = new MyASTVisitorSmall();
					node.accept(v);
					try {
						if (v.good && v.maxDepth <= 3) {
							processExpression(unit, node, v);
						}
					} catch (Exception e) {
						throw new Error(e);
					}
				}
			}
		}
		MyASTVisitorBig vi = new MyASTVisitorBig();
		CompilationUnit ast = EclipseUtil.compile(unit);
		ast.accept(new SlopTesterASTVisitor(unit.getSource()) {

			@Override
			public void doNode(ASTNode node) {
				try {
					processExpression(unit, node, null);
				} catch (Exception e) {

					e.printStackTrace();
					System.exit(0);

					throw new Error(e);
				}
			}
		});
		// System.out.println("expCount = " + processedExpressionCount);
		// System.out.println("unitCount = " + processedUnitCount);
	}

	public void start(IApplicationContext context) throws Exception {

		PrintWriter sam = new PrintWriter(new FileWriter(
				"C:\\Home\\J.ASE\\data\\samples_to_do.txt"));

		IWorkspace w = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot wr = w.getRoot();
		IJavaModel m = JavaCore.create(wr);

		for (IJavaProject p : m.getJavaProjects()) {
			System.out.println("p = " + p);

			// int zimbraIndex = p.toString().startsWith("sf-zimbracommon") ? 0
			// : p.toString().startsWith("sf-zimbraim") ? 1 : p.toString()
			// .startsWith("sf-zimbraserver") ? 2 : -1;
			// if (zimbraIndex < 0 || zimbraIndex == 0) {
			keysWithLits = new Vector();
			keysNoLits = new Vector();
			// }

			for (IPackageFragmentRoot r : p.getPackageFragmentRoots()) {
				if (r.getKind() == IPackageFragmentRoot.K_SOURCE) {
					for (IJavaElement j : r.getChildren()) {
						for (IJavaElement je : ((IPackageFragment) j)
								.getChildren()) {
							ICompilationUnit cu = (ICompilationUnit) je;
							try {
								processCompilationUnit(cu);
							} catch (Throwable t) {
								t.printStackTrace();
								System.exit(0);
							}
							System.out.print(".");
							if (UU.r.nextInt(30) == 0)
								System.out.println();
						}
					}
				}
				System.out.print(".");
			}
			System.out.println();

			// if (zimbraIndex == 0 || zimbraIndex == 1)
			// continue;

			// Collections.shuffle(keysWithLits);
			Collections.shuffle(keysNoLits);
			for (int i = 0; i < 500; i++) {
				// sam.println(keysWithLits.get(i));
				sam.println(keysNoLits.get(i));
			}
			sam.flush();

			System.out.println("done writing samples");
		}

		sam.close();

	}

	static abstract class SlopTesterASTVisitor extends ASTVisitor {
		String source = null;

		ITypeBinding thisType = null;

		int sampleNum = 0;

		int classDepth = 0;

		IMethodBinding thisMethod = null;

		boolean thisMethodTemp = false;

		Stack<String> funcStack = new Stack<String>();

		public SlopTesterASTVisitor(String source) {
			this.source = source;
		}

		public void pushFuncStack() {
			funcStack.push("");
		}

		public void popFuncStack() {
			funcStack.pop();
		}

		public boolean variableScopeNode(ASTNode node) {
			return node instanceof MethodDeclaration || node instanceof Block
					|| node instanceof ForStatement;
		}

		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		public boolean visit(AnnotationTypeDeclaration node) {
			return false;
		}

		public boolean visitAbstractType(AbstractTypeDeclaration node) {

			classDepth++;

			if (thisType == null) {
				thisType = node.resolveBinding();
				return true;
			} else {
				return false;
			}
		}

		public void endVisitAbstractType(AbstractTypeDeclaration node) {

			classDepth--;

			if (thisType != null) {
				if (classDepth == 0) {
					thisType = null;
				}
			}
		}

		public boolean visit(EnumDeclaration node) {
			return visitAbstractType(node);
		}

		public boolean visit(TypeDeclaration node) {
			return visitAbstractType(node);
		}

		public void endVisit(EnumDeclaration node) {
			endVisitAbstractType(node);
		}

		public void endVisit(TypeDeclaration node) {
			endVisitAbstractType(node);
		}

		public boolean visit(MethodDeclaration node) {
			if (thisMethod == null) {
				thisMethod = node.resolveBinding();
				pushFuncStack();
				return true;
			} else {
				thisMethodTemp = true;
				return false;
			}
		}

		public void endVisit(MethodDeclaration node) {
			if (thisMethodTemp) {
				thisMethodTemp = false;
				return;
			}

			if (thisMethod != null) {
				popFuncStack();
				thisMethod = null;
			}
		}

		class FuncTreeException extends Exception {
			String message;

			public FuncTreeException(String message) {
				this.message = message;
			}

			public String toString() {
				return "##" + message;
			}
		}

		public static boolean validFuncTree(ASTNode node, int depth) {
			if (depth < 0) {
				return false;
			}
			if (node instanceof ClassInstanceCreation) {
				ClassInstanceCreation a = (ClassInstanceCreation) node;
				if (a.getAnonymousClassDeclaration() != null) {
					return false;
				}
				for (Object o : a.arguments()) {
					if (!validFuncTree((ASTNode) o, depth - 1))
						return false;
				}
				return true;
			} else if (node instanceof MethodInvocation) {
				MethodInvocation a = (MethodInvocation) node;
				Expression expression = a.getExpression();
				boolean hasThis = (expression != null);
				if (expression != null) {
					if (expression instanceof SimpleName) {
						IBinding b = ((SimpleName) expression).resolveBinding();
						if (b instanceof ITypeBinding) {
							hasThis = false;
						}
					}
				}
				if (hasThis) {
					if (!validFuncTree(expression, depth - 1))
						return false;
				}
				for (Object o : a.arguments()) {
					if (!validFuncTree((ASTNode) o, depth - 1))
						return false;
				}
				return true;
			} else if (node instanceof FieldAccess) {
				FieldAccess a = (FieldAccess) node;
				
				// check for array.length
				if (a.getName().toString().equals("length")) {
					if (a.getExpression().resolveTypeBinding().isArray()) {
						return false;
					}
				}
				
				if (!validFuncTree(a.getExpression(), depth - 1))
					return false;
				return true;
			} else if (node instanceof QualifiedName) {
				QualifiedName a = (QualifiedName) node;
				IBinding binding = a.resolveBinding();
				if (binding instanceof IVariableBinding) {
					
					// check for array.length
					if (a.getName().toString().equals("length")) {
						if (a.getQualifier().resolveTypeBinding().isArray()) {
							return false;
						}
					}
					
					if (a.getQualifier().resolveBinding() instanceof ITypeBinding) {
						return true;
					} else {
						if (!validFuncTree(a.getQualifier(), depth - 1))
							return false;
						return true;
					}
				} else {
					return false;
				}
			} else if (node instanceof SimpleName) {
				IBinding binding = ((SimpleName) node).resolveBinding();
				if (binding instanceof IVariableBinding) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}
		
		public static boolean validFuncTree_hasDepth(ASTNode node) {
			if (node instanceof ClassInstanceCreation) {
				ClassInstanceCreation a = (ClassInstanceCreation) node;
				return a.arguments().size() > 0;
			} else if (node instanceof MethodInvocation) {
				MethodInvocation a = (MethodInvocation) node;
				Expression expression = a.getExpression();
				boolean hasThis = (expression != null);
				if (expression != null) {
					if (expression instanceof SimpleName) {
						IBinding b = ((SimpleName) expression).resolveBinding();
						if (b instanceof ITypeBinding) {
							hasThis = false;
						}
					}
				}
				if (hasThis) {
					return true;
				}
				return a.arguments().size() > 0;
			} else if (node instanceof FieldAccess) {
				FieldAccess a = (FieldAccess) node;
				return true;
			} else if (node instanceof QualifiedName) {
				QualifiedName a = (QualifiedName) node;
				IBinding binding = a.resolveBinding();
				if (binding instanceof IVariableBinding) {
					if (a.getQualifier().resolveBinding() instanceof ITypeBinding) {
						return false;
					} else {
						return true;
					}
				} else {
					return false;
				}
			} else if (node instanceof SimpleName) {
				return false;
			}
			return false;
		}

		public void preVisit(ASTNode node) {

			if (variableScopeNode(node)) {
				pushFuncStack();
			}

			if (thisType != null && thisMethod != null) {
				if (node instanceof Expression) {
					boolean valid = validFuncTree(node, 2);
					if (!valid)
						return;

					ASTNode parent = node.getParent();
					if (parent instanceof SingleVariableDeclaration) {
						SingleVariableDeclaration s = (SingleVariableDeclaration) parent;
						if (s.getName() == node) {
							return;
						}
					}
					if (parent instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment f = (VariableDeclarationFragment) parent;
						if (f.getName() == node) {
							return;
						}
					}
					// make sure there isn't a dot before us
					if (UU.match("\\.\\s*$", source.substring(0, node
							.getStartPosition()))) {
						return;
					}
					if (UU.match("^\\s*\\.", source.substring(node
							.getStartPosition()
							+ node.getLength()))) {
						return;
					}

					Ident quack = new Ident(node.toString());
					if (quack.words.size() < 2) {
						return;
					}
					
					// work here
					// make sure we have at least one child,
					// this is for inference test
					if (!validFuncTree_hasDepth(node)) {
						return;
					}

					doNode(node);
				}
			}
		}

		public abstract void doNode(ASTNode node);

		public void possiblyAddVariable(IVariableBinding var) {
			if (var.isField())
				return;
		}

		public void postVisit(ASTNode node) {

			if (node instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration a = (SingleVariableDeclaration) node;
				possiblyAddVariable(a.resolveBinding());

			}
			if (node instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment a = (VariableDeclarationFragment) node;

				possiblyAddVariable(a.resolveBinding());
			}

			if (variableScopeNode(node)) {
				popFuncStack();
			}
		}
	}

}
