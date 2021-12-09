/* 
 * This is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * 
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain. This software is experimental.
 * NIST assumes no responsibility whatsoever for its use by other
 * parties, and makes no guarantees, expressed or implied, about its
 * quality, reliability, or any other characteristic. We would appreciate
 * acknowledgment if the software is used. This software can be
 * redistributed and/or modified freely provided that any derivative works
 * bear some notice that they are derived from it, and any modified
 * versions bear some notice that they have been modified.
 * 
 */
package com.github.wshackle.fanuccrclservermain;

import com.github.wshackle.fanuc.robotserver.FREExecuteConstants;
import com.github.wshackle.fanuc.robotserver.FREStepTypeConstants;
import com.github.wshackle.fanuc.robotserver.ITPProgram;
import com.github.wshackle.fanuc.robotserver.IVar;
import crcl.ui.IconImages;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class FanucCRCLServerJFrame extends javax.swing.JFrame implements FanucCRCLServerDisplayInterface {

	/**
	 * Creates new form FanucCRCLServerJFrame
	 */
	@SuppressWarnings({ "nullness", "initialization" })
	public FanucCRCLServerJFrame() {
		initComponents();
//        timer = new Timer(500, e -> updateDisplay());
//        timer.start();
//        this.jTableCartesianLimits.getModel().addTableModelListener(e -> updateCartLimits(false));
//        this.jTableJointLimits.getModel().addTableModelListener(e -> updateJointLimits(false));
		setIconImage(SERVER_IMAGE);
		readPropertiesFile();
		if (jCheckBoxMenuItemStartClient.isSelected()) {
			launchClient();
		}
		jCheckBoxMenuItemDebug.setSelected(FanucCRCLMain.isDebug());
		if (jCheckBoxMenuItemStartPressureServer.isSelected()) {
			throw new UnsupportedOperationException("pressure sensor server not implemented");
//            launchPressureSensorServer();
//            ServerSensorJFrame serverSensorJFrame = fanucCRCLServerJPanel1.getSensorJFrame();
//            if (null != serverSensorJFrame) {
//                serverSensorJFrame.setVisible(jCheckBoxMenuItemShowPressureOutput.isSelected());
//            }
		}

	}

	public @Nullable IVar getOverrideVar() {
		return fanucCRCLServerJPanel1.getOverrideVar();
	}

	public void setMorSafetyStatVar(IVar morSafetyStatVar) {
		fanucCRCLServerJPanel1.setMorSafetyStatVar(morSafetyStatVar);
	}

	public @Nullable IVar getMorSafetyStatVar() {
		return fanucCRCLServerJPanel1.getMorSafetyStatVar();
	}

	public void setMoveGroup1ServoReadyVar(IVar var) {
		fanucCRCLServerJPanel1.setMoveGroup1ServoReadyVar(var);
	}

	public @Nullable IVar getMoveGroup1ServoReadyVar() {
		return fanucCRCLServerJPanel1.getMoveGroup1ServoReadyVar();
	}

	/**
	 * Set the value of overrideVar
	 *
	 * @param overrideVar new value of overrideVar
	 */
	public void setOverrideVar(IVar overrideVar) {
		fanucCRCLServerJPanel1.setOverrideVar(overrideVar);
	}

	private void launchClient() {
		fanucCRCLServerJPanel1.launchClient();
	}

	private void launchPressureSensorServer() {
		fanucCRCLServerJPanel1.launchPressureSensorServer();
	}

	@Override
	final public void setIconImage(Image image) {
		if (null != image) {
			super.setIconImage(image);
		}
	}

	@SuppressWarnings("nullness")
	private static Image createImage(Dimension d, Color bgColor, Color textColor, Image baseImage) {
		BufferedImage bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = bi.createGraphics();
		g2d.setBackground(bgColor);
		g2d.setColor(textColor);
		g2d.clearRect(0, 0, d.width, d.height);
		g2d.setFont(new Font(g2d.getFont().getName(), g2d.getFont().getStyle(), 24));
		g2d.drawImage(baseImage, 0, 0, null);
		bi.flush();
		return bi;
	}

//    public static Image getRobotImage() {
//        final Image img;
//        try {
//            final URL systemResourceRobotImageUrl = Objects.requireNonNull(ClassLoader.getSystemResource("robot.png"),"ClassLoader.getSystemResource(\"robot.png\")");
//            img = ImageIO.read(systemResourceRobotImageUrl);
//        } catch (IOException ex) {
//            Logger.getLogger(FanucCRCLServerJFrame.class.getName()).log(Level.SEVERE, "", ex);
//            throw new RuntimeException(ex);
//        }
//        return img;
//    }

	private static final Dimension ICON_SIZE = new Dimension(32, 32);
	private static final Image BASE_IMAGE = IconImages.BASE_IMAGE;
	public static final Image SERVER_IMAGE = createImage(ICON_SIZE, Color.MAGENTA, Color.BLACK, BASE_IMAGE);

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		buttonGroupConnectType = new javax.swing.ButtonGroup();
		fanucCRCLServerJPanel1 = new com.github.wshackle.fanuccrclservermain.FanucCRCLServerJPanel();
		jMenuBar1 = new javax.swing.JMenuBar();
		jMenu1 = new javax.swing.JMenu();
		jMenuItemResetAlarms = new javax.swing.JMenuItem();
		jMenuItemReconnectRobot = new javax.swing.JMenuItem();
		jMenu2 = new javax.swing.JMenu();
		jMenuRunProgram = new javax.swing.JMenu();
		jMenuConvertProgram = new javax.swing.JMenu();
		jMenuTools = new javax.swing.JMenu();
		jMenuItemLaunchClient = new javax.swing.JMenuItem();
		jMenuItem1 = new javax.swing.JMenuItem();
		jCheckBoxMenuItemShowPressureOutput = new javax.swing.JCheckBoxMenuItem();
		jMenuItemLaunchWeb = new javax.swing.JMenuItem();
		jMenu3 = new javax.swing.JMenu();
		jCheckBoxMenuItemStartClient = new javax.swing.JCheckBoxMenuItem();
		jCheckBoxMenuItemStartPressureServer = new javax.swing.JCheckBoxMenuItem();
		jCheckBoxMenuItemDebug = new javax.swing.JCheckBoxMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Fanuc CRCL Server");
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosed(java.awt.event.WindowEvent evt) {
				formWindowClosed(evt);
			}

			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}
		});

		jMenu1.setText("File");

		jMenuItemResetAlarms.setText("Reset Alarms");
		jMenu1.add(jMenuItemResetAlarms);

		jMenuItemReconnectRobot.setText("Reconnect Robot");
		jMenu1.add(jMenuItemReconnectRobot);

		jMenuBar1.add(jMenu1);

		jMenu2.setText("Edit");
		jMenuBar1.add(jMenu2);

		jMenuRunProgram.setText("Run Program");
		jMenuBar1.add(jMenuRunProgram);

		jMenuConvertProgram.setText("Convert Program");
		jMenuBar1.add(jMenuConvertProgram);

		jMenuTools.setText("Tools");

		jMenuItemLaunchClient.setText("Launch Client");
		jMenuItemLaunchClient.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemLaunchClientActionPerformed(evt);
			}
		});
		jMenuTools.add(jMenuItemLaunchClient);

		jMenuItem1.setText("Launch Finger Pressure Sensor Server");
		jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItem1ActionPerformed(evt);
			}
		});
		jMenuTools.add(jMenuItem1);

		jCheckBoxMenuItemShowPressureOutput.setSelected(true);
		jCheckBoxMenuItemShowPressureOutput.setText("Show Finger Pressure Sensor Output");
		jCheckBoxMenuItemShowPressureOutput.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jCheckBoxMenuItemShowPressureOutputActionPerformed(evt);
			}
		});
		jMenuTools.add(jCheckBoxMenuItemShowPressureOutput);

		jMenuItemLaunchWeb.setText("Launch Web Server/Application");
		jMenuItemLaunchWeb.setEnabled(false);
		jMenuItemLaunchWeb.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItemLaunchWebActionPerformed(evt);
			}
		});
		jMenuTools.add(jMenuItemLaunchWeb);

		jMenuBar1.add(jMenuTools);

		jMenu3.setText("Options");

		jCheckBoxMenuItemStartClient.setText("Start Client on Startup");
		jCheckBoxMenuItemStartClient.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jCheckBoxMenuItemStartClientActionPerformed(evt);
			}
		});
		jMenu3.add(jCheckBoxMenuItemStartClient);

		jCheckBoxMenuItemStartPressureServer.setText("Start Pressure Sensor Server on Startup");
		jMenu3.add(jCheckBoxMenuItemStartPressureServer);

		jCheckBoxMenuItemDebug.setText("Debug");
		jCheckBoxMenuItemDebug.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jCheckBoxMenuItemDebugActionPerformed(evt);
			}
		});
		jMenu3.add(jCheckBoxMenuItemDebug);

		jMenuBar1.add(jMenu3);

		setJMenuBar(jMenuBar1);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(fanucCRCLServerJPanel1,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addGap(17, 17, 17)));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(fanucCRCLServerJPanel1,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void jMenuItemLaunchClientActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemLaunchClientActionPerformed
		launchClient();
	}// GEN-LAST:event_jMenuItemLaunchClientActionPerformed

	private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItem1ActionPerformed
		launchPressureSensorServer();
	}// GEN-LAST:event_jMenuItem1ActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing
		shutDown();
	}// GEN-LAST:event_formWindowClosing

	private void formWindowClosed(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosed
		shutDown();
	}// GEN-LAST:event_formWindowClosed

	private void jCheckBoxMenuItemShowPressureOutputActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jCheckBoxMenuItemShowPressureOutputActionPerformed
		throw new UnsupportedOperationException("pressure sensor server not implemented");
