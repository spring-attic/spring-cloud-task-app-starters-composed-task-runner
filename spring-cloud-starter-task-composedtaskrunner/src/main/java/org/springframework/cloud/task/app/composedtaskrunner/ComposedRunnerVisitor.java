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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskVisitor;
import org.springframework.cloud.dataflow.core.dsl.FlowNode;
import org.springframework.cloud.dataflow.core.dsl.LabelledComposedTaskNode;
import org.springframework.cloud.dataflow.core.dsl.SplitNode;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TransitionNode;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Creates the Job Flow based on the ComposedTaskVisitor implementation
 * provided by the TaskParser in Spring Cloud Data Flow.
 *
 * @author Glenn Renfro.
 */
public class ComposedRunnerVisitor extends ComposedTaskVisitor {

	private FlowBuilder<org.springframework.batch.core.job.flow.Flow> flowBuilder;

	private boolean isInitialized = false;

	private Map<String, Integer> taskBeanSuffixes = new HashMap<>();

	private Stack flowStack= new Stack();

	@Autowired
	private ApplicationContext context;

	private static final Log logger = LogFactory.getLog(ComposedRunnerVisitor.class);

	public ComposedRunnerVisitor() {
		initialize();
	}

	/**
	 * The first call made to a visitor.
	 */
	public void startVisit() {
		logger.debug("Start Visit");
	}

	/**
	 * The last call made to a visitor.
	 */
	public void endVisit() {
		logger.debug("End Visit");
	}

	/**
	 * @param firstNode the sequence number, where the primary sequence is 0
	 * @return false to skip visiting the specified sequence
	 */
	public boolean preVisitSequence(LabelledComposedTaskNode firstNode, int sequenceNumber) {
		logger.debug("Pre Visit Sequence");
		return true;
	}

	public void postVisitSequence(LabelledComposedTaskNode firstNode, int sequenceNumber) {
		logger.debug("Post Visit Sequence");
	}

	/**
	 * @param flow the flow which represents things to execute in sequence
	 * @return false to skip visiting this flow
	 */
	public boolean preVisit(FlowNode flow) {
		logger.debug("Pre Visit Flow:  " + flow);
		flowStack.push(new FlowMarker(FlowMarker.FlowMarkerType.FLOW, flow.toString()));
		return true;
	}

	public void visit(FlowNode flow) {
		logger.debug("Visit Flow:  " + flow);

	}

	public void postVisit(FlowNode flow) {
		logger.debug("Post Visit Flow:  " + flow);
		Stack executionStack = new Stack();
		FlowBuilder<org.springframework.batch.core.job.flow.Flow> taskAppFlowBuilder =
				new FlowBuilder("Flow" + UUID.randomUUID().toString());
		while (!flowStack.isEmpty()) {
			if (flowStack.peek() instanceof FlowMarker) {
				flowStack.pop();// pop off marker
				boolean isFirst = true;
				while (!executionStack.isEmpty()){
					if(isFirst){
						taskAppFlowBuilder.start((org.springframework.batch.core.job.flow.Flow) executionStack.pop());
						isFirst = false;
					}
					else {
						taskAppFlowBuilder.next((org.springframework.batch.core.job.flow.Flow) executionStack.pop());
					}
				}
				break;
			}
			else {
				executionStack.push(flowStack.pop());
			}
		}
		flowStack.push(taskAppFlowBuilder.end());
	}

	/**
	 * @param split the split which represents things to execute in parallel
	 * @return false to skip visiting this split
	 */
	public boolean preVisit(SplitNode split) {
		logger.debug("Pre Visit Split:  " + split);
		flowStack.push(new FlowMarker(FlowMarker.FlowMarkerType.SPLIT, split.toString()));
		return true;
	}

	public void visit(SplitNode split) {
		logger.debug("Visit Split:  " + split);
	}

	public void postVisit(SplitNode split) {
		logger.debug("Post Visit Split:  " + split);
		Stack executionStack = new Stack();
		FlowBuilder<org.springframework.batch.core.job.flow.Flow> taskAppFlowBuilder =
				new FlowBuilder("Flow" + UUID.randomUUID().toString());
		List<org.springframework.batch.core.job.flow.Flow> flows = new ArrayList<>();
		while (!flowStack.isEmpty()) {
			if (flowStack.peek() instanceof FlowMarker) {
				flowStack.pop(); //Remove Marker
				while (!executionStack.isEmpty()){
					flows.add((org.springframework.batch.core.job.flow.Flow) executionStack.pop());
				}
				break;
			}
			else {
				executionStack.push(flowStack.pop());
			}
		}
		org.springframework.batch.core.job.flow.Flow splitFlow =
				new FlowBuilder.SplitBuilder<>(
						new FlowBuilder<org.springframework.batch.core.job.flow.Flow>("Split"+UUID.randomUUID().toString()),
						new SimpleAsyncTaskExecutor())
						.add(flows.toArray(new org.springframework.batch.core.job.flow.Flow[flows.size()])).build();

		flowStack.push(taskAppFlowBuilder.next(splitFlow).end());
	}


