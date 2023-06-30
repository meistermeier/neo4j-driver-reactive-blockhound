package com.meistermeier.neo4j.driver;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;

/**
 * @author Gerrit Meier
 */
public class ReactiveBlocker {

	public static Driver getDriver() {
		return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "verysecret"), Config.builder()
			.withLogging(Logging.slf4j())
			.build());
	}
}
