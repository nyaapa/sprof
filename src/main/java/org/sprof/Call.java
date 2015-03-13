package org.sprof;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Call {
	public final String function;
	private final Map<String, CallPoint> times = new HashMap<>();
	private final Map<String, List<Long>> calls = new HashMap<>();

	public Call(String name) {
		function = name;
	}

	public void push(String caller, long time) {
		String tCaller = caller + "##" + Thread.currentThread().getId();
		if (!calls.containsKey(tCaller))
			calls.put(tCaller, new LinkedList<Long>());
		calls.get(tCaller).add(time);
	}

	public void pop(String caller, long time) {
		String tCaller = caller + "##" + Thread.currentThread().getId();
		List<Long> stack = calls.get(tCaller);
		long start = stack.remove(stack.size() - 1);
		if (!times.containsKey(caller))
			times.put(caller, new CallPoint(caller));
		times.get(caller).addCall((time - start) / 1000000.0);
	}

	public String dump(String prefix) {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<String, CallPoint> calls : times.entrySet()) {
			result.append(prefix + "\t" + function + "\t" + calls.getValue() + "\n");
		}
		return result.toString();
	}

	private class CallPoint {
		public final String function;
		private long calls = 0;
		private double min = 0;
		private double max = 0;
		private double total = 0;

		public CallPoint(String name) {
			function = name;
		}

		public void addCall(double time) {
			if (total == 0) {
				max = time;
				min = time;
			} else if (time < min) {
				min = time;
			} else if (time > max) {
				max = time;
			}
			total += time;
			calls++;
		}

		@Override
		public String toString() {
			return function + "\t" + total + "\t" + calls + "\t" + min + "\t" + max + "\t0";
		}
	}

}
