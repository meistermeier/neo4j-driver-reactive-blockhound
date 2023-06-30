package com.meistermeier.neo4j.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

/**
 * @author Gerrit Meier
 */
public class BlockingReactiveTest {

	static {
		BlockHound.install();
	}

	private final Driver driver = ReactiveBlocker.getDriver();

	@BeforeEach
	void setup() {
		try (var session = driver.session()) {
			session.run("MATCH (n) detach delete n").consume();
			session.run("UNWIND range(1,100) as count with count CREATE (u:VersionedExternalIdListBased) SET u.numberThing=count").consume();
		}
	}

	@Test
	void reactiveBlockingTest() {
		String cypher = "MATCH (n) RETURN elementId(n) as a";
		String cypher2 = "MATCH (n) WHERE elementId(n) = $elementId RETURN elementId(n) as b";

		StepVerifier.create(Flux.usingWhen(
				Mono.just(driver.session(ReactiveSession.class)),
				session ->
					Flux.from(session.run(cypher))
						.flatMap(a -> a.records())
						.map(a -> a.get(0).asString())
						.flatMap(elementId ->
							Flux.usingWhen(Mono.just(driver.session(ReactiveSession.class)),
								innerSession ->
									Flux.from(innerSession.run(cypher2, Map.of("elementId", elementId)))
										.flatMap(result -> result.records())
										.map(result -> result.get(0).asString())
										.doOnNext(returnedElementId -> System.out.println("elementId is " + returnedElementId)),
								innerSession -> Mono.fromDirect(innerSession.close())
							)),
				session -> Mono.fromDirect(session.close())))
			.expectNextCount(100)
			.verifyComplete();
	}
}
