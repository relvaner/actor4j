/*
 * Copyright (c) 2015-2023, David A. Bauer. All rights reserved.
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
package io.actor4j.analyzer.fx.runtime.visual;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

public class FXVisualActorMenuBar extends MenuBar {
	protected Menu exampleMenu;
	
	protected MenuItem exampleMenuItem;
	
	public FXVisualActorMenuBar() {
		super();
		
		initialize();
	}

	public void initialize() {
		exampleMenu = new Menu("exampleMenu");
		exampleMenuItem = new MenuItem("exampleMenuItem");
		exampleMenu.getItems().add(exampleMenuItem);
		
		getMenus().add(exampleMenu);
	}
}
