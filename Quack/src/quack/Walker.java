package quack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class Walker {
	public List<String> guesses = new Vector<String>();

	public ASTNode getTheNodeWeWant(CompilationUnit n, final int nodeOffset) {
		class MyASTVisitor extends ASTVisitor {
			ASTNode nodeWeWant = null;

			public void preVisit(ASTNode node) {
				if (node.getStartPosition() == nodeOffset) {
					if (node instanceof NullLiteral) {
						nodeWeWant = node;
						throw new Error("found it!");
					}
				}
			}
		}
		MyASTVisitor vi = new MyASTVisitor();
		try {
			n.accept(vi);
		} catch (Error e) {
			if (!e.getMessage().equals("found it!"))
				throw e;
		}
		return vi.nodeWeWant;
	}

	public void getParents(ASTNode node, Set<ASTNode> parents,
			Set<ITypeBinding> types) {
		ASTNode parent = node.getParent();
		if (parent != null) {
			parents.add(parent);
			if (parent instanceof TypeDeclaration) {
				types.add(((TypeDeclaration) parent).resolveBinding());
			} else if (parent instanceof EnumDeclaration) {
				types.add(((EnumDeclaration) parent).resolveBinding());
			}
			getParents(parent, parents, types);
		}
	}

	public Walker(final CompilationUnit n, final Model model,
			final int nodeOffset, final Ident quack, final Deslopper d) {

		final int originalTypeCount = model.keyToType.size();
		final ASTNode desiredNode = getTheNodeWeWant(n, nodeOffset);
		if (desiredNode == null)
			return;
		final Set<ASTNode> pathToDesiredNode = new HashSet();
		final Set<ITypeBinding> typesToDesiredNode = new HashSet();
		getParents(desiredNode, pathToDesiredNode, typesToDesiredNode);
		final Vector<Vector<Func>> scopes = new Vector();

		class Done extends Error {

		}

		class MyASTVisitor extends ASTVisitor {
			ITypeBinding processType;

			boolean processTypeStatic;

			ITypeBinding mostRecentType;

			Set<String> fieldsSoFarInThisType = new HashSet();

			Vector<Func> localVars = new Vector<Func>();

			public MyASTVisitor() {
			}

			public boolean visit(AnnotationTypeDeclaration node) {
				return false;
			}

			public void newScope(Set<String> fields) {
				if (localVars.size() > 0) {
					scopes.add(localVars);
					localVars = new Vector<Func>();
				}
				if (processType != null) {
					model.funcGroups.get("#temp");
					model
							.processType_local(processType, processTypeStatic,
									new HashSet<String>(), fields, processType,
									"#temp");
					scopes.add(model.funcGroups.remove("#temp"));

					mostRecentType = processType;
					processType = null;
					processTypeStatic = false;
					fieldsSoFarInThisType = new HashSet();
				}
			}

			public boolean visit(ForStatement node) {
				if (pathToDesiredNode.contains(node)) {
					newScope(null);
					return true;
				}
				return false;
			}

			public boolean visit(Block node) {
				if (pathToDesiredNode.contains(node)) {
					newScope(null);
					return true;
				}
				return false;
			}

			public boolean visit(MethodDeclaration node) {
				if (pathToDesiredNode.contains(node)) {
					processTypeStatic = Modifier.isStatic(node.getModifiers());
					newScope(null);
					return true;
				}
				return false;
			}

			public boolean visit(AnnotationTypeMemberDeclaration node) {
				if (pathToDesiredNode.contains(node)) {
					processTypeStatic = Modifier.isStatic(node.getModifiers());
					newScope(null);
					return true;
				}
				return false;
			}

			public boolean visit(VariableDeclarationFragment node) {
				if (pathToDesiredNode.contains(node)) {
					IVariableBinding vb = node.resolveBinding();
					if (vb != null && vb.isField()) {
						processTypeStatic = Modifier
								.isStatic(vb.getModifiers());
					}
					return true;
				}
				return false;
			}

			public boolean visit(AnonymousClassDeclaration node) {
				if (pathToDesiredNode.contains(node)) {
					newScope(null);
					processType = node.resolveBinding();
					return true;
				}
				return false;
			}

			public boolean dealWithTypeDeclaration(ASTNode node,
					ITypeBinding b, int mods) {
				if (pathToDesiredNode.contains(node)) {
					// add constructors for all the non-static classes in here,
					// except for the one we are in,
					// since that constructor will be added later
					for (ITypeBinding child : b.getDeclaredTypes()) {
						if (!Modifier.isStatic(b.getModifiers())
								&& !typesToDesiredNode.contains(child)) {
							model.processType_local_addConstructors(child,
									"#local");
						}
					}

					processTypeStatic = Modifier.isStatic(mods);
					newScope(null);
					processType = b;
					return true;
				} else {
					// if this is a method-class,
					// that we are passing along the way to our destination,
					// add constructors for it
					if (EclipseUtil.isMethodClass(b)) {
						model.processType_local_addConstructors(b, "#local");
					}
				}
				return false;
			}

			public boolean visit(EnumDeclaration node) {
				return dealWithTypeDeclaration(node, node.resolveBinding(),
						node.getModifiers());
			}

			public boolean visit(TypeDeclaration node) {
				return dealWithTypeDeclaration(node, node.resolveBinding(),
						node.getModifiers());
			}

			public void possiblyAddLocalVariable(IVariableBinding var) {
				if (var != null) {
					if (var.isField()) {
						fieldsSoFarInThisType.add(var.getName());
					} else {
						Func func = new Func(var, var.getName(), model
								.getType(var.getType()));
						localVars.add(func);
					}
				}
			}

			public void endVisit(SingleVariableDeclaration node) {
				possiblyAddLocalVariable(node.resolveBinding());
			}

			public void endVisit(VariableDeclarationFragment node) {
				possiblyAddLocalVariable(node.resolveBinding());
			}

			public void preVisit(ASTNode node) {
				if (node == desiredNode) {

					// this is the final scope
					newScope(fieldsSoFarInThisType);

					// add "this"
					Func func = new Func(null, "this", model
							.getType(mostRecentType));
					scopes.get(scopes.size() - 1).add(func);

					// keep the correct stuff from all the scopes
					Set<String> variableNames = new HashSet();
					Set<String> methodNames = new HashSet();
					Vector<Func> locals = model.funcGroups.get("#local");
					int dist = 0;
					for (int i = scopes.size() - 1; i >= 0; i--) {
						dist++;
						Vector<Func> funcs = scopes.get(i);
						for (Func f : funcs) {
							f.contextDist = dist;
							if (f.method) {
								if (!methodNames.contains(f.name)) {
									locals.add(f);
								}
							} else {
								if (!variableNames.contains(f.name)) {
									locals.add(f);
								}
							}
						}
						for (Func f : funcs) {
							if (f.method) {
								methodNames.add(f.name);
							} else {
								variableNames.add(f.name);
							}
						}
					}

					// get return type
					Type returnType = model.getType(EclipseUtil
							.getHoleType(node));

					Main.getMain().log("desired type: " + returnType);
					
					Main.getMain().log3("" + returnType);

					// if we added any new types,
					// recomputer the type list
					if (model.keyToType.size() > originalTypeCount) {
						model.createTypeList();
					}

					try {
						guesses = d.deslop(quack, returnType, model);
					} catch (Exception e) {
						throw new Error(e);
					}
					throw new Done();
				}
			}
		}
		MyASTVisitor vi = new MyASTVisitor();
		try {
			n.accept(vi);
		} catch (Done e) {
		}
		model.funcGroups.remove("#local");
	}
}
