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
 * Benchmarks parsing large messages by increasing repeating groups entries.
 */
@State(Scope.Benchmark)
public class LargeMessageParsePerfTest extends AbstractPerfTest {

    @Param({"50", "200", "1000"})
    public int entries;

    private DataDictionary dd44;
    private ValidationSettings vs;
    private String largeSnapshot;

    @Setup
    public void setup() throws ConfigError {
        dd44 = new DataDictionary(LargeMessageParsePerfTest.class.getClassLoader().getResourceAsStream("FIX44.xml"));
        vs = new ValidationSettings();
        String soh = "\u0001";
        StringBuilder msg = new StringBuilder(1024 + entries * 64);
        msg.append("8=FIX.4.4").append(soh)
           .append("9=").append(100).append(soh) // ignored
           .append("35=W").append(soh)
           .append("49=SENDER").append(soh)
           .append("56=TARGET").append(soh)
           .append("34=1").append(soh)
           .append("52=20200101-00:00:00").append(soh)
           .append("55=SYMB").append(soh)
           .append("268=").append(entries).append(soh);
        for (int i = 0; i < entries; i++) {
            msg.append("279=0").append(soh)
               .append("270=").append(10000 + i).append('.').append(i % 100).append(soh)
               .append("271=").append(1000 + i).append(soh); // optional size
        }
        msg.append("10=000").append(soh);
        largeSnapshot = msg.toString();
    }

    @Benchmark
    public Message parseLarge_withDictionary() throws Exception {
        Message m = new Message();
        m.fromString(largeSnapshot, dd44, vs, false);
        return m;
    }

    @Benchmark
    public Message parseLarge_withoutDictionary() throws Exception {
        Message m = new Message();
        m.fromString(largeSnapshot, null, vs, false);
        return m;
    }
}

