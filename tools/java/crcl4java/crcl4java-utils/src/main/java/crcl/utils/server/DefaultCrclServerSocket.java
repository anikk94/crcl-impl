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
import crcl.utils.CRCLPosemath;
import crcl.utils.CRCLSocket;
import crcl.utils.ThreadLockedHolder;
import static crcl.utils.server.DefaultCRCLServerSocketStateGenerator.DEFAULT_STATE_GENERATOR;
import java.io.IOException;
import java.net.InetAddress;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DefaultCrclServerSocket extends CRCLServerSocket<CRCLServerClientState> {
    
    @SuppressWarnings("nullness")
    public DefaultCrclServerSocket() throws IOException{
        super(CRCLSocket.DEFAULT_PORT,DEFAULT_STATE_GENERATOR);
        super.setServerSideStatus(crclStatus);
    }
    
    @SuppressWarnings("nullness")
    public DefaultCrclServerSocket(int port) throws IOException {
        super(port,DEFAULT_STATE_GENERATOR);
        super.setServerSideStatus(crclStatus);
    }
    
    @SuppressWarnings("nullness")
    public DefaultCrclServerSocket(int port, int backlog, InetAddress addr) throws IOException {
        super(port,backlog,addr,DEFAULT_STATE_GENERATOR);
        super.setServerSideStatus(crclStatus);
    }
    
    @SuppressWarnings("nullness")
    public DefaultCrclServerSocket(int port, int backlog, InetAddress addr, boolean multithreaded) throws IOException {
        super(port,backlog,addr,multithreaded,DEFAULT_STATE_GENERATOR);
        super.setServerSideStatus(crclStatus);
    }
    
    private final ThreadLockedHolder<CRCLStatusType> crclStatus
            = new ThreadLockedHolder<>("DefaultCrclServerSocket.crclSTatus", 
                    CRCLPosemath.newFullCRCLStatus());

}
