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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import io.actor4j.core.runtime.InternalActorCell;
import io.actor4j.core.runtime.InternalActorSystem;

public class VisualActorFrame extends JFrame {
	protected static final long serialVersionUID = 6808210435112913511L;
	
	protected final InternalActorSystem system;

	protected JPanel contentPane;
	
	protected VisualActorMenuBar menuBar;
	
	protected VisualActorViewPanel leftViewPanel;
	protected VisualActorViewPanel rightViewPanel;

	protected JLabel statusLabel;
	
	public VisualActorFrame(InternalActorSystem system) {
		super();
		
		this.system = system;
		
		initialize();
	}
	
	public void initialize() {
		setBounds(0, 0, 1280, 800);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JPanel paContent = new JPanel();
		contentPane.add(paContent, BorderLayout.CENTER);
		paContent.setLayout(new GridLayout(1, 2, 0, 0));
		
		leftViewPanel  = new VisualActorStructureViewPanel(system);
		rightViewPanel = new VisualActorBehaviourViewPanel(system);
		
		paContent.add(leftViewPanel);
		paContent.add(rightViewPanel);
		
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout(0, 0));
		statusPanel.setPreferredSize(new Dimension(getWidth(), 16));
		JPanel statusLine = new JPanel();
		statusLine.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusLine.setPreferredSize(new Dimension(getWidth(), 2));
		statusLabel = new JLabel("");
		statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
		statusPanel.add(statusLine, BorderLayout.NORTH);
		statusPanel.add(statusLabel, BorderLayout.CENTER);
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		
		menuBar = new VisualActorMenuBar();
		menuBar.getCbStructure().addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	leftViewPanel.setVisible(menuBar.getCbStructure().isSelected());
	        	if (!menuBar.getCbStructure().isSelected())
	        		paContent.remove(leftViewPanel);
	        	else
	        		paContent.add(leftViewPanel, 0);
	        	
	        	paContent.revalidate();
	        }
		});
		menuBar.getCbBehaviour().addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent event) {
	        	rightViewPanel.setVisible(menuBar.getCbBehaviour().isSelected());
	        	if (!menuBar.getCbBehaviour().isSelected())
	        		paContent.remove(rightViewPanel);
	        	else
	        		paContent.add(rightViewPanel);
	        	
	        	paContent.revalidate();
	        }
		});
		setJMenuBar(menuBar);
	}

	public void setStatus(String status) {
		statusLabel.setText(status);
	}

	public void analyzeStructure(Map<UUID, InternalActorCell> actorCells, boolean showDefaultParent, boolean showRootSystem, boolean colorize) {
		((VisualActorStructureViewPanel)leftViewPanel).analyzeStructure(actorCells, showDefaultParent, showRootSystem, colorize);
		((VisualActorStructureViewPanel)leftViewPanel).updateStructure();
	}
	
	public String analyzeBehaviour(Map<UUID, InternalActorCell> actorCells, Map<UUID, Map<UUID, Long>> deliveryRoutes, boolean showRootSystem, boolean colorize) {
		String result = ((VisualActorBehaviourViewPanel)rightViewPanel).analyzeBehaviour(actorCells, deliveryRoutes, showRootSystem, colorize);
		((VisualActorBehaviourViewPanel)rightViewPanel).updateStructure();
		
		return result;
	}
}
