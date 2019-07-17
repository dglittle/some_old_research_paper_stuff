package sloptester;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import MyUtil.Bag;
import MyUtil.Pair;
import MyUtil.u;
import deslopper2.Deslopper2;
import deslopper3.Deslopper3;

public class MyRunnable implements IPlatformRunnable {

	public int findTag(int pos, Map<Pair<Integer, Integer>, Integer> tags) {
		for (Pair<Integer, Integer> key : tags.keySet()) {
			if (pos >= key.left && pos <= key.right) {
				return tags.get(key);
			}
		}
		return -1;
	}

	//
	// public void tagTests(List<Test> tests,
	// Map<Pair<Integer, Integer>, Integer> tags) {
	//
	// Set<Integer> takenTags = new HashSet<Integer>();
	//
	// for (Test test : tests) {
	// int pos = test.node.getStartPosition();
	// int tag = findTag(pos, tags);
	// if (tag != -1) {
	// if (takenTags.add(tag)) {
	// test.tag = tag;
	// }
	// }
	// }
	// }

	public static String fill(String s, int size) {
		while (s.length() < size) {
			s += " ";
		}
		return s;
	}

	public Map<Integer, Vector<Pair<String, String>>> getUserSlop(
			Map<Pair<Integer, Integer>, Integer> tags, String dataDir)
			throws Exception {

		Map<Integer, Vector<Pair<String, String>>> map = new HashMap<Integer, Vector<Pair<String, String>>>();
		for (Integer i : tags.values()) {
			Vector<Pair<String, String>> lines = new Vector<Pair<String, String>>();

			System.out.println("data dir = " + dataDir);

			for (File f : new File(dataDir).listFiles()) {
				String filename = f.getName();
				Matcher m = Pattern.compile("^user(\\d+)_rand").matcher(
						filename);
				if (m.find()) {
					String s = u.slurp(f);
					String userID = m.group(1);
					m = Pattern
							.compile(
									"(?ms)^image = (.*?)\\.png$.*?^realState = (\\d+)$.*?^keywords = (.*?)\ncomments = ")
							.matcher(s);
					if (m.find()) {
						int tag = new Integer(m.group(2));
						if (i == tag) {
							String kind = userID + "\t" + m.group(1);
							String slop = m.group(3).replaceAll("\\\\\"", "\"")
									.replaceAll("\\s", " ");
							lines.add(new Pair<String, String>(kind, slop));
						}
					}
				}
				System.out.print(".");
			}

			map.put(i, lines);
		}
		System.out.println();
		return map;

	}

	public ICompilationUnit findUnitNamed(Vector<ICompilationUnit> units,
			String name) {
		for (ICompilationUnit unit : units) {
			String thisName = EclipseUtil.getName(unit);
			if (thisName.equals(name)) {
				return unit;
			}
		}
		return null;

	}

	// public void runTestsOnUserCorpus(Vector<ICompilationUnit> units,
	// DeslopperInterface[] ds, String baseDir) {
	//
	// System.out.println("runTestsOnUserCorpus:");
	// try {
	// ICompilationUnit unit = findUnitNamed(units, "UserCorpus.java");
	// System.out.println("found the user corpus.");
	// if (unit == null) {
	// throw new Exception("bad");
	// }
	// PrintWriter out = new PrintWriter(new FileWriter(baseDir
	// + "/output/user_slop_output.txt"));
	//
	// CompilationUnit n = EclipseUtil.compile(unit);
	// if (EclipseUtil.hasErrors(n)) {
	// throw new Exception("bad");
	// }
	//
	// Map<Pair<Integer, Integer>, Integer> tags = listComments(unit, n);
	// Map<Integer, Vector<Pair<String, String>>> userSlop = getUserSlop(tags,
	// baseDir + "/input/data");
	//
	// Tomb tomb = new Tomb(n);
	//
	// Walker walker = new Walker(n, tomb);
	// tagTests(walker.tests, tags);
	// int i = 0;
	// for (DeslopperInterface d : ds) {
	// for (Test t : walker.tests) {
	// if (t.tag >= 0) {
	// System.out.println("ast: " + t.node);
	// for (Pair<String, String> p : userSlop.get(t.tag)) {
	// String slop = p.right;
	// String output = t.test(d, slop);
	// out.println(d + "\t" + p.left + "\t" + t.node
	// + "\t" + slop + "\t" + output);
	// System.out.println("output = " + output);
	// }
	// }
	// }
	// }
	// out.close();
	// } catch (Exception e) {
	// e.printStackTrace();
	// System.out.println("Failed to run tests on user corpus!");
	// System.exit(0);
	// }
	// }

	public static int phase = 1;

	public static Bag<String> blah = new Bag<String>();

	public static Map<String, int[]> samples = new HashMap<String, int[]>();

