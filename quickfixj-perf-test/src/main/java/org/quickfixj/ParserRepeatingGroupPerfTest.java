package org.quickfixj;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.Message;
import quickfix.ValidationSettings;

/**
 * Benchmarks parsing messages with repeating groups (e.g., MDEntries).
 */
@State(Scope.Benchmark)
public class ParserRepeatingGroupPerfTest extends AbstractPerfTest {

    @Param({
            // Minimal MarketDataSnapshotFullRefresh with 3 MDEntries
            "8=FIX.4.4\u00019=200\u000135=W\u000149=SENDER\u000156=TARGET\u000134=1\u000152=20200101-00:00:00\u000155=SYMB\u0001268=3\u0001279=0\u0001270=12.34\u000110=000\u0001279=0\u0001270=12.35\u000110=000\u0001279=0\u0001270=12.36\u000110=000\u0001"
    })
    public String snapshot;

    private DataDictionary dd44;
    private ValidationSettings vs;

    @Setup
    public void setup() throws ConfigError {
        dd44 = new DataDictionary(ParserRepeatingGroupPerfTest.class.getClassLoader().getResourceAsStream("FIX44.xml"));
        vs = new ValidationSettings();
    }

    @Benchmark
    public Message parseSnapshot() throws Exception {
        Message m = new Message();
        m.fromString(snapshot, dd44, vs, false);
        return m;
    }
}

