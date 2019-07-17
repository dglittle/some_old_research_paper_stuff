import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.impl.xpath.XPath;

import MyUtil.U;

public class GenerateDatabase {

	Document doc;

	Element root;

	public GenerateDatabase(String filename) throws Exception {

		doc = U.createDocument();
		root = doc.getDocumentElement();

		processChunks(filename);

		U.save(doc, new File("C:\\Home\\J.ASE\\data\\data.xml"));
	}

	public void processChunk(String chunk) throws Exception {
		GramExp g = new GramExp(
				"S = (?msi)\\{(??key)\\s*(??cacheTime)\\s*(??expression)\\s*(??depth)\\s*(??numLiterals)\\s*(??numStringLiterals)\\s*(??numKeywords)\\s*(??returnType)\\s*(??trialSection)*\\s*\\}",
				"key = key: (.*)",
				"cacheTime = (?:cache time: (.*))?",
				"expression = (?:expression: (.*))?",
				"depth = (?:depth: (.*))?",
				"numLiterals = (?:(?:num|has) literals: (.*))?",
				"numStringLiterals = (?:(?:num|has) string literals: (.*))?",
				"numKeywords = num keywords: (.*)",
				"returnType = return type: (.*)",
				"trialSection = (?msi)\\s*\\{i = (??i)(??trial)*\\s*\\}",
				"i = (\\d+)",
				"trial = (?msi)\\s*\\{(??quack)(??stats)(??time)(??result)*\\s*\\}",
				"quack = \\s*quack: (.*)",
				"stats = \\s*stats: w:(??w) l:(??l) f:(??f) t(??t)",
				"w = (\\d+)", "l = (\\d+)", "f = (\\d+)", "t = (\\d+)",
				"time = \\s*time: \\-?(\\d+)", "result = \\s*result: (.*)");
		Object o = g.match(chunk);
		Document d = GramExp.createXml(o, "entry");

		Element root = U.getElement("//entry", d.getDocumentElement());
		String expression = U.getElement(".//expression", root)
				.getTextContent();
		for (Element trial : U.getElements(".//trial", root)) {
			int rank = -1;
			int i = 1;
			for (Element result : U.getElements(".//result", trial)) {
				String text = result.getTextContent();
				if (text.replaceAll("\\s+", "").equals(
						expression.replaceAll("\\s+", ""))) {
					rank = i;
					break;
				}
				i++;
			}
			for (Element result : U.getElements(".//result", trial)) {
				trial.removeChild(result);
			}
			Element rankE = d.createElement("rank");
			trial.appendChild(rankE);
			rankE.appendChild(d.createTextNode("" + rank));
		}

		root.appendChild(U.getElement(".//f", root));
		root.appendChild(U.getElement(".//t", root));
		for (Element ft : U.getElements(".//trial//f | .//trial//t", root)) {
			ft.getParentNode().removeChild(ft);
		}

		for (Element trialSection : U.getElements(".//trialSection", root)) {
			int count = 0;
			int w = 0;
			int L = 0;
			int time = 0;
			int[] ranks = new int[] { 0, 0, 0, 0 };
			for (Element trial : U.getElements(".//trial", trialSection)) {
				count++;
				w += Integer.parseInt(U.getElement(".//w", trial)
						.getTextContent());
				L += Integer.parseInt(U.getElement(".//l", trial)
						.getTextContent());
				time += Integer.parseInt(U.getElement(".//time", trial)
						.getTextContent());
				int rank = Integer.parseInt(U.getElement(".//rank", trial)
						.getTextContent());
				if (rank < 0)
					rank = 0;
				ranks[rank]++;

				trial.getParentNode().removeChild(trial);
			}
			trialSection.setAttribute("count", "" + count);
			trialSection.setAttribute("w", "" + U.safeDivide(w, count));
			trialSection.setAttribute("l", "" + U.safeDivide(L, count));
			trialSection.setAttribute("time", "" + U.safeDivide(time, count));
			for (int i = 1; i < ranks.length; i++) {
				trialSection.setAttribute("rank" + i, "" + ranks[i]);
			}
		}

		doc.adoptNode(root);
		doc.getDocumentElement().appendChild(root);
	}

	public void processChunks(String filename) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(filename));
		StringBuffer buf = new StringBuffer();
		int i = 1;
		while (in.ready()) {
			String line = in.readLine();

			if (line.startsWith("{key: ")) {
				buf = new StringBuffer();
			}
			buf.append(line + "\n");
			if (line.startsWith("}}}")) {
				try {
					processChunk(buf.toString());

					Runtime r = Runtime.getRuntime();
					long totalMem = r.totalMemory();
					long memFree = r.freeMemory();
					long usedMem = (totalMem - memFree) / (1024 * 1024);
					System.out.println("done: " + i++ + " mem: " + usedMem + "mb");
				} catch (Exception e) {
//					System.out.println("chunk: " + buf.toString());
//					e.printStackTrace();
//					System.exit(0);
				}
			}
		}
		in.close();
	}
}