	public static Map<String, Integer> indexes = new HashMap<String, Integer>();

	public static boolean getNext(String proj) {
		boolean yes = false;
		synchronized (samples) {
			int i = indexes.get(proj);
			yes = samples.get(proj)[i] != 0;
			indexes.put(proj, i + 1);
		}
		return yes;
	}

	public void initSamples() {
		// Map<String, Integer> blah = new HashMap<String, Integer>();
		// blah.put("ch-jruby", 18458);
		// blah.put("ch-radeox", 1115);
		// blah.put("jakarta-commons-codec", 1371);
		// blah.put("owf-carol", 2262);
		// blah.put("sf-azureus2", 75909);
		// blah.put("sf-buddi", 7221);
		// blah.put("sf-cmusphinx-sphinx4", 12556);
		// blah.put("sf-dnsjava", 2576);
		// blah.put("sf-jedit", 24561);
		// blah.put("sf-jmemorize", 2196);
		// blah.put("sf-jmol", 43216);
		// blah.put("sf-rssowl", 22393);
		// blah.put("sf-tvbrowser", 26481);
		// blah.put("sf-zimbracommon", 1104);
		// blah.put("sf-zimbraim", 15258);
		// blah.put("sf-zimbraserver", 56554);

		// blah.put("sf-zimbraim", 8318);
		// blah.put("sf-jmemorize", 1291);
		// blah.put("sf-tvbrowser", 11909);
		// blah.put("ch-jruby", 7314);
		// blah.put("ch-radeox", 539);
		// blah.put("sf-zimbraserver", 26490);
		// blah.put("sf-rssowl", 7720);
		// blah.put("sf-azureus2", 31090);
		// blah.put("sf-cmusphinx-sphinx4", 5276);
		// blah.put("sf-dnsjava", 1707);
		// blah.put("sf-jedit", 10166);
		// blah.put("sf-zimbracommon", 599);
		// blah.put("owf-carol", 1102);
		// blah.put("jakarta-commons-codec", 806);
		// blah.put("sf-jmol", 11308);
		// blah.put("sf-buddi", 2400);

		blah.put("sf-zimbraim", 16325);
		blah.put("sf-jmemorize", 2604);
		blah.put("sf-tvbrowser", 29255);
		blah.put("ch-jruby", 19198);
		blah.put("ch-radeox", 1304);
		blah.put("sf-zimbraserver", 59415);
		blah.put("sf-rssowl", 23685);
		blah.put("sf-azureus2", 82006);
		blah.put("sf-cmusphinx-sphinx4", 13217);
		blah.put("sf-dnsjava", 2900);
		blah.put("sf-jedit", 25875);
		blah.put("sf-zimbracommon", 1214);
		blah.put("owf-carol", 2478);
		blah.put("jakarta-commons-codec", 1806);
		blah.put("sf-jmol", 44478);
		blah.put("sf-buddi", 7807);

		// sf-zimbraim=8318, sf-jmemorize=1291,
		// sf-tvbrowser=11909,
		// ch-jruby=7314, ch-radeox=539, sf-zimbraserver=26490, sf-rssowl=7720,
		// sf-azureus2=31090, sf-cmusphinx-sphinx4=5276, sf-dnsjava=1707,
		// sf-jedit=10166, sf-zimbracommon=599, owf-carol=1102,
		// jakarta-commons-codec=806, sf-jmol=11308, sf-buddi=2400}
		//		

		for (String s : blah.keySet()) {
			int size = blah.get(s);
			int[] a = new int[size];

			int desiredCount = 550;
			for (int i = 0; i < desiredCount; i++) {
				while (true) {
					int r = u.r.nextInt(size);
					if (a[r] == 0) {
						a[r] = 1;
						break;
					}
				}
			}

			samples.put(s, a);
			indexes.put(s, 0);
		}
	}

	public void runTestsOnArtificialCorpus(Vector<ICompilationUnit> units,
			String baseDir) throws Exception {

		List<ICompilationUnit> sync = Collections
				.synchronizedList(new LinkedList<ICompilationUnit>(units));

		System.out.println("runTestsOnArtificialCorpus======================:");

		PrintWriter out = new PrintWriter(new FileWriter(baseDir
				+ "/output/artificial_slop_output.txt"), true);

		Thread t1 = null;
		Thread t2 = null;

		// phase = 1;
		//
		// u.printProgress(0, sync.size());
		// t1 = new TesterGrabber(sync, out);
		// t2 = new TesterGrabber(sync, out);
		// t1.join();
		// t2.join();
		//		
		// // work here
		// System.out.println("balh: " + blah);
		// // System.exit(0);
		// u.saveString(new File(baseDir + "/output/sample_counts.txt"), "" +
		// blah);

		System.out.println("begin init...");
		initSamples();
		System.out.println("done init...");

		phase = 2;

		sync = Collections.synchronizedList(new LinkedList<ICompilationUnit>(
				units));
		u.printProgress(0, sync.size());
		t1 = new TesterGrabber(sync, out);
		t2 = new TesterGrabber(sync, out);
		t1.join();
		t2.join();

		out.close();

	}

