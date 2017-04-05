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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.FlowNode;
import org.springframework.cloud.dataflow.core.dsl.LabelledTaskNode;
import org.springframework.cloud.dataflow.core.dsl.SplitNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.TransitionNode;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Genererates a Composed Task Job Flow.
 *
 * @author Glenn Renfro
 */
public class ComposedRunnerJobFactory implements FactoryBean<Job> {

	private static final String WILD_CARD = "*";

	@Autowired
	private ApplicationContext context;

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private TaskNameResolver taskNameResolver;

	private FlowBuilder<Flow> flowBuilder;

	private Map<String, Integer> taskBeanSuffixes = new HashMap<>();

	private Deque<Flow> jobDeque = new LinkedList<>();

	private Deque<LabelledTaskNode> visitorDeque;

	private String dsl;

	public ComposedRunnerJobFactory(String dsl) {
		Assert.notNull(dsl, "The DSL must not be null");
		this.dsl = dsl;
		this.flowBuilder = new FlowBuilder<>(UUID.randomUUID().toString());
	}

	@Override
	public Job getObject() throws Exception {
		ComposedRunnerVisitor composedRunnerVisitor = new ComposedRunnerVisitor();

		TaskParser taskParser = new TaskParser("composed-task-runner",
				this.dsl,false,true);
		taskParser.parse().accept(composedRunnerVisitor);

		this.visitorDeque = composedRunnerVisitor.getFlow();

		return this.jobBuilderFactory
				.get(this.taskNameResolver.getTaskName())
				.start(this.flowBuilder
						.start(createFlow())
						.end())
				.end()
				.build();
	}