	/**
	 * <b>This preVisit/visit/postVisit sequence for taskApp is not invoked for inlined references to apps
	 * in transitions, for example: <tt>appA 0->:foo 1->appB</tt>. The reference to <tt>appB</tt> would be
	 * seen in the transition visit below.
	 * @param taskApp the use of a task app in a composed task dsl
	 * @return false to skip visiting this taskApp
	 */
	public boolean preVisit(TaskAppNode taskApp) {
		logger.debug("Pre Visit taskApp:  " + taskApp);
		return true;
	}

	public void visit(TaskAppNode taskApp) {
		logger.debug("Visit taskApp:  " + taskApp);
		String beanName = getBeanName(taskApp);
		Step currentStep = context.getBean(beanName, Step.class);
		org.springframework.batch.core.job.flow.Flow taskAppFlow;
		if(taskApp.getTransitions() != null  && !taskApp.getTransitions().isEmpty()) {
			taskAppFlow = getTransitionFlow(beanName, currentStep, taskApp.getTransitions());
			flowStack.push(taskAppFlow);
		}
		else {
			taskAppFlow = new
					FlowBuilder<org.springframework.batch.core.job.flow.Flow>(beanName)
							.from(currentStep).end();
			flowStack.push(taskAppFlow);
		}

	}

	public void postVisit(TaskAppNode taskApp) {
		logger.debug("Post Visit taskApp:  " + taskApp);
	}


	/**
	 * After <tt>visit(TaskApp)</tt> and before <tt>postVisit(TaskApp)</tt> the transitions (if there
	 * are any) are visited for that task app.
	 * @param transition the transition
	 * @return false to skip visiting this transition
	 */
	public boolean preVisit(TransitionNode transition) {
		logger.debug("Pre Visit transition:  " + transition);
		return true;
	}

	public void visit(TransitionNode transition) {
		logger.debug("Visit transition:  " + transition);
	}

	public void postVisit(TransitionNode transition) {
		logger.debug("Post Visit transition:  " + transition);
	}

	public FlowBuilder<org.springframework.batch.core.job.flow.Flow> getFlowBuilder() {
		org.springframework.batch.core.job.flow.Flow batchFlow =
				(org.springframework.batch.core.job.flow.Flow)flowStack.pop();
		return flowBuilder.start(batchFlow);
	}

	private void initialize() {
		if(!isInitialized) {
			flowBuilder = new FlowBuilder<>(UUID.randomUUID().toString());
			isInitialized = true;
		}
	}

	private org.springframework.batch.core.job.flow.Flow getTransitionFlow(String beanName, Step currentStep, List<TransitionNode> transitions) {
		FlowBuilder<org.springframework.batch.core.job.flow.Flow> taskAppFlow = new
				FlowBuilder(beanName).from(currentStep);
		for(TransitionNode transition : transitions) {
			String transitionName = getBeanName(transition);
			Step transitionStep = context.getBean(transitionName, Step.class);
			taskAppFlow.on(transition.getStatusToCheck())
					.to(transitionStep).from(currentStep);
		}
		return taskAppFlow.end();
	}


	private String getBeanName(TransitionNode transition) {
		if(transition.getTargetLabel() != null) {
			return transition.getTargetLabel();
		}
		return getBeanName(transition.getTargetApp());
	}


	private String getBeanName(TaskAppNode taskApp) {
		if(taskApp.getLabel() != null) {
			return taskApp.getLabel().stringValue();
		}
		String taskName = taskApp.getName();
		if(taskName.contains("->")) {
			taskName = taskName.substring(taskName.indexOf("->")+2);
		}
		return getBeanName(taskName);
	}


	private String getBeanName(String taskName) {
		Integer taskSuffix;
		if(taskBeanSuffixes.containsKey(taskName)) {
			taskSuffix = taskBeanSuffixes.get(taskName);
		}
		else {
			taskSuffix = 0;
		}
		String result = String.format("%s_%s", taskName, taskSuffix++);
		taskBeanSuffixes.put(taskName, taskSuffix);
		return result;
	}


}
