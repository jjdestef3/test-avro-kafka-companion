package org.acme

import io.smallrye.common.annotation.Identifier
import io.smallrye.mutiny.Uni

/**
 * Responsibility: <br/>
 *  <br/>
 *
 * Creator: johndestefano
 * Original creation date: 7/24/23
 */
import io.smallrye.reactive.messaging.kafka.SerializationFailureHandler

import jakarta.enterprise.context.ApplicationScoped
import org.apache.kafka.common.header.Headers
import prg.acme.test.testout.PersonOut
import java.time.Duration


/**
 * Responsibility: <br/>
 * Retry if a serialization failure occurs when writing
 * to the outbound topic. This could occur if there are
 * intermittent errors reaching the schema registry<br/>
 *
 * Creator: johndestefano
 * Original creation date: 7/21/23
 */
@ApplicationScoped
@Identifier("serialization-failure-fallback") // Set the name of the failure handler
class ExampleSerializationFailureHandler : SerializationFailureHandler<PersonOut?> {
    override fun decorateSerialization(
        serialization: Uni<ByteArray>?,
        topic: String?,
        isKey: Boolean,
        serializer: String?,
        data: PersonOut?,
        headers: Headers?
    ): ByteArray? {
        return serialization?.onFailure()?.retry()?.atMost(3)
            ?.await()?.atMost(Duration.ofMillis(200))
    }
}