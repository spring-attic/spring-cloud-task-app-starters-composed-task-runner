/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties used to setup the ComposedTaskRunner.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@ConfigurationProperties
public class ComposedTaskProperties {

	public static final int MAX_WAIT_TIME_DEFAULT = 0;

	public static final int INTERVAL_TIME_BETWEEN_CHECKS_DEFAULT = 10000;

	public static final int SPLIT_THREAD_CORE_POOL_SIZE_DEFAULT = 4;

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
	private URI dataflowServerUri;

	/**
	 * The optional username for the dataflow server that will receive task launch requests.
	 * Used to access the the dataflow server using Basic Authentication. Not used if {@link #dataflowServerAccessToken} is set.
	 */
	private String dataflowServerUsername;

	/**
	 * The optional password for the dataflow server that will receive task launch requests.
	 * Used to access the the dataflow server using Basic Authentication. Not used if {@link #dataflowServerAccessToken} is set.
	 */
	private String dataflowServerPassword;

	/**
	 * The optional OAuth2 Access Token.
	 */
	private String dataflowServerAccessToken;

	/**
	 * The OAuth2 Client Id (Used for the client credentials grant).
	 *
	 * If not null, then the following properties are ignored:
	 *
	 * <ul>
	 *   <li>dataflowServerUsername
	 *   <li>dataflowServerPassword
	 *   <li>dataflowServerAccessToken
	 * <ul>
	 */
	private String oauth2ClientCredentialsClientId;

	/**
	 * The OAuth2 Client Secret (Used for the client credentials grant).
	 */
	private String oauth2ClientCredentialsClientSecret;

	/**
	 * Token URI for the OAuth2 provider (Used for the client credentials grant).
	 */
	private String oauth2ClientCredentialsTokenUri;

	/**
	 * OAuth2 Authorization scopes (Used for the client credentials grant).
	 */
	private Set<String> oauth2ClientCredentialsScopes;

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
	 * Default is 4;
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

	/**
	 * Allows a single ComposedTaskRunner instance to be re-executed without
	 * changing the parameters. Default is false which means a
	 * ComposedTaskRunner instance can only be executed once with a given set
	 * of parameters, if true it can be re-executed.
	 */
	private boolean incrementInstanceEnabled = false;

	public ComposedTaskProperties() {
		try {
			this.dataflowServerUri = new URI("http://localhost:9393");
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid Spring Cloud Data Flow Server URI", e);
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

	public URI getDataflowServerUri() {
		return dataflowServerUri;
	}

	public void setDataflowServerUri(URI dataflowServerUri) {
		this.dataflowServerUri = dataflowServerUri;
	}

	public String getDataflowServerUsername() {
		return dataflowServerUsername;
	}

	public void setDataflowServerUsername(String dataflowServerUsername) {
		this.dataflowServerUsername = dataflowServerUsername;
	}

	public String getDataflowServerPassword() {
		return dataflowServerPassword;
	}

	public void setDataflowServerPassword(String dataflowServerPassword) {
		this.dataflowServerPassword = dataflowServerPassword;
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

	public boolean isIncrementInstanceEnabled() {
		return incrementInstanceEnabled;
	}

	public void setIncrementInstanceEnabled(boolean incrementInstanceEnabled) {
		this.incrementInstanceEnabled = incrementInstanceEnabled;
	}

	public String getDataflowServerAccessToken() {
		return dataflowServerAccessToken;
	}

	public void setDataflowServerAccessToken(String dataflowServerAccessToken) {
		this.dataflowServerAccessToken = dataflowServerAccessToken;
	}

	public String getOauth2ClientCredentialsClientId() {
		return oauth2ClientCredentialsClientId;
	}

	public void setOauth2ClientCredentialsClientId(String oauth2ClientCredentialsClientId) {
		this.oauth2ClientCredentialsClientId = oauth2ClientCredentialsClientId;
	}

	public String getOauth2ClientCredentialsClientSecret() {
		return oauth2ClientCredentialsClientSecret;
	}

	public void setOauth2ClientCredentialsClientSecret(String oauth2ClientCredentialsClientSecret) {
		this.oauth2ClientCredentialsClientSecret = oauth2ClientCredentialsClientSecret;
	}

	public String getOauth2ClientCredentialsTokenUri() {
		return oauth2ClientCredentialsTokenUri;
	}

	public void setOauth2ClientCredentialsTokenUri(String oauth2ClientCredentialsTokenUri) {
		this.oauth2ClientCredentialsTokenUri = oauth2ClientCredentialsTokenUri;
	}

	public Set<String> getOauth2ClientCredentialsScopes() {
		return oauth2ClientCredentialsScopes;
	}

	public void setOauth2ClientCredentialsScopes(Set<String> oauth2ClientCredentialsScopes) {
		this.oauth2ClientCredentialsScopes = oauth2ClientCredentialsScopes;
	}

}
