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
import java.util.Stack;
import java.util.UUID;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskParser;
import org.springframework.cloud.dataflow.core.dsl.FlowNode;
import org.springframework.cloud.dataflow.core.dsl.LabelledComposedTaskNode;
import org.springframework.cloud.dataflow.core.dsl.SplitNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TransitionNode;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

/**
 * Genererates a Composed Task Job Flow.
 *
 * @author Glenn Renfro
 */
public class ComposedRunnerJobBuilder {

	private static final String WILD_CARD = "*";

	@Autowired
	private ApplicationContext context;

	private FlowBuilder<Flow> flowBuilder;

	private Map<String, Integer> taskBeanSuffixes = new HashMap<>();

	private Stack visitorStack = new Stack<>();

	private Stack<Flow> jobStack = new Stack<>();

	public ComposedRunnerJobBuilder(ComposedRunnerVisitor composedRunnerVisitor) {
		Assert.notNull(composedRunnerVisitor, "composedRunnerVisitor must not be null");
		this.flowBuilder = new FlowBuilder<>(UUID.randomUUID().toString());
		this.visitorStack = composedRunnerVisitor.getFlowStack();
	}

	/**
	 * Create a FlowBuilder based on the stack provided by the
	 * ComposedRunnerVisitor.

	 * @return FlowBuilder that generates the composed task flow.
	 */
	public FlowBuilder<Flow> getFlowBuilder() {
		return this.flowBuilder.start(createFlow());
	}

	private Flow createFlow() {
		Stack<Flow> executionStack = new Stack<>();
		while (!this.visitorStack.isEmpty()) {
			if (this.visitorStack.peek() instanceof TaskAppNode) {
				TaskAppNode taskAppNode = (TaskAppNode) this.visitorStack.pop();
				if (taskAppNode.hasTransitions()) {
					handleTransition(executionStack, taskAppNode);
				}
				else {
					executionStack.push(
							getTaskAppFlow(taskAppNode));
				}
			}
			//When end marker of a split is found, process the split
			else if (this.visitorStack.peek() instanceof SplitNode) {
				handleSplit(executionStack, (SplitNode) this.visitorStack.pop());
			}
			//When start marker of a DSL flow is found, process it.
			else if (this.visitorStack.peek() instanceof FlowNode) {
				handleFlow(executionStack);
			}
		}
		return this.jobStack.pop();
	}

	private void handleFlow(Stack<Flow> executionStack) {
		boolean isFirst = true;
		while (!executionStack.isEmpty()) {
			if (isFirst) {
				this.flowBuilder.start(executionStack.pop());
				isFirst = false;
			}
			else {
				this.flowBuilder.next(executionStack.pop());
			}
		}
		this.visitorStack.pop();
		this.jobStack.push(this.flowBuilder.end());
	}

	private void handleSplit(Stack<Flow> executionStack, SplitNode splitNode) {
		FlowBuilder<Flow> taskAppFlowBuilder =
				new FlowBuilder<>("Flow" + UUID.randomUUID().toString());
		List<Flow> flows = new ArrayList<>();
		//For each node in the split process it as a DSL flow.
		for (LabelledComposedTaskNode taskNode : splitNode.getSeries()) {
			Stack<Flow> elementFlowStack = processSplitFlow(taskNode);
			while (!elementFlowStack.isEmpty()) {
				flows.add(elementFlowStack.pop());
			}
		}
		Flow splitFlow = new FlowBuilder.SplitBuilder<>(
				new FlowBuilder<Flow>("Split" + UUID.randomUUID().toString()),
				new SimpleAsyncTaskExecutor())
				.add(flows.toArray(new Flow[flows.size()])).build();
		//remove the nodes of the split since it has already been processed
		while (!(this.visitorStack.peek() instanceof SplitNode)) {
			this.visitorStack.pop();
		}
		// pop the SplitNode that marks the beginning of the split from the stack
		this.visitorStack.pop();
		executionStack.push(taskAppFlowBuilder.start(splitFlow).end());
	}

	/**
	 * Processes each node in split as a  DSL Flow.
	 * @param node represents a single node in the split.
	 * @return Stack of Job Flows that was obtained from the Node.
	 */
	private Stack<Flow> processSplitFlow(LabelledComposedTaskNode node) {
		ComposedTaskParser taskParser = new ComposedTaskParser();
		ComposedRunnerVisitor splitElementVisitor = new ComposedRunnerVisitor();
		taskParser.parse("aname", node.stringify()).accept(splitElementVisitor);
		Stack splitElementStack = splitElementVisitor.getFlowStack();
		Stack<Flow> elementFlowStack = new Stack<>();
		Stack<Flow> resultFlowStack = new Stack<>();
		while (!splitElementStack.isEmpty()) {
			if (splitElementStack.peek() instanceof TaskAppNode) {
				TaskAppNode taskAppNode = (TaskAppNode) splitElementStack.pop();
				if (taskAppNode.hasTransitions()) {
					handleTransition(elementFlowStack, taskAppNode);
				}
				else {
					elementFlowStack.push(
							getTaskAppFlow(taskAppNode));
				}
			}
			else if (splitElementStack.peek() instanceof FlowNode) {
				handleFlowForSegment(elementFlowStack, resultFlowStack);
				splitElementStack.pop();
			}
		}
		return resultFlowStack;
	}

	private void handleFlowForSegment(Stack<Flow> executionStack,
			Stack<Flow> resultStack) {
		boolean isFirst = true;
		FlowBuilder<Flow> localTaskAppFlowBuilder =
				new FlowBuilder<>("Flow" + UUID.randomUUID().toString());
		while (!executionStack.isEmpty()) {
			if (isFirst) {
				localTaskAppFlowBuilder.start(executionStack.pop());
				isFirst = false;
			}
			else {
				localTaskAppFlowBuilder.next(executionStack.pop());
			}
		}
		resultStack.push(localTaskAppFlowBuilder.end());
	}

	private void handleTransition(Stack<Flow> executionStack,
			TaskAppNode taskAppNode) {
		String beanName = getBeanName(taskAppNode);
		Step currentStep = this.context.getBean(beanName, Step.class);
		FlowBuilder<Flow> builder = new FlowBuilder<Flow>(beanName)
				.from(currentStep);
		boolean wildCardPresent = false;
		for (TransitionNode transitionNode : taskAppNode.getTransitions()) {
			String transitionBeanName = getBeanName(transitionNode);
			if (transitionNode.getStatusToCheck().equals(WILD_CARD)) {
				wildCardPresent = true;
			}
			Step transitionStep = this.context.getBean(transitionBeanName,
					Step.class);
			builder.on(transitionNode.getStatusToCheck()).to(transitionStep)
					.from(currentStep);
		}
		if (wildCardPresent && !executionStack.isEmpty()) {
			throw new IllegalStateException(
					"Invalid flow following '*' specifier.");
		}
		else {
			//if there are nodes are in the execution stack.  Make sure that
			//they are processed as a target of the wildcard instead of the
			//whole transition.
			if (!executionStack.isEmpty()) {
				Stack<Flow> resultStack = new Stack<>();
				handleFlowForSegment(executionStack, resultStack);
				builder.on(WILD_CARD).to(resultStack.pop()).from(currentStep);
			}
		}
		executionStack.push(builder.end());
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
		Integer taskSuffix;
		if (this.taskBeanSuffixes.containsKey(taskName)) {
			taskSuffix = this.taskBeanSuffixes.get(taskName);
		}
		else {
			taskSuffix = 0;
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
