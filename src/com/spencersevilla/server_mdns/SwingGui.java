package com.spencersevilla.server_mdns;
import com.spencersevilla.mdns.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.HeadlessException;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.*;

import java.util.*;

import java.io.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class SwingGui {
	protected MultiDNS mdns;
	
	JFrame groups_window;
	JFrame services_window;
	JFrame joinGroupWindow;
	JFrame newServiceFrame;
	JFrame newGroupFrame;
	
	JFormattedTextField portField;
	JTextField serviceName;
	JTextField groupName;
	JTextField groupAddr;
	JTextField groupPort;
	JTextField parentName;
	JCheckBox createBox;
	
	JList groupsList;
	JList serviceList;
	JList otherGroupsList;
	
	
	DNSGroup selection;
	boolean is_adhoc = true;
	
	public SwingGui() throws Exception {
		mdns = new MultiDNS();
		
		try {
			createGroupsWindow();
			createServicesWindow();
			createJoinGroupWindow();
			createNewServiceWindow();
			createNewGroupWindow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public SwingGui(MultiDNS m) throws Exception {
		mdns = m;

		try {
			createGroupsWindow();
			createServicesWindow();
			createJoinGroupWindow();
			createNewServiceWindow();
			createNewGroupWindow();
		} catch (HeadlessException e) {
			System.out.println("WARNING: system running headless, could not start SwingGui");
		}

	}

	private void createGroupsWindow() throws HeadlessException {
		// Makes the menus appear in the proper spot on the Mac top bar
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		
		groups_window = new JFrame("DNS Groups");
		groups_window.setSize(200, 300);
		//groups_window.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		groups_window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				mdns.shutDown();
			}
		});
		
		groupsList = new JList();
		groupsList.setListData(mdns.groupList.toArray());
		groupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupsList.setVisibleRowCount(-1);
		
		JScrollPane listScroller = new JScrollPane(groupsList);		
		groups_window.getContentPane().add(listScroller);
		
		JButton new_group = new JButton("New Subgroup Of Selection");
		new_group.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newGroupPopup(true);
				}
		});
		
		JButton leave_group = new JButton("Leave Group");
		leave_group.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("leave group not implemented yet...");
			}
		});
		
		JPanel buttonPane = new JPanel(new GridLayout(0,1));
		buttonPane.add(new_group);
		buttonPane.add(leave_group);
		
		groups_window.getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
		groups_window.show();
	}
	
	private void createServicesWindow() throws Exception {
		services_window = new JFrame("Services Offered");
		services_window.setSize(200, 300);
		//shared_window.setDefaultCloseOperation(EXIT_ON_CLOSE);
				
		// Swing List		
		serviceList = new JList();
		serviceList.setListData(mdns.serviceList.toArray());
		serviceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		serviceList.setVisibleRowCount(-1);
		
		JScrollPane listScroller = new JScrollPane(serviceList);		
		services_window.getContentPane().add(listScroller);
				
		JButton new_group = new JButton("New Service");
		new_group.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newServicePopup();
				}
		});

		JButton delete_service = new JButton("Delete Service");
		delete_service.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteService();
				}
		});
						
		JPanel buttonPane = new JPanel(new GridLayout(0,1));
		buttonPane.add(new_group);
		buttonPane.add(delete_service);
		
		services_window.getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
		services_window.show();
	}
	
	private void createJoinGroupWindow() throws Exception {
		joinGroupWindow = new JFrame("Other Groups");
		joinGroupWindow.setSize(200, 300);
		//shared_window.setDefaultCloseOperation(EXIT_ON_CLOSE);
				
		// Swing List
		otherGroupsList = new JList();
		otherGroupsList.setListData(mdns.allGroups.toArray());
		otherGroupsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		otherGroupsList.setVisibleRowCount(-1);
		
		JScrollPane listScroller = new JScrollPane(otherGroupsList);		
		joinGroupWindow.getContentPane().add(listScroller);
				
		JButton new_group = new JButton("Create New Ad-Hoc Group");
		new_group.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newGroupPopup(false);
				}
		});

		JButton join_group = new JButton("Join Selected Group");
		join_group.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				joinGroup();
				}
		});
						
		JButton refresh_list = new JButton("Refresh List");
		refresh_list.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshList();
				}
		});

		JPanel buttonPane = new JPanel(new GridLayout(0,1));
		buttonPane.add(new_group);
		buttonPane.add(join_group);
		buttonPane.add(refresh_list);
		
		joinGroupWindow.getContentPane().add(buttonPane, BorderLayout.SOUTH);
		
		joinGroupWindow.show();
	}
	
	private void createNewServiceWindow() throws Exception {
		newServiceFrame = new JFrame("Create Service");
		newServiceFrame.setSize(200, 100);

		JLabel nameLabel = new JLabel("Service Name: ");
		serviceName = new JTextField();
		nameLabel.setLabelFor(serviceName);

		JLabel portLabel = new JLabel("Port: ");
		portField = new JFormattedTextField(NumberFormat.getIntegerInstance());
		portField.setColumns(10);
		portLabel.setLabelFor(portField);

		JButton create_service = new JButton("Create Service");
		create_service.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newServiceFrame.setVisible(false);
				createService();
				}
		});

		JButton cancel_button = new JButton("Cancel");
		cancel_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newServiceFrame.setVisible(false);
				}
		});

		JPanel fieldPane = new JPanel(new GridLayout(0,1));
		fieldPane.add(serviceName);
		fieldPane.add(portField);
		fieldPane.add(cancel_button);

		JPanel labelPane = new JPanel(new GridLayout(0,1));
		labelPane.add(nameLabel);
		labelPane.add(portLabel);
		labelPane.add(create_service);

		JPanel layout = new JPanel();
		layout.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		layout.add(labelPane, BorderLayout.CENTER);
		layout.add(fieldPane, BorderLayout.LINE_END);

		//Add contents to the window.
		newServiceFrame.add(layout);
		newServiceFrame.pack();
		
		// NOT shown by default!
	}
	
	private void createNewGroupWindow() throws Exception {
		newGroupFrame = new JFrame("Create Group");
		newGroupFrame.setSize(200, 100);

		JLabel nameLabel = new JLabel("Group Name: ");
		groupName = new JTextField();
		nameLabel.setLabelFor(groupName);

		JLabel addrLabel = new JLabel("Multicast Address: ");
		groupAddr = new JTextField("224.4.5.6");
		addrLabel.setLabelFor(groupAddr);

		JLabel portLabel = new JLabel("Port: ");
		groupPort = new JTextField("6363");
		portLabel.setLabelFor(groupPort);
		
		JButton create_group = new JButton("Create Group");
		create_group.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newGroupFrame.setVisible(false);
				createGroup();
				}
		});

		JButton cancel_button = new JButton("Cancel");
		cancel_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newGroupFrame.setVisible(false);
				}
		});

		JPanel fieldPane = new JPanel(new GridLayout(0,1));
		fieldPane.add(groupName);
		fieldPane.add(groupAddr);
		fieldPane.add(groupPort);
		fieldPane.add(create_group);

		JPanel labelPane = new JPanel(new GridLayout(0,1));
		labelPane.add(nameLabel);
		labelPane.add(addrLabel);
		labelPane.add(portLabel);
		labelPane.add(cancel_button);

		JPanel layout = new JPanel();
		layout.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		layout.add(labelPane, BorderLayout.CENTER);
		layout.add(fieldPane, BorderLayout.LINE_END);

		//Add contents to the window.
		newGroupFrame.add(layout);
		newGroupFrame.pack();
		
		// NOT shown by default!
	}
	
	private void newServicePopup() {
		serviceName.setText("");
		portField.setText("");
		
		newServiceFrame.show();
	}
	
	private void newGroupPopup(boolean is_subgroup) {
		if(is_subgroup) {
			selection = (DNSGroup) groupsList.getSelectedValue();
			if(selection == null) {
				return;
			}
			is_adhoc = false;
		} else {
			is_adhoc = true;
		}
		
		groupName.setText("testgroup");		
		groupAddr.setText("224.4.5.6");		
		groupPort.setText("6363");		
		newGroupFrame.show();
	}
	
	private void createService() {
		String sn = serviceName.getText();
		Number port = (Number)portField.getValue();
		if (sn.equals("") || port == null) {
			return;
		}
		
		mdns.createService(sn);
		serviceList.setListData(mdns.serviceList.toArray());
	}
	
	private void deleteService() {
		Service s = (Service) serviceList.getSelectedValue();
		
		if (s == null) { // no selection made
			return;
		}
		
		mdns.deleteService(s);
		serviceList.setListData(mdns.serviceList.toArray());
	}
	
	private void joinGroup() {
		DNSGroup group = (DNSGroup) otherGroupsList.getSelectedValue();
		if (group == null) {
			return;
		}
		
		mdns.joinGroup(group);
		groupsList.setListData(mdns.groupList.toArray());
	}
	
	private void createGroup() {
		ArrayList<String> args = new ArrayList<String>();

		String gname = groupName.getText();
		if (gname.equals("")) {
			return;
		}
		
		args.add(gname);

		String addr = groupAddr.getText();
		if (addr.equals("")) {
			addr = "224.4.5.6";
		}

		args.add(addr);
		
		String port = groupPort.getText();
		if (port.equals("")) {
			port = "6363";
		}
		
		args.add(port);

		if (is_adhoc) {
			DNSGroup g = mdns.createGroup(0, args);
			if (g == null) {
				return;
			}
			mdns.joinGroup(g);
			groupsList.setListData(mdns.groupList.toArray());
			return;
		}
		
		if (selection == null) {
			System.out.println("cannot create subgroup of (null)!");
			return;
		}
		
		DNSGroup g = mdns.createSubGroup(selection, 0, args);
		if (g == null) {
			return;
		}
		mdns.joinGroup(g);
		groupsList.setListData(mdns.groupList.toArray());
	}
	
	private void refreshList() {
		mdns.findOtherGroups();
		otherGroupsList.setListData(mdns.allGroups.toArray());
	}
}