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

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;

/**
 * Supports ability for a ComposedTaskRunner to create a new Job Instance on
 * subsequent runs.
 *
 * @author Glenn Renfro
 */
public class ComposedTaskJobIncrementer implements JobParametersIncrementer {
	@Override
	public JobParameters getNext(JobParameters parameters) {
		if (parameters == null || parameters.isEmpty()) {
			return new JobParametersBuilder().
					addLong("composed_task_runner_run.id", 1L).toJobParameters();
		}
		long id = parameters.getLong("composed_task_runner_run.id", 1L) + 1;
		return new JobParametersBuilder().addLong("composed_task_runner_run.id", id).toJobParameters();
	}
}
