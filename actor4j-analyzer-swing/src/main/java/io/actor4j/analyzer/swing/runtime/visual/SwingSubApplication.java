/*
 * Copyright (c) 2015-2017, David A. Bauer. All rights reserved.
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
package io.actor4j.analyzer.swing.runtime.visual;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

public class SwingSubApplication {
	
	protected String title = null;
	protected LookAndFeel lookAndFeel = null;
	protected JFrame frame = null;
	
	public void setTitle(String value) {
		title = value;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setLookAndFeel(LookAndFeel value) {
		lookAndFeel = value;
		try {  
			UIManager.setLookAndFeel(value.toString());
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public LookAndFeel getLookAndFeel(LookAndFeel value) {
		return lookAndFeel;
	}
	
	public void set(String title, LookAndFeel value) {
		setTitle(title);
		setLookAndFeel(value);
	}
	
	public void setApplication(String title, LookAndFeel value) {
		set(title, value);
	}
	
	public void run(JFrame frame) {
		if (frame!=null) {
			this.frame = frame;
			
			if (title!=null) frame.setTitle(title);
			
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = frame.getSize();
            if (frameSize.height > screenSize.height) {
                  frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                  frameSize.width = screenSize.width;
            }
            frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setVisible(true);
		}
	}
	
	public void runApplication(JFrame frame) {
		run(frame);
	}
	
	public JFrame getFrame() {
		return frame;
	}
}