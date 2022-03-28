/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crcl.ui.forcetorquesensorsimulator;

import static crcl.utils.CRCLUtils.requireNonNull;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import crcl.base.CRCLStatusType;
import crcl.base.ConfigureStatusReportType;
import crcl.base.ForceTorqueSensorStatusType;
import crcl.base.GetStatusType;
import crcl.base.GripperStatusType;
import crcl.base.PointType;
import crcl.base.PoseStatusType;
import crcl.base.PoseType;
import crcl.base.SensorStatusesType;
import crcl.base.SettingsStatusType;
import crcl.copier.CRCLCopier;
import crcl.ui.AutomaticPropertyFileUtils;
import crcl.ui.PoseDisplay;
import crcl.ui.PoseDisplayMode;
import crcl.ui.client.CrclSwingClientJPanel;
import crcl.ui.client.CurrentPoseListener;
import crcl.ui.client.CurrentPoseListenerUpdateInfo;
import crcl.utils.CRCLException;
import crcl.utils.CRCLSocket;
import crcl.utils.CRCLUtils;
import crcl.utils.ThreadLockedHolder;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import crcl.utils.outer.interfaces.PropertyOwner;
import crcl.utils.server.CRCLServerClientState;
import crcl.utils.server.CRCLServerSocket;
import crcl.utils.server.CRCLServerSocketEvent;
import crcl.utils.server.CRCLServerSocketEventListener;
import crcl.utils.server.CRCLServerSocketStateGenerator;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
@SuppressWarnings("serial")
public class ForceTorqueSimJPanel extends javax.swing.JPanel implements PropertyOwner {

