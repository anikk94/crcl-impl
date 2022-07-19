/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package crcl.utils.server;

import crcl.base.CRCLStatusType;
import crcl.utils.CRCLSocket;
import crcl.utils.XFuture;
import crcl.utils.XFutureVoid;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CRCLServerClientState implements AutoCloseable {

    private final CRCLSocket cs;
    public int cmdsRecieved = 0;
    public long lastCmdTime = 0;
    public long cmdId = -999;
    public boolean pureLocal = false;
    public final CRCLStatusFilterSettings filterSettings;
    public @Nullable XFutureVoid directReturnedStatusSupplierFuture ;
    private volatile @Nullable CRCLStatusType directReturnedStatus;
    private volatile StackTraceElement[] directReturnedStatusSetTrace;
     private volatile Thread directReturnedStatusSetThread;
    
    public CRCLServerClientState(CRCLSocket cs) {
        this.cs = cs;
        this.filterSettings = new CRCLStatusFilterSettings();
    }

    public CRCLStatusType getDirectReturnedStatus() {
        return directReturnedStatus;
    }

    public void setDirectReturnedStatus(CRCLStatusType directReturnedStatus) {
        this.directReturnedStatus = directReturnedStatus;
        final Thread currentThread = Thread.currentThread();
        directReturnedStatusSetThread= currentThread;
        directReturnedStatusSetTrace = currentThread.getStackTrace();
    }

    
    @Override
    public void close() {
        try {
            cs.close();
        } catch (IOException ex) {
            Logger.getLogger(CRCLServerClientState.class.getName()).log(Level.SEVERE, "", ex);
        }
    }

    public CRCLSocket getCs() {
        return cs;
    }

    @Override
    public String toString() {
        return "CRCLServerClientState{" +
                "cs=" + cs +
                ", cmdsRecieved=" + cmdsRecieved +
                ", lastCmdTime=" + lastCmdTime +
                ", cmdId=" + cmdId +
                ", filterSettings=" + filterSettings +
                ", pureLocal=" + pureLocal +
                ", directReturnedStatus=" + directReturnedStatus +
                ", directReturnedStatusSetThread=" + directReturnedStatusSetThread +
                ", directReturnedStatusSetTrace=" + XFuture.traceToString(directReturnedStatusSetTrace) +
                '}';
    }
}
