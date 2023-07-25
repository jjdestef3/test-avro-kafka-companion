package org.acme

import io.quarkus.logging.Log
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.common.annotation.Identifier
import io.smallrye.reactive.messaging.kafka.DeserializationFailureHandler
import jakarta.enterprise.context.ApplicationScoped
import org.apache.kafka.common.header.Headers
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import prg.acme.test.testin.PersonIn
import java.util.concurrent.CompletableFuture


@ApplicationScoped
@Identifier("failure-dead-letter")
class CoverageDeserializationFailureHandler(
    @Channel("dead-letter") val dlqChannel: Emitter<DLQMessage>
) : DeserializationFailureHandler<PersonIn?> {

    /**
     * Write message to the DLQ
     */
    override fun handleDeserializationFailure(
        topic: String?,
        isKey: Boolean,
        deserializer: String?,
        data: ByteArray?,
        exception: Exception?,
        headers: Headers?
    ): PersonIn? {
        Log.error("ERROR: ${exception?.message}")
        dlqChannel.send(
            Message.of(DLQMessage(topic, isKey, deserializer, data, exception, headers))
                .withAck {
                    // Called when the message is acked
                    Log.error("SENT TO DEAD LETTER")
                    CompletableFuture.completedFuture(null)
                }
                .withNack { throwable ->
                    // Called when the message is nacked
                    Log.error("ERROR, NOT SENT DEAD LETTER: ${throwable.message}", throwable)
                    CompletableFuture.completedFuture(null)
                })
        return null
    }
}

/**
 * Holder for message
 */
@RegisterForReflection
data class DLQMessage(
    val topic: String? = null,
    val isKey: Boolean = false,
    val deserializer: String? = null,
    val data: ByteArray? = null,
    val exception: Exception? = null,
    val headers: Headers? = null
)