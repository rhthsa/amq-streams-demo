package org.acme.song.app;

import java.util.UUID;
// import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import  jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;


import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.context.Context;
import io.smallrye.reactive.messaging.TracingMetadata;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

import org.jboss.logging.Logger;
@Path("/")
public class SongResource {
    
    private static final Logger LOG = Logger.getLogger(SongResource.class);
    private final MeterRegistry registry;

    @Inject
    @Channel("songs")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
    Emitter<String> songs;

    SongResource(MeterRegistry registry) {

        this.registry = registry;
    }

    @POST
    @Path("/songs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Play song")
    @APIResponse(responseCode = "204")
    public CompletionStage<Void> createSong(Song song) {
        song.setOp(song.getOp());
        song.setId(UUID.randomUUID().toString());
        //song.setOp(Operation.ADD);
        LOG.info("song: "+song.getId()+", Name: "+song.getName());
        TracingMetadata tm = TracingMetadata.withPrevious(Context.current());
        KafkaRecord<String, String> msg = KafkaRecord.of(song.id, JsonbBuilder.create().toJson(song));
        songs.send(msg.addMetadata(tm));
        registry.counter("org.acme.song.app.SongResource.createSong.count").increment();
        return msg.ack();
    }

}