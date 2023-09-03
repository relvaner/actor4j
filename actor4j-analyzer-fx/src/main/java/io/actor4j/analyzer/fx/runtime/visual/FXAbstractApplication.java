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

import javafx.application.Application;
import javafx.stage.Stage;

public abstract class FXAbstractApplication extends Application {
	protected Stage primaryStage;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		
		run(primaryStage);
		
		primaryStage.setTitle(getTitle());
		
		primaryStage.show();
		
		afterShow();
	}
	
	public abstract String getTitle();
	
	public abstract void run(Stage primaryStage);
	
	public abstract void afterShow();
	
	public Stage primaryStage() {
		return primaryStage;
	}
}