	@Override
	public Class<?> getObjectType() {
		return Job.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private Flow createFlow() {
		Deque<Flow> executionDeque = new LinkedList<>();

		while (!this.visitorDeque.isEmpty()) {

			if (this.visitorDeque.peek() instanceof TaskAppNode) {
				TaskAppNode taskAppNode = (TaskAppNode) this.visitorDeque.pop();

				if (taskAppNode.hasTransitions()) {
					handleTransition(executionDeque, taskAppNode);
				}
				else {
					executionDeque.push(getTaskAppFlow(taskAppNode));
				}
			}
			//When end marker of a split is found, process the split
			else if (this.visitorDeque.peek() instanceof SplitNode) {
				handleSplit(executionDeque, (SplitNode) this.visitorDeque.pop());
			}
			//When start marker of a DSL flow is found, process it.
			else if (this.visitorDeque.peek() instanceof FlowNode) {
				handleFlow(executionDeque);
			}
		}

		return this.jobDeque.pop();
	}

	private void handleFlow(Deque<Flow> executionDeque) {
		if(!executionDeque.isEmpty()) {
			this.flowBuilder.start(executionDeque.pop());
		}

		while (!executionDeque.isEmpty()) {
				this.flowBuilder.next(executionDeque.pop());
		}

		this.visitorDeque.pop();
		this.jobDeque.push(this.flowBuilder.end());
	}

	private void handleSplit(Deque<Flow> executionDeque, SplitNode splitNode) {
		FlowBuilder<Flow> taskAppFlowBuilder =
				new FlowBuilder<>("Flow" + UUID.randomUUID().toString());
		List<Flow> flows = new ArrayList<>();

		//For each node in the split process it as a DSL flow.
		for (LabelledTaskNode taskNode : splitNode.getSeries()) {
			Deque<Flow> elementFlowDeque = processSplitFlow(taskNode);
			while (!elementFlowDeque.isEmpty()) {
				flows.add(elementFlowDeque.pop());
			}
		}

		Flow splitFlow = new FlowBuilder.SplitBuilder<>(
				new FlowBuilder<Flow>("Split" + UUID.randomUUID().toString()),
				taskExecutor)
				.add(flows.toArray(new Flow[flows.size()]))
				.build();

		//remove the nodes of the split since it has already been processed
		while (!(this.visitorDeque.peek() instanceof SplitNode)) {
			this.visitorDeque.pop();
		}

		// pop the SplitNode that marks the beginning of the split from the deque
		this.visitorDeque.pop();
		executionDeque.push(taskAppFlowBuilder.start(splitFlow).end());
	}

	/**
	 * Processes each node in split as a  DSL Flow.
	 * @param node represents a single node in the split.
	 * @return Deque of Job Flows that was obtained from the Node.
	 */
	private Deque<Flow> processSplitFlow(LabelledTaskNode node) {
		TaskParser taskParser = new TaskParser("split_flow", node.stringify(),
				false, true);
		ComposedRunnerVisitor splitElementVisitor = new ComposedRunnerVisitor();
		taskParser.parse().accept(splitElementVisitor);

		Deque splitElementDeque = splitElementVisitor.getFlow();
		Deque<Flow> elementFlowDeque = new LinkedList<>();
		Deque<Flow> resultFlowDeque = new LinkedList<>();

		while (!splitElementDeque.isEmpty()) {

			if (splitElementDeque.peek() instanceof TaskAppNode) {

				TaskAppNode taskAppNode = (TaskAppNode) splitElementDeque.pop();

				if (taskAppNode.hasTransitions()) {
					handleTransition(elementFlowDeque, taskAppNode);
				}
				else {
					elementFlowDeque.push(
							getTaskAppFlow(taskAppNode));
				}
			}
			else if (splitElementDeque.peek() instanceof FlowNode) {
				handleFlowForSegment(elementFlowDeque, resultFlowDeque);
				splitElementDeque.pop();
			}
		}

		return resultFlowDeque;
	}

	private void handleFlowForSegment(Deque<Flow> executionDeque,
			Deque<Flow> resultDeque) {
		FlowBuilder<Flow> localTaskAppFlowBuilder =
				new FlowBuilder<>("Flow" + UUID.randomUUID().toString());

		if(!executionDeque.isEmpty()) {
			localTaskAppFlowBuilder.start(executionDeque.pop());

		}

		while (!executionDeque.isEmpty()) {
			localTaskAppFlowBuilder.next(executionDeque.pop());
		}

		resultDeque.push(localTaskAppFlowBuilder.end());
	}

	private void handleTransition(Deque<Flow> executionDeque,
			TaskAppNode taskAppNode) {
		String beanName = getBeanName(taskAppNode);
		Step currentStep = this.context.getBean(beanName, Step.class);
		FlowBuilder<Flow> builder = new FlowBuilder<Flow>(beanName)
				.from(currentStep);

		boolean wildCardPresent = false;

		for (TransitionNode transitionNode : taskAppNode.getTransitions()) {
			String transitionBeanName = getBeanName(transitionNode);

			wildCardPresent = transitionNode.getStatusToCheck().equals(WILD_CARD);

			Step transitionStep = this.context.getBean(transitionBeanName,
					Step.class);
			builder.on(transitionNode.getStatusToCheck()).to(transitionStep)
					.from(currentStep);
		}

		if (wildCardPresent && !executionDeque.isEmpty()) {
			throw new IllegalStateException(
					"Invalid flow following '*' specifier.");
		}
		else {
			//if there are nodes are in the execution Deque.  Make sure that
			//they are processed as a target of the wildcard instead of the
			//whole transition.
			if (!executionDeque.isEmpty()) {
				Deque<Flow> resultDeque = new LinkedList<>();
				handleFlowForSegment(executionDeque, resultDeque);
				builder.on(WILD_CARD).to(resultDeque.pop()).from(currentStep);
			}
		}

		executionDeque.push(builder.end());
	}

	private String getBeanName(TransitionNode transition) {
		if (transition.getTargetLabel() != null) {
			return transition.getTargetLabel();
		}

		return getBeanName(transition.getTargetApp());
	}


	private String getBeanName(TaskAppNode taskApp) {
		if (taskApp.getLabel() != null) {
			return taskApp.getLabel().stringValue();
		}

		String taskName = taskApp.getName();

		if (taskName.contains("->")) {
			taskName = taskName.substring(taskName.indexOf("->") + 2);
		}

		return getBeanName(taskName);
	}

	private String getBeanName(String taskName) {
		int taskSuffix = 0;

		if (this.taskBeanSuffixes.containsKey(taskName)) {
			taskSuffix = this.taskBeanSuffixes.get(taskName);
		}

		String result = String.format("%s_%s", taskName, taskSuffix++);
		this.taskBeanSuffixes.put(taskName, taskSuffix);

		return result;
	}

	private Flow getTaskAppFlow(TaskAppNode taskApp) {
		String beanName = getBeanName(taskApp);
		Step currentStep = this.context.getBean(beanName, Step.class);

		return new FlowBuilder<Flow>(beanName).from(currentStep).end();
	}
}
