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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.task.app.composedtaskrunner.support.TaskExecutionTimeoutException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.util.Assert;

/**
 * Executes task launch request using Spring Cloud Data Flow's Restful API
 * then returns the execution id once the task launched.
 *
 * @author Glenn Renfro
 */
public class TaskLauncherTasklet implements Tasklet {

	private ComposedTaskProperties composedTaskProperties;

	private TaskExplorer taskExplorer;

	private TaskOperations taskOperations;

	private Map<String, String> properties;

	private List<String> arguments;

	private String taskName;

	private static final Log logger = LogFactory.getLog(TaskLauncherTasklet.class);


	public TaskLauncherTasklet(
			TaskOperations taskOperations, TaskExplorer taskExplorer,
			ComposedTaskProperties composedTaskProperties, String taskName) {
		Assert.hasText(taskName, "taskName must not be empty nor null.");
		Assert.notNull(taskOperations, "taskOperations must not be null.");
		Assert.notNull(taskExplorer, "taskExplorer must not be null.");
		Assert.notNull(composedTaskProperties,
				"composedTaskProperties must not be null");

		this.taskName = taskName;
		this.taskOperations = taskOperations;
		this.taskExplorer = taskExplorer;
		this.composedTaskProperties = composedTaskProperties;
	}

	public void setProperties(Map<String, String> properties) {
		if(properties != null) {
			this.properties = properties;
		}
		else {
			this.properties = new HashMap<>(0);
		}
	}

	public void setArguments(List<String> arguments) {
		if(arguments != null) {
			this.arguments = arguments;
		}
		else {
			this.arguments = new ArrayList<>(0);
		}
	}

	/**
	 * Executes the task as specified by the taskName with the associated
	 * properties and arguments.
	 * @param contribution mutable state to be passed back to update the current step execution
	 * @param chunkContext contains the task-execution-id used by the listener.
	 * @return Repeat status of FINISHED.
	 */
	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		String tmpTaskName = this.taskName.substring(0,
				this.taskName.lastIndexOf('_'));

		List<String> args = this.arguments;

		ExecutionContext stepExecutionContext = chunkContext.getStepContext().getStepExecution().
				getExecutionContext();
		if(stepExecutionContext.containsKey("task-arguments")) {
			args = (List<String>) stepExecutionContext.get("task-arguments");
		}

		long executionId = this.taskOperations.launch(tmpTaskName,
				this.properties, args);

		stepExecutionContext.put("task-execution-id", executionId);
		stepExecutionContext.put("task-arguments", args);

		if(!waitForTaskToComplete(executionId)) {
			throw new TaskExecutionTimeoutException(String.format(
					"Timeout occurred while processing task with Execution Id %s",
					executionId));
		}
		return RepeatStatus.FINISHED;
	}

	private boolean waitForTaskToComplete(long taskExecutionId) {
		long timeout = System.currentTimeMillis() +
				this.composedTaskProperties.getMaxWaitTime();
		boolean isComplete = false;
		boolean isSuccessful = false;
		logger.debug("Wait time for this task to complete is " +
				this.composedTaskProperties.getMaxWaitTime());
		logger.debug("Interval check time for this task to complete is " +
				this.composedTaskProperties.getIntervalTimeBetweenChecks());

		while (!isComplete) {
			try {
				Thread.sleep(this.composedTaskProperties.getIntervalTimeBetweenChecks());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}

			TaskExecution taskExecution =
					this.taskExplorer.getTaskExecution(taskExecutionId);
			if(taskExecution != null && taskExecution.getEndTime() != null) {
				isComplete = true;
				isSuccessful = true;
			}

			if(this.composedTaskProperties.getMaxWaitTime() > 0 &&
					System.currentTimeMillis() > timeout) {
				isComplete = true;
			}
		}
		return isSuccessful;
	}

}
