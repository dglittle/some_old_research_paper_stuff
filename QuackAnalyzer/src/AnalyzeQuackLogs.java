import java.io.File;
import java.util.Map;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import MyUtil.U;

public class AnalyzeQuackLogs {
	public AnalyzeQuackLogs() throws Exception {
		String baseDir = "C:/Home/KittenStuff/EclipseWorkspace/jASE08/Field Study Results";
		String[] people = new String[] { "M6w88a9_logs", "H718A8_logs",
				"D61H194J_logs" };

		GramExp g = new GramExp(
				"S = (??version)(??input)(??cacheMiss)?(??returnType)(??stats)(??guess)*(??chosen)?(??noGuesses)?",
				"version = \\s*(??time): version: (.*)",
				"input = \\s*(??time): input: (.*)",
				"cacheMiss = \\s*(??time): model cache miss",
				"returnType = \\s*(??time): desired type: (.*)",
				"stats = \\s*(??time): stats: w:(??w) l:(??l) f:(??f) t(??t)",
				"w = \\d+", "l = \\d+", "f = \\d+", "t = \\d+",
				"guess = \\s*(??time): guess: (.*)",
				"chosen = \\s*(??time): chosen: (.*)", "time = \\d+",
				"noGuesses = \\s*(??time): no guesses");
		for (String person : people) {
			String filename = baseDir + "/" + person + "/quack.log";
			String s = U.slurp(filename);
			Vector<Map<String, Object>> findAll = g.findAll(s);
			Document d = g.createXml(findAll, "entry");
			Element body = d.getDocumentElement();

			// add total time information
			for (Element e : U.getElements("//entry", body)) {
				long start = new Long(U.getString(".//version/time", e));
				long end = new Long(U.getString(
						".//guess/time | .//noGuesses/time", e));
				long total = end - start;
				Element t = d.createElement("totalTime");
				t.appendChild(d.createTextNode("" + total));
				e.appendChild(t);
				
				Node node = U.getNode(".//chosen/time", e);
				if (node != null) {
					long endEnd = new Long(node.getTextContent());
					total = endEnd - end;
					t = d.createElement("pickTime");
					t.appendChild(d.createTextNode("" + total));
					e.appendChild(t);
				}
			}

			System.out.println("----------------:" + person);
			System.out.println("chosen: "
					+ U.countNodes("//entry[chosen]", body));

			int cacheMisses = U.countNodes("//entry[cacheMiss]", body);
			System.out.println("cacheMiss: " + cacheMisses);
			System.out.println("cacheMiss: "
					+ U.sumNodes("//entry[cacheMiss]/totalTime", body)
					/ cacheMisses);

			{
				System.out.println("f: "
						+ U.avgNodes("//entry[not(cacheMiss)]/stats/f", body));
				System.out.println("t: "
						+ U.avgNodes("//entry[not(cacheMiss)]/stats/t", body));
				System.out.println("w: "
						+ U.avgNodes("//entry[not(cacheMiss)]/stats/w", body));
				System.out.println("l: "
						+ U.avgNodes("//entry[not(cacheMiss)]/stats/l", body));
			}

			int cacheHits = U.countNodes("//entry[not(cacheMiss)]", body);
			System.out.println("cacheHit: " + cacheHits);
			System.out.println("cacheHit: "
					+ U.sumNodes("//entry[not(cacheMiss)]/totalTime", body)
					/ cacheHits);

			int picks = U.countNodes("//entry[chosen]", body);
			System.out.println("picks: " + picks);
			System.out.println("picks: "
					+ U.avgNodes("//entry[chosen]/pickTime", body));

			System.out.println("total: " + U.countNodes("//entry", body));
			// System.exit(0);
		}

		// 1203116710651: version: 0.0.9
		// 1203116710651: input: s change value
		// 1203116710748: model cache miss
		// 1203116712349: desired type: null
		// 1203116712572: stats: w:216 l:73 f:7392 t1153
		// 1203116712574: guess: s.changeObject(VALUE_TYPE)
		// 1203116712574: guess: s.changeObject(getValueType())
		// 1203116712574: guess:
		// s.changeObject(LocationStateVariable.VALUE_TYPE)

	}
}
