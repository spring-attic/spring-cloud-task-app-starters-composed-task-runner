package org.springframework.cloud.task.app.composedtaskrunner;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes={ComposedTaskRunnerStepFactoryTests.StepFactoryConfiguration.class})
public class ComposedTaskRunnerStepFactoryTests {

	@Autowired
	ComposedTaskRunnerStepFactory stepFactory;

	@Test
	public void testStep() throws Exception{
		Step step = stepFactory.getObject();
		Assert.assertEquals("FOOBAR", step.getName());
		Assert.assertEquals(Integer.MAX_VALUE, step.getStartLimit());
	}

	@Configuration
	public static class StepFactoryConfiguration {

		@MockBean
		public StepExecutionListener composedTaskStepExecutionListener;

		@MockBean
		public TaskOperations taskOperations;

		@Bean
		public StepBuilderFactory steps(){
			return new StepBuilderFactory(mock(JobRepository.class), mock(PlatformTransactionManager.class));
		}

		@Bean
		public TaskConfigurer taskConfigurer() {
			return new TaskConfigurer() {
				@Override
				public TaskRepository getTaskRepository() {
					return null;
				}

				@Override
				public PlatformTransactionManager getTransactionManager() {
					return null;
				}

				@Override
				public TaskExplorer getTaskExplorer() {
					return mock(TaskExplorer.class);
				}

				@Override
				public DataSource getTaskDataSource() {
					return mock(DataSource.class);
				}
			};
		}

		@Bean
		public ComposedTaskRunnerStepFactory stepFactory() {
			return new ComposedTaskRunnerStepFactory(new ComposedTaskProperties(), "FOOBAR");
		}
	}
}
