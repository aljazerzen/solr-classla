package eu.murerzen.lucene_grpc;

import eu.murerzen.lucene_grpc.grpc.AnalyzeReply;
import eu.murerzen.lucene_grpc.grpc.AnalyzeRequest;
import eu.murerzen.lucene_grpc.grpc.AnalyzerGrpc;
import eu.murerzen.lucene_grpc.grpc.Token;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class GrpcAnalyzer extends Analyzer {
    private static final Logger log = LoggerFactory.getLogger(GrpcAnalyzer.class);

    private static final String DEFAULT_SERVICE_URI = "localhost:50051";

    private static final Channel channel = openChannel();

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        log.info("createComponents({})", fieldName);
        Tokenizer tokenizer = new GrpcConnection(channel);
        return new TokenStreamComponents(tokenizer);
    }

    protected static Channel openChannel() {
        String targetUri = System.getenv("CLASSLA_GRPC_URI");
        if (targetUri == null) {
            targetUri = DEFAULT_SERVICE_URI;
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        return ManagedChannelBuilder.forTarget(targetUri)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
    }

    public static class GrpcConnection extends Tokenizer {

        private final CharTermAttribute charTermAtt = addAttribute(CharTermAttribute.class);

        // TODO: don't use blocking client
        private final AnalyzerGrpc.AnalyzerBlockingStub blockingStub;

        protected final Queue<Token> buffer = new ArrayDeque<>();

        public GrpcConnection(Channel channel) {
            blockingStub = AnalyzerGrpc.newBlockingStub(channel);
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (buffer.isEmpty()) {
                readAndAnalyze();
            }

            if (buffer.isEmpty()) return false;

            clearAttributes();

            var token = buffer.poll();
            assert token != null;
            log.info("returning token: {}", token.getLemma());

            var lemma = token.getLemma().toCharArray();
            charTermAtt.copyBuffer(lemma, 0, lemma.length);

            return true;
        }

        protected void readAndAnalyze() throws IOException {
            String text = readAll();
            if (text.isBlank()) return;

            log.info("analyzing '{}'...", text);

            var tokens = makeRequest(text);

            log.info("analyzed into {} tokens", tokens.size());

            for (var token : tokens) {
                if (token.getUpos().equals("PUNCT")) continue;
                if (token.getLemma() == null) continue;

                buffer.add(token);
            }
        }

        protected String readAll() throws IOException {
            char[] arr = new char[1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = input.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            return buffer.toString();
        }

        protected List<Token> makeRequest(String text) {
            var request = AnalyzeRequest.newBuilder().setText(text).build();
            AnalyzeReply response;
            try {
                response = blockingStub.analyze(request);
            } catch (StatusRuntimeException e) {
                log.warn("RPC failed: {}", e.getStatus());
                return Collections.emptyList();
            }

            return response.getTokensList();
        }
    }
}