//        ServerSensorJFrame serverSensorJFrame = fanucCRCLServerJPanel1.getSensorJFrame();
//        if (null != serverSensorJFrame) {
//            serverSensorJFrame.setVisible(jCheckBoxMenuItemShowPressureOutput.isSelected());
//        }
//        saveProperties();
	}// GEN-LAST:event_jCheckBoxMenuItemShowPressureOutputActionPerformed

	private void jCheckBoxMenuItemStartClientActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jCheckBoxMenuItemStartClientActionPerformed
		saveProperties();
	}// GEN-LAST:event_jCheckBoxMenuItemStartClientActionPerformed

	private void jMenuItemLaunchWebActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jMenuItemLaunchWebActionPerformed
//        launchWebServer();
	}// GEN-LAST:event_jMenuItemLaunchWebActionPerformed

	private void jCheckBoxMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jCheckBoxMenuItemDebugActionPerformed
		FanucCRCLMain.setDebug(jCheckBoxMenuItemDebug.isSelected());
	}// GEN-LAST:event_jCheckBoxMenuItemDebugActionPerformed

	private void shutDown() {
		fanucCRCLServerJPanel1.shutDown();
	}

//    private void launchWebServer() {
//        fanucCRCLServerJPanel1.launchWebServer();
//    }

	public JTextField getjTextFieldHostName() {
		return fanucCRCLServerJPanel1.getjTextFieldHostName();
	}

	public void saveProperties() {
		fanucCRCLServerJPanel1.saveProperties();
	}

	public void setPrograms(List<ITPProgram> _programs) {
		fanucCRCLServerJPanel1.setPrograms(_programs);
		List<ITPProgram> programs = fanucCRCLServerJPanel1.getPrograms();
		this.jMenuRunProgram.removeAll();
		this.jMenuConvertProgram.removeAll();
		if (null != programs) {
			synchronized (programs) {
				programs.sort(Comparator.comparing(ITPProgram::name));
				for (ITPProgram p : programs) {
					JMenuItem jmi = new JMenuItem(p.name());
					jmi.addActionListener(e -> {
						runReconnectAction(e);
						if (JOptionPane.showConfirmDialog(FanucCRCLServerJFrame.this,
								"Run " + p.name()) == JOptionPane.YES_OPTION) {
							p.run(FREStepTypeConstants.frStepNone, 1, FREExecuteConstants.frExecuteFwd);
							runReconnectAction(e);
						}
					});
					this.jMenuRunProgram.add(jmi);
					jmi = new JMenuItem(p.name());
					jmi.addActionListener(e -> {
						ConvertProgram(p);
					});
					this.jMenuConvertProgram.add(jmi);
				}
			}
		}
	}

	public void ConvertProgram(ITPProgram program) {
		fanucCRCLServerJPanel1.ConvertProgram(program);
	}

	public void runReconnectAction(ActionEvent e) {
		for (ActionListener al : jMenuItemReconnectRobot.getActionListeners()) {
			al.actionPerformed(e);
		}
	}

	public void setMain(FanucCRCLMain _main) {
		fanucCRCLServerJPanel1.setMain(_main);
	}

	public JSlider getjSliderOverride() {
		return fanucCRCLServerJPanel1.getjSliderOverride();
	}

	public JSlider getjSliderMaxOverride() {
		return fanucCRCLServerJPanel1.getjSliderMaxOverride();
	}

	public JRadioButton getjRadioButtonUseDirectIP() {
		return fanucCRCLServerJPanel1.getjRadioButtonUseDirectIP();
	}

	public JRadioButton getjRadioButtonUseRobotNeighborhood() {
		return fanucCRCLServerJPanel1.getjRadioButtonUseRobotNeighborhood();
	}

	public JTextField getjTextFieldRobotNeighborhoodPath() {
		return fanucCRCLServerJPanel1.getjTextFieldRobotNeighborhoodPath();
	}

	public JTextField getjTextFieldLimitSafetyBumper() {
		return fanucCRCLServerJPanel1.getjTextFieldLimitSafetyBumper();
	}

	public void setConnected(boolean _connected) {
		fanucCRCLServerJPanel1.setConnected(_connected);
	}

	public JTextArea getjTextAreaErrors() {
		return fanucCRCLServerJPanel1.getjTextAreaErrors();
	}

	public JCheckBox getjCheckBoxLogAllCommands() {
		return fanucCRCLServerJPanel1.getjCheckBoxLogAllCommands();
	}

	public void updatePerformanceString(String s) {
		fanucCRCLServerJPanel1.updatePerformanceString(s);
	}

	public void setPreferRobotNeighborhood(boolean b) {
		fanucCRCLServerJPanel1.setPreferRobotNeighborhood(b);
	}

	public void updateCartesianLimits(float xMax, float xMin, float yMax, float yMin, float zMax, float zMin) {
		fanucCRCLServerJPanel1.updateCartesianLimits(xMax, xMin, yMax, yMin, zMax, zMin);
	}

	public void updateJointLimits(float lowerJointLimits[], float upperJointLimits[]) {
		fanucCRCLServerJPanel1.updateJointLimits(lowerJointLimits, upperJointLimits);
	}

	private void readPropertiesFile() {
//        try {
//            if (PROPERTIES_FILE.exists()) {
//                props.load(new FileReader(PROPERTIES_FILE));
//            }
//            fingerSensorServerCmd = getProperty("fingerSensorServerCmd", DEFAULT_FINGER_SENSOR_SERVER_COMMAND);
//            fingerSensorServerDirectory = getProperty("fingerSensorServerDirectory", DEFAULT_FINGER_SENSOR_SERVER_DIRECTORY);
//            jCheckBoxMenuItemStartClient.setSelected(getBooleanProperty("autoStartClient", false));
//            jCheckBoxMenuItemStartPressureServer.setSelected(getBooleanProperty("autoStartPressureSensorServer", false));
//            jCheckBoxMenuItemShowPressureOutput.setSelected(getBooleanProperty("showPressureOutput", false));
//        } catch (IOException ex) {
//            Logger.getLogger(FanucCRCLServerJFrame.class.getName()).log(Level.SEVERE, "", ex);
//        }
	}

	public JMenuItem getjMenuItemReconnectRobot() {
		return jMenuItemReconnectRobot;
	}

	public JMenuItem getjMenuItemResetAlarms() {
		return jMenuItemResetAlarms;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
		// (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
		 * look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger(FanucCRCLServerJFrame.class.getName())
					.log(java.util.logging.Level.SEVERE, "", ex);
		}
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new FanucCRCLServerJFrame().setVisible(true);
			}
		});
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.ButtonGroup buttonGroupConnectType;
	private com.github.wshackle.fanuccrclservermain.FanucCRCLServerJPanel fanucCRCLServerJPanel1;
	private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebug;
	private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowPressureOutput;
	private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartClient;
	private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemStartPressureServer;
	private javax.swing.JMenu jMenu1;
	private javax.swing.JMenu jMenu2;
	private javax.swing.JMenu jMenu3;
	private javax.swing.JMenuBar jMenuBar1;
	private javax.swing.JMenu jMenuConvertProgram;
	private javax.swing.JMenuItem jMenuItem1;
	private javax.swing.JMenuItem jMenuItemLaunchClient;
	private javax.swing.JMenuItem jMenuItemLaunchWeb;
	private javax.swing.JMenuItem jMenuItemReconnectRobot;
	private javax.swing.JMenuItem jMenuItemResetAlarms;
	private javax.swing.JMenu jMenuRunProgram;
	private javax.swing.JMenu jMenuTools;
	// End of variables declaration//GEN-END:variables
}
