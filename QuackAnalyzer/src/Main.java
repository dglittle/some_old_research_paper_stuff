import java.io.File;
import java.util.Collections;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import MyUtil.U;

public class Main {

	public static void main(String[] args) throws Exception {

		if (false) {
			AnalyzeQuackLogs a = new AnalyzeQuackLogs();
			System.exit(0);
		}

		// work here
		// String s = U.slurp(new File("C:\\Home\\J.ASE\\data\\data.xml"));
		// s = s.replaceAll("</entry>", "</entry>\n");
		// U.save(new File("C:\\Home\\J.ASE\\data\\data_backup.xml"), s);
		// System.exit(0);

		// new GenerateDatabase("C:\\Home\\J.ASE\\data\\sample_text.txt");
		if (false) {
			new GenerateDatabase("C:\\Home\\J.ASE\\data\\jase_output.txt");
			System.exit(0);
		}

		Document d = U.loadXml(new File(
				"C:\\Home\\J.ASE\\data\\compare inference\\data.xml"));
		Element body = d.getDocumentElement();

		int t = 0;

		// {
		// for (int size = 2; size <= 2; size++) {
		// String xpath = "//entry[not(contains(expression, '.length')) and
		// numKeywords = "
		// + size
		// + " and trialSection[@rank1 = 0 and @rank2 = 0 and @rank3 = 0]]";
		// Vector<Element> v = new Vector();
		// for (Element e : U.getElements(xpath, d)) {
		// v.add(e);
		// }
		// Collections.shuffle(v);
		// for (int i = 0; i < 2; i++) {
		// System.out.println(U.getString(".//key", v.get(i)));
		// }
		// }
		// }
		// System.exit(0);

		for (int x = 2; x <= 13; x++) {
			for (int y = 0; y <= x; y++) {
				// String xpathTop = "//entry[numKeywords = '" + x +
				// "' and trialSection[@i = '4' and @rank1 > 0]]";
				String xpathTop = "//entry[numKeywords = " + x
						+ " and trialSection[i = " + y + " and @rank1 > 0]]";

				String xpathBottom = "//entry[numKeywords = " + x + "]";

				String xpathTime = "//entry[numKeywords = " + x
						+ "]/trialSection[i = " + y + "]/@time";

				// for (Element e : U
				// .getElements(
				// "//entry[numKeywords = 2 and trialSection[i = 2 and @rank1 =
				// 0
				// and @rank2 = 0 and @rank3 = 0]]",
				// d)) {
				// System.out.println("expaa: " + U.getString(".//expression",
				// e));
				// t++;
				// if (t >= 1)
				// System.exit(0);
				//				
				// }

				// System.out.println("exp: " + U.getString(".//expression",
				// e));
				// System.out.println("exp: " + U.getElement(".//trialSection",
				// e));

				int a = U.countNodes(xpathTop, body);
				int b = U.countNodes(xpathBottom, body);
				System.out.println("" + x + "/" + y + " = " + ((double) a / b));
				System.out.println("time: " + U.avgNodes(xpathTime, body));
			}
			System.out.println();
		}
		System.exit(0);

		// - assuming 4 keywords:
		// - x axis : keyword in expression
		// - y axis : percent where max rank is 1
		/*
		 * body entry key expression depth numKeywords numLiterals
		 * numStringLiterals returnType cacheTime f t trialSection i @time @l @w
		 * @count @rank1 @rank2 @rank3 @rank4
		 */
	}
}
