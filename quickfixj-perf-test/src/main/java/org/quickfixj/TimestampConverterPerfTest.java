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
        // API changed: parsing is handled by convert(String) regardless of precision
        parsed = UtcTimestampConverter.convert(ts);
    }

    @Benchmark
    public Date parse_seconds() throws Exception {
        // Parse timestamp string to Date (milliseconds precision)
        return UtcTimestampConverter.convert(ts);
    }

    @Benchmark
    public Date parse_millis() throws Exception {
        // Same parsing path; retained for historical comparison naming
        return UtcTimestampConverter.convert(ts);
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
