package org.acme.song.indexer.app;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;

// import io.micrometer.core.instrument.MeterRegistry;


@ApplicationScoped
public class SongResource {

    private static final Logger LOG = Logger.getLogger(SongResource.class);
    
    @Incoming("songs")
    public CompletionStage<Void> process(IncomingKafkaRecord<String, String> msg) throws InterruptedException {
       
        
        var metadata = msg.getMetadata(IncomingKafkaRecordMetadata.class).orElseThrow();
        LOG.info("Key: " + msg.getKey()+", Payload: "+msg.getPayload()+", Metadata: "+metadata.getTimestamp());
        // return CompletableFuture.completedFuture(null);
        return msg.ack();
    }


}