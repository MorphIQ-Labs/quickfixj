# QuickFIX/J Performance test

This is a [JMH](https://github.com/openjdk/jmh) benchmark module for QuickFIX/J FIX protocol implementation. 

## How to run

### One‑command run (recommended)

Run JMH with the perf Maven profile (skips tests/javadoc and runs JMH at verify phase):

- Docker (Java 25):
```
docker run --rm -t -v "$PWD":/ws -w /ws openjdk:25 bash -lc './mvnw -P perf -pl quickfixj-perf-test -am verify'
```

- Local (if JDK 25 is installed):
```
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./mvnw -P perf -pl quickfixj-perf-test -am verify
```

By default, results are saved to a timestamped JSON file at `quickfixj-perf-test/target/perf/jmh-results-<timestamp>.json` and also printed to stdout.

To customize the output format/location, run the shaded JAR directly and pass JMH options, e.g.:

```
java -jar quickfixj-perf-test/target/quickfixj-perf-test.jar -wi 3 -i 5 -f 1 -tu ns -bm thrpt -rf csv -rff quickfixj-perf-test/target/perf/jmh-results.csv

You can also override perf profile properties, e.g.:

```
./mvnw -P perf -pl quickfixj-perf-test -am verify \
  -Dperf.wi=5 -Dperf.i=10 -Dperf.bm=thrpt -Dperf.rf=json -Dperf.tu=us
```
```

### Using your favorite IDE

Performance regression classes can be individually run using your favorite IDE.

### Creating executable jar for the performance testing

Build executable jar using following maven command

```
$ mvn clean package
```

Use following command to run complete set of performance regression test cases

```
$ java -jar target/quickfixj-perf-test.jar
```

You can list available performance benchmarks using `-l` option
```
$ java -jar target/quickfixj-perf-test.jar -l
```

You can run individual benchmarks by providing the class name or benchmark method name
```
$ java -jar target/quickfixj-perf-test.jar MessageCrackerPerfTest.crack

$ java -jar target/quickfixj-perf-test.jar MessageCrackerPerfTest
```

You can change the time unit used in the benchmark test using `-tu` option
Following command is an example of using micro second for describing benchmark test results.
```
$ java -jar target/quickfixj-perf-test.jar MessageCrackerPerfTest -tu us
```

For more available options use `-h` option

```
$ java -jar target/quickfixj-perf-test.jar -h
```

#### Guideline for future performance enhancements

1. Check if there is already benchmark available for the code you are planning to optimize.
2. If there is no benchmark code, first create a benchmark (Example `a-missing-perf-regression` branch) and make pull request to `master` branch.
3. Make your performance improvement in a new branch (Example `a-perf-improvement` branch).
4. When you make pull request provide comparison between current benchmark (`master`) and new benchmark values (`a-perf-improvement`)
5. This means providing output of the benchmark execution in two branches in a PR comment
