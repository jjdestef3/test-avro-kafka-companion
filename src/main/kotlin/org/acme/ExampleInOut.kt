package org.acme

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import io.smallrye.reactive.messaging.kafka.Record
import prg.acme.test.testin.PersonIn
import prg.acme.test.testout.PersonOut


@ApplicationScoped
class ExampleInOut {

    @Incoming("test-in")
    @Outgoing("test-out")
    fun exampleTest(record: Record<String, PersonIn>): Record<String, PersonOut> =
        Record.of(
            "Test Key Out",
            PersonOut().apply {
                personId = record.value().personId
                personName = record.value().personName
            }
        )

}