/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.apps.mytracks.io.docs;

import biz.varkon.shelvesom.util.TextUtilities;

/**
 * <p>
 * This class builds a string of XML tags used to talk to Docs using GData.
 * 
 * <p>
 * Sample Usage:
 * 
 * <code>
 *   String tags = new DocsTagBuilder(false)
 *       .append("tagName", "tagValue")
 *       .appendLargeUnits("bigTagName", 1.0)
 *       .build();
 * </code>
 * 
 * <p>
 * results in:
 * 
 * <code>
 *    <gsx:tagName><![CDATA[tagValue]]></gsx:tagName>
 *    <gsx:bigTagName><![CDATA[1.00]]></gsx:bigTagName>
 * </code>
 * 
 * @author Matthew Simmons
 */
public class DocsTagBuilder {

	protected final StringBuilder stringBuilder;

	/**
	 * @param metricUnits
	 *            True if metric units are to be used. If false, imperial units
	 *            will be used.
	 */
	public DocsTagBuilder() {
		stringBuilder = new StringBuilder();
	}

	/** Appends a tag containing a string value */
	public DocsTagBuilder append(String name, String value) {
		name = name.replace("_", "");

		appendTag(name, value);
		return this;
	}

	/**
	 * Appends a tag containing a numeric value. The value will be formatted
	 * according to the large distance of the specified measurement system (i.e.
	 * kilometers or miles).
	 * 
	 * @param name
	 *            The tag name.
	 * @param d
	 *            The value to be formatted, in kilometers.
	 */
	/*
	 * DocsTagBuilder appendLargeUnits(String name, double d) { double value =
	 * metricUnits ? d : (d * UnitConversions.KM_TO_MI); appendTag(name,
	 * LARGE_UNIT_FORMAT.format(value)); return this; }
	 */

	/**
	 * Appends a tag containing a numeric value. The value will be formatted
	 * according to the small distance of the specified measurement system (i.e.
	 * meters or feet).
	 * 
	 * @param name
	 *            The tag name.
	 * @param d
	 *            The value to be formatted, in meters.
	 */

	/** Returns a string containing all tags which have been added. */
	public String build() {
		return stringBuilder.toString();
	}

	private void appendTag(String name, String value) {
		stringBuilder.append("<gsx:").append(name).append(">")
				.append(TextUtilities.stringAsCData(value)).append("</gsx:")
				.append(name).append(">");
	}
}
