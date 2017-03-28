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

package org.springframework.cloud.task.app.composedtaskrunner.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.PagedResources;

/**
 * @author Glenn Renfro
 */
@Configuration
@EnableAutoConfiguration
public class DataFlowTestConfiguration {

	@Bean
	public TaskOperations taskOperations() {
		return new TestTaskOperations();
	}

	public static class TestTaskOperations implements TaskOperations {

		private boolean isLaunched;

		@Override
		public PagedResources<TaskDefinitionResource> list() {
			return null;
		}

		@Override
		public TaskDefinitionResource create(String name, String definition) {
			return null;
		}

		@Override
		public long launch(String name, Map<String, String> properties, List<String> arguments) {
			this.isLaunched = true;
			return 1;
		}

		@Override
		public void destroy(String name) {

		}

		@Override
		public PagedResources<TaskExecutionResource> executionList() {
			return null;
		}

		@Override
		public PagedResources<TaskExecutionResource> executionListByTaskName(String taskName) {
			return null;
		}

		@Override
		public TaskExecutionResource taskExecutionStatus(long id) {
			return null;
		}

		@Override
		public void cleanup(long l) {

		}

		public boolean isLaunched() {
			return this.isLaunched;
		}

	}
}
