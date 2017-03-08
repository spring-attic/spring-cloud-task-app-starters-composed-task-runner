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

/**
 * Used to mark the beginning of a flow in the stack.
 * @author Glenn Renfro
 */
public class FlowMarker {

	public enum FlowMarkerType  {FLOW, SPLIT}

	private FlowMarkerType type;

	private String flowMarkeUUID;

	public FlowMarker(FlowMarkerType type, String flowMarkeUUID) {
		this.type = type;
		this.flowMarkeUUID = flowMarkeUUID;
	}

	public FlowMarkerType getType() {
		return type;
	}

	public void setType(FlowMarkerType type) {
		this.type = type;
	}

	public String getFlowMarkeUUID() {
		return flowMarkeUUID;
	}

	public void setFlowMarkeUUID(String flowMarkeUUID) {
		this.flowMarkeUUID = flowMarkeUUID;
	}
}
