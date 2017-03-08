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

package org.springframework.cloud.task.app.composedtaskrunner;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the beans required for Connectivity to the Data Flow Server.
 *
 * @author Glenn Renfro
 */
@Configuration
@EnableConfigurationProperties(ComposedTaskProperties.class)
public class DataFlowConfiguration {

	@Autowired
	private ComposedTaskProperties properties;

	@Bean
	public TaskOperations taskOperations() {
		TaskOperations taskOperations = null;
		DataFlowTemplate dataFlowTemplate = new DataFlowTemplate(properties.getDataFlowUri());
		taskOperations = dataFlowTemplate.taskOperations();
		return taskOperations;
	}
}
