package org.acme

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.kafka.InjectKafkaCompanion
import io.quarkus.test.kafka.KafkaCompanionResource
import io.smallrye.common.annotation.Identifier
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import prg.acme.test.testin.PersonIn
import java.time.Duration
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.kotest.matchers.shouldBe
import prg.acme.test.testout.PersonOut

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource::class, restrictToAnnotatedClass = true)
class ExampleInOutTest {

    @InjectKafkaCompanion
    lateinit var companion: KafkaCompanion

    @Identifier("default-kafka-broker")
    lateinit var kafkaConfig: MutableMap<String, Any?>

    @Test
    fun exampleTest() {
        val topicList = companion.topics().list()
        if (!topicList.contains("dev-in")) companion.topics().create("dev-in", 1)
        if (!topicList.contains("dev-out")) companion.topics().create("dev-out", 1)

        val addConfigMaps = companion.commonClientConfig + kafkaConfig
        // Set up avro
        val personInSerde = SpecificAvroSerde<PersonIn>().apply {
            configure(addConfigMaps, false)
        }
        companion.registerSerde(PersonIn::class.java, personInSerde)

        val personOutSerde = SpecificAvroSerde<PersonOut>().apply {
            configure(addConfigMaps, false)
        }
        companion.registerSerde(PersonOut::class.java, personOutSerde)

        companion.produce(
            Serdes.StringSerde(),
            personInSerde
        ).fromRecords(
            ProducerRecord(
                "dev-in", "Test Key",
                PersonIn("12345678", "Joe Smith")
            )
        ).awaitRecords(1, Duration.ofSeconds(10))
        .count() shouldBe 1

        val recConsumer = companion.consume(
            Serdes.StringSerde(),
            personOutSerde
        ).fromTopics("dev-out", 1)
        val personOutRec = recConsumer.awaitCompletion(Duration.ofSeconds(5)).lastRecord
        personOutRec.value().personId shouldBe "12345678"
    }
}