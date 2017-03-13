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

package org.springframework.cloud.task.app.composedtaskrunner.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * Contains tools for extracting properties for a specific task definition and
 * placing them into a Collection to be used by the ComposedTaskRunner.
 *
 * @author Glenn Renfro
 */
public class PropertyUtility {

	/**
	 * Pattern used for parsing a String of comma-delimited key=value pairs.
	 */
	private static final Pattern DEPLOYMENT_PROPERTIES_PATTERN = Pattern.compile(",\\s*app\\.[^\\.]+\\.[^=]+=");

	/**
	 * Extracts the properties that are prefixed with app.taskName and returns
	 * a map where the key is the property key and the map value is the
	 * property value.
	 * @param taskName The name task to search for in the properties
	 * @param properties The list of properties to search
	 * @return Map containing the properties for the task.
	 */
	public static Map<String, String> getPropertiesForTask(String taskName,
			ComposedTaskProperties properties) {
		String appPrefix = String.format("app.%s.", taskName);
		Map<String, String> deploymentProperties = new HashMap<>();
		for (Map.Entry<String, String> entry : getAllProperties(
				properties.getComposedTaskProperties()).entrySet()) {
			if (entry.getKey().startsWith(appPrefix)) {
				deploymentProperties.put(entry.getKey()
						.substring(appPrefix.length()), entry.getValue());
			}
		}
		return deploymentProperties;
	}

	private static  Map<String, String> getAllProperties(String composedTaskProperties) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		if (!StringUtils.isEmpty(composedTaskProperties)) {
			Matcher matcher = DEPLOYMENT_PROPERTIES_PATTERN.matcher(
					composedTaskProperties);
			int start = 0;
			while (matcher.find()) {
				addKeyValuePairAsProperty(composedTaskProperties.substring(start,
						matcher.start()), deploymentProperties);
				start = matcher.start() + 1;
			}
			addKeyValuePairAsProperty(composedTaskProperties.substring(start),
					deploymentProperties);
		}
		return deploymentProperties;
	}

	/**
	 * Adds a String of format key=value to the provided Map as a key/value pair.
	 *
	 * @param pair       the String representation
	 * @param properties the Map to which the key/value pair should be added
	 */
	private static void addKeyValuePairAsProperty(String pair, Map<String, String> properties) {
		int firstEquals = pair.indexOf('=');
		if (firstEquals != -1) {
			// todo: should key only be a "flag" as in: put(key, true)?
			properties.put(pair.substring(0, firstEquals).trim(), pair.substring(firstEquals + 1).trim());
		}
	}
}
