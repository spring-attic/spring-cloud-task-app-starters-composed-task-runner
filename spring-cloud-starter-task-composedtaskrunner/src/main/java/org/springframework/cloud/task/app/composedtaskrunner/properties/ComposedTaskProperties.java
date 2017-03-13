/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.app.composedtaskrunner.properties;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties used to setup the ComposedTaskRunner.
 *
 * @author Glenn Renfro
 */
@ConfigurationProperties
public class ComposedTaskProperties {

	/**
	 * The maximum amount of time in millis that a individual step can run before
	 * the execution of the Composed task is failed.
	 */
	private int maxWaitTime = 30000;

	/**
	 * The amount of time in millis that the ComposedTaskStepExecutionListener
	 * will wait between checks of the database to see if a task has completed.
	 */
	private int intervalTimeBetweenChecks = 500;

	/**
	 * The URI for the dataflow server that will receive task launch requests.
	 */
	private URI dataFlowUri;

	/**
	 * The DSL for the composed task directed graph.
	 */
	private String graph;

	/**
	 * The properties to be used for each of the tasks as well as their deployments.
	 */
	private String composedTaskProperties;

	/**
	 * The arguments to be used for each of the tasks.
	 */
	private String composedTaskArguments;

	public ComposedTaskProperties() {
		try {
			this.dataFlowUri = new URI("http://localhost:9393");
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid Spring Cloud Data Flow URI");
		}
	}

	public int getMaxWaitTime() {
		return this.maxWaitTime;
	}

	public void setMaxWaitTime(int maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public int getIntervalTimeBetweenChecks() {
		return intervalTimeBetweenChecks;
	}

	public void setIntervalTimeBetweenChecks(int intervalTimeBetweenChecks) {
		this.intervalTimeBetweenChecks = intervalTimeBetweenChecks;
	}

	public URI getDataFlowUri() {
		return this.dataFlowUri;
	}

	public void setDataFlowUri(URI dataFlowUri) {
		this.dataFlowUri = dataFlowUri;
	}

	public String getGraph() {
		return this.graph;
	}

	public void setGraph(String graph) {
		this.graph = graph;
	}

	public String getComposedTaskProperties() {
		return this.composedTaskProperties;
	}

	public void setComposedTaskProperties(String composedTaskProperties) {
		this.composedTaskProperties = composedTaskProperties;
	}

	public String getComposedTaskArguments() {
		return this.composedTaskArguments;
	}

	public void setComposedTaskArguments(String composedTaskArguments) {
		this.composedTaskArguments = composedTaskArguments;
	}
}
