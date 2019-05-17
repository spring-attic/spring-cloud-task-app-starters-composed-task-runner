/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.app.composedtaskrunner;

import java.util.Collections;
import java.util.Date;

import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.platform.commons.util.CollectionUtils;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.task.app.composedtaskrunner.support.TaskExecutionTimeoutException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.VndErrors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={EmbeddedDataSourceConfiguration.class,
		TaskLauncherTaskletTests.TestConfiguration.class})
public class TaskLauncherTaskletTests {

	private static final String TASK_NAME = "testTask1_0";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Autowired
	private TaskRepositoryInitializer taskRepositoryInitializer;

	@Autowired
	private JdbcTaskExecutionDao taskExecutionDao;

	private TaskOperations taskOperations;

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;


	@Before
	public void setup() throws Exception{
		this.taskRepositoryInitializer.setDataSource(this.dataSource);

		this.taskRepositoryInitializer.afterPropertiesSet();
		this.taskOperations = mock(TaskOperations.class);
		TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean =
				new TaskExecutionDaoFactoryBean(this.dataSource);
		this.taskRepository = new SimpleTaskRepository(taskExecutionDaoFactoryBean);
		this.taskExplorer = new SimpleTaskExplorer(taskExecutionDaoFactoryBean);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(500);
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTasklet() throws Exception{
		createCompleteTaskExecution(0);
		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertEquals(1L, chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id"));

		mockReturnValForTaskExecution(2L);
		chunkContext = chunkContext();
		createCompleteTaskExecution(0);
		taskLauncherTasklet = getTaskExecutionTasklet();
		execute(taskLauncherTasklet, null, chunkContext);
		assertEquals(2L, chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id"));
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletIgnoreExitMessage() throws Exception {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(Collections.singletonList("--ignoreExitMessage=true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertEquals(1L, chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id"));
		assertTrue(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE));
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletIgnoreExitMessageViaProperties() throws Exception {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setProperties(Collections.singletonMap("app.foo." + TaskLauncherTasklet.IGNORE_EXIT_MESSAGE_PROPERTY, "true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertEquals(1L, chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id"));
		assertTrue(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE));
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletIgnoreExitMessageViaCommandLineOverride() throws Exception {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(Collections.singletonList("--ignoreExitMessage=false"));
		taskLauncherTasklet.setProperties(Collections.singletonMap("app.foo." + TaskLauncherTasklet.IGNORE_EXIT_MESSAGE_PROPERTY, "true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertEquals(1L, chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id"));
		boolean value = chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE);
		assertTrue(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE));
		assertFalse((Boolean)chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE));
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletTimeout() {
		mockReturnValForTaskExecution(1L);
		this.composedTaskProperties.setMaxWaitTime(500);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(1000);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		Throwable exception = assertThrows(TaskExecutionTimeoutException.class, () -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo("Timeout occurred while " +
				"processing task with Execution Id 1");
	}

	@Test
	@DirtiesContext
	public void testInvalidTaskName() {
		final String ERROR_MESSAGE =
				"Could not find task definition named " + TASK_NAME;
		VndErrors errors = new VndErrors("message", ERROR_MESSAGE, new Link("ref"));
		Mockito.doThrow(new DataFlowClientException(errors))
				.when(this.taskOperations)
				.launch(ArgumentMatchers.anyString(),
						ArgumentMatchers.any(),
						ArgumentMatchers.any());
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		Throwable exception = assertThrows(DataFlowClientException.class,
				() -> taskLauncherTasklet.execute(null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
	}

	@Test
	@DirtiesContext
	public void testNoDataFlowServer() {
		final String ERROR_MESSAGE =
				"I/O error on GET request for \"http://localhost:9393\": Connection refused; nested exception is java.net.ConnectException: Connection refused";
		Mockito.doThrow(new ResourceAccessException(ERROR_MESSAGE))
				.when(this.taskOperations).launch(ArgumentMatchers.anyString(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any());
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		Throwable exception = assertThrows(ResourceAccessException.class,
				() -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletFailure() {
		mockReturnValForTaskExecution(1L);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(1);
		Throwable exception = assertThrows(UnexpectedJobExecutionException.class,
				() -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo("Task returned a non zero exit code.");
	}

	private RepeatStatus execute(TaskLauncherTasklet taskLauncherTasklet, StepContribution contribution,
			ChunkContext chunkContext)  throws Exception{
		RepeatStatus status = taskLauncherTasklet.execute(contribution, chunkContext);
		if (!status.isContinuable()) {
			throw new IllegalStateException("Expected continuable status for the first execution.");
		}
		return taskLauncherTasklet.execute(contribution, chunkContext);

	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletNullResult() throws Exception {
		mockReturnValForTaskExecution(1L);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		getCompleteTaskExecutionWithNull();
		Throwable exception = assertThrows(UnexpectedJobExecutionException.class,
				() -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo("Task returned a null exit code.");
	}

	private void createCompleteTaskExecution(int exitCode) {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		this.taskRepository.completeTaskExecution(taskExecution.getExecutionId(),
				exitCode, new Date(), "");
	}

	private TaskExecution getCompleteTaskExecutionWithNull() {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		taskExecutionDao.completeTaskExecution(taskExecution.getExecutionId(), null, new Date(), "hello", "goodbye");
		return taskExecution;
	}

	private TaskLauncherTasklet getTaskExecutionTasklet() {
		return new TaskLauncherTasklet(this.taskOperations,
				this.taskExplorer, this.composedTaskProperties,
				TASK_NAME);
	}

	private ChunkContext chunkContext ()
	{
		final long JOB_EXECUTION_ID = 123L;
		final String STEP_NAME = "myTestStep";

		JobExecution jobExecution = new JobExecution(JOB_EXECUTION_ID);
		StepExecution stepExecution = new StepExecution(STEP_NAME, jobExecution);
		StepContext stepContext = new StepContext(stepExecution);
		return new ChunkContext(stepContext);
	}

	private void mockReturnValForTaskExecution(long executionId) {
		Mockito.doReturn(executionId)
				.when(this.taskOperations)
				.launch(ArgumentMatchers.anyString(),
						ArgumentMatchers.any(),
						ArgumentMatchers.any());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableConfigurationProperties(ComposedTaskProperties.class)
	public static class TestConfiguration {

		@Bean
		TaskRepositoryInitializer taskRepositoryInitializer() {
			return new TaskRepositoryInitializer();
		}

		@Bean
		TaskExecutionDao taskExecutionDao(DataSource dataSource) {
			return new JdbcTaskExecutionDao(dataSource);
		}

	}
}
