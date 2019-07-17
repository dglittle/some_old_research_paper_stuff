package sloptester;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import MyUtil.MyNode;
import MyUtil.Pair;
import MyUtil.u;

public class TempWalker {
	public int sum = 0;

	public int total = 0;
	
	public ASTNode overall_skippy = null;
	public int overall_count = 0;
	public int overall_total = 0;

	public TempWalker(CompilationUnit n, final Tomb tomb) {

		class MyASTVisitor extends ASTVisitor {
			ITypeBinding thisType = null;

			int sampleNum = 0;

			int classDepth = 0;

			IMethodBinding thisMethod = null;

			boolean thisMethodTemp = false;

			Stack<Erasure> funcStack = new Stack<Erasure>();

			class Erasure {
				int oldFuncSize;

				Map<Integer, Func> erasedFuncs = new HashMap<Integer, Func>();

				public Erasure() {
					this.oldFuncSize = tomb.funcs.size();
				}

				public void addLocalVar(IVariableBinding binding) {
					Func func = new Func(binding, binding.getName(), tomb
							.getType(binding.getType()), true);

					for (int i = funcStack.firstElement().oldFuncSize; i < tomb.funcs
							.size(); i++) {
						Func oldFunc = tomb.funcs.get(i);
						if (oldFunc.name.equals(func.name)) {
							erasedFuncs.put(i, oldFunc);
							oldFunc.unregister(tomb);
							tomb.funcs.set(i, func);
							func.register(tomb);
							return;
						}
					}
					func.bonus = 0.001;
					func.add(tomb);
				}

				public void undo() {
					for (Map.Entry<Integer, Func> e : erasedFuncs.entrySet()) {
						tomb.funcs.set(e.getKey(), e.getValue()).unregister(
								tomb);
						e.getValue().register(tomb);
					}
					for (int i = oldFuncSize; i < tomb.funcs.size(); i++) {
						Func f = tomb.funcs.get(i);
						f.unregister(tomb);
					}
					tomb.funcs.setSize(oldFuncSize);
				}
			}

			public void pushFuncStack() {
				funcStack.push(new Erasure());
			}

			public void popFuncStack() {
				funcStack.pop().undo();
			}

			public MyASTVisitor() {
			}

			public boolean variableScopeNode(ASTNode node) {
				return node instanceof MethodDeclaration
						|| node instanceof Block
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
					tomb.processType_local(thisType, !Modifier
							.isStatic(thisMethod.getModifiers()), true,
							new HashSet<String>());
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

			public sloptester.Type myGetType(ITypeBinding binding)
					throws FuncTreeException {
				sloptester.Type t = tomb.getTypeWeak(binding);
				if (t == null) {
					throw new FuncTreeException("couldn't find: "
							+ binding.getQualifiedName());
				}
				return t;
			}

			public Func getFunc(IBinding binding, boolean hasThis, boolean local)
					throws Exception {
				String key = Func.makeKey(binding, hasThis, local);
				Func func = tomb.keyToFunc.get(key);
				if (func == null) {
					throw new FuncTreeException("can't find: " + key);
				} else {
					return func;
				}
			}

			public MyNode<Func> makeFuncTree(ASTNode node, int depth)
					throws Exception {
				// if (depth < 0) {
				// throw new FuncTreeException("too deep");
				// }
				if (node instanceof ClassInstanceCreation) {
					ClassInstanceCreation a = (ClassInstanceCreation) node;
					IMethodBinding binding = a.resolveConstructorBinding();
					Func f = getFunc(binding, false, false);
					MyNode<Func> n = new MyNode<Func>(f);
					for (Object o : a.arguments()) {
						n.addChild(makeFuncTree((ASTNode) o, depth - 1));
					}
					return n;
				} else if (node instanceof MethodInvocation) {
					MethodInvocation a = (MethodInvocation) node;
					IMethodBinding binding = a.resolveMethodBinding();
					Expression expression = a.getExpression();
					boolean hasThis = (expression != null);
					boolean local = (expression == null);
					if (expression != null) {
						if (expression instanceof SimpleName) {
							IBinding b = ((SimpleName) expression)
									.resolveBinding();
							if (b instanceof ITypeBinding) {
								hasThis = false;
							}
						}
					}
					Func f = getFunc(binding, hasThis, local);
					MyNode<Func> n = new MyNode<Func>(f);
					if (hasThis) {
						n.addChild(makeFuncTree(expression, depth - 1));
					}
					for (Object o : a.arguments()) {
						n.addChild(makeFuncTree((ASTNode) o, depth - 1));
					}
					return n;
				} else if (node instanceof FieldAccess) {
					FieldAccess a = (FieldAccess) node;
					Func f = getFunc(a.resolveFieldBinding(), true, false);
					MyNode<Func> n = new MyNode<Func>(f);
					n.addChild(makeFuncTree(a.getExpression(), depth - 1));
					return n;
				} else if (node instanceof QualifiedName) {
					QualifiedName a = (QualifiedName) node;
					IBinding binding = a.resolveBinding();
					if (binding instanceof IVariableBinding) {
						if (a.getQualifier().resolveBinding() instanceof ITypeBinding) {
							Func f = getFunc(binding, false, false);
							return new MyNode<Func>(f);
						} else {
							Func f = getFunc(binding, true, false);
							MyNode<Func> n = new MyNode<Func>(f);
							n
									.addChild(makeFuncTree(a.getQualifier(),
											depth - 1));
							return n;
						}
					} else {
						throw new FuncTreeException(
								"qualified name not variable: " + node);
					}
				} else if (node instanceof SimpleName) {
					IBinding binding = ((SimpleName) node).resolveBinding();
					if (binding instanceof IVariableBinding) {
						Func f = getFunc(binding, false, true);
						return new MyNode<Func>(f);
					} else {
						throw new FuncTreeException(
								"simple name not variable: " + node);
					}
				}
				throw new FuncTreeException("bad node type: " + node);
			}

			public Vector<Func> getFuncs(MyNode<Func> tree) {
				Vector<Func> v = new Vector<Func>();
				getFuncs_helper(tree, v);
				return v;
			}

			public void getFuncs_helper(MyNode<Func> tree, Vector<Func> funcs) {
				funcs.add(tree.data);
				for (MyNode<Func> child : tree.children) {
					getFuncs_helper(child, funcs);
				}
			}

			public int countAmbiguities(Vector<Func> funcs, Type returnType) {
				Stack<Pair<Integer, Type>> returnTypes = new Stack<Pair<Integer, Type>>();
				returnTypes.add(new Pair<Integer, Type>(0, returnType));
				int[] sum = new int[] { 0 };
				int[] safety = new int[] { 900 };
				if (funcs.size() > 60) {
					return -1;
				}
				try {
					countAmbiguities_helper(funcs, returnTypes, 0L, 0, sum,
							safety);
				} catch (Exception e) {
				}
				return sum[0];
			}

			public void countAmbiguities_helper(Vector<Func> funcs,
					Stack<Pair<Integer, Type>> returnTypes, long used,
					int usedCount, int[] sum, int[] safety) {
				safety[0]--;
				if (safety[0] <= 0) {
					sum[0] = -1;
					throw new IllegalArgumentException(
							"safety counter violation");
				}
				if (returnTypes.isEmpty() || (usedCount == funcs.size())) {
					if (returnTypes.isEmpty() && (usedCount == funcs.size())) {
						sum[0]++;
						if (sum[0] > 100) {
							sum[0] = 101;
							throw new IllegalArgumentException("too ambiguous");
						}
					}
				} else if (returnTypes.peek().left >= 3) {
				} else {
					for (int i = 0; i < funcs.size(); i++) {
						if ((used & (1 << i)) == 0) {
							Func f = funcs.get(i);
							Pair<Integer, Type> p = returnTypes.peek();
							int depth = p.left;
							Type rt = p.right;
							if (rt == null
									|| rt.subTypes.contains(f.returnType)) {
								used |= 1 << i;
								returnTypes.pop();
								for (Type param : f.params) {
									returnTypes.push(new Pair<Integer, Type>(
											depth + 1, param));
								}
								countAmbiguities_helper(funcs, returnTypes,
										used, usedCount + 1, sum, safety);
								returnTypes.push(p);
								used &= ~(1 << i);
							}
						}
					}
				}
			}

			public String sloppyPrint2(Func f) {
				return f.ident.words.get(u.r.nextInt(f.ident.words.size()));
			}

			//
			public String sloppyPrint(Func f) {
				Vector<String> v = new Vector<String>(f.ident.words);
				Collections.shuffle(v);
				return u.join(v, " ");
			}

			public String sloppyPrint(MyNode<Func> tree) {
				Vector<String> v = new Vector<String>();
				v.add(sloppyPrint(tree.data));
				for (MyNode<Func> child : tree.children) {
					v.add(sloppyPrint(child));
				}
				Collections.shuffle(v);
				return u.join(v, " ");
			}

			public String sloppyPrint2(MyNode<Func> tree) {
				Vector<String> v = new Vector<String>();
				v.add(sloppyPrint2(tree.data));
				for (MyNode<Func> child : tree.children) {
					v.add(sloppyPrint2(child));
				}
				Collections.shuffle(v);
				return u.join(v, " ");
			}

			public String sloppyPrintFreq(Func f) {
				return tomb.mostUniqueWord(f.ident.words);
			}

			public String sloppyPrintFreq(MyNode<Func> tree) {
				Vector<String> v = new Vector<String>();
				v.add(sloppyPrintFreq(tree.data));
				for (MyNode<Func> child : tree.children) {
					v.add(sloppyPrintFreq(child));
				}
				Collections.shuffle(v);
				return u.join(v, " ");
			}

			public void preVisit(ASTNode node) {

				if (variableScopeNode(node)) {
					pushFuncStack();

				}

				if (thisType != null && thisMethod != null) {
					if (node instanceof Expression) {

						if (overall_skippy != null) {

						} else {

							try {
								// if (u.r.nextDouble() > 0.02) {
								// throw new Exception("random skip");
								// }

								MyNode<Func> tree = makeFuncTree(node, 2);
								String slop = sloppyPrint(tree);
								List<String> keywords = u.keywords(slop);
								if (keywords.size() < 2) {
									throw new Exception("trivial");
								}

								int depth = tree.getHeight();

								if (depth <= 2) {
									overall_count++;
								}
								overall_total++;
								overall_skippy = node;

							} catch (Exception e) {
							}
						}
					}
				}
			}

			public void possiblyAddVariable(IVariableBinding var) {
				if (var.isField())
					return;

				funcStack.peek().addLocalVar(var);
			}

			public void postVisit(ASTNode node) {
				
				if (node == overall_skippy) {
					overall_skippy = null;
				}

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
		MyASTVisitor vi = new MyASTVisitor();
		n.accept(vi);
	}
}
