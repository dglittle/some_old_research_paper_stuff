package quack;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringBufferInputStream;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import MyUtil.Bag;
import MyUtil.Pair;
import MyUtil.UU;

public class Main {

	public static Main main;

	public IProject lastProject;

	public ModelCache modelCache;

	public Main() {
		main = this;
		modelCache = new ModelCache(1);
	}

	public static Main getMain() {
		if (main == null) {
			new Main();
		}
		return main;
	}

	public void log(String s) {
		if (s.startsWith("stats: ")) {
			log2(s);
		}
	}

	public static String lastLog2;

	public void log2(String s) {
		lastLog2 = s;
	}

	public static String lastLog3;

	public void log3(String s) {
		lastLog3 = s;
	}

	public void log(String origin, Throwable t) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(out));
		log("ERROR IN " + origin + ":\n" + out.toString());
	}

	public List<MyCompletionProposal> getProposals(ICompilationUnit unit,
			String doc, int selectionOffset, int selectionLength) {
		try {
			lastProject = unit.getJavaProject().getProject();

			UU.profileClear();
			UU.profile("all");

			UU.profile("part 1");

			Main main = Main.getMain();
			Vector<MyCompletionProposal> list = new Vector();
			int cursorOffset = selectionOffset + selectionLength;
			IJavaProject javaProject = unit.getJavaProject();

			// find the quack (keyword command)
			String quack;
			int quackOffset;
			quack = doc.substring(selectionOffset, selectionOffset
					+ selectionLength);
			quackOffset = selectionOffset;
			Ident quackIdent = new Ident(quack);

			log("version: 0.0.7");
			log("input: " + quack);

			UU.profile("part 1");

			UU.profile("ast build");

			String standinExpression = "null";
			StringBuffer buf = new StringBuffer(doc);
			buf.replace(quackOffset, cursorOffset, standinExpression);
			CompilationUnit ast = EclipseUtil.compile(unit, unit
					.getJavaProject(), buf.toString().toCharArray(), 0);

			UU.profile("ast build");

			UU.profile("model");

			Model model = modelCache.getModel(unit, ast);
			synchronized (model) {
				model.processTypesForAST(ast);

				UU.profile("model");

				UU.profile("count funcs");

				Bag<String> functionCallCounts = new Bag();
				EclipseUtil.countCallsToDifferentMethodsAndFields(ast,
						functionCallCounts);

				UU.profile("count funcs");

				UU.profile("walker{}");

				model.functionCallCounts = functionCallCounts;
				Deslopper d = new Deslopper();
				Walker w = new Walker(ast, model, quackOffset, quackIdent, d);

				UU.profile("walker{}");

				UU.profile("all");
				// UU.profilePrint();

				if (w.guesses.size() == 0) {
					log("no guesses");
				}

				int i = 0;
				for (String guess : w.guesses) {
					list.add(new MyCompletionProposal(unit.getJavaProject()
							.getProject(), guess, quackOffset, quack.length(),
							guess.length(), null, guess + " [from Quack]",
							null, null, 1000000 - i));
					i++;
				}
				return list;
			}
		} catch (Throwable e) {
			
			System.out.println("file: " + unit.getElementName());
			
			log("Main.java(at end)", e);
			throw new Error(e);
		}
	}
}
