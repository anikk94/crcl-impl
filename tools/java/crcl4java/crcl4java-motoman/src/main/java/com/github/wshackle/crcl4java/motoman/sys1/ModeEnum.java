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
package com.github.wshackle.crcl4java.motoman.sys1;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public enum ModeEnum {
    INVALID_MODE(0),
    TEACH_MODE(1),
    PLAY_MODE(2);
    
    private final int id;

    private ModeEnum(int id) {
        this.id = id;
    }

    private static final Map<Integer, ModeEnum> map = new HashMap<>();
//
    static {
        for (int i = 0; i < ModeEnum.values().length; i++) {
            ModeEnum m = ModeEnum.values()[i];
            map.put(m.getId(), m);
        }
    }
    
    public static ModeEnum fromId(int id) {
        return map.getOrDefault(id,ModeEnum.INVALID_MODE);
    }
    
    public int getId() { 
        return id;
    }

}
