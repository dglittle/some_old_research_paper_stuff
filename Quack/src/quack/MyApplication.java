package quack;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import MyUtil.Bag;
import MyUtil.Pair;
import MyUtil.UU;

public class MyApplication implements IApplication {

	public PrintWriter contextOut;
	public Vector<String> keys;

	public ICompilationUnit lastUnit = null;

	public int processedExpressionCount = 0;
	public int processedUnitCount = 0;
	public int maxWordSize = 0;

	public Vector<Pair<Boolean, String>> getTokens(String s) {
		Vector<Pair<Boolean, String>> v = new Vector<Pair<Boolean, String>>();
		Matcher m = Pattern.compile(Ident.keywordRegex).matcher(s);
		while (m.find()) {
			boolean literal = (m.group(2) == null);
			String keyword = m.group(1);
			if (!literal) {
				keyword = keyword.toLowerCase();
			}
			v.add(new Pair<Boolean, String>(literal, keyword));
		}
		return v;
	}

	public void processExpression(PrintWriter out, ICompilationUnit unit,
			ASTNode node, Set<String> doneSet, Set<String> todoSet)
			throws Exception {
		processedExpressionCount++;

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

			key = getTrimmedKey(key);

			// work here
			// if (key
			// .equals("/sf-jedit/org/gjt/sp/jedit/ActionListHandler.java:1648:1658:"))
			// {
			//
			// } else {
			// return;
			// }
			if (!todoSet.contains(key))
				return;
			if (!doneSet.add(key))
				return;

			StringBuffer buf = new StringBuffer();
			buf.append("{key: " + key + "\n");

			String expression = node.toString();
			Vector<Pair<Boolean, String>> tokens = getTokens(expression);

			// if (tokens.size() > 13) {
			// System.out.println("wtf? 664");
			// System.exit(0);
			// return;
			// }
			// if (v.numLiterals > 4) {
			// System.out.println("wtf? 665");
			// System.exit(0);
			// return;
			// }

			// get it cached
			// (we run this even on a cache hit, so that we get the return type)
			{
				boolean cacheMiss = (unit != lastUnit);
				lastUnit = unit;
				long startTime = System.currentTimeMillis();
				List<MyCompletionProposal> ps = Main.getMain().getProposals(
						unit, unit.getSource(), nodeStart, nodeEnd - nodeStart);
				long endTime = System.currentTimeMillis();
				long time = endTime - startTime;
				if (cacheMiss) {
					buf.append("cache time: " + time + "\n");
				}
			}

			// sort tokens by frequency (infrequent should come first)
			Model m = Main.getMain().modelCache.cache.get(unit);
			final Bag<String> freq = new Bag();
			for (Vector<Func> funcs : m.funcGroups.values()) {
				for (Func func : funcs) {
					if (!func.literal) {
						for (String word : func.ident.words) {
							freq.add(word);
						}
					}
				}
			}
			Collections.sort(tokens, new Comparator<Pair<Boolean, String>>() {
				@Override
				public int compare(Pair<Boolean, String> o1,
						Pair<Boolean, String> o2) {
					return freq.get(o1.right) - freq.get(o2.right);
				}
			});

			// work here
			// System.exit(0);

			buf.append("expression: " + expression + "\n");
			// buf.append("depth: " + v.maxDepth + "\n");
			// buf.append("num literals: " + v.numLiterals + "\n");
			// buf.append("num string literals: " + v.numStringLiterals + "\n");
			buf.append("num keywords: " + tokens.size() + "\n");
			buf.append("return type: " + Main.lastLog3 + "\n");

			// Vector<Pair<Boolean, String>> lits = new Vector();
			// for (Pair<Boolean, String> p : tokens) {
			// if (p.left) {
			// lits.add(p);
			// }
			// }
			{
				Vector<Pair<Boolean, String>> toks = new Vector();
				for (int i = 0; i <= tokens.size(); i++) {
					if (i > 0) {
						toks.add(tokens.get(i - 1));
					}

					buf.append("{i = " + i + "\n");

					Collections.shuffle(toks);

					StringBuffer quackBuf = new StringBuffer();
					for (Pair<Boolean, String> tok : toks) {
						quackBuf.append(tok.right + " ");
					}
					String quack = quackBuf.toString();
					buf.append("{quack: " + quack + "\n");

					StringBuffer source = new StringBuffer(unit.getSource());
					source.replace(nodeStart, nodeEnd, quack);

					// make sure we have enough memory
					long totalMem = Runtime.getRuntime().totalMemory();
					long freeMem = Runtime.getRuntime().freeMemory();
					if (totalMem > 470L * 1024L * 1024L) {
						throw new Error("too little memory, too slow: "
								+ totalMem + ":" + freeMem);
					}

					long startTime = System.currentTimeMillis();
					List<MyCompletionProposal> ps = Main.getMain()
							.getProposals(unit, source.toString(), nodeStart,
									quack.length());
					long endTime = System.currentTimeMillis();

					buf.append(Main.getMain().lastLog2 + "\n");
					buf.append("time: " + (endTime - startTime) + "\n");

					for (MyCompletionProposal p : ps) {
						buf.append("result: " + p.toString() + "\n");
					}

					buf.append("}");

					buf.append("}");
				}
			}

			if (false) {
				for (int i = 0; i <= 4; i++) {
					if (tokens.size() < i)
						break;

					buf.append("{i = " + i + "\n");

					for (Vector<Pair<Boolean, String>> toks : UU.choose(tokens,
							i)) {

						Collections.shuffle(toks);

						StringBuffer quackBuf = new StringBuffer();
						for (Pair<Boolean, String> tok : toks) {
							quackBuf.append(tok.right + " ");
						}
						String quack = quackBuf.toString();
						buf.append("{quack: " + quack + "\n");

						StringBuffer source = new StringBuffer(unit.getSource());
						source.replace(nodeStart, nodeEnd, quack);

						// make sure we have enough memory
						long totalMem = Runtime.getRuntime().totalMemory();
						long freeMem = Runtime.getRuntime().freeMemory();
						if (totalMem > 470L * 1024L * 1024L) {
							throw new Error("too little memory, too slow: "
									+ totalMem + ":" + freeMem);
						}

						long startTime = System.currentTimeMillis();
						List<MyCompletionProposal> ps = Main.getMain()
								.getProposals(unit, source.toString(),
										nodeStart, quack.length());
						long endTime = System.currentTimeMillis();

						buf.append(Main.getMain().lastLog2 + "\n");
						buf.append("time: " + (endTime - startTime) + "\n");

						for (MyCompletionProposal p : ps) {
							buf.append("result: " + p.toString() + "\n");
						}

						buf.append("}");
					}
					buf.append("}");
				}
			}

			buf.append("}\n");
			out.print(buf.toString());
			out.flush();

			// work here : write context
			contextOut.println("------------------------");
			contextOut.println("key: " + key);
			int size = 100;
			contextOut.print(unit.getSource().substring(
					Math.max(0, node.getStartPosition() - size),
					node.getStartPosition()));
			contextOut
					.print("<<<"
							+ unit.getSource().substring(
									node.getStartPosition(),
									node.getStartPosition() + node.getLength())
							+ ">>>");
			contextOut.print(unit.getSource().substring(
					node.getStartPosition() + node.getLength(),
					Math.min(unit.getSource().length(), node.getStartPosition()
							+ node.getLength() + size)));

			System.out.println("got one!: " + doneSet.size());

		} catch (Throwable t) {

			t.printStackTrace();
			System.exit(0);

			throw new Error(t);
		}
	}

	public void processCompilationUnit(final PrintWriter out,
			final ICompilationUnit unit, final Set<String> doneSet,
			final Set<String> todoSet) throws Exception {
		processedUnitCount++;

		CompilationUnit ast = EclipseUtil.compile(unit);
		ast.accept(new MyApplicationGetSamples.SlopTesterASTVisitor(unit
				.getSource()) {

			@Override
			public void doNode(ASTNode node) {
				try {
					processExpression(out, unit, node, doneSet, todoSet);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
			}

		});
		// System.out.println("expCount = " + processedExpressionCount);
		// System.out.println("unitCount = " + processedUnitCount);
	}

	public String getTrimmedKey(String key) {
		Matcher m = Pattern.compile("^.*?:\\d+:\\d+:").matcher(key);
		m.find();
		return m.group(0);
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {

		if (false) {
			MyApplicationGetSamples blah = new MyApplicationGetSamples();
			blah.start(context);
			System.exit(0);
		}

		Set<String> todoSet = new HashSet<String>();
		{
			BufferedReader in = new BufferedReader(new FileReader(
					"C:\\Home\\J.ASE\\data\\samples_to_do.txt"));
			while (in.ready()) {
				String line = in.readLine();
				todoSet.add(getTrimmedKey(line));
			}
			in.close();
		}

		Set<String> doneSet = new HashSet();
		{
			BufferedReader in = new BufferedReader(new FileReader(
					"C:\\Home\\J.ASE\\data\\jase_output.txt"));
			while (in.ready()) {
				String line = in.readLine();
				if (UU.match("^\\{key: (.*)", line)) {
					String key = UU.m.group(1);
					doneSet.add(getTrimmedKey(key));
				}
			}
			in.close();

			System.out.println("done: " + doneSet.size());
		}

		// clear any empty strings from sets
		for (Iterator<String> it = todoSet.iterator(); it.hasNext();) {
			String e = it.next();
			if (e.matches("\\s*")) {
				it.remove();
			}
		}
		for (Iterator<String> it = doneSet.iterator(); it.hasNext();) {
			String e = it.next();
			if (e.matches("\\s*")) {
				it.remove();
			}
		}
		System.out.println("done1: " + doneSet.size());

		{
			PrintWriter out = new PrintWriter(new FileWriter(
					"C:\\Home\\J.ASE\\data\\jase_output.txt", true), true);

			contextOut = new PrintWriter(new FileWriter(
					"C:\\Home\\J.ASE\\data\\jase_output_context.txt", true),
					true);

			IWorkspace w = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot wr = w.getRoot();
			IJavaModel m = JavaCore.create(wr);

			for (IJavaProject p : m.getJavaProjects()) {
				System.out.println("p = " + p);

				// work here
				// if (("" + p).startsWith("sf-jedit")) {
				//
				// } else {
				// continue;
				// }

				for (IPackageFragmentRoot r : p.getPackageFragmentRoots()) {
					if (r.getKind() == IPackageFragmentRoot.K_SOURCE) {
						for (IJavaElement j : r.getChildren()) {
							for (IJavaElement je : ((IPackageFragment) j)
									.getChildren()) {
								ICompilationUnit cu = (ICompilationUnit) je;
								try {
									processCompilationUnit(out, cu, doneSet,
											todoSet);
								} catch (Throwable t) {
									String error = "error: " + t;
									System.out.println(error);
									if (error.contains("OutOfMemory")) {
										System.exit(0);
									}
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
			}
		}

		return null;
	}

	@Override
	public void stop() {
	}

}
