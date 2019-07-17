import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import MyUtil.*;

public class Analyzer {

    public static Vector<Vector<String>> loadFile(String filename)
            throws Exception {
        Vector<Vector<String>> v = new Vector<Vector<String>>();
        BufferedReader in = new BufferedReader(new FileReader(filename));
        while (in.ready()) {
            String line = in.readLine();
            Vector<String> vv = new Vector<String>(Arrays.asList(line
                    .split("\t")));
            v.add(vv);
        }
        return v;
    }

    public static Map<String, Vector<Vector<String>>> group(
            Vector<Vector<String>> data, int... columns) {
        Map<String, Vector<Vector<String>>> m = new HashMap<String, Vector<Vector<String>>>();
        for (Vector<String> v : data) {
            StringBuffer buf = new StringBuffer();
            for (int col : columns) {
                buf.append(v.get(col));
                buf.append("\t");
            }
            String key = buf.toString();

            Vector<Vector<String>> dest = m.get(key);
            if (dest == null) {
                dest = new Vector<Vector<String>>();
                m.put(key, dest);
            }
            dest.add(v);
        }
        return m;
    }

    public static double getValue(String s) {
        if (s.equals("true")) {
            return 1;
        } else if (s.equals("false")) {
            return 0;
        } else {
            return new Double(s);
        }
    }

    public static Vector<Double> getValues(Vector<String> v) {
        Vector<Double> values = new Vector<Double>();
        for (String s : v) {
            values.add(getValue(s));
        }
        return values;
    }

    public static Vector<Double> getValues(Vector<Vector<String>> v, int column) {
        Vector<Double> values = new Vector<Double>();
        for (Vector<String> ss : v) {
            values.add(getValue(ss.get(column)));
        }
        return values;
    }

    public static double calcAverage(Vector<Double> data) {
        double sum = 0;
        for (double d : data) {
            sum += d;
        }
        return sum / data.size();
    }

    public static double calcVariance(Vector<Double> data, double avg) {
        double sum = 0;
        for (double d : data) {
            sum += Math.pow(d - avg, 2);
        }
        return sum / (data.size() - 1);
    }

    public static double[] calcAverageAndStdError(Vector<Double> data) {
        double avg = calcAverage(data);
        double var = calcVariance(data, avg);
        double stdErr = Math.sqrt(var / data.size());
        return new double[] { avg, stdErr };
    }

    public static double[] calcAverageAndStdError(Vector<Vector<String>> data,
            int column) {
        return calcAverageAndStdError(getValues(data, column));

    }

    public static String printAverages(Vector<Vector<String>> data, int column) {
        StringBuffer buf = new StringBuffer();
        double[] a = calcAverageAndStdError(data, column);
        buf.append(a[0] + "\t" + a[1] + "\t" + data.size() + "\n");
        return buf.toString();
    }

    public static String printAverages(
            Map<String, Vector<Vector<String>>> data, int column) {
        StringBuffer buf = new StringBuffer();
        for (Map.Entry<String, Vector<Vector<String>>> e : data.entrySet()) {
            double[] a = calcAverageAndStdError(e.getValue(), column);
            buf.append(e.getKey() + a[0] + "\t" + a[1] + "\t"
                    + e.getValue().size() + "\n");
        }
        return buf.toString();
    }

    public static void printRowSamples(Vector<Vector<String>> data) {
        int i = 0;
        for (String s : data.get(0)) {
            System.out.println("[" + i + "] = " + s);
            i++;
        }
    }

    public static void newColumn_ColumnsEquals(Vector<Vector<String>> data,
            int colA, int colB) {
        for (Vector<String> v : data) {
            String a = v.get(colA);
            String b = v.get(colB);
            v.add("" + (a.equals(b)));
        }
    }
    
    public static String printLine(Vector<String> data) {
        StringBuffer buf = new StringBuffer();
        int i = 0;
        for (String s : data) {
            if (i > 0) {
                buf.append("\t");
            }
            buf.append(s);
            i++;
        }
        buf.append("\n");
        return buf.toString();
    }
    
    public static String printLines(Vector<Vector<String>> data) {
        String s = "";
        for (Vector<String> example : data) {
            s += printLine(example);
        }
        return s;
    }

    public static Vector<Vector<String>> getExamplesWhere(
            Vector<Vector<String>> data, int col, String value, int max) {

        Vector<Vector<String>> examples = new Vector<Vector<String>>();
        for (Vector<String> example : data) {
            if (example.get(col).equals(value)) {
                examples.add(example);
            }
        }
        examples.setSize(max);
        return examples;
    }

    public static void main(String[] args) throws Exception {
    	
//    	long last = System.currentTimeMillis();
//    	for (int i = 0; i < 1000000; i++) {
//    		long next = System.currentTimeMillis();
//    		if (next < last) {
//    			System.out.println("what?: " + i);
//    			break;
//    		}
//    		last = next;
//    	}
//    	
//    	System.exit(0);
    	
    	
        String outputDir = new File("../output").getCanonicalPath();
//        if (args.length > 0) {
//            outputDir = args[0];
//        }

        if (true) {
        	
//        	System.out.println("blah = " + outputDir
//                    + "/artificial_slop_output.txt");
        	
            Vector<Vector<String>> data = 
            	loadFile("C:\\Home\\OOPSLA06_test_framework\\output\\good23.txt");
            printRowSamples(data);
            
//            String output = printLines(getExamplesWhere(data, 7, "0", 20));

            System.out.println(printAverages(group(data, 0), 6));
            
//            File outputFile = new File(outputDir + "/artificial_slop_avgs.txt");
//            u.saveString(outputFile, output);            
        }
        
        if (false) {
            Vector<Vector<String>> data = loadFile(outputDir
                    + "/user_slop_output.txt");
            newColumn_ColumnsEquals(data, 3, 5);
            printRowSamples(data);

            u.saveString(new File(outputDir + "/user_slop_avgs.txt"),
                    printAverages(group(data, 0, 2, 3), 6));
        }

        if (false) {
            Vector<Vector<String>> data = loadFile(outputDir
                    + "/artificial_slop_output.txt");
            printRowSamples(data);
            
            String output = printLines(getExamplesWhere(data, 7, "0", 20));

//            output = printAverages(group(data, 10, 7), 6));
            
            File outputFile = new File(outputDir + "/artificial_slop_avgs.txt");
            u.saveString(outputFile, output);            
        }
    }
}
