
package MyUtil;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.regex.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.text.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.nio.channels.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.jar.*;
import java.security.*;
import java.math.*;
import java.nio.channels.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class u {
	public static Random r = new Random();
    
    public static class ListListIterator<T> implements Iterable<T>, Iterator<T> {
        Collection<T>[] listOfLists;
        int i;
        Iterator j;
        
        public ListListIterator(Collection<T> ... listOfLists) {
            this.listOfLists = listOfLists;
            i = 0;
            setJ();
        }
        
        public void setJ() {
            if (i < listOfLists.length) {
                j = listOfLists[i].iterator();
                if (!j.hasNext()) {
                    i++;
                    setJ();
                }
            } else {
                j = null;
            }
        }
        
        public Iterator<T> iterator() {
            return this;
        }
        
        public boolean hasNext() {
            return j != null;
        }
        
        public T next() {
            T next = (T)j.next();
            if (!j.hasNext()) {
                i++;
                setJ();
            }
            return next;
        }
        
        public void remove() {
            throw new IllegalArgumentException("not implemented");
        }
    }
    
    public static <T> ListListIterator<T> iterate(Collection<T> ... listOfLists) {
        return new ListListIterator(listOfLists);
    }
    
    public static ListListIterator iterate2(Collection ... listOfLists) {
        return new ListListIterator(listOfLists);
    }
    
    
    public static Vector<String> tokenize(String s) {
        Vector<String> tokens = new Vector<String>();
        Matcher m = Pattern.compile("([a-z]+|[A-Z]+[a-z]*|[0-9]+|\\S)").matcher(s);
        while (m.find()) {
            tokens.add(m.group(1).toLowerCase());
        }
        return tokens;
    }
    
    public static String removeNestedAngleBrackets(String s) {
        while (true) {
            String newS = s.replaceAll("<[^<>]*>", "");
            if (newS.equals(s)) {
                break;
            }
            s = newS;
        }
        return s;
    }
    
    public static PrintWriter debugOut;
    public static void debug(String blah) {
        System.out.println(blah);
//        if (debugOut == null) {
//            try {
//                debugOut = new PrintWriter(new FileWriter("c:/Working/debugOut.txt"), true);
//            } catch (IOException e) {
//                throw new Error(e);
//            }
//        }
//        debugOut.println(blah);
    }
    
    public static double sum(double[] a) {
        double sum = 0;
        for (double aa : a) {
            sum += aa;
        }
        return sum;
    }
    
    public static <T> int intersectionSize(Set<T> s1, Set<T> s2) {
        if (s1.size() <= s2.size()) {
            int count = 0;
            for (T t : s1) {
                if (s2.contains(t)) {
                    count++;
                }
            }
            return count;
        } else {
            return intersectionSize(s2, s1);
        }
    }
		
	public static ArrayList<String> test() {
		ArrayList<String> t = new ArrayList<String>();
		t.add("hello");
		t.add("what?");
		return t;
	}
    
    public static <T> Vector<Vector<T>> permutations(java.util.List<T> list) {
        Vector<Vector<T>> permutations = new Vector<Vector<T>>();
        permutationsHelper(new LinkedList<T>(list), new LinkedList<T>(), permutations);
        return permutations;
    }
    public static <T> void permutationsHelper(LinkedList<T> remaining, LinkedList<T> listSoFar, Vector<Vector<T>> results) {
        if (remaining.size() == 0) {
            results.add(new Vector<T>(listSoFar));
        } else {
            for (int i = 0; i < remaining.size(); i++) {
                listSoFar.add(remaining.removeFirst());
                permutationsHelper(remaining, listSoFar, results);
                remaining.add(listSoFar.removeLast());
            }
        }        
    }
    
    public static <K, V> Vector<Pair<K, V>> getPairs(Map<K, V> m) {
        Vector<Pair<K, V>> pairs = new Vector<Pair<K, V>>();
        for (K k : m.keySet()) {
            pairs.add(new Pair<K, V>(k, m.get(k)));
        }
        return pairs;
    }
    
    public static <A, B> Vector<B> right(Vector<Pair<A, B>> pairs) {
        Vector<B> v = new Vector<B>();
        for (Pair<A, B> pair : pairs) {
            v.add(pair.right);
        }
        return v;
    }
    
    public static <A, B> Vector<A> left(Vector<Pair<A, B>> pairs) {
        Vector<A> v = new Vector<A>();
        for (Pair<A, B> pair : pairs) {
            v.add(pair.left);
        }
        return v;
    }
    
    // Profiling Stuff
    
    public static class ProfileEntry {
        public long lastStartTime = -1;
        public long timeAccum = 0;
            
        public ProfileEntry() {
        }
        
        public void begin() {
            lastStartTime = System.currentTimeMillis();
        }
        
        public void end() {
            timeAccum += System.currentTimeMillis() - lastStartTime;
            lastStartTime = -1;
        }
        
        public double seconds() {
            return (double)timeAccum / 1000.0;
        }
    }
        
    public static Map<String, ProfileEntry> profileEntries = new HashMap<String, ProfileEntry>();
    public static Bag<String> profileCounts = new Bag<String>();
        
    public static void profileClear() {
        profileEntries = new HashMap<String, ProfileEntry>();
        profileCounts = new Bag<String>();
    }
    
    public static void profileCount(String tag) {
        profileCounts.add(tag);
    }
        
    public static void profile(String tag) {
        ProfileEntry pe = profileEntries.get(tag);
        if (pe == null) {
            pe = new ProfileEntry();
            profileEntries.put(tag, pe);
        }
        
        if (pe.lastStartTime < 0) {
            pe.begin();
        } else {
            pe.end();
        }
    }
        
    public static void profileStart(String tag) {
        profileBegin(tag);
    }
    public static void profileBegin(String tag) {
        ProfileEntry pe = profileEntries.get(tag);
        if (pe == null) {
            pe = new ProfileEntry();
            profileEntries.put(tag, pe);
        }
        
        pe.begin();
    }
    
    public static void profileStop(String tag) {
        profileEnd(tag);
    }
    public static void profileEnd(String tag) {
        ProfileEntry pe = profileEntries.get(tag);
        if (pe == null) {
            pe = new ProfileEntry();
            profileEntries.put(tag, pe);
        }
        
        pe.end();
    }
    
    public static void profilePrint() {
        for (String tag : profileEntries.keySet()) {
            ProfileEntry pe = profileEntries.get(tag);
            debug(tag + ": " + pe.seconds());
        }
        for (String tag : profileCounts.keySet()) {
            debug(tag + ": " + profileCounts.get(tag));
        }
    }
    
    public static String profileToString() {
        StringBuffer buf = new StringBuffer();
        for (String tag : profileEntries.keySet()) {
            ProfileEntry pe = profileEntries.get(tag);
            buf.append(tag + ": " + pe.seconds() + "\n");
        }
        return buf.toString();
    }
    
    // Misc Stuff
    
    public static String makeJavaQuote(String s) {
        return "\"" + s.replaceAll("\\\\", "\\\\").replaceAll("\"", "\\\"") + "\"";
    }
        
    public static String join(Collection<String> strings, String joiner) {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (String s : strings) {
            if (!first) {
                buf.append(joiner);
            }
            
            buf.append(s);
            
            first = false;
        }
        return buf.toString();
    }
    
    public static <T> Vector<T> vector(Iterable<T> it) {
        Vector<T> v = new Vector<T>();
        for (T t : it) {
            v.add(t);
        }
        return v;
    }
    
    // Xml Stuff
        
    public static class NodeListIterator implements Iterable<Node>, Iterator<Node> {
        NodeList nl;
        int i;
        
        public NodeListIterator(NodeList nl) {
            this.nl = nl;
            this.i = 0;
        }
        
        public Iterator<Node> iterator() {
            return this;
        }
        
        public boolean hasNext() {
            return i < nl.getLength();
        }
        
        public Node next() {
            return nl.item(i++);
        }
        
        public void remove() {
            throw new IllegalArgumentException("not implemented");
        }
        
        public Vector<Node> vector() {
            return u.vector(this);
        }
    }
        
    public static class ElementListIterator implements Iterable<Element>, Iterator<Element> {
        NodeList nl;
        int i;
        
        public ElementListIterator(NodeList nl) {
            this.nl = nl;
            this.i = 0;
        }
        
        public Iterator<Element> iterator() {
            return this;
        }
        
        public boolean hasNext() {
            return i < nl.getLength();
        }
        
        public Element next() {
            return (Element)nl.item(i++);
        }
        
        public void remove() {
            throw new IllegalArgumentException("not implemented");
        }
        
        public Vector<Element> vector() {
            return u.vector(this);
        }
    }
        
    public static class NodeChildIterator implements Iterable<Element>, Iterator<Element> {
        Node root;
        Node cursor;
        
        public NodeChildIterator(Node root) {
            this.root = root;
            this.cursor = root.getFirstChild();
            while (cursor != null && !(cursor instanceof Element)) {
                cursor = cursor.getNextSibling();
            }
        }
        
        public Iterator<Element> iterator() {
            return this;
        }
        
        public boolean hasNext() {
            return cursor != null;
        }
        
        public Element next() {
            Node save = cursor;
            cursor = cursor.getNextSibling();
            while (cursor != null && !(cursor instanceof Element)) {
                cursor = cursor.getNextSibling();
            }
            return (Element)save;
        }
        
        public void remove() {
            throw new IllegalArgumentException("not implemented");
        }
        
        public Vector<Element> vector() {
            return u.vector(this);
        }
    }
    
    public static DocumentBuilder documentBuilder;
    public static XPathFactory xpathFactory = XPathFactory.newInstance();
    public static XPath x = XPathFactory.newInstance().newXPath();
    public static Map<String, XPathExpression> xpaths = new HashMap<String, XPathExpression>();
        
    public static DocumentBuilder getDocumentBuilder() throws Exception {
        if (documentBuilder == null) {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        return documentBuilder;
    }
    
    public static Document createXml() throws Exception {
        return createDocument();
    }
    public static Document createDocument() throws Exception {
        Document d = getDocumentBuilder().newDocument();
        d.appendChild(d.createElement("body"));
        return d;
    }
    
    public static Document loadXml(File file) throws Exception {
        DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return b.parse(new FileInputStream(file));
    }
    
    public static Document loadXmlString(String fromMe) throws Exception {
        DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return b.parse(new StringBufferInputStream(fromMe));
    }
    
    public static void saveXml(File f, Document d) throws Exception {
        save(f, d);
    }
    public static void saveXml(Document d, File f) throws Exception {
        save(d, f);
    }
    
    public static void save(Document d, File f) throws Exception {
        TransformerFactory.newInstance().newTransformer().transform(
            new DOMSource(d), new StreamResult(f));
    }
    
    public static void save(File f, Document d) throws Exception {
        TransformerFactory.newInstance().newTransformer().transform(
            new DOMSource(d), new StreamResult(f));
    }
    
    public static String toString(Document d) throws Exception {
        StringWriter s = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(
            new DOMSource(d), new StreamResult(s));
        return s.toString();
    }
    
    public static XPathExpression getXPath(String xpath) throws Exception {
        XPathExpression e = xpaths.get(xpath);
        if (e == null) {
            e = x.compile(xpath);
            xpaths.put(xpath, e);
        }
        return e;
    }
    
    public static String getTextContent(Node n) throws Exception {
        StringBuffer buf = new StringBuffer();
        getTextContent(n, buf);
        return buf.toString();
    }
    
    public static void getTextContent(Node n, StringBuffer buf) throws Exception {
        if (n instanceof Text) {
            if (buf.length() > 0) {
                buf.append(" ");
            }
            buf.append(n.getNodeValue());
        } else {
            n = n.getFirstChild();
            while (n != null) {
                getTextContent(n, buf);
                n = n.getNextSibling();
            }
        }
    }
    
    public static NodeChildIterator getChildren(Node root) {
        return children(root);
    }
    
    public static NodeChildIterator children(Node root) {
        return new NodeChildIterator(root);
    }
    
    public static NodeListIterator getNodes(String xpath, Node root) throws Exception {
        return new NodeListIterator((NodeList)getXPath(xpath).evaluate(root, XPathConstants.NODESET));
    }
    
    public static NodeListIterator getNodes(Node root) throws Exception {
        return new NodeListIterator((NodeList)getXPath("//*").evaluate(root, XPathConstants.NODESET));
    }
    
    public static ElementListIterator getElements(String xpath, Node root) throws Exception {
        return new ElementListIterator((NodeList)getXPath(xpath).evaluate(root, XPathConstants.NODESET));
    }
    
    public static ElementListIterator getElements(Node root) throws Exception {
        return new ElementListIterator((NodeList)getXPath("//*").evaluate(root, XPathConstants.NODESET));
    }
    
    public static boolean getBoolean(String xpath, Node root) throws Exception {
        return (Boolean)getXPath(xpath).evaluate(root, XPathConstants.BOOLEAN);
    }
    
    public static String getString(String xpath, Node root) throws Exception {
        return (String)getXPath(xpath).evaluate(root, XPathConstants.STRING);
    }
   
    public static Node getNode(String xpath, Node root) throws Exception {
        return (Node)getXPath(xpath).evaluate(root, XPathConstants.NODE);
    }
    
    public static Element getElement(String xpath, Node root) throws Exception {
        return (Element)getXPath(xpath).evaluate(root, XPathConstants.NODE);
    }
    
    public static String getXPath(Node n) throws Exception {
        String name = n.getNodeName();
        int index = 0;
        StringBuffer xpath = new StringBuffer();
        while (true) {
            Node prev = n.getPreviousSibling();
            if (prev == null) {
                if (xpath.length() > 0) {
                    xpath.insert(0, "/");
                }
                xpath.insert(0, "]");
                xpath.insert(0, index + 1);
                xpath.insert(0, "[");
                xpath.insert(0, name);
                
                n = n.getParentNode();
                if (n == null) {
                    break;
                }
                name = n.getNodeName();
                if (name.equals("#document")) {
                    break;
                }
                
                index = 0;
            } else {
                n = prev;
                if (n.getNodeName().equals(name)) {
                    index++;
                }
            }
        }
        return xpath.toString();
    }
    
    // Window Stuff
    public static void exitOnClose(JFrame f) {
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    // Calendar Date Time Stuff
    public static String[] shortDay = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    public static String[] shortMonth = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    public static String[] longDay = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saterday"};
    public static String[] longMonth = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    public static int getMonth(String s) {
        Vector<String> longMonths = new Vector<String>(Arrays.asList(longMonth));
        return longMonths.indexOf(s);
    }
    
    // Exception Stuff
    public static void myCatch(Exception e) {
        e.printStackTrace();
        System.exit(1);
    }
	
	// String Stuff
	public static void blahh() {
	}
	
	public static String concat(Vector stringUsTogether) {
		return concat(stringUsTogether, " ");
	}
	
	public static String concat(Vector stringUsTogether, String separator) {
		StringBuffer buf = new StringBuffer();
		boolean firstTime = true;
		for (Object o : stringUsTogether) {
			if (firstTime) {
				firstTime = false;
			} else {
				buf.append(separator);
			}
			buf.append(o);
		}
		return buf.toString();
	}
    
    // Debug Printing Stuff
    
    public static void print(Map m) {
        for (Object o : m.keySet()) {
            System.out.println(o + " = " + m.get(o));
        }
    }
    public static void print(Collection c) {
        for (Object o : c) {
            System.out.println(o);
        }
    }
    public static void println(Collection c) {
        for (Object o : c) {
            System.out.println(o);
        }
    }
    public static void println(Object[] a) {
        for (Object o : a) {
            System.out.println(o);
        }
    }
    public static void print(Object[] a) {
        for (Object o : a) {
            System.out.println(o);
        }
    }
    
    public static void printIndent(int howFar) {
        indent(howFar);
    }
    public static void indent(int howFar) {
        for (int i = 0; i < howFar; i++) {
            System.out.print("\t");
        }
    }
    public static void indent(int howFar, String c) {
        for (int i = 0; i < howFar; i++) {
            System.out.print(c);
        }
    }
    
	// Color Stuff
	
	public static Color randomColor() {
		return getRandomColor();
	}
	public static Color getRandomColor() {
		return new Color(Color.HSBtoRGB((float)u.r.nextDouble(), 1.0f, 1.0f));
	}
	
	// Random Stuff
    
    public static <T> T random(Vector<T> v) {
        return v.get(r.nextInt(v.size()));
    }
	
	public static boolean flipCoin() {
		return r.nextBoolean();
	}
	
	public static boolean flipCoin(double headWeight) {
		return r.nextDouble() < headWeight;
	}
	
	public static <T> T getRandomElement(AbstractCollection<T> collection) throws Exception {
		int randomIndex = r.nextInt(collection.size());
		int index = 0;
		for (T element : collection) {
			if (index == randomIndex)
				return element;
			index++;
		}
		throw new Exception("this shouldn't happen.");
	}
		
	// Enumeration Stuff
	public static ArrayList<String> getStringList(Enumeration e) {
		ArrayList<String> list = new ArrayList<String>();
		while (e.hasMoreElements()) {
			list.add((String)e.nextElement());
		}
		return list;
	}
	public static <T> ArrayList<T> toList(Enumeration<T> e) {
		return getList(e);
	}
	public static <T> ArrayList<T> getList(Enumeration<T> e) {
		ArrayList<T> list = new ArrayList<T>();
		while (e.hasMoreElements()) {
			list.add(e.nextElement());
		}
		return list;
	}
		
	// Set Stuff
	public static <T> HashSet<T> getSubset(HashSet<T> source, int sizeOfSubset) {
		T[] array = (T[])source.toArray();
		HashSet<T> subset = new HashSet<T>();
		while (subset.size() < sizeOfSubset) {
			subset.add(array[u.r.nextInt(array.length)]);
		}
		return subset;
	}
	
	// Font Stuff
    public static Font monoFont;
    public static Font getMonoFont() {
        if (monoFont == null) {
            monoFont = new Font("Monospaced", Font.PLAIN, 12);
        }
        return monoFont;
    }
    public static Font getFixedWidthFont() {
        return getMonoFont();
    }
    
	public static Dimension getSize(Graphics2D g, String s) {
		//~ Rectangle2D rect = g.getFont().getStringBounds(s, g.getFontRenderContext());		
		//~ return new Dimension((int)rect.getWidth(), (int)rect.getHeight());
		
		TextLayout t = new TextLayout(s, g.getFont(), g.getFontRenderContext());
		Rectangle2D rect = t.getBounds();
		return new Dimension((int)rect.getWidth(), (int)rect.getHeight());
	}
	
	public static void drawString(Graphics2D g, String s, int x, int y, double hotX, double hotY) {
		Dimension size = getSize(g, s);
		g.drawString(s,
			(int)(x - (hotX * size.getWidth())),
			(int)lerp(0, y + size.getHeight(), 1, y, hotY));
		
		//~ g.drawRect( 
			//~ (int)(x - (hotX * size.getWidth())),
			//~ (int)lerp(0, y + size.getHeight(), 1, y, hotY) - (int)size.getHeight(),
			//~ (int)size.getWidth(),
			//~ (int)size.getHeight());
	}
	
	public static void drawStringWithBorder(Graphics2D g, String s, int x, int y, double hotX, double hotY, Color front, Color back) {
		g.setColor(back);
		for (int xOffset = -1; xOffset <= 1; xOffset++) {
			for (int yOffset = -1; yOffset <= 1; yOffset++) {
				drawString(g, s, x + xOffset, y + yOffset, hotX, hotY);
			}
		}
		g.setColor(front);
		drawString(g, s, x, y, hotX, hotY);
	}
	
	// HTML Stuff (derived from http://www.rgagnon.com/javadetails/java-0306.html)
	public static String HTMLencode(String s) {
		return escapeHTML(s);
	}
	public static String encodeHTML(String s) {
		return escapeHTML(s);
	}
	public static String escapeHTML(String s) {
		StringBuffer sb = new StringBuffer();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '&': sb.append("&amp;"); break;
				case '"': sb.append("&quot;"); break;
				case '\n': sb.append("<br>\r\n"); break;
				case ' ': sb.append("&nbsp;"); break;
				case '\t': sb.append("&nbsp; &nbsp; "); break;
				
				default:
					if (c >= ' ' && c <= 0x7e)
						sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	// Math Stuff
    
    public static double[] minus(double[] a, double[] b) {
        return diff(a, b);
    }
    public static double[] diff(double[] a, double[] b) {
        double[] c = new double[a.length];
        
        for (int i = 0; i < a.length; i++) {
            c[i] = a[i] - b[i];
        }
        
        return c;
    }
    public static double[] minusEquals(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] - b[i];
        }
        return a;
    }
    
    public static double angleBetween(double[] a, double[] b) {
        return angle(a, b);
    }
    public static double angle(double[] a, double[] b) {
        return Math.acos(cosMetric(a, b));
    }
    
    public static double cosMetric(double[] a, double[] b) {
        return dot(a, b) / (length(a) * length(b));
    }
    
    public static double getLength(double[] a) {
        return length(a);
    }
    public static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }
    public static double dist(double[] a, double[] b) {
        return length(a, b);
    }
    public static double length(double[] a, double[] b) {
        return length(minus(a, b));
    }
    
    public static double dotProduct(double[] a, double[] b) {
        return dot(a, b);
    }
    public static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
	
	public static double hypersphereVolume(double radius, int dim) {
		// see http://mathworld.wolfram.com/Hypersphere.html
		return unitHypersphereSurfaceArea(dim) * Math.pow(radius, dim) / dim;
	}
	public static double unitHypersphereSurfaceArea(int dim) {
		// see http://mathworld.wolfram.com/Hypersphere.html
		if (dim % 2 == 0) {
			return
				2.0 * Math.pow(Math.PI, dim / 2) /
				factorial(dim/2 - 1);
		} else {
			return
				Math.pow(2, (dim+1) / 2) * Math.pow(Math.PI, (dim-1) / 2) /
				factorial2(dim - 2);
		}
	}
    public static long factorial(long n) {
		if (n == 0) {
			return 1;
		} else {
			return n * factorial(n - 1);
		}
	}
	public static long factorial2(long n) {
		if (n <= 1) {
			return 1;
		} else {
			return n * factorial2(n - 2);
		}
	}
	
	public static double getMean(Vector<Double> values) {
		double sum = 0.0;
		for (Double v : values) {
			sum += v;
		}
		return sum / values.size();
	}
	
	public static double getVariance(Vector<Double> values, double mean) {
		double sum = 0.0;
		for (Double v : values) {
			sum += Math.pow(v - mean, 2.0);
		}
		return sum / (values.size() - 1);
	}
	
	public static double getVariance(Vector<Double> values) {
		return getVariance(values, getMean(values));
	}
	
	public static boolean boxesIntersect(int a_x, int a_y, int a_width, int a_height, int b_x, int b_y, int b_width, int b_height) {
		if (a_x + a_width - 1 < b_x)
			return false;
		if (b_x + b_width - 1 < a_x)
			return false;
		if (a_y + a_height - 1 < b_y)
			return false;
		if (b_y + b_height - 1 < a_x)
			return false;
		return true;
	}
	
	public static Color lerp(double t0, Color v0, double t1, Color v1, double t) {
        return new Color(
            (int)lerp(t0, v0.getRed(), t1, v1.getRed(), t),
            (int)lerp(t0, v0.getGreen(), t1, v1.getGreen(), t),
            (int)lerp(t0, v0.getBlue(), t1, v1.getBlue(), t),
            (int)lerp(t0, v0.getAlpha(), t1, v1.getAlpha(), t));
	}
	
	public static double lerp(double t0, double v0, double t1, double v1, double t) {
		return (t - t0) * (v1 - v0) / (t1 - t0) + v0;
	}
	
	public static double lerpCap(double t0, double v0, double t1, double v1, double t) {
        if (t <= t0) return v0;
        if (t >= t1) return v1;
		return (t - t0) * (v1 - v0) / (t1 - t0) + v0;
	}
	
	// Digest Stuff
	public static String getSHA1(String digestMe) throws Exception {
		MessageDigest d = MessageDigest.getInstance("SHA-1");
		d.reset();
		d.update(digestMe.getBytes());
		return new BigInteger(1, d.digest()).toString(16);
	}
	public static String getSHA(String digestMe) throws Exception {
		return getSHA1(digestMe);
	}
	
	// Math Stuff
	public static double cubeRoot(double x) {
		if (x < 0) {
			return -Math.pow(-x, 1.0 / 3.0);
		} else {
			return Math.pow(x, 1.0 / 3.0);
		}
	}
	
	// Regex Stuff
	public static Matcher m;
	public static Matcher createMatcher(String regex, String s) {
		return Pattern.compile(regex).matcher(s);
	}
	public static boolean matches(String regex, String s) {
		return match(regex, s);
	}
	public static boolean hasMatch(String regex, String s) {
		return match(regex, s);
	}
	public static boolean match(String regex, String s) {
		m = createMatcher(regex, s);
		return m.find();
	}
	public static void replaceInFiles(Vector files, String replaceThis, String withThis) throws Exception {
		for (int i = 0; i < files.size(); i++) {
			File f = (File)files.get(i);
			
			replaceInFile(f, replaceThis, withThis);
		}
	}
	public static void replaceInFile(File f, String replaceThis, String withThis) throws Exception {
		save(f, slurp(f).replaceAll(replaceThis, withThis));
	}
	
	// Command Line Stuff
	public static boolean run_error = false;
	public static void run(String command) throws Exception {
		class SystemOutPipe extends Thread {
			BufferedInputStream in;
			boolean setError;
			
			public SystemOutPipe(InputStream inputStream, boolean setError) {
				in = new BufferedInputStream(inputStream);
				start();
			}
			
			public void run() {
				try {
					int c;
					while (!interrupted() && (c = in.read()) >= 0) {
						if (setError)
							run_error = true;
						System.out.print((char)c);
					}
				} catch (InterruptedIOException ex) {
					// this is fine, we just stop
				} catch (Exception ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
		}
		
		System.out.println("Executing: " + command);
		Process p = Runtime.getRuntime().exec(command);
		run_error = false;
		SystemOutPipe inputPipe = new SystemOutPipe(p.getInputStream(), false);
		SystemOutPipe errorPipe = new SystemOutPipe(p.getErrorStream(), true);
		p.waitFor();
		inputPipe.interrupt();
		errorPipe.interrupt();
		if (p.exitValue() != 0 || run_error)
			throw new IllegalArgumentException("Error executing: " + command);
		else
			System.out.println("Done Executing: " + command);
	}
	
	// Graphics Stuff
	public static void layTile(Graphics g, Image i, int width, int height) {
		tile(g, i, width, height);
	}
	public static void tile(Graphics g, Image i, int width, int height) {
		int xOffset = 0;
		int yOffset = 0;
		while (true) {
			g.drawImage(i, xOffset, yOffset, null);
			xOffset += i.getWidth(null);
			if (xOffset >= width) {
				xOffset = 0;
				yOffset += i.getHeight(null);
				if (yOffset >= height) {
					break;
				}
			}
		}
	}
	
	public static void drawPoint(Graphics g, double x, double y) {
		drawPoint(g, x, y, 4);
	}
	public static void drawPoint(Graphics g, double x, double y, double size) {
		g.fillOval((int)(x - (size / 2)), (int)(y - (size / 2)), (int)size, (int)size);
	}
	public static void drawPoint(Graphics g, double x, double y, Color c, double size) {
		g.setColor(c);
		g.fillOval((int)(x - (size / 2)), (int)(y - (size / 2)), (int)size, (int)size);
	}
	
	// Timer Stuff
	static long beginTime = 0;
	static long endTime = 0;
	public static void startTimer() {
		beginTime = new Date().getTime();
	}
	public static double stopTimer() {
		endTime = new Date().getTime();		
		return readTimer();
	}
	public static double readTimer() {
		return (double)(endTime - beginTime) / 1000.0;
	}
	
	public static class LoopTimer {
		long lastSampleTime = new Date().getTime();
		double averageLoopTime = -1.0;
		static final double averageLoopTime_newSampleWeight = 0.3;
		static final DecimalFormat f = new DecimalFormat("00");
		
		public void print(int remainingIterations) {
			// update average
			long curSampleTime = new Date().getTime();
			double loopTime = (double)(curSampleTime - lastSampleTime) / 1000.0;
			if (averageLoopTime < 0.0) {
				averageLoopTime = loopTime;
			} else {
				averageLoopTime = averageLoopTime_newSampleWeight * loopTime + (1.0 - averageLoopTime_newSampleWeight) * averageLoopTime;
			}
			lastSampleTime = curSampleTime;
			
			// print estimate
			int totalSeconds = (int)(averageLoopTime * remainingIterations);
			int seconds = totalSeconds % 60;
			int minutes = (totalSeconds / 60) % 60;
			int hours = totalSeconds / 60 / 60;
			
			System.out.println("time to go: " + f.format(hours) + ":" + f.format(minutes) + ":" + f.format(seconds));
		}
	}
	
	// File Stuff
    public static PrintWriter printWriter(String filename) throws Exception {
        return new PrintWriter(new FileWriter(new File(filename)), true);
    }
    
	public static void saveObject(Serializable obj, String filename) throws Exception {
		saveObject(obj, new File(filename));
	}
	public static void saveObject(Serializable obj, File f) throws Exception {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
		out.writeObject(obj);
		out.close();
	}
	public static Object loadObject(String filename) throws Exception {
		return loadObject(new File(filename));
	}
	public static Object loadObject(File f) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
		Object obj = in.readObject();		
		in.close();
		return obj;
	}
	
	public static String getFreeFilename(String filename) {
		if (new File(filename).exists()) {
			String preExt;
			String ext;
			if (match("(.*?)(\\.[^\\.\\\\/]+)", filename)) {
				preExt = m.group(1);
				ext = m.group(2);
			} else {
				preExt = filename;
				ext = "";
			}
		
			for (int i = 2; ; i++) {
				if (new File(preExt + i + ext).exists()) {
				} else {
					return preExt + i + ext;
				}
			}
		} else {
			return filename;
		}
	}
	
	
	public static String slurp(String filename) throws Exception {
		return slurp(new File(filename));
	}
	public static String slurp(File f) throws Exception {
		int length = (int)f.length();
		char buf[] = new char[length];
		BufferedReader in = new BufferedReader(new FileReader(f));
		in.read(buf, 0, length);
		in.close();
		return new String(buf);
	}
	public static String slurp(BufferedReader in) throws Exception {
		StringBuffer buf = new StringBuffer();
		while (true) {
			int c = in.read();
			if (c == -1) {
				break;
			} else {
				buf.append((char)c);
			}
		}
		return buf.toString();
	}
	public static String slurp(URL url) throws Exception {
		return slurp(new BufferedReader(new InputStreamReader(url.openStream())));
	}
	public static String webPost(URL url, String ... args) throws Exception {
		URLConnection c = url.openConnection();
		c.setDoOutput(true);
		OutputStreamWriter out = new OutputStreamWriter(c.getOutputStream());
		String argString = "";
		for (int i = 0; i < args.length; i += 2) {
			if (i != 0) {
				argString += "&";
			}
			argString +=
				URLEncoder.encode(args[i], "UTF-8") + "=" +
				URLEncoder.encode(args[i + 1], "UTF-8");
		}
		out.write(argString);
		out.flush();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
		
		String text = slurp(in);
		
		in.close();
		out.close();
		return text;
	}
	
	public static void saveString(File f, String s) throws Exception {
		save(f, s);
	}
	public static void save(File f, String s) throws Exception {
		PrintWriter out = new PrintWriter(new FileWriter(f), true);
		out.print(s);
		out.close();
	}
	
	public static Vector<File> getFilesDeep(String dir, String regex) {
		return getFilesDeep(new File(dir), regex);
	}
	public static Vector<File> getFilesDeep(File dir, String regex) {
		Vector<File> v = new Vector<File>();
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.isDirectory()) {
				v.addAll(getFilesDeep(f, regex));
			} else if (f.getName().matches(regex)) {
				v.add(f);
			}
		}
		return v;
	}
	public static Vector<File> getDirsDeep(File dir) {
		Vector<File> v = new Vector<File>();
		v.add(dir);
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				v.addAll(getDirsDeep(f));
			}
		}
		return v;
	}
	
	// Sorting Stuff
	public static void sortArrayUsingArray(int[] sortMe, double[] usingMe, boolean ascending) {
		class Pair implements Comparable {
			public int a;
			public double b;
			public boolean ascending;
			public Pair(int a, double b, boolean ascending) {
				this.a = a;
				this.b = b;
				this.ascending = ascending;
			}
			public int compareTo(Object o) {
				Pair that = (Pair)o;
				return (new Double(this.b).compareTo(new Double(that.b))) * (ascending ? 1 : -1);
			}
		}
		Vector<Pair> v = new Vector<Pair>();
		for (int i = 0; i < sortMe.length; i++) {
			v.add(new Pair(sortMe[i], usingMe[i], ascending));
		}
		Collections.sort(v);
		for (int i = 0; i < sortMe.length; i++) {
			sortMe[i] = v.get(i).a;
		}
	}
	public static void sortArrayUsingArray(Object[] sortMe, double[] usingMe, boolean ascending) {
		class Pair implements Comparable {
			public Object a;
			public double b;
			public boolean ascending;
			public Pair(Object a, double b, boolean ascending) {
				this.a = a;
				this.b = b;
				this.ascending = ascending;
			}
			public int compareTo(Object o) {
				Pair that = (Pair)o;
				return (new Double(this.b).compareTo(new Double(that.b))) * (ascending ? 1 : -1);
			}
		}
		Vector<Pair> v = new Vector<Pair>();
		for (int i = 0; i < sortMe.length; i++) {
			v.add(new Pair(sortMe[i], usingMe[i], ascending));
		}
		Collections.sort(v);
		for (int i = 0; i < sortMe.length; i++) {
			sortMe[i] = ((Pair)v.get(i)).a;
		}
	}
	public static void sortVectorUsingArray(Vector sortMe, double[] usingMe, boolean ascending) {
		class Pair implements Comparable {
			public Object a;
			public double b;
			public boolean ascending;
			public Pair(Object a, double b, boolean ascending) {
				this.a = a;
				this.b = b;
				this.ascending = ascending;
			}
			public int compareTo(Object o) {
				Pair that = (Pair)o;
				return (new Double(this.b).compareTo(new Double(that.b))) * (ascending ? 1 : -1);
			}
		}
		Vector<Pair> v = new Vector<Pair>();
		for (int i = 0; i < sortMe.size(); i++) {
			v.add(new Pair(sortMe.get(i), usingMe[i], ascending));
		}
		Collections.sort(v);
		sortMe.clear();
		for (int i = 0; i < v.size(); i++) {
			sortMe.add(((Pair)v.get(i)).a);
		}
	}
	public static int getWeightedRandomIndex(double standardDeviations, int maxIndex) {
		//
		// this function returns an index in the range [0, maxIndex - 1],
		// with a bias for returning low indexes according to the following diagram...
		//
		//  XXX    |
		//  XXXX   |-1 standard deviation	
		//  XXXX   |
		//  XXXXX  |
		//  XXXXX  |                           standardDeviations
		//  XXXXXX |                                   |
		//  XXXXXXX|                                   |
		//  XXXXXXXXXXX                                |
		//  XXXXXXXXXXXXXXXXXXXXXXX                    |
		//  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
		// [0]  [5] . . .                 . . .  [maxIndex - 1]
		//
		//
		// tested with...
		//
		// int[] bins = new int[150];
		// for (int i = 0; i < 10000; i++) {
		//     bins[MyUtil.weightedRandomIndex(3.3, bins.length)]++;
		// }
		// for (int i = 0; i < bins.length; i++) {
		//     System.out.println("bins[" + i + "] = " + bins[i]);
		// }
		//
		// and it seems to provide a nice gaussian distribution over the bins
		//
		double g = r.nextGaussian() / standardDeviations;
		if (g < 0) g *= -1;
		int index = (int)(maxIndex * g) % maxIndex;
		if (index < 0) index += maxIndex;
		return index;
	}
	
	// Array Stuff
	public static void printArray(int[] a) {
		System.out.println("array {");
		for (int i = 0; i < a.length; i++) {
			System.out.println("[" + i + "] = " + a[i]);
		}
		System.out.println("}");
	}
	public static void printArray(double[] a) {
		System.out.println("array {");
		for (int i = 0; i < a.length; i++) {
			System.out.println("[" + i + "] = " + a[i]);
		}
		System.out.println("}");
	}
}
