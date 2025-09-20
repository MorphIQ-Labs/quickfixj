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
 * Benchmarks parsing messages with large RawData (95/96).
 */
@State(Scope.Benchmark)
public class RawDataParsingPerfTest extends AbstractPerfTest {

    @Param({"1024", "8192", "65536"})
    public int rawSize;

    private DataDictionary dd44;
    private ValidationSettings vs;
    private String rawMessage;

    @Setup
    public void setup() throws ConfigError {
        dd44 = new DataDictionary(RawDataParsingPerfTest.class.getClassLoader().getResourceAsStream("FIX44.xml"));
        vs = new ValidationSettings();
        StringBuilder sb = new StringBuilder(rawSize);
        for (int i = 0; i < rawSize; i++) {
            sb.append((char)('A' + (i % 26))); // ASCII payload
        }
        String soh = "\u0001";
        String payload = sb.toString();
        // Build basic message with RawDataLength(95) and RawData(96)
        StringBuilder msg = new StringBuilder();
        msg.append("8=FIX.4.4").append(soh)
           .append("9=") // BodyLength placeholder is ignored by fromString
           .append(rawSize + 50).append(soh)
           .append("35=D").append(soh)
           .append("49=SENDER").append(soh)
           .append("56=TARGET").append(soh)
           .append("34=1").append(soh)
           .append("52=20200101-00:00:00").append(soh)
           .append("95=").append(rawSize).append(soh)
           .append("96=").append(payload).append(soh)
           .append("10=000").append(soh);
        rawMessage = msg.toString();
    }

    @Benchmark
    public Message parseRaw_withDictionary() throws Exception {
        Message m = new Message();
        m.fromString(rawMessage, dd44, vs, false);
        return m;
    }

    @Benchmark
    public Message parseRaw_withoutDictionary() throws Exception {
        Message m = new Message();
        m.fromString(rawMessage, null, vs, false);
        return m;
    }
}

