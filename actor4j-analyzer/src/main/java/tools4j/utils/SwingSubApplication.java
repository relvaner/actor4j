/*
 * tools4j - Java Library
 * Copyright (c) 2008-2017, David A. Bauer
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tools4j.utils;

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