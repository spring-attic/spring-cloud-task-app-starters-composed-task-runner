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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.app.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

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
@TestPropertySource(properties = {"graph=AAA && BBB && CCC","maxWaitTime=1000"})
public class ComposedTaskRunnerConfigurationTests {

	@Autowired
	JobRepository jobRepository;

	@Autowired
	Job job;

	@Autowired
	DataFlowTestConfiguration.TestTaskOperations taskOperations;

	@Test
	@DirtiesContext
	public void testComposedConfiguration() throws Exception {
		JobExecution jobExecution = this.jobRepository.createJobExecution(
				"ComposedTest", new JobParameters());
		job.execute(jobExecution);
		assertTrue(this.taskOperations.isLaunched());
	}

}
