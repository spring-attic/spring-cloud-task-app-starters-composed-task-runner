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

	public static final int MAX_WAIT_TIME_DEFAULT = 0;

	public static final int INTERVAL_TIME_BETWEEN_CHECKS_DEFAULT = 10000;

	public static final int SPLIT_THREAD_CORE_POOL_SIZE_DEFAULT = 1;

	public static final int SPLIT_THREAD_KEEP_ALIVE_SECONDS_DEFAULT = 60;

	public static final int SPLIT_THREAD_MAX_POOL_SIZE_DEFAULT = Integer.MAX_VALUE;

	public static final int SPLIT_THREAD_QUEUE_CAPACITY_DEFAULT = Integer.MAX_VALUE;

	/**
	 * The maximum amount of time in millis that a individual step can run before
	 * the execution of the Composed task is failed.
	 */
	private int maxWaitTime = MAX_WAIT_TIME_DEFAULT;

	/**
	 * The amount of time in millis that the ComposedTaskRunner
	 * will wait between checks of the database to see if a task has completed.
	 */
	private int intervalTimeBetweenChecks = INTERVAL_TIME_BETWEEN_CHECKS_DEFAULT;

	/**
	 * The URI for the dataflow server that will receive task launch requests.
	 * Default is http://localhost:9393;
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

	/**
	 * Specifies whether to allow split core threads to timeout.
	 * Default is false;
	 */
	private boolean splitThreadAllowCoreThreadTimeout;

	/**
	 * Split's core pool size.
	 * Default is 1;
	 */
	private int splitThreadCorePoolSize = SPLIT_THREAD_CORE_POOL_SIZE_DEFAULT;

	/**
	 * Split's thread keep alive seconds.
	 * Default is 60.
	 */
	private int splitThreadKeepAliveSeconds = SPLIT_THREAD_KEEP_ALIVE_SECONDS_DEFAULT;

	/**
	 * Split's maximum pool size.
	 * Default is {@code Integer.MAX_VALUE}.
	 */
	private int splitThreadMaxPoolSize = SPLIT_THREAD_MAX_POOL_SIZE_DEFAULT;

	/**
	 * Capacity for Split's  BlockingQueue.
	 * Default is {@code Integer.MAX_VALUE}.
	 */
	private int splitThreadQueueCapacity = SPLIT_THREAD_QUEUE_CAPACITY_DEFAULT;

	/**
	 * Whether to wait for scheduled tasks to complete on shutdown, not
	 * interrupting running tasks and executing all tasks in the queue.
	 * Default is false;
	 */
	private boolean splitThreadWaitForTasksToCompleteOnShutdown;

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

	public boolean isSplitThreadAllowCoreThreadTimeout() {
		return splitThreadAllowCoreThreadTimeout;
	}

	public void setSplitThreadAllowCoreThreadTimeout(boolean splitThreadAllowCoreThreadTimeout) {
		this.splitThreadAllowCoreThreadTimeout = splitThreadAllowCoreThreadTimeout;
	}

	public int getSplitThreadCorePoolSize() {
		return splitThreadCorePoolSize;
	}

	public void setSplitThreadCorePoolSize(int splitThreadCorePoolSize) {
		this.splitThreadCorePoolSize = splitThreadCorePoolSize;
	}

	public int getSplitThreadKeepAliveSeconds() {
		return splitThreadKeepAliveSeconds;
	}

	public void setSplitThreadKeepAliveSeconds(int splitThreadKeepAliveSeconds) {
		this.splitThreadKeepAliveSeconds = splitThreadKeepAliveSeconds;
	}

	public int getSplitThreadMaxPoolSize() {
		return splitThreadMaxPoolSize;
	}

	public void setSplitThreadMaxPoolSize(int splitThreadMaxPoolSize) {
		this.splitThreadMaxPoolSize = splitThreadMaxPoolSize;
	}

	public int getSplitThreadQueueCapacity() {
		return splitThreadQueueCapacity;
	}

	public void setSplitThreadQueueCapacity(int splitThreadQueueCapacity) {
		this.splitThreadQueueCapacity = splitThreadQueueCapacity;
	}

	public boolean isSplitThreadWaitForTasksToCompleteOnShutdown() {
		return splitThreadWaitForTasksToCompleteOnShutdown;
	}

	public void setSplitThreadWaitForTasksToCompleteOnShutdown(boolean splitThreadWaitForTasksToCompleteOnShutdown) {
		this.splitThreadWaitForTasksToCompleteOnShutdown = splitThreadWaitForTasksToCompleteOnShutdown;
	}
}