    /**
     * Creates new form ForceTorqueSimJPanel
     */
    @SuppressWarnings({"nullness", "initialization", "rawtypes"})
    public ForceTorqueSimJPanel() {
        statusOut = new ThreadLockedHolder<>("ForceTorqueSimJPanel.statusOut", new CRCLStatusType(), false);
        final CRCLStatusType statOut = this.statusOut.get();
        statOut.setSensorStatuses(new SensorStatusesType());
        sensorStatus = new ForceTorqueSensorStatusType();
        statOut.getSensorStatuses().getForceTorqueSensorStatus().add(sensorStatus);
        if (!CRCLUtils.isGraphicsEnvironmentHeadless()) {
            initComponents();

        } else {
            jTablePose = new javax.swing.JTable();
            jTablePose.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{
                        {"X", null},
                        {"Y", null},
                        {"Z", null},
                        {"XI", null},
                        {"XJ", null},
                        {"XK", null},
                        {"ZI", null},
                        {"ZJ", null},
                        {"Zk", null}
                    },
                    new String[]{
                        "Pose Axis", "Position"
                    }
            ) {
                Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Double.class
                };
                boolean[] canEdit = new boolean[]{
                    false, false
                };

                @SuppressWarnings("unchecked")
		public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }
            });

            jTablePoseForceOut = new javax.swing.JTable();
            jTablePoseForceOut.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{
                        {"X", null},
                        {"Y", null},
                        {"Z", null},
                        {"XI", null},
                        {"XJ", null},
                        {"XK", null},
                        {"ZI", null},
                        {"ZJ", null},
                        {"Zk", null}
                    },
                    new String[]{
                        "Pose Axis", "Position"
                    }
            ) {
                Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Double.class
                };
                boolean[] canEdit = new boolean[]{
                    false, false
                };

                @SuppressWarnings("unchecked")
		public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }
            });
            jTableObjects = new javax.swing.JTable();
            jTableObjects.setModel(new javax.swing.table.DefaultTableModel(
                    new Object[][]{},
                    new String[]{
                        "Name", "Count", "Width", "Length", "Height", "X", "Y", "Z", "Rotation", "Scale"
                    }
            ) {
                Class[] types = new Class[]{
                    java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
                };

                @SuppressWarnings("unchecked")
		public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }
            });
            inOutJPanel1 = new crcl.ui.forcetorquesensorsimulator.InOutJPanel();
        }
        PoseDisplay.updateDisplayMode(jTablePose, PoseDisplayMode.XYZ_RPY, false);
        PoseDisplay.updateDisplayMode(jTablePoseForceOut, PoseDisplayMode.XYZ_RPY, false);
        internalUpdateSensorStatus();
        final DefaultTableModel model = (DefaultTableModel) jTableObjects.getModel();
        model.addTableModelListener((TableModelEvent e) -> {
            inOutJPanel1.setStacks(modelToList(model));
        });
        getPoseServiceThreadFactory = createPoseServiceThreadFactory(
                getPoseCRCLPort(),
                (Thread thread) -> {
                    this.getPoseServiceThread = thread;
                });
        getPoseService = Executors.newSingleThreadExecutor(getPoseServiceThreadFactory);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "nullness", "CanBeFinal"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuFileSaveProperties = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItemFileLoadProperties = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelCommunications = new javax.swing.JPanel();
        jPanelCrclSensorServerOut = new javax.swing.JPanel();
        jCheckBoxStartSensorOutServer = new javax.swing.JCheckBox();
        jTextFieldCRCLSensorOutPort = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanelCRCLPositionIn = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldPoseCRCLHost = new javax.swing.JTextField();
        jCheckBoxEnablePoseInConnection = new javax.swing.JCheckBox();
        jTextFieldPoseCRCLPort = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jTablePose = new javax.swing.JTable();
        jPanelForceOut = new javax.swing.JPanel();
        jTablePoseForceOut = new javax.swing.JTable();
        jPanelOffsets = new javax.swing.JPanel();
        valueJPanelFx = new crcl.ui.forcetorquesensorsimulator.ValueJPanel();
        valueJPanelFy = new crcl.ui.forcetorquesensorsimulator.ValueJPanel();
        valueJPanelFz = new crcl.ui.forcetorquesensorsimulator.ValueJPanel();
        valueJPanelTx = new crcl.ui.forcetorquesensorsimulator.ValueJPanel();
        valueJPanelTy = new crcl.ui.forcetorquesensorsimulator.ValueJPanel();
        valueJPanelTz = new crcl.ui.forcetorquesensorsimulator.ValueJPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jTextFieldObjectsFile = new javax.swing.JTextField();
        jButtonDeleteObject = new javax.swing.JButton();
        jButtonOpenObjectsFile = new javax.swing.JButton();
        jButtonSaveObjectsFile = new javax.swing.JButton();
        jButtonAddObject = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableObjects = new javax.swing.JTable();
        jPanel5 = new javax.swing.JPanel();
        inOutJPanel1 = new crcl.ui.forcetorquesensorsimulator.InOutJPanel();
        jSlider1 = new javax.swing.JSlider();

        jMenuFileSaveProperties.setText("File");

        jMenuItem1.setText("Save Properties ...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenuFileSaveProperties.add(jMenuItem1);

        jMenuItemFileLoadProperties.setText("Load Properties ...");
        jMenuItemFileLoadProperties.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFileLoadPropertiesActionPerformed(evt);
            }
        });
        jMenuFileSaveProperties.add(jMenuItemFileLoadProperties);

        jMenuBar1.add(jMenuFileSaveProperties);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        jPanelCrclSensorServerOut.setBorder(javax.swing.BorderFactory.createTitledBorder("CRCL Sensor Out Server"));

        jCheckBoxStartSensorOutServer.setText("Start");
        jCheckBoxStartSensorOutServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxStartSensorOutServerActionPerformed(evt);
            }
        });

        jTextFieldCRCLSensorOutPort.setText("8888");

        jLabel1.setText("Port: ");

        javax.swing.GroupLayout jPanelCrclSensorServerOutLayout = new javax.swing.GroupLayout(jPanelCrclSensorServerOut);
        jPanelCrclSensorServerOut.setLayout(jPanelCrclSensorServerOutLayout);
        jPanelCrclSensorServerOutLayout.setHorizontalGroup(
            jPanelCrclSensorServerOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCrclSensorServerOutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCrclSensorServerOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCrclSensorServerOutLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCRCLSensorOutPort))
                    .addGroup(jPanelCrclSensorServerOutLayout.createSequentialGroup()
                        .addComponent(jCheckBoxStartSensorOutServer)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelCrclSensorServerOutLayout.setVerticalGroup(
            jPanelCrclSensorServerOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCrclSensorServerOutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCrclSensorServerOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldCRCLSensorOutPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxStartSensorOutServer)
                .addContainerGap())
        );

        jPanelCRCLPositionIn.setBorder(javax.swing.BorderFactory.createTitledBorder("CRCL Pose In Connection"));

        jLabel3.setText("Host: ");

        jLabel2.setText("Port: ");

        jTextFieldPoseCRCLHost.setText("localhost");

        jCheckBoxEnablePoseInConnection.setText("Enable");
        jCheckBoxEnablePoseInConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxEnablePoseInConnectionActionPerformed(evt);
            }
        });

        jTextFieldPoseCRCLPort.setText("64444");

        jButton1.setText("Update Pose");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelCRCLPositionInLayout = new javax.swing.GroupLayout(jPanelCRCLPositionIn);
        jPanelCRCLPositionIn.setLayout(jPanelCRCLPositionInLayout);
        jPanelCRCLPositionInLayout.setHorizontalGroup(
            jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCRCLPositionInLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanelCRCLPositionInLayout.createSequentialGroup()
                            .addComponent(jLabel3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextFieldPoseCRCLHost))
                        .addGroup(jPanelCRCLPositionInLayout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jTextFieldPoseCRCLPort, javax.swing.GroupLayout.PREFERRED_SIZE, 601, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanelCRCLPositionInLayout.createSequentialGroup()
                        .addComponent(jCheckBoxEnablePoseInConnection)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)))
                .addContainerGap())
        );
        jPanelCRCLPositionInLayout.setVerticalGroup(
            jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCRCLPositionInLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldPoseCRCLPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldPoseCRCLHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelCRCLPositionInLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxEnablePoseInConnection)
                    .addComponent(jButton1))
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Pose In"));

        jTablePose.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"X", null},
                {"Y", null},
                {"Z", null},
                {"XI", null},
                {"XJ", null},
                {"XK", null},
                {"ZI", null},
                {"ZJ", null},
                {"Zk", null}
            },
            new String [] {
                "Pose Axis", "Position"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTablePose, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTablePose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanelForceOut.setBorder(javax.swing.BorderFactory.createTitledBorder("Force  Torque Out"));

        jTablePoseForceOut.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"X", null},
                {"Y", null},
                {"Z", null},
                {"XI", null},
                {"XJ", null},
                {"XK", null},
                {"ZI", null},
                {"ZJ", null},
                {"Zk", null}
            },
            new String [] {
                "Pose Axis", "Position"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        javax.swing.GroupLayout jPanelForceOutLayout = new javax.swing.GroupLayout(jPanelForceOut);
        jPanelForceOut.setLayout(jPanelForceOutLayout);
        jPanelForceOutLayout.setHorizontalGroup(
            jPanelForceOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelForceOutLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTablePoseForceOut, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelForceOutLayout.setVerticalGroup(
            jPanelForceOutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelForceOutLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTablePoseForceOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelCommunicationsLayout = new javax.swing.GroupLayout(jPanelCommunications);
        jPanelCommunications.setLayout(jPanelCommunicationsLayout);
        jPanelCommunicationsLayout.setHorizontalGroup(
            jPanelCommunicationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCommunicationsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelCommunicationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCrclSensorServerOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelCommunicationsLayout.createSequentialGroup()
                        .addGroup(jPanelCommunicationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelCRCLPositionIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanelCommunicationsLayout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanelForceOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelCommunicationsLayout.setVerticalGroup(
            jPanelCommunicationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCommunicationsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelCrclSensorServerOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanelCRCLPositionIn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanelCommunicationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelForceOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(184, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Communications", jPanelCommunications);

        valueJPanelFx.setName("Fx"); // NOI18N

        valueJPanelFy.setName("Fy"); // NOI18N

        valueJPanelFz.setName("Fz"); // NOI18N

        valueJPanelTx.setName("Tx"); // NOI18N

        valueJPanelTy.setName("Ty"); // NOI18N

        valueJPanelTz.setName("Tz"); // NOI18N

        javax.swing.GroupLayout jPanelOffsetsLayout = new javax.swing.GroupLayout(jPanelOffsets);
        jPanelOffsets.setLayout(jPanelOffsetsLayout);
        jPanelOffsetsLayout.setHorizontalGroup(
            jPanelOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOffsetsLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanelOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(valueJPanelFx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueJPanelTz, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueJPanelFy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueJPanelTx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueJPanelFz, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(valueJPanelTy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelOffsetsLayout.setVerticalGroup(
            jPanelOffsetsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelOffsetsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(valueJPanelFx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueJPanelFy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueJPanelFz, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueJPanelTx, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueJPanelTy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(valueJPanelTz, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(245, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Offset: ", jPanelOffsets);

        jButtonDeleteObject.setText("Delete");
        jButtonDeleteObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteObjectActionPerformed(evt);
            }
        });

        jButtonOpenObjectsFile.setText("Open");
        jButtonOpenObjectsFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenObjectsFileActionPerformed(evt);
            }
        });

        jButtonSaveObjectsFile.setText("Save");
        jButtonSaveObjectsFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveObjectsFileActionPerformed(evt);
            }
        });

        jButtonAddObject.setText("Add");
        jButtonAddObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddObjectActionPerformed(evt);
            }
        });

        jTableObjects.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Count", "Width", "Length", "Height", "X", "Y", "Z", "Rotation", "Scale"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTableObjects);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jTextFieldObjectsFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonOpenObjectsFile)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonSaveObjectsFile))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jButtonAddObject)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonDeleteObject)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldObjectsFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonSaveObjectsFile)
                    .addComponent(jButtonOpenObjectsFile))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAddObject)
                    .addComponent(jButtonDeleteObject))
                .addContainerGap())
        );

        javax.swing.GroupLayout inOutJPanel1Layout = new javax.swing.GroupLayout(inOutJPanel1);
        inOutJPanel1.setLayout(inOutJPanel1Layout);
        inOutJPanel1Layout.setHorizontalGroup(
            inOutJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        inOutJPanel1Layout.setVerticalGroup(
            inOutJPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 284, Short.MAX_VALUE)
        );

        jSlider1.setMaximum(89);
        jSlider1.setPaintTicks(true);
        jSlider1.setValue(30);
        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider1StateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(inOutJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(15, 15, 15))
                    .addComponent(jSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(23, 23, 23)
                .addComponent(inOutJPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(384, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                    .addGap(259, 259, 259)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        jTabbedPane1.addTab("Stacks", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jCheckBoxStartSensorOutServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxStartSensorOutServerActionPerformed
        boolean doStart = jCheckBoxStartSensorOutServer.isSelected();
        try {
            if (doStart) {
                startServer();
            } else if (null != crclServerSocket) {
                crclServerSocket.close();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "connect=" + doStart, ex);
        }
    }//GEN-LAST:event_jCheckBoxStartSensorOutServerActionPerformed

    private volatile @Nullable
    CRCLSocket poseInConnection = null;
    private volatile @Nullable
    CRCLStatusType poseStatus = null;
    private volatile javax.swing.@Nullable Timer timer = null;

    @UIEffect
    private void jCheckBoxEnablePoseInConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxEnablePoseInConnectionActionPerformed
        if (jCheckBoxEnablePoseInConnection.isSelected()) {
            startPoseInConnection();
        } else {
            stopPoseInConnection();
        }
    }//GEN-LAST:event_jCheckBoxEnablePoseInConnectionActionPerformed

    private void stopPoseInConnection() {
        try {
            if (null != poseInConnection) {
                poseInConnection.close();
                poseInConnection = null;
            }
            if (null != timer) {
                timer.stop();
                timer = null;
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ForceTorqueSimJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    private void startPoseInConnection() throws NumberFormatException {
        try {
            final int port = Integer.parseInt(jTextFieldPoseCRCLPort.getText().trim());
            final String host = jTextFieldPoseCRCLHost.getText();
            CRCLSocket newConnection
                    = new CRCLSocket(host, port);
            ConfigureStatusReportType confStatus = new ConfigureStatusReportType();
            confStatus.setReportPoseStatus(true);
            confStatus.setReportGripperStatus(true);
            confStatus.setReportSettingsStatus(true);
            newConnection.writeCommand(confStatus);
            poseInConnection = newConnection;
            timer = new javax.swing.Timer(50, e -> {
                getAndUpdatePose();
            });
            timer.start();
        } catch (CRCLException | IOException ex) {
            java.util.logging.Logger.getLogger(ForceTorqueSimJPanel.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    @SuppressWarnings("nullness")
    private XFutureVoid getAndUpdatePose() {
        XFuture<@Nullable CurrentPoseListenerUpdateInfo> poseFuture
                = getPoseFuture();
        return poseFuture
                .thenAccept((CurrentPoseListenerUpdateInfo updateInfo) -> {
                    updateDisplayAndStatusWithPose(updateInfo);
                });
    }

    private void updateDisplayAndStatusWithPose(@Nullable CurrentPoseListenerUpdateInfo poseUpdateInfo) {
        if (null == poseUpdateInfo) {
            return;
        }
        final CRCLStatusType poseUpdateInfoStat = poseUpdateInfo.getStat();
        if (null == poseUpdateInfoStat) {
            return;
        }
        final PoseStatusType poseStatus1 = poseUpdateInfoStat.getPoseStatus();
        if (null == poseStatus1) {
            return;
        }
        PoseType pose = poseStatus1.getPose();
        if (null != pose) {
            javax.swing.SwingUtilities.invokeLater(() -> PoseDisplay.updatePoseTable(pose, jTablePose, PoseDisplayMode.XYZ_RPY));
            updateSensorStatusWithPose(pose);
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        getPoseListenerUpdateInfo();
    }//GEN-LAST:event_jButton1ActionPerformed

    private double addx = 0;

    private void jButtonAddObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddObjectActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTableObjects.getModel();
        addx += 100.0;
        model.addRow(new Object[]{"object" + model.getRowCount(), 1, 100.0, 100.0, 100.0, addx, 0.0, 0.0, 0.0, -1.00});
        this.inOutJPanel1.setStacks(modelToList(model));
    }//GEN-LAST:event_jButtonAddObjectActionPerformed

    private void jButtonDeleteObjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteObjectActionPerformed
        final int selectedRow = jTableObjects.getSelectedRow();
        if (selectedRow < 0 || selectedRow > jTableObjects.getRowCount()) {
            return;
        }
        DefaultTableModel model = (DefaultTableModel) jTableObjects.getModel();
        model.removeRow(selectedRow);
        this.inOutJPanel1.setStacks(modelToList(model));
    }//GEN-LAST:event_jButtonDeleteObjectActionPerformed

    private void jButtonOpenObjectsFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenObjectsFileActionPerformed
        JFileChooser chooser = new JFileChooser();
        String text = jTextFieldObjectsFile.getText();
        if (text != null && text.length() > 0) {
            File f = new File(text);
            File parent = f.getParentFile();
            if (null != parent) {
                chooser.setCurrentDirectory(parent);
            }
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = chooser.getSelectedFile();
            if (null != selectedFile) {
                loadObjectsFile(selectedFile);
            }
        }
    }//GEN-LAST:event_jButtonOpenObjectsFileActionPerformed

    private void jButtonSaveObjectsFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveObjectsFileActionPerformed
        JFileChooser chooser = new JFileChooser();
        String text = jTextFieldObjectsFile.getText();
        if (text != null && text.length() > 0) {
            File f = new File(text);
            File parent = f.getParentFile();
            if (null != parent) {
                chooser.setCurrentDirectory(parent);
            }
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = chooser.getSelectedFile();
            if (null != selectedFile) {
                saveObjectsFile(selectedFile);
            }
        }
    }//GEN-LAST:event_jButtonSaveObjectsFileActionPerformed

    private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider1StateChanged
        if (null != inOutJPanel1 && null != jSlider1) {
            inOutJPanel1.setHeightViewAngle((double) jSlider1.getValue());
        }
    }//GEN-LAST:event_jSlider1StateChanged

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        savePrefsAction();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItemFileLoadPropertiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFileLoadPropertiesActionPerformed
        loadPrefsAction();
    }//GEN-LAST:event_jMenuItemFileLoadPropertiesActionPerformed

    public void savePrefsAction() {
        JFileChooser chooser = new JFileChooser(new File(CRCLUtils.getCrclUserHomeDir()));
        if (JFileChooser.APPROVE_OPTION == chooser.showSaveDialog(this)) {
            File f = chooser.getSelectedFile();
            if (null != f) {
                setPropertiesFile(f);
                saveRecentPropertiesFile();
                saveProperties();
            }
        }
    }

    public void saveRecentPropertiesFile() {
        if (null != propertiesFile) {
            File crcljavaDir = new File(CRCLUtils.getCrclUserHomeDir(), CRCLJAVA_USER_DIR);
            boolean made_dir = crcljavaDir.mkdirs();
            File settingsRef = new File(crcljavaDir, SETTINGSREF);
            try ( PrintStream psRef = new PrintStream(new FileOutputStream(settingsRef))) {
                psRef.println(propertiesFile.getCanonicalPath());
            } catch (Exception ex) {
                showMessage(ex);
            }
        }
    }

    public void loadPrefsAction() {
        JFileChooser chooser = new JFileChooser(new File(CRCLUtils.getCrclUserHomeDir()));
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) {
            File f = chooser.getSelectedFile();
            if (null != f) {
                setPropertiesFile(f);
                saveRecentPropertiesFile();
                loadProperties();
            }
        }
    }

    void listToModel(DefaultTableModel model, List<TrayStack> newList) {
        final int nameColumn = model.findColumn("Name");
        final int countColumn = model.findColumn("Count");
        final int widthColumn = model.findColumn("Width");
        final int lengthColumn = model.findColumn("Length");
        final int heightColumn = model.findColumn("Height");
        final int xColumn = model.findColumn("X");
        final int yColumn = model.findColumn("Y");
        final int zColumn = model.findColumn("Z");
        final int rotationColumn = model.findColumn("Rotation");
        final int scaleColumn = model.findColumn("Scale");
//        final Vector dataVector = model.getDataVector();
//        dataVector.setSize(newList.size());
        model.setRowCount(newList.size());
        for (int i = 0; i < newList.size(); i++) {
            TrayStack stack = newList.get(i);
//            final Vector rowVector = (Vector) dataVector.elementAt(i);

            model.setValueAt(stack.name, i, nameColumn);
            model.setValueAt(stack.count, i, countColumn);
            model.setValueAt(stack.width, i, widthColumn);
            model.setValueAt(stack.length, i, lengthColumn);
            model.setValueAt(stack.height, i, heightColumn);
            model.setValueAt(stack.x, i, xColumn);
            model.setValueAt(stack.y, i, yColumn);
            model.setValueAt(stack.z, i, zColumn);
        }
    }

    List<TrayStack> modelToList(DefaultTableModel model) {
        List<TrayStack> newList = new ArrayList<>();
        final int nameColumn = model.findColumn("Name");
        final int countColumn = model.findColumn("Count");
        final int widthColumn = model.findColumn("Width");
        final int lengthColumn = model.findColumn("Length");
        final int heightColumn = model.findColumn("Height");
        final int xColumn = model.findColumn("X");
        final int yColumn = model.findColumn("Y");
        final int zColumn = model.findColumn("Z");
        final int rotationColumn = model.findColumn("Rotation");
        final int scaleColumn = model.findColumn("Scale");
        @SuppressWarnings({"rawtypes", "unchecked"})
        final Vector<Vector> dataVector = model.getDataVector();
//        System.out.println("dataVector = " + dataVector);
        for (int i = 0; i < model.getRowCount(); i++) {
            TrayStack stack = new TrayStack();
            final Object nameObject = model.getValueAt(i, nameColumn);
            if (nameObject instanceof String) {
                stack.name = (String) nameObject;
            } else {
                continue;
            }
            Object countObject = model.getValueAt(i, countColumn);
            if (countObject instanceof Integer) {
                stack.count = (Integer) countObject;
            } else if (countObject != null) {
                stack.count = Integer.parseInt(countObject.toString());
            } else {
                continue;
            }
            Object widthObject = model.getValueAt(i, widthColumn);
            if (widthObject instanceof Double) {
                stack.width = (Double) widthObject;
            } else if (widthObject != null) {
                stack.width = Double.parseDouble(widthObject.toString());
            } else {
                continue;
            }
            Object lengthObject = model.getValueAt(i, lengthColumn);
            if (lengthObject instanceof Double) {
                stack.length = (Double) lengthObject;
            } else if (lengthObject != null) {
                stack.length = Double.parseDouble(lengthObject.toString());
            } else {
                continue;
            }
            Object heightObject = model.getValueAt(i, heightColumn);
            if (heightObject instanceof Double) {
                stack.height = (Double) heightObject;
            } else if (heightObject != null) {
                stack.height = Double.parseDouble(heightObject.toString());
            }
            Object xObject = model.getValueAt(i, xColumn);
            if (xObject instanceof Double) {
                stack.x = (Double) xObject;
            } else if (xObject != null) {
                stack.x = Double.parseDouble(xObject.toString());
            } else {
                continue;
            }
            Object yObject = model.getValueAt(i, yColumn);
            if (yObject instanceof Double) {
                stack.y = (Double) yObject;
            } else if (yObject != null) {
                stack.y = Double.parseDouble(yObject.toString());
            } else {
                continue;
            }
            Object zObject = model.getValueAt(i, zColumn);
            if (zObject instanceof Double) {
                stack.z = (Double) zObject;
            } else if (zObject != null) {
                stack.z = Double.parseDouble(zObject.toString());
            } else {
                continue;
            }
            Object rotationObject = model.getValueAt(i, rotationColumn);
            if (rotationObject instanceof Double) {
                stack.rotationRadians = Math.toRadians((Double) rotationObject);
            } else if (rotationObject != null) {
                stack.rotationRadians = Math.toRadians(Double.parseDouble(rotationObject.toString()));
            } else {
                continue;
            }
            Object scaleObject = model.getValueAt(i, scaleColumn);
            if (scaleObject instanceof Double) {
                stack.scale = (Double) scaleObject;
            } else if (scaleObject != null) {
                stack.scale = Double.parseDouble(scaleObject.toString());
            } else {
                continue;
            }
            newList.add(stack);
        }
        return newList;
    }

    private void loadObjectsFile(final File selectedFile) {
        try {
            final String selectedFileCanonicalPath = selectedFile.getCanonicalPath();
            if (null != jTextFieldObjectsFile) {
                jTextFieldObjectsFile.setText(selectedFileCanonicalPath);
            }
            this.objectsFileName = selectedFileCanonicalPath;
            final DefaultTableModel model = (DefaultTableModel) jTableObjects.getModel();
            readCsvFileToTableAndMap(false, model, selectedFile, null, null, null);
            this.inOutJPanel1.setStacks(modelToList(model));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "selectedFile=" + selectedFile, ex);
        }
    }

    private void saveObjectsFile(final File selectedFile) {
        try {
            jTextFieldObjectsFile.setText(selectedFile.getCanonicalPath());
            saveTableModel(selectedFile, (DefaultTableModel) jTableObjects.getModel());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "selectedFile=" + selectedFile, ex);
        }
    }

    private static String[] tableHeaders(TableModel tm) {
        String headers[] = new String[tm.getColumnCount()];
        for (int i = 0; i < tm.getColumnCount() && i < headers.length; i++) {
            headers[i] = tm.getColumnName(i);
        }
        return headers;
    }

    private static void saveTableModel(File f, TableModel tm) throws IOException {
        try ( CSVPrinter printer = new CSVPrinter(new PrintStream(new FileOutputStream(f)), CSVFormat.DEFAULT.withHeader(tableHeaders(tm)))) {

            List<String> colNameList = new ArrayList<>();
            for (int i = 0; i < tm.getColumnCount(); i++) {
                colNameList.add(tm.getColumnName(i));
            }
            for (int i = 0; i < tm.getRowCount(); i++) {
                List<Object> l = new ArrayList<>();
                for (int j = 0; j < tm.getColumnCount(); j++) {
                    Object o = tm.getValueAt(i, j);
                    if (o == null) {
                        l.add("");
                    }
                    if (o instanceof File) {
                        File parentFile = f.getParentFile();
                        if (null != parentFile) {
                            Path rel = parentFile.toPath().toRealPath().relativize(Paths.get(((File) o).getCanonicalPath())).normalize();
                            if (rel.toString().length() < ((File) o).getCanonicalPath().length()) {
                                l.add(rel);
                            } else {
                                l.add(o);
                            }
                        } else {
                            l.add(o);
                        }
                    } else {
                        l.add(o);
                    }
                }
                printer.printRecord(l);
            }
        }
    }

    private volatile String objectsFileName = "";

    public String getObjectsFileName() {
        if (null == jTextFieldObjectsFile) {
            return objectsFileName;
        } else {
            return jTextFieldObjectsFile.getText();
        }
    }

    public void setObjectsFileName(String name) {
        if (name == null || name.trim().length() < 1) {
            if (null != jTextFieldObjectsFile) {
                jTextFieldObjectsFile.setText("");
            }
            this.objectsFileName = "";
            return;
        }
        this.objectsFileName = name;
        File f = new File(name);
        if (f.exists()) {
            loadObjectsFile(f);
        } else {
            File f2 = new File(propertiesFile.getParentFile(), f.getName());
            if (f2.exists()) {
                loadObjectsFile(f2);
                this.objectsFileName = f.getName();
                if (null != jTextFieldObjectsFile) {
                    jTextFieldObjectsFile.setText(f.getName());
                }
            }
        }
    }

    @UIEffect
    public static <T> void readCsvFileToTableAndMap(
            boolean forceColumns,
            @Nullable DefaultTableModel dtm,
            File f,
            @Nullable String nameRecord,
            @Nullable Map<String, T> map,
            @Nullable Function<CSVRecord, T> recordToValue) {

        if (null != dtm) {
            dtm.setRowCount(0);
        }
        try ( CSVParser parser = new CSVParser(new FileReader(f), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (forceColumns && null != dtm) {
                dtm.setRowCount(0);
                dtm.setColumnCount(0);
                for (String key : headerMap.keySet()) {
                    dtm.addColumn(key);
                }
            }
            List<CSVRecord> records = parser.getRecords();
            int skipRows = 0;
            if (null != dtm) {
                for (CSVRecord rec : records) {
                    String colName = dtm.getColumnName(0);
                    Integer colIndex = headerMap.get(colName);
                    if (colIndex == null) {
                        throw new IllegalArgumentException(f + " does not have field " + colName);
                    }
                    String val0 = rec.get(colIndex);
                    if (!val0.equals(colName) && val0.length() > 0) {
                        break;
                    }
                    if (val0.length() < 1) {
                        LOGGER.log(Level.WARNING, "skipping record with empty name field : rec=" + rec + " in file=" + f.getCanonicalPath() + ", colName=" + colName + ", val0=" + val0 + ",skipRows=" + skipRows);
                    }
                    skipRows++;
                }
                dtm.setRowCount(records.size() - skipRows);
            }
            ROW_LOOP:
            for (int i = skipRows; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                if (null != dtm) {
                    for (int j = 0; j < dtm.getColumnCount(); j++) {
                        String colName = dtm.getColumnName(j);
                        Integer colIndex = headerMap.get(colName);
                        if (colIndex == null) {
                            continue ROW_LOOP;
                        }
                        String val = rec.get(colIndex);
                        try {
                            if (null != val) {
                                if (val.equals(colName) || (j == 0 && val.length() < 1)) {
                                    continue ROW_LOOP;
                                }
                                Class<?> colClass = dtm.getColumnClass(j);
                                if (colClass == Double.class) {
                                    dtm.setValueAt(Double.valueOf(val), i - skipRows, j);
                                } else if (colClass == Boolean.class) {
                                    dtm.setValueAt(Boolean.valueOf(val), i - skipRows, j);
                                } else {
                                    dtm.setValueAt(val, i - skipRows, j);
                                }
                            }
                        } catch (Exception exception) {
                            String msg = "colName=" + colName + ", colIndex=" + colIndex + ", val=" + val + ", rec=" + rec;
                            LOGGER.log(Level.SEVERE, msg, exception);
                            throw new RuntimeException(msg, exception);
                        }
                    }
                }
                try {
                    if (null != nameRecord && null != map && null != recordToValue) {
                        String name = rec.get(nameRecord);
                        T value = recordToValue.apply(rec);
                        map.put(name, value);
                    }
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "rec=" + rec, exception);
                    throw new RuntimeException(exception);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,
                    "forceColumns=" + forceColumns
                    + ", dtm=" + dtm
                    + ", f=" + f
                    + ", nameRecord=" + nameRecord
                    + ", map=" + map
                    + ", recordToValue=" + recordToValue,
                    ex
            );
            throw new RuntimeException(ex);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ForceTorqueSimJPanel.class.getName());

    public int getCRCLSensorOutPort() {
        if (crclServerSocket != null) {
            int p = crclServerSocket.getPort();
            this.crclSensorOutPort = p;
            return p;
        } else if (null != jTextFieldCRCLSensorOutPort) {
            int p = Integer.parseInt(jTextFieldCRCLSensorOutPort.getText());
            this.crclSensorOutPort = p;
            return p;
        } else {
            return this.crclSensorOutPort;
        }
    }

    private int crclSensorOutPort = 8888;

    public void setCRCLSensorOutPort(int port) {
        if (crclServerSocket != null && crclServerSocket.getPort() != port) {
            crclServerSocket.close();
            crclServerSocket = null;
            try {
                startServer(port);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ForceTorqueSimJPanel.class.getName()).log(Level.SEVERE, "", ex);
            }
        }
        if (null != jTextFieldCRCLSensorOutPort) {
            jTextFieldCRCLSensorOutPort.setText(Integer.toString(port));
        }
        this.crclSensorOutPort = port;
    }

    private int poseCrclPort = 64444;

    public int getPoseCRCLPort() {
        if (poseInConnection != null) {
            int p = poseInConnection.getPort();
            this.poseCrclPort = p;
            return p;
        } else if (null != jTextFieldPoseCRCLPort) {
            int p = Integer.parseInt(jTextFieldPoseCRCLPort.getText());
            this.poseCrclPort = p;
            return p;
        } else {
            return this.poseCrclPort;
        }
    }

    public void setPoseCRCLPort(int port) {
        if (poseInConnection != null && port != poseInConnection.getPort() && null == this.crclClientPanel) {
            stopPoseInConnection();
            if (null != getPoseServiceThread) {
                getPoseServiceThread.setName(tcount.incrementAndGet() + "ForceTorqueSimGetPose" + port);
            }
            startPoseInConnection();
        }
        if (null != jTextFieldPoseCRCLPort) {
            jTextFieldPoseCRCLPort.setText(Integer.toString(port));
        }
        this.poseCrclPort = port;
    }

    private String poseCrclHost;

    public String getPoseCRCLHost() {
        if (null != jTextFieldPoseCRCLHost) {
            String h = jTextFieldPoseCRCLHost.getText();
            this.poseCrclHost = h;
            return h;
        } else {
            return this.poseCrclHost;
        }
    }

    public void setPoseCRCLHost(String host) {
        if (null != jTextFieldPoseCRCLHost) {
            jTextFieldPoseCRCLHost.setText(host);
        }
        this.poseCrclHost = host;
    }

    private final AtomicInteger getPoseCount = new AtomicInteger();

    private CrclSwingClientJPanel crclClientPanel;

    /**
     * Get the value of crclClientPanel
     *
     * @return the value of crclClientPanel
     */
    public CrclSwingClientJPanel getCrclClientPanel() {
        return crclClientPanel;
    }

    /**
     * Set the value of crclClientPanel
     *
     * @param crclClientPanel new value of crclClientPanel
     */
    public void setCrclClientPanel(CrclSwingClientJPanel crclClientPanel) {
        this.crclClientPanel = crclClientPanel;
        if (null != crclClientPanel) {
            if (null != jTextFieldPoseCRCLHost) {
                this.jTextFieldPoseCRCLHost.setEditable(false);
                this.jTextFieldPoseCRCLHost.setEnabled(false);
            }
            if (null != jTextFieldPoseCRCLPort) {
                this.jTextFieldPoseCRCLPort.setEditable(false);
                this.jTextFieldPoseCRCLPort.setEnabled(false);
            }
            this.setPoseCRCLPort(crclClientPanel.getPort());
            this.setPoseCRCLHost(crclClientPanel.getHost());
            this.crclClientPanel.addCurrentPoseListener(currentPoseListener);
            if (null != jPanelCRCLPositionIn) {
                final Border border = jPanelCRCLPositionIn.getBorder();
                if (border instanceof TitledBorder) {
                    TitledBorder titledBorder = (TitledBorder) border;
                    titledBorder.setTitle("CRCL Pose In Connection : " + crclClientPanel);
                }
            }
        } else {
            if (null != jTextFieldPoseCRCLHost) {
                this.jTextFieldPoseCRCLHost.setEditable(true);
                this.jTextFieldPoseCRCLHost.setEnabled(true);
            }
            if (null != jTextFieldPoseCRCLPort) {
                this.jTextFieldPoseCRCLPort.setEditable(true);
                this.jTextFieldPoseCRCLPort.setEnabled(true);
            }
            if (null != jPanelCRCLPositionIn) {
                final Border border = jPanelCRCLPositionIn.getBorder();
                if (border instanceof TitledBorder) {
                    TitledBorder titledBorder = (TitledBorder) border;
                    titledBorder.setTitle("CRCL Pose In Connection");
                }
            }
        }
    }

    private volatile boolean lastIsHoldingObjectExpected = false;
    private final AtomicInteger holdingObjectChanges = new AtomicInteger();

    private final CurrentPoseListener currentPoseListener = new CurrentPoseListener() {
        @Override
        public synchronized void handlePoseUpdate(CurrentPoseListenerUpdateInfo updateInfo) {
            CRCLStatusType stat = updateInfo.getStat();
            boolean isHoldingObjectExpected = updateInfo.isIsHoldingObjectExpected();
            ForceTorqueSimJPanel.this.poseInfoToProvide = updateInfo;
            if (isHoldingObjectExpected != lastIsHoldingObjectExpected) {
                lastIsHoldingObjectExpected = isHoldingObjectExpected;
                holdingObjectChanges.incrementAndGet();
                System.out.println("isHoldingObjectExpected = " + isHoldingObjectExpected);
            }
        }
    };

    private @Nullable
    CurrentPoseListenerUpdateInfo getPoseListenerUpdateInfo() {
        final CRCLSocket poseInConnectionStackCopy = poseInConnection;
        if (null != poseInConnectionStackCopy) {
            try {
                final GetStatusType getStatusCmd = new GetStatusType();
                getStatusCmd.setName("ForceTorqueSimGetPose" + getPoseCount.incrementAndGet());
                poseInConnectionStackCopy.writeCommand(getStatusCmd);
                CRCLStatusType newStatus
                        = CRCLUtils.requireNonNull(poseInConnectionStackCopy.readStatus(),
                                "poseInConnection.readStatus()");
                this.poseStatus = newStatus;
                final PoseStatusType newPoseStatus = CRCLUtils.requireNonNull(newStatus.getPoseStatus(), "newStatus.getPoseStatus()");
                final PoseType pose = CRCLUtils.requireNonNull(newPoseStatus.getPose(), "newPoseStatus.getPose()");
                GripperStatusType gripperStatus = newStatus.getGripperStatus();
                boolean holdingObjectExpected = false;
                if (null == gripperStatus) {
                    final SettingsStatusType settingsStatus = newStatus.getSettingsStatus();
                    if (null != settingsStatus) {
                        if (null != settingsStatus.getEndEffectorSetting()) {
                            holdingObjectExpected = settingsStatus.getEndEffectorSetting() < 0.5;
                        }
                    }
                } else {
                    holdingObjectExpected = gripperStatus.isHoldingObject();
                }
                return new CurrentPoseListenerUpdateInfo(crclClientPanel, newStatus, null, holdingObjectExpected, System.currentTimeMillis());
            } catch (CRCLException ex) {
                java.util.logging.Logger.getLogger(ForceTorqueSimJPanel.class.getName()).log(Level.SEVERE, "", ex);
                return null;
            }
        } else {
            return null;
        }
    }

    private volatile @Nullable
    Thread getPoseServiceThread = null;

    private static final AtomicInteger tcount = new AtomicInteger();

    private final ThreadFactory getPoseServiceThreadFactory;

    private static ThreadFactory createPoseServiceThreadFactory(int poseCRCLPort, Consumer<Thread> consumer) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, tcount.incrementAndGet() + "ForceTorqueSimGetPose" + poseCRCLPort);
                thread.setDaemon(true);
                consumer.accept(thread);
                return thread;
            }
        };
    }

    private final ExecutorService getPoseService;
    private volatile @Nullable
    CurrentPoseListenerUpdateInfo poseInfoToProvide = null;

    @SuppressWarnings("nullness")
    private XFuture<@Nullable CurrentPoseListenerUpdateInfo> getPoseFuture() {
        if (null != poseInfoToProvide) {
            return XFuture.completedFuture(poseInfoToProvide);
        }
        if (null != crclClientPanel) {
            return XFuture.completedFuture(crclClientPanel.getCurrentPoseListenerUpdateInfo());
        }
        if (null == poseInConnection) {
            return XFuture.completedFuture(null);
        }
        return XFuture.supplyAsync("getPoseFuture", this::getPoseListenerUpdateInfo, getPoseService);
    }

    public void startServer() {
        int port = Integer.parseInt(jTextFieldCRCLSensorOutPort.getText());
        try {
            startServer(port);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "port=" + port, ex);
        }
    }

    private void startServer(int port) throws IOException {
        if (!jCheckBoxStartSensorOutServer.isSelected()) {
            jCheckBoxStartSensorOutServer.setSelected(true);
        }
        CRCLServerSocket<ForceTorqueSimClientState> newCrclServerSocket
                = new CRCLServerSocket<>(port, FORCE_TORQUE_SIM_STATE_GENERATOR);
        newCrclServerSocket.addListener(crclSocketEventListener);
        newCrclServerSocket.setServerSideStatus(statusOut);
        newCrclServerSocket.setAutomaticallySendServerSideStatus(true);
        newCrclServerSocket.setAutomaticallyConvertUnits(true);
        newCrclServerSocket.setUpdateStatusSupplier(this::updateSensorStatus);
        statusOut.releaseLockThread();
        internalUpdateSensorStatus();
        statusOut.releaseLockThread();
        newCrclServerSocket.start();
        this.crclServerSocket = newCrclServerSocket;
    }

    private File propertiesFile;

    /**
     * Get the value of propertiesFile
     *
     * @return the value of propertiesFile
     */
    public File getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the value of propertiesFile
     *
     * @param propertiesFile new value of propertiesFile
     */
    public void setPropertiesFile(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    private volatile @Nullable
    File loadedPrefsFile = null;
    private volatile StackTraceElement loadPrefsTrace @Nullable []  = null;
    private volatile @Nullable
    Thread loadPrefsThread = null;

    private static final String SETTINGSREF = "forcetorquesimsettingsref";
    private static final String CRCLJAVA_USER_DIR = ".crcljava";
    private static final String recent_files_dir = ".force_torque_sim_recent_files";

    private void loadPrefsFile(File f) {
        try {
            loadPrefsTrace = Thread.currentThread().getStackTrace();
            loadedPrefsFile = f;
            loadPrefsThread = Thread.currentThread();
            File crcljavaDir = new File(CRCLUtils.getCrclUserHomeDir(), CRCLJAVA_USER_DIR);
            boolean made_dir = crcljavaDir.mkdirs();
            File settingsRef = new File(crcljavaDir, SETTINGSREF);
            try ( PrintStream psRef = new PrintStream(new FileOutputStream(settingsRef))) {
                psRef.println(f.getCanonicalPath());
            }
            Map<String, Object> targetMap = new TreeMap<>();
            targetMap.put("inOutJPanel1.", inOutJPanel1);
            Object defaultTarget = this;
            AutomaticPropertyFileUtils.loadPropertyFile(f, targetMap, defaultTarget);
            if (null != jSlider1) {
                this.jSlider1.setValue((int) inOutJPanel1.getHeightViewAngle());
            }
        } catch (IOException iOException) {
            showMessage("Failed to loadPrefsFile " + f + " : " + iOException);
        }
    }

    public void showMessage(Throwable t) {
        this.showMessage(t.toString());
    }

    private volatile boolean showing_message = false;
    private volatile long last_message_show_time = -1;

    public void showMessage(final String s) {
        System.out.println(s);
        if (showing_message) {
            return;
        }
        showing_message = true;
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                long t = System.currentTimeMillis();
                if (t - last_message_show_time > 5000) {
                    last_message_show_time = System.currentTimeMillis();
//                    Window window = ForceTorqueSimJPanel.getOuterWindow();
//                    if (null != window && window instanceof JFrame) {
//                        MultiLineStringJPanel.showText(s,
//                                (JFrame) window,
//                                "Message from Client",
//                                true);
//                    }
                }
                last_message_show_time = System.currentTimeMillis();
                showing_message = false;
            }
        });
    }

    public void loadProperties() {
        System.out.println("ForceTorqueSimJPanel.loadProperties() : propertiesFile = " + propertiesFile);
        if (null != propertiesFile && propertiesFile.exists()) {
            loadPrefsFile(propertiesFile);
        }
    }

    public void saveProperties() {
        try {
            AutomaticPropertyFileUtils.saveObjectProperties(propertiesFile, this);
            AutomaticPropertyFileUtils.appendObjectProperties(propertiesFile, "inOutJPanel1.", inOutJPanel1);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static class ForceTorqueSimClientState extends CRCLServerClientState {

        public ForceTorqueSimClientState(CRCLSocket cs) {
            super(cs);
            final ConfigureStatusReportType configureStatusReportType = new ConfigureStatusReportType();
            configureStatusReportType.setReportGripperStatus(false);
            configureStatusReportType.setReportPoseStatus(false);
            configureStatusReportType.setReportSettingsStatus(false);
            configureStatusReportType.setReportJointStatuses(false);
            configureStatusReportType.setReportSensorsStatus(true);
            super.filterSettings.setConfigureStatusReport(configureStatusReportType);
        }
        int i;
    }
    private final ThreadLockedHolder<CRCLStatusType> statusOut;
    private final ForceTorqueSensorStatusType sensorStatus;

    public static final CRCLServerSocketStateGenerator<ForceTorqueSimClientState> FORCE_TORQUE_SIM_STATE_GENERATOR
            = ForceTorqueSimClientState::new;

    @SuppressWarnings({"nullness", "initialization"})
    private final CRCLServerSocketEventListener<ForceTorqueSimClientState> crclSocketEventListener
            = this::handleCrclServerSocketEvent;

    private @Nullable
    CRCLServerSocket<ForceTorqueSimClientState> crclServerSocket;

    @SuppressWarnings("EmptyMethod")
    private void handleCrclServerSocketEvent(CRCLServerSocketEvent<ForceTorqueSimClientState> evt) {

    }

    private volatile @Nullable
    XFuture<@Nullable CurrentPoseListenerUpdateInfo> lastUpdateSensorStatusPoseFuture = null;
    private volatile @Nullable
    XFuture<CRCLStatusType> lastUpdateSensorStatusRet = null;

    private final AtomicInteger updateSensorStatusStartCount = new AtomicInteger();

    private XFuture<CRCLStatusType> updateSensorStatus() {
        int startCount = updateSensorStatusStartCount.incrementAndGet();
        System.out.println("ForceTorqueSimJPanel.updateSensorStatus : startCount = " + startCount);
        XFuture<CRCLStatusType> ret = internalUpdateSensorStatus();
        this.lastUpdateSensorStatusRet = ret;
        return ret;
    }

    private XFuture<CRCLStatusType> internalUpdateSensorStatus() {
        XFuture<@Nullable CurrentPoseListenerUpdateInfo> f = getPoseFuture();
        this.lastUpdateSensorStatusPoseFuture = f;
        XFuture<CRCLStatusType> ret
                = f.thenApply(
                        "statusFromPose",
                        (CurrentPoseListenerUpdateInfo pose) -> {
                            if (null == pose) {
                                throw new NullPointerException("pose");
                            }
                            return statusFromPose(pose);
                        }
                );
        return ret;
    }

    private volatile @Nullable
    CRCLStatusType lastStatusFromPoseRet = null;

    private volatile @Nullable
    PoseType lastStatusFromPosePose = null;

    private final AtomicInteger statusFromPoseEndCount = new AtomicInteger();

    private CRCLStatusType statusFromPose(CurrentPoseListenerUpdateInfo info) {
        PoseType pose = info.getStat().getPoseStatus().getPose();
        this.lastStatusFromPosePose = pose;
        ForceTorqueSensorStatusType forceTorqueSensorStatusType
                = updateSensorStatusWithPose(pose);
        final CRCLStatusType stat = statusOut.get();
        addForceTorqueSensorToStatus(stat, forceTorqueSensorStatusType);
        CRCLStatusType statCopy = requireNonNull(CRCLCopier.copy(stat), "copy(stat)");
        this.lastStatusFromPoseRet = statCopy;
        int endCount = statusFromPoseEndCount.incrementAndGet();
        System.out.println("ForceTorqueSimJPanel.statusFromPose : endCount = " + endCount);
        return statCopy;
    }

    public List<TrayStack> getStacks() {
        return inOutJPanel1.getStacks();
    }

    /**
     * Set the value of stacks
     *
     * @param stacks new value of stacks
     */
    public void setStacks(List<TrayStack> stacks) {
        listToModel((DefaultTableModel) jTableObjects.getModel(), stacks);
        inOutJPanel1.setStacks(stacks);
        this.repaint();
    }

    private volatile int lastUpdateSensorStatusWithPoseHoldingObjectChanges = -1;

    private synchronized ForceTorqueSensorStatusType updateSensorStatusWithPose(@Nullable PoseType pose) {
        double xposeEffect = 0.0;
        double yposeEffect = 0.0;
        double zposeEffect = 0.0;
        if (null != pose) {
            final PointType posePoint = pose.getPoint();
//            Point2D.Double pt2d = new Point2D.Double(point.getX(), point.getY());
            if (null != posePoint) {
                inOutJPanel1.setRobotPose(pose);
                int changesCount = holdingObjectChanges.get();
                boolean newHoldingStatus = changesCount != lastUpdateSensorStatusWithPoseHoldingObjectChanges;
                lastUpdateSensorStatusWithPoseHoldingObjectChanges = changesCount;
                List<TrayStack> stacks = inOutJPanel1.getStacks();
                if (null != stacks) {
                    for (TrayStack stack : stacks) {
                        if (newHoldingStatus) {
                            boolean inside = InOutJPanel.insideStack(stack, new Point2D.Double(posePoint.getX(), posePoint.getY()));
                            if (inside) {
                                if (lastIsHoldingObjectExpected) {
                                    if (stack.count > 0) {
                                        stack.count--;
                                        inOutJPanel1.setHoldingFromStack(stack);
                                        listToModel((DefaultTableModel) jTableObjects.getModel(), stacks);
                                    }
                                } else {
                                    stack.count++;
                                    inOutJPanel1.setHoldingFromStack(null);
                                    listToModel((DefaultTableModel) jTableObjects.getModel(), stacks);
                                }
                                newHoldingStatus = false;
                            }
                        }
                        final double zdiff = inOutJPanel1.poseStackZDiff(stack);
                        if (Double.isFinite(zdiff) && zdiff > 0) {
                            zposeEffect += stack.scale * zdiff;
                        }
                    }
                }
            }
        }
        return completeUpdateStatus(xposeEffect, yposeEffect, zposeEffect);
    }

    private ForceTorqueSensorStatusType completeUpdateStatus(double xposeEffect, double yposeEffect, double zposeEffect) {
        final double fxValue = valueJPanelFx.getValue();
        final double fyValue = valueJPanelFy.getValue();
        final double fzValue = valueJPanelFz.getValue();
        final double txValue = valueJPanelTx.getValue();
        final double tyValue = valueJPanelTy.getValue();
        final double tzValue = valueJPanelTz.getValue();
        final ForceTorqueSensorStatusType sensorStatusCopy;
        synchronized (sensorStatus) {
            sensorStatus.setFx(fxValue + xposeEffect);
            sensorStatus.setFy(fyValue + yposeEffect);
            sensorStatus.setFz(fzValue + zposeEffect);
            sensorStatus.setTx(txValue);
            sensorStatus.setTy(tyValue);
            sensorStatus.setTz(tzValue);
            sensorStatus.setSensorID("ForceTorqueSim");
            sensorStatusCopy = CRCLCopier.copy(sensorStatus);
        }
        if (null == sensorStatusCopy) {
            throw new NullPointerException("sensorStatusCopy");
        }
        javax.swing.SwingUtilities.invokeLater(() -> updateForceTorqueDisplay(sensorStatusCopy));
        if (null == crclServerSocket) {
            throw new NullPointerException("crclServerSocket");
        }
        crclServerSocket.addToUpdateServerSideRunnables(() -> {
            CRCLStatusType stat = statusOut.get();
            addForceTorqueSensorToStatus(stat, sensorStatusCopy);
        });
        return sensorStatusCopy;
    }

    private void addForceTorqueSensorToStatus(CRCLStatusType stat, final ForceTorqueSensorStatusType sensorStatusCopy) {
        synchronized (stat) {
            SensorStatusesType sensorStatuses = stat.getSensorStatuses();
            if (null == sensorStatuses) {
                sensorStatuses = new SensorStatusesType();
            }
            final List<ForceTorqueSensorStatusType> forceTorqueSensorStatusList
                    = new ArrayList<>(sensorStatuses.getForceTorqueSensorStatus());
            for (int i = 0; i < forceTorqueSensorStatusList.size(); i++) {
                ForceTorqueSensorStatusType forceTorqueSensorStatusI
                        = forceTorqueSensorStatusList.get(i);
                if (forceTorqueSensorStatusI.getSensorID().equals(sensorStatusCopy.getSensorID())) {
                    forceTorqueSensorStatusList.remove(i);
                    break;
                }
            }
            forceTorqueSensorStatusList.add(sensorStatusCopy);
            sensorStatuses.getForceTorqueSensorStatus().clear();
            sensorStatuses.getForceTorqueSensorStatus().addAll(forceTorqueSensorStatusList);
            stat.setSensorStatuses(sensorStatuses);
        }
    }

    private void updateForceTorqueDisplay(ForceTorqueSensorStatusType sensorStatusCopy) {
        jTablePoseForceOut.setValueAt(sensorStatusCopy.getFx(), 0, 1);
        jTablePoseForceOut.setValueAt(sensorStatusCopy.getFy(), 1, 1);
        jTablePoseForceOut.setValueAt(sensorStatusCopy.getFz(), 2, 1);
        jTablePoseForceOut.setValueAt(sensorStatusCopy.getTx(), 3, 1);
        jTablePoseForceOut.setValueAt(sensorStatusCopy.getTy(), 4, 1);
        jTablePoseForceOut.setValueAt(sensorStatusCopy.getTz(), 5, 1);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private crcl.ui.forcetorquesensorsimulator.InOutJPanel inOutJPanel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonAddObject;
    private javax.swing.JButton jButtonDeleteObject;
    private javax.swing.JButton jButtonOpenObjectsFile;
    private javax.swing.JButton jButtonSaveObjectsFile;
    private javax.swing.JCheckBox jCheckBoxEnablePoseInConnection;
    private javax.swing.JCheckBox jCheckBoxStartSensorOutServer;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu2;
    final javax.swing.JMenuBar jMenuBar1 = new javax.swing.JMenuBar();
    private javax.swing.JMenu jMenuFileSaveProperties;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItemFileLoadProperties;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanelCRCLPositionIn;
    private javax.swing.JPanel jPanelCommunications;
    private javax.swing.JPanel jPanelCrclSensorServerOut;
    private javax.swing.JPanel jPanelForceOut;
    private javax.swing.JPanel jPanelOffsets;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableObjects;
    private javax.swing.JTable jTablePose;
    private javax.swing.JTable jTablePoseForceOut;
    private javax.swing.JTextField jTextFieldCRCLSensorOutPort;
    private javax.swing.JTextField jTextFieldObjectsFile;
    private javax.swing.JTextField jTextFieldPoseCRCLHost;
    private javax.swing.JTextField jTextFieldPoseCRCLPort;
    private crcl.ui.forcetorquesensorsimulator.ValueJPanel valueJPanelFx;
    private crcl.ui.forcetorquesensorsimulator.ValueJPanel valueJPanelFy;
    private crcl.ui.forcetorquesensorsimulator.ValueJPanel valueJPanelFz;
    private crcl.ui.forcetorquesensorsimulator.ValueJPanel valueJPanelTx;
    private crcl.ui.forcetorquesensorsimulator.ValueJPanel valueJPanelTy;
    private crcl.ui.forcetorquesensorsimulator.ValueJPanel valueJPanelTz;
    // End of variables declaration//GEN-END:variables
}
