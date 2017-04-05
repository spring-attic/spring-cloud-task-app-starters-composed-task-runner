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
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.TaskVisitor;
import org.springframework.cloud.dataflow.core.dsl.TransitionNode;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.task.app.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Creates the Steps necessary to execute the directed graph of a Composed
 * Task.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class StepBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar,
		EnvironmentAware {

	private Environment env;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		ComposedTaskProperties properties = composedTaskProperties();
		TaskParser taskParser = new TaskParser("bean-registration",
				properties.getGraph(), false, true);
		Map<String, Integer> taskSuffixMap = getTaskApps(taskParser);
		for (String taskName : taskSuffixMap.keySet()) {
			//handles the possibility that multiple instances of
			// task definition exist in a composed task
			for (int taskSuffix = 0; taskSuffixMap.get(taskName) >= taskSuffix; taskSuffix++) {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder
						.rootBeanDefinition(ComposedTaskRunnerStepFactory.class);
				builder.addConstructorArgValue(properties);
				builder.addConstructorArgValue(String.format("%s_%s",
						taskName, taskSuffix));
				builder.addPropertyValue("taskSpecificProps",
						getPropertiesForTask(taskName, properties));

				registry.registerBeanDefinition(String.format("%s_%s",
						taskName, taskSuffix), builder.getBeanDefinition());
			}
		}
	}

	private Map<String, String> getPropertiesForTask(String taskName, ComposedTaskProperties properties) {
		Map<String, String> definitionProperties =
				DeploymentPropertiesUtils.parse(properties.getComposedTaskProperties());

		String appPrefix = String.format("app.%s.", taskName);
		Map<String, String> deploymentProperties = new HashMap<>();
		for (Map.Entry<String, String> entry : definitionProperties.entrySet()) {
			if (entry.getKey().startsWith(appPrefix)) {
				deploymentProperties.put(entry.getKey()
						.substring(appPrefix.length()), entry.getValue());
			}
		}

		return deploymentProperties;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.env = environment;
	}


	private ComposedTaskProperties composedTaskProperties() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		String dataFlowUriString = this.env.getProperty("dataFlowUri");
		String maxWaitTime = this.env.getProperty("maxWaitTime");
		String intervalTimeBetweenChecks =
				this.env.getProperty("intervalTimeBetweenChecks");
		properties.setGraph(this.env.getProperty("graph"));
		properties.setComposedTaskArguments(
				this.env.getProperty("composedTaskArguments"));
		properties.setComposedTaskProperties("composedTaskProperties");

		if (maxWaitTime != null) {
			properties.setMaxWaitTime(Integer.valueOf(maxWaitTime));
		}
		if (intervalTimeBetweenChecks != null) {
			properties.setIntervalTimeBetweenChecks(Integer.valueOf(
					intervalTimeBetweenChecks));
		}
		if (dataFlowUriString != null) {
			try {
				properties.setDataFlowUri(new URI(dataFlowUriString));
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid Data Flow URI");
			}
		}
		return properties;
	}

	/**
	 * @return a {@link Map} of task app name as the key and the number of times it occurs
	 * as the value.
	 */
	private Map<String, Integer> getTaskApps(TaskParser taskParser) {
		TaskAppsMapCollector collector = new TaskAppsMapCollector();
		taskParser.parse().accept(collector);
		return collector.getTaskApps();
	}

	/**
	 * Simple visitor that discovers all the tasks in use in the composed
	 * task definition.
	 */
	static class TaskAppsMapCollector extends TaskVisitor {

		Map<String, Integer> taskApps = new HashMap<>();

		@Override
		public void visit(TaskAppNode taskApp) {
			if (taskApps.containsKey(taskApp.getName())) {
				Integer updatedCount = taskApps.get(taskApp.getName()) + 1;
				taskApps.put(taskApp.getName(), updatedCount);
			}
			else {
				taskApps.put(taskApp.getName(), 0);
			}
		}

		@Override
		public void visit(TransitionNode transition) {
			if (transition.isTargetApp()) {
				if (taskApps.containsKey(transition.getTargetApp())) {
					Integer updatedCount = taskApps.get(transition.getTargetApp()) + 1;
					taskApps.put(transition.getTargetApp().getName(), updatedCount);
				}
				else {
					taskApps.put(transition.getTargetApp().getName(), 0);
				}
			}
		}

		public Map<String, Integer> getTaskApps() {
			return taskApps;
		}

	}

}
