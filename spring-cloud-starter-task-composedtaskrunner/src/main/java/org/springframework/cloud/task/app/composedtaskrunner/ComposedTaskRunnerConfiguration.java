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

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * Configures the Job that will execute the Composed Task Execution.
 *
 * @author Glenn Renfro
 */
@Configuration
@EnableBatchProcessing
@EnableTask
@EnableConfigurationProperties(ComposedTaskProperties.class)
@Import(StepBeanDefinitionRegistrar.class)
public class ComposedTaskRunnerConfiguration {

	@Autowired
	private ComposedTaskProperties properties;

	@Bean
	public StepExecutionListener composedTaskStepExecutionListener(
			TaskConfigurer taskConfigurer) {
		return new ComposedTaskStepExecutionListener(taskConfigurer.getTaskExplorer());
	}

	@Bean
	public ComposedRunnerJobFactory composedTaskJob() {

		return new ComposedRunnerJobFactory(this.properties.getGraph());
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(properties.getSplitThreadCorePoolSize());
		taskExecutor.setMaxPoolSize(properties.getSplitThreadMaxPoolSize());
		taskExecutor.setKeepAliveSeconds(properties.getSplitThreadKeepAliveSeconds());
		taskExecutor.setAllowCoreThreadTimeOut(
				properties.isSplitThreadAllowCoreThreadTimeout());
		taskExecutor.setQueueCapacity(properties.getSplitThreadQueueCapacity());
		taskExecutor.setWaitForTasksToCompleteOnShutdown(
				properties.isSplitThreadWaitForTasksToCompleteOnShutdown());
		return taskExecutor;
	}
}
