/*
 *
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.task.app.composedtaskrunner.configuration;

import java.net.URISyntaxException;

import org.junit.Test;

import org.springframework.cloud.task.app.composedtaskrunner.DataFlowConfiguration;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Gunnar Hillert
 */
public class DataFlowConfigurationTests {

	@Test
	public void testTaskOperationsConfiguredWithMissingPassword() throws URISyntaxException{
		final ComposedTaskProperties composedTaskProperties = new ComposedTaskProperties();
		composedTaskProperties.setDataflowServerUsername("foo");
		final DataFlowConfiguration dataFlowConfiguration = new DataFlowConfiguration();
		ReflectionTestUtils.setField(dataFlowConfiguration, "properties", composedTaskProperties);
		try {
			dataFlowConfiguration.taskOperations(dataFlowConfiguration.dataFlowOperations(null, null));
		}
		catch (IllegalArgumentException e) {
			assertEquals("A username may be specified only together with a password", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}

	@Test
	public void testTaskOperationsConfiguredWithMissingUsername() throws URISyntaxException{
		final ComposedTaskProperties composedTaskProperties = new ComposedTaskProperties();
		composedTaskProperties.setDataflowServerPassword("bar");
		final DataFlowConfiguration dataFlowConfiguration = new DataFlowConfiguration();
		ReflectionTestUtils.setField(dataFlowConfiguration, "properties", composedTaskProperties);
		try {
			dataFlowConfiguration.taskOperations(dataFlowConfiguration.dataFlowOperations(null, null));
		}
		catch (IllegalArgumentException e) {
			assertEquals("A password may be specified only together with a username", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}

}
