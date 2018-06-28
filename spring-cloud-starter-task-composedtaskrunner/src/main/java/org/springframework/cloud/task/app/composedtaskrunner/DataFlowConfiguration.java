/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.task.app.composedtaskrunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the beans required for Connectivity to the Data Flow Server.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@Configuration
@EnableConfigurationProperties(ComposedTaskProperties.class)
public class DataFlowConfiguration {
	private static Log logger = LogFactory.getLog(DataFlowConfiguration.class);

	@Autowired
	private ComposedTaskProperties properties;

	@Bean
	public TaskOperations taskOperations() {

		final RestTemplate restTemplate = DataFlowTemplate.getDefaultDataflowRestTemplate();

		validateUsernamePassword(properties.getDataflowServerUsername(),
				properties.getDataflowServerPassword());
		if (StringUtils.hasText(properties.getDataflowServerUsername())
				&& StringUtils.hasText(properties.getDataflowServerPassword())) {
			restTemplate.setRequestFactory(HttpClientConfigurer.create(this.properties.getDataflowServerUri())
					.basicAuthCredentials(properties.getDataflowServerUsername(), properties.getDataflowServerPassword())
					.buildClientHttpRequestFactory());
			logger.debug("Configured basic security for accessing the Data Flow Server");
		}
		else {
			logger.debug("Not configuring basic security for accessing the Data Flow Server");
		}

		final DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(
				this.properties.getDataflowServerUri(), restTemplate);

		return dataFlowTemplate.taskOperations();
	}

	private void validateUsernamePassword(String userName, String password) {
		if (!StringUtils.isEmpty(password) && StringUtils.isEmpty(userName)) {
			throw new IllegalArgumentException("A password may be specified only together with a username");
		}
		if (StringUtils.isEmpty(password) && !StringUtils.isEmpty(userName)) {
			throw new IllegalArgumentException("A username may be specified only together with a password");
		}
	}
}
