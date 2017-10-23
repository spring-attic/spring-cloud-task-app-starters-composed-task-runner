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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.app.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes={EmbeddedDataSourceConfiguration.class,
		TaskLauncherTaskletTests.TestConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class,
		DataFlowTestConfiguration.class,StepBeanDefinitionRegistrar.class,
		ComposedTaskRunnerConfiguration.class,
		StepBeanDefinitionRegistrar.class})
@TestPropertySource(properties = {"graph=AAA && BBB && CCC","max-wait-time=1010",
		"composed-task-properties=app.AAA.format=yyyy, app.BBB.format=mm",
		"interval-time-between-checks=1100", "composed-task-arguments=--baz=boo",
		"dataflow-server-uri=http://bar"})
public class ComposedTaskRunnerConfigurationWithPropertiesTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private Job job;

	@Autowired
	private TaskOperations taskOperations;

	@Autowired
	ComposedTaskProperties composedTaskProperties;

	@Test
	@DirtiesContext
	public void testComposedConfiguration() throws Exception {
		JobExecution jobExecution = this.jobRepository.createJobExecution(
				"ComposedTest", new JobParameters());
		job.execute(jobExecution);

		Map<String, String> props = new HashMap<>(1);
		props.put("format", "yyyy");
		assertEquals(1010, composedTaskProperties.getMaxWaitTime());
		assertEquals(1100, composedTaskProperties.getIntervalTimeBetweenChecks());
		assertEquals("http://bar", composedTaskProperties.getDataflowServerUri().toASCIIString());

		List<String> args = new ArrayList<>(1);
		args.add("--baz=boo");
		Assert.isNull(job.getJobParametersIncrementer(), "JobParametersIncrementer must be null.");
		verify(this.taskOperations).launch("AAA", props, args);
	}
}
