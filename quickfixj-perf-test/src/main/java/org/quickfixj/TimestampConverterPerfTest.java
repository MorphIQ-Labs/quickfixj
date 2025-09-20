package org.quickfixj;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import quickfix.field.converter.UtcTimestampConverter;

import java.util.Date;

/**
 * Benchmarks UtcTimestampConverter in both parse and format directions.
 */
@State(Scope.Benchmark)
public class TimestampConverterPerfTest extends AbstractPerfTest {

    @Param({
            "20060324-01:05:58",
            "20060324-01:05:58.123",
            "20060324-01:05:58.123456",
            "20060324-01:05:58.123456789"
    })
    public String ts;

    private Date parsed;

    @Setup
    public void setup() throws Exception {
        // try parse with seconds-only precision; if it fails, parse with millis then nanos
        try {
            parsed = UtcTimestampConverter.convert(ts, false);
        } catch (Exception e) {
            parsed = UtcTimestampConverter.convert(ts, true);
        }
    }

    @Benchmark
    public Date parse_seconds() throws Exception {
        return UtcTimestampConverter.convert(ts, false);
    }

    @Benchmark
    public Date parse_millis() throws Exception {
        return UtcTimestampConverter.convert(ts, true);
    }

    @Benchmark
    public String format_seconds() {
        return UtcTimestampConverter.convert(parsed, false);
    }

    @Benchmark
    public String format_millis() {
        return UtcTimestampConverter.convert(parsed, true);
    }
}

