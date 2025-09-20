package org.quickfixj;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.ValidationSettings;

/**
 * Benchmarks validation cost using DataDictionary over a parsed message.
 */
@State(Scope.Benchmark)
public class DictionaryValidationPerfTest extends AbstractPerfTest {

    private DataDictionary dd;
    private Message message;
    private ValidationSettings validationSettings;

    @Param({
            // Short ExecutionReport
            "8=FIX.4.4\u00019=309\u000135=8\u000149=ASX\u000156=CL1_FIX44\u000134=4\u000152=20060324-01:05:58\u000117=X-B-WOW-1494E9A0:58BD3F9D-1109\u0001150=D\u000139=0\u000111=184271\u000138=200\u0001198=1494E9A0:58BD3F9D\u0001526=4324\u000137=B-WOW-1494E9A0:58BD3F9D\u000155=WOW\u000154=1\u0001151=200\u000114=0\u000140=2\u000144=15\u000159=1\u00016=0\u0001453=3\u0001448=AAA35791\u0001447=D\u0001452=3\u0001448=8\u0001447=D\u0001452=4\u0001448=FIX11\u0001447=D\u0001452=36\u000160=20060320-03:34:29\u000110=169\u0001"
    })
    public String data;

    @Setup
    public void setup() throws ConfigError, InvalidMessage {
        dd = new DataDictionary(DictionaryValidationPerfTest.class.getClassLoader().getResourceAsStream("FIX44.xml"));
        validationSettings = new ValidationSettings();
        message = new Message();
        message.fromString(data, dd, validationSettings, false);
    }

    @Benchmark
    public void validate() throws Exception {
        dd.validate(message, false, validationSettings);
    }
}
