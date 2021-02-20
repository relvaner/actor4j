/*
 * Copyright (c) 2015-2020, David A. Bauer. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.actor4j.analyzer.internal.visual;

import static io.actor4j.core.logging.ActorLogger.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class Utils {
	public static final List<String> colorPalette;
	
	static {
		colorPalette = new ArrayList<>();
		colorPalette.add("#FED842");
		colorPalette.add("#FA9A93");
		colorPalette.add("#6484ED");
		colorPalette.add("#A9C6AB");
		colorPalette.add("#F88B50");
		
		colorPalette.add("#C4A9C6");
		colorPalette.add("#47C1E9");
		colorPalette.add("#8ACE2A");
		colorPalette.add("#19AC91");
		colorPalette.add("#338B9E");
		colorPalette.add("#EEE606");
		
		colorPalette.add("#DC3701");
		colorPalette.add("#C60C88");
		colorPalette.add("#9903FD");
		colorPalette.add("#3CF7B2");
		colorPalette.add("#082D65");
		colorPalette.add("#B25462");
		colorPalette.add("#760408");
		
		if (!(new HashSet<String>(colorPalette).size()==colorPalette.size()))
			systemLogger().log(ERROR, String.format("[ANALYZER] color palette inconsistent"));
	}
	
	public static Color randomColor(int index, int parallelism) {
		float hue = 1.0f;
		if (index!=parallelism)
			hue = (index*(360f/parallelism)+1)/(float)(parallelism*(360f/parallelism)+1);
		float saturation = 0.9f;
		float brightness = 1.0f;
		return Color.getHSBColor(hue, saturation, brightness);
	}
	
	public static String colorToHex(Color color) {
		return "#"+Integer.toHexString(color.getRGB()).substring(2);
	}
	
	public static String randomColorAsHex(int index, int parallelism) {
		if (index<colorPalette.size())
			return colorPalette.get(index);
		
		return colorToHex(randomColor(index, parallelism));
	}
}
