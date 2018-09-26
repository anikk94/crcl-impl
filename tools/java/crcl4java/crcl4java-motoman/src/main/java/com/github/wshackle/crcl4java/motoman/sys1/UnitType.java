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
public enum UnitType {
    MP_POS_UNIT_DEGREE(1),
    MP_POS_UNIT_DISTANCE(2),
    MP_POS_UNIT_RADIAN(3);
    
    private final int id;

    private UnitType(int id) {
        this.id = id;
    }

    private static Map<Integer, UnitType> map = new HashMap<>();
//
    static {
        for (int i = 0; i < UnitType.values().length; i++) {
            UnitType m = UnitType.values()[i];
            map.put(m.getId(), m);
        }
    }
    
    public static UnitType fromId(int id) {
        return map.get(id);
    }
    
    public int getId() { 
        return id;
    }

}