	public void countExpressions(Vector<ICompilationUnit> units, String baseDir)
			throws Exception {

		int count = 0;
		int total = 0;
		
		int i = -1;
		for (ICompilationUnit unit : units) {
			i++;
			String name = EclipseUtil.getName(unit);
			try {

				String project = unit.getJavaProject().getProject().getName();

				// out.println(project + "\t" + name + "\t" +
				// EclipseUtil.loc(unit));
				// if (true) return;

				System.out.println("name = " + name);

				CompilationUnit n = EclipseUtil.compile(unit);
				if (EclipseUtil.hasErrors(n)) {
					continue;
				}

//				Tomb tomb = new Tomb(n);
//				TempWalker w = new TempWalker(n, tomb);
//				count += w.overall_count;
//				total += w.overall_total;
//				System.out.println("count: " + count + "/" + total);
//				u.printProgress(i, units.size());
			} catch (Exception e) {
//				e.printStackTrace();
//				System.out.println("something bad in: " + name);
//				System.out.println("in project: "
//						+ unit.getJavaProject().getElementName());
//				System.out.println("I'm not dead yet!");
			}
		}

	}

	public class TesterGrabber extends Thread {
		List<ICompilationUnit> units;

		DeslopperInterface[] ds;

		PrintWriter out;

		public TesterGrabber(List<ICompilationUnit> units, PrintWriter out) {
			this.units = units;
			this.ds = new DeslopperInterface[] { // new Deslopper1(),
			new Deslopper2(), new Deslopper3(), };

			this.out = out;
			start();

		}

		public void run() {
			try {
				while (true) {
					ICompilationUnit unit = units.remove(0);
					u.printProgress(units.size(), -1);
					Tester tester = new Tester(unit, out, ds);
					tester.join(1000 * 60 * 10);
					tester.stop();
				}
			} catch (Exception e) {
			}
		}
	}

	public class Tester extends Thread {
		ICompilationUnit unit;

		DeslopperInterface[] ds;

		PrintWriter out;

		public Tester(ICompilationUnit unit, PrintWriter out,
				DeslopperInterface[] ds) {
			this.unit = unit;
			this.out = out;
			this.ds = ds;
			start();
		}

		public void run() {
			String name = EclipseUtil.getName(unit);
			try {
				if (name.equals("UserCorpusXYZ.java")) {
					return;
				}

				String project = unit.getJavaProject().getProject().getName();

				// out.println(project + "\t" + name + "\t" +
				// EclipseUtil.loc(unit));
				// if (true) return;

				System.out.println("name = " + name);

				CompilationUnit n = EclipseUtil.compile(unit);
				if (EclipseUtil.hasErrors(n)) {
					return;
				}

				Tomb tomb = new Tomb(n);
				Walker w = new Walker(n, tomb, ds, out, project + "\t" + name,
						project);
				System.out.println("both: " + w.sum + "/" + w.total + " = "
						+ ((double) w.sum / w.total));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("something bad in: " + name);
				System.out.println("in project: "
						+ unit.getJavaProject().getElementName());
				System.out.println("I'm not dead yet!");
			}
		}
	}

	public Object run(Object arg0) throws Exception {
		// get a list of all the compilation units in this project that we want
		// to process
		Vector<ICompilationUnit> units = new Vector<ICompilationUnit>();
		IWorkspace w = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot wr = w.getRoot();
		IJavaModel m = JavaCore.create(wr);

		File eclipseDir = new File("" + w.getRoot().getRawLocation());
		String baseDir = eclipseDir.getParentFile().getParentFile()
				.getCanonicalPath();

		System.out.println("baseDir = " + baseDir);
		System.out.flush();

		for (IJavaProject p : m.getJavaProjects()) {
			System.out.println("p = " + p);
			for (IPackageFragmentRoot r : p.getPackageFragmentRoots()) {
				if (r.getKind() == IPackageFragmentRoot.K_SOURCE) {
					for (IJavaElement j : r.getChildren()) {
						for (IJavaElement je : ((IPackageFragment) j)
								.getChildren()) {
							ICompilationUnit cu = (ICompilationUnit) je;
							units.add(cu);
						}
					}
				}
				System.out.print(".");
			}
			System.out.println();
		}

		System.out.println("size = " + units.size());

		// runTestsOnUserCorpus(units, ds, baseDir);
		// runTestsOnArtificialCorpus(units, baseDir);
		countExpressions(units, baseDir);

		return null;
	}
}
