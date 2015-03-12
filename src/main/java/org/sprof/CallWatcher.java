package org.sprof;

import javassist.CtClass;
import javassist.CtMethod;

import java.lang.System;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CallWatcher {
    public static final CallWatcher instance = new CallWatcher();
    private final long startTime = System.nanoTime();
    private final Map<String, Call> calls = new HashMap<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					instance.dump();
				} catch ( Exception e ) {
					System.err.println("Got " + e);
				}
				System.out.println("Trace done in /tmp/trace");
			}
		});
	}

    private CallWatcher() {}

    void dump() throws FileNotFoundException, UnsupportedEncodingException {
        double total = (System.nanoTime() - startTime) / 1000000.0;
        PrintWriter writer = new PrintWriter("/tmp/trace", "UTF-8");
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        String pid = processName.split("@")[0];
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        writer.print(time + "\t" + pid + "\t" + "MAIN" + "\t" + "\t" + total + "\t" + 1 + "\t" + total + "\t" + total + "\t" + 0 + "\n");
        for ( Call call : calls.values() ) {
            writer.print(call.dump(time + "\t" + pid));
        }
        writer.close();
    }

    public void push(String methodId, long startTime) {
		if ( !calls.containsKey(methodId) ) calls.put(methodId, new Call(methodId));
        calls.get(methodId).push(getCaller(), startTime);
    }

    public void pop(String methodId, long endTime) {
        calls.get(methodId).pop(getCaller(), endTime);
    }

    private String getCaller() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if ( trace.length < 5 )
            return "MAIN";
        else
            return getMethodName(trace[4]);
    }

    static public String getMethodName(CtMethod ctMethod) {
        String className;
        CtClass mClass = ctMethod.getDeclaringClass();
        if ( mClass.getName().contains("anonfun") ) {
            className = mClass.getName().split("\\$\\$anonfun\\$")[0] + ":" + ctMethod.getMethodInfo().getLineNumber(0) + " λ" ;
        } else
            className = shantiClassName(mClass.getName());
        String methName = ctMethod.getName();
        if ( methName.equals("apply") )
            methName = "()";
        else if ( methName.equals("apply$mcZI$sp") )
            methName = ".filter";
        else if ( methName.equals("apply$mcII$sp") )
            methName = ".map";
        else if ( methName.equals("apply$mcVI$sp") )
            methName = ".foreach";
        else methName = "." + methName;
        return className + methName;
    }

	static public String getMethodName(StackTraceElement ctMethod) {
        String className;
        if ( ctMethod.getClassName().contains("anonfun") )
            className = ctMethod.getClassName().split("\\$\\$anonfun\\$")[0] + ":" + ctMethod.getLineNumber() + " λ";
        else
            className = shantiClassName(ctMethod.getClassName());

        String methName = ctMethod.getMethodName();
        if ( methName.equals("apply") )
            methName = "()";
        else if ( methName.equals("apply$mcZI$sp") )
            methName = ".filter";
        else if ( methName.equals("apply$mcII$sp") )
            methName = ".map";
        else if ( methName.equals("apply$mcVI$sp") )
            methName = ".foreach";
        else methName = "." + methName;
        return className + methName;
    }

	static public String shantiClassName(String name) {if ( name.endsWith("$") ) {
        return "@" + name.replace("$", "");
    }
        return name;
    }

}
