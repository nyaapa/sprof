package org.sprof;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LineNumberAttribute;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallWatcher {
	public static final CallWatcher instance = new CallWatcher();
	private final long startTime = System.nanoTime();
	private final Map<String, Call> calls = new HashMap<>();
	static final Map<String, String> classNames;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					instance.dump();
				} catch (Exception e) {
					System.err.println("Got " + e);
				}
			}
		});

		InputStream stream = CallWatcher.class.getResourceAsStream("/org/sprof/class_names");
		if ( stream != null ) {
			HashMap<String, String> tmp;
			try {
				ObjectInputStream o = new ObjectInputStream(stream);
				tmp = (HashMap<String, String>) o.readObject();
			} catch (Exception e) {
				System.err.println("Can't read class names: " + e);
				tmp = new HashMap<>();
			}
			classNames = tmp;
		} else {
			classNames = new HashMap<>();
		}
	}

	private CallWatcher() {
	}

	static public String getMethodName(CtMethod ctMethod) {
		CtClass mClass = ctMethod.getDeclaringClass();
		String className = classNames.get(mClass.getName());
		if ( className == null ) {
			if (mClass.getName().contains("$$anonfun$")) {
				className = mClass.getName().replaceAll("\\$\\$anonfun\\$", ".[λ]").replaceAll("\\$", "#") + ":" + ctMethod.getMethodInfo().getLineNumber(0);
			} else if (mClass.getName().contains("$$anon$")) {
				try {
					className =  mClass.getName().replace("$$anon", ".[α <: " + mClass.getSuperclass().getName() + "]").replaceAll("\\$", "#") + ":" + mClass.getEnclosingBehavior().getMethodInfo().getLineNumber(0);
				} catch (Exception e) {
					className = shantiClassName(mClass.getName());
				}
			} else
				className = shantiClassName(mClass.getName());
			classNames.put(mClass.getName(), className);
		}
		String methName = ctMethod.getName();
		if (methName.equals("apply"))
			methName = "()";
		else if (methName.equals("apply$mcZI$sp"))
			methName = ".filter";
		else if (methName.equals("apply$mcII$sp"))
			methName = ".map";
		else if (methName.equals("apply$mcVI$sp"))
			methName = ".foreach";
		else methName = "." + methName;
		return className + methName;
	}

	static public String getMethodName(StackTraceElement ctMethod) {
		String className = classNames.get(ctMethod.getClassName());
		if ( className == null ) {
			String orig = ctMethod.getClassName();
			if (orig.contains("$$anonfun$")) {
				className = orig.replaceAll("\\$\\$anonfun\\$", ".[λ]").replaceAll("\\$", "#") + ":???";
			} else if (orig.contains("$$anon$")) {
				try {
					className = orig.replace("$$anon", ".[α <: ???]").replaceAll("\\$", "#") + ":???";
				} catch (Exception e) {
					className = shantiClassName(orig);
				}
			} else
				className = shantiClassName(orig);
			classNames.put(orig, className);
		}
		String methName = ctMethod.getMethodName();
		if (methName.equals("apply"))
			methName = "()";
		else if (methName.equals("apply$mcZI$sp"))
			methName = ".filter";
		else if (methName.equals("apply$mcII$sp"))
			methName = ".map";
		else if (methName.equals("apply$mcVI$sp"))
			methName = ".foreach";
		else methName = "." + methName;
		return className + methName;
	}

	static public String shantiClassName(String name) {
		return (name.endsWith("$") ? ("[σ]" + name.replaceAll("\\$$", "")) : name).replaceAll("\\$", ".");
	}

	void dump() throws FileNotFoundException, UnsupportedEncodingException {
		double total = (System.nanoTime() - startTime) / 1000000.0;
		File theDir = new File("/tmp/traces/");
		if (!theDir.exists()) theDir.mkdir();
		String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		String pid = processName.split("@")[0];
		String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		String fname = "/tmp/traces/" + time + "." + pid;
		PrintWriter writer = new PrintWriter(fname, "UTF-8");
		writer.print(time + "\t" + pid + "\t" + "MAIN" + "\t" + "\t" + total + "\t" + 1 + "\t" + total + "\t" + total + "\t" + 0 + "\n");
		for (Call call : calls.values()) {
			writer.print(call.dump(time + "\t" + pid));
		}
		writer.close();
		System.out.println("Trace done in " + fname);
	}

	public void push(String methodId, long startTime) {
		if (!calls.containsKey(methodId)) calls.put(methodId, new Call(methodId));
		calls.get(methodId).push(getCaller(), startTime);
	}

	public void pop(String methodId, long endTime) {
		calls.get(methodId).pop(getCaller(), endTime);
	}

	private String getCaller() {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		if (trace.length < 5)
			return "MAIN";
		else
			return getMethodName(trace[4]);
	}

}
