Simple scala profiler draft as javaagent

Make binary via ```sbt assembly```

And then run ```java -javaagent:target/scala-2.10/sprof-assembly-0.1-SNAPSHOT.jar -jar your-cool-jar.jar``` and read output from ```/tmp/trace```