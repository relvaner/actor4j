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
package io.actor4j.analyzer.runtime.visual;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

public class VisualActorMenuBar extends JMenuBar {
	protected static final long serialVersionUID = -3388697604006363727L;
	
	protected JMenu meLayout;
	protected JMenu meLayoutStructure;
	protected JMenu meLayoutBehaviour;
	protected JMenu meShowPanel;
	
	protected JRadioButtonMenuItem rbLayoutS0;
	protected JRadioButtonMenuItem rbLayoutS1;
	protected JRadioButtonMenuItem rbLayoutS2;
	protected JRadioButtonMenuItem rbLayoutB0;
	protected JRadioButtonMenuItem rbLayoutB1;
	protected JRadioButtonMenuItem rbLayoutB2;
	
	protected JCheckBoxMenuItem cbStructure;
	protected JCheckBoxMenuItem cbBehaviour;
	
	public VisualActorMenuBar() {
		super();
		
		initialize();
	}
	
	protected void initialize() {
		meLayout = new JMenu("Layout");
		meLayoutStructure = new JMenu("Structure");
		rbLayoutS0 = new JRadioButtonMenuItem("Organic Layout");
		rbLayoutS1 = new JRadioButtonMenuItem("Horizontal Layout");
		rbLayoutS2 = new JRadioButtonMenuItem("Vertical Layout");
		meLayoutBehaviour = new JMenu("Behaviour");
		rbLayoutB0 = new JRadioButtonMenuItem("Organic Layout");
		rbLayoutB1 = new JRadioButtonMenuItem("Horizontal Layout");
		rbLayoutB2 = new JRadioButtonMenuItem("Vertical Layout");
		
		meShowPanel = new JMenu("View");
		cbStructure = new JCheckBoxMenuItem("Structure");
		cbBehaviour = new JCheckBoxMenuItem("Behaviour");
		
		add(meLayout);
		meLayout.add(meLayoutStructure);
		meLayoutStructure.add(rbLayoutS0);
		meLayoutStructure.add(rbLayoutS1);
		meLayoutStructure.add(rbLayoutS2);
		meLayout.add(meLayoutBehaviour);
		meLayoutBehaviour.add(rbLayoutB0);
		meLayoutBehaviour.add(rbLayoutB1);
		meLayoutBehaviour.add(rbLayoutB2);
		
		add(meShowPanel);
		meShowPanel.add(cbStructure);
		meShowPanel.add(cbBehaviour);
		
		rbLayoutS0.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rbLayoutS0.setSelected(true);
	        	rbLayoutS1.setSelected(false);
	        	rbLayoutS2.setSelected(false);
	        	
	        	VisualActorStructureViewPanel.layoutIndex.set(0);
	        }
	    });
		rbLayoutS1.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rbLayoutS0.setSelected(false);
	        	rbLayoutS1.setSelected(true);
	        	rbLayoutS2.setSelected(false);
	        	
	        	VisualActorStructureViewPanel.layoutIndex.set(1);
	        }
	    });
		rbLayoutS2.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rbLayoutS0.setSelected(false);
	        	rbLayoutS1.setSelected(false);
	        	rbLayoutS2.setSelected(true);
	        	
	        	VisualActorStructureViewPanel.layoutIndex.set(2);
	        }
	    });
		
		rbLayoutB0.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rbLayoutB0.setSelected(true);
	        	rbLayoutB1.setSelected(false);
	        	rbLayoutB2.setSelected(false);
	        	
	        	VisualActorBehaviourViewPanel.layoutIndex.set(0);
	        }
	    });
		rbLayoutB1.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rbLayoutB0.setSelected(false);
	        	rbLayoutB1.setSelected(true);
	        	rbLayoutB2.setSelected(false);
	        	
	        	VisualActorBehaviourViewPanel.layoutIndex.set(1);
	        }
	    });
		rbLayoutB2.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rbLayoutB0.setSelected(false);
	        	rbLayoutB1.setSelected(false);
	        	rbLayoutB2.setSelected(true);
	        	
	        	VisualActorBehaviourViewPanel.layoutIndex.set(2);
	        }
	    });
		
		rbLayoutS0.setSelected(true);
		VisualActorStructureViewPanel.layoutIndex.set(0);
		rbLayoutB0.setSelected(true);
		VisualActorBehaviourViewPanel.layoutIndex.set(0);
		cbStructure.setSelected(true);
		cbBehaviour.setSelected(true);
	}

	public JCheckBoxMenuItem getCbStructure() {
		return cbStructure;
	}

	public JCheckBoxMenuItem getCbBehaviour() {
		return cbBehaviour;
	}
}
