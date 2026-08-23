package simulator;

import java.io.IOException;

public interface TransactionEventWriter {
    void writeEvent(TransactionEvent e) throws IOException;
    void close() throws IOException;
}
