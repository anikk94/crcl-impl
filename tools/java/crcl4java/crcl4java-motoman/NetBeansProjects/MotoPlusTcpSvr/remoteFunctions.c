/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

#include "motoPlus.h"
#include "remoteFunctions.h"

extern void *malloc(size_t);
extern void free(void *);

#define BUFF_MAX    1023

static int recvN(int handle, char *buf, int n, int flags) {
    int lastRecv = -1;
    int totalRecv = 0;
    do {
        lastRecv = mpRecv(handle, (buf + totalRecv), n - totalRecv, flags);
        if (lastRecv == 0) {
            fprintf(stderr, "tcpSvr: recv returned 0\n");
            return lastRecv;
        }
        if (lastRecv < 1) {
            fprintf(stderr, "tcpSvr: recv error : %s\n", strerror(errno));
            return lastRecv;
        }
        totalRecv += lastRecv;
    } while (totalRecv < n);
    return totalRecv;
}

static int sendN(int handle, char *buf, int n, int flags) {
    int lastSend = -1;
    int totalSend = 0;
    do {
        lastSend = mpSend(handle, (buf + totalSend), n - totalSend, flags);
        if (lastSend < 1) {
            fprintf(stderr, "tcpSvr: send error : %s\n", strerror(errno));
            return lastSend;
        }
        totalSend += lastSend;
    } while (totalSend < n);
    return totalSend;
}

#ifdef DO_SWAP

static void swap(char *buf, int offset, int sz) {
    int i = 0;
    int ret = -1;
    char tmp;
    /*
        printf("swap(%p,%d,%d)\n",buf,offset,sz);
     */
    for (i = 0; i < sz / 2; i++) {
        tmp = buf[offset + i];
        buf[offset + i] = buf[offset + sz - 1 - i];
        buf[offset + sz - 1 - i] = tmp;
    }
}
#endif

// Stupid C won't include a lib function that won't make 
// me tear my hair out looking to make sure it is defined on every
// platform to do this
// so it gets reimplemented yet again.

#ifndef DO_NOT_NEED_STANDARD_INT_TYPES
typedef short int16_t;
typedef int int32_t;
#endif

static int16_t getInt16(char *buf, int offset) {
#ifdef DO_SWAP
    swap(buf, offset, 2);
#endif
    return *((int16_t *) (buf + offset));
}

static void setInt16(char *buf, int offset, int16_t val) {
    *((int16_t *) (buf + offset)) = val;
#ifdef DO_SWAP
    swap(buf, offset, 2);
#endif
}

static int32_t getInt32(char *buf, int offset) {
#ifdef DO_SWAP
    swap(buf, offset, 4);
#endif
    return *((int32_t *) (buf + offset));
}

static void setInt32(char *buf, int offset, int32_t val) {

    *((int32_t *) (buf + offset)) = val;
#ifdef DO_SWAP
    swap(buf, offset, 4);
#endif
}

// Return 0 for success, anything else will be treated like a fatal error closing
// the connection.

int handleSys1FunctionRequest(int acceptHandle, char *inBuffer, char *outBuffer, int type, int msgSize) {

    int sendRet = 0;
    int i = 0;
    MP_VAR_INFO varInfo[25];
    MP_VAR_DATA varData[25];
    MP_IO_INFO ioInfo[25];
    MP_IO_DATA ioData[25];
    LONG rData[25];
    USHORT iorData[25];
    MP_MODE_RSP_DATA modeData;
    MP_CYCLE_RSP_DATA cycleData;
    MP_ALARM_STATUS_RSP_DATA alarmStatusData;
    MP_ALARM_CODE_RSP_DATA alarmCodeData;
    LONG num;
    int ret;
    MP_CTRL_GRP_SEND_DATA ctrlGrpSendData;
    MP_CART_POS_RSP_DATA cartPosRspData;
    MP_PULSE_POS_RSP_DATA pulsePosRspData;
    MP_FB_PULSE_POS_RSP_DATA fbPulsePosRspData;
    MP_DEG_POS_RSP_DATA_EX degPosRspDataEx;
    MP_SERVO_POWER_RSP_DATA servoPowerRspData;
    MP_SERVO_POWER_SEND_DATA servoPowerSendData;
    MP_STD_RSP_DATA stdRspData;

    int32_t controlGroup = 0;

    switch (type) {
        case SYS1_GET_VAR_DATA:
            num = getInt32(inBuffer, 12);
            if (num < 1 || num > 24) {
                fprintf(stderr, "tcpSvr: invalid num for mpGetVarData num = %ld\n", num);
                return -1;
            }
            if (msgSize != 12 + (4 * num)) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetVarData = %d != %ld for num = %ld\n", msgSize, 12 + (4 * num), num);
                return -1;
            }
            for (i = 0; i < num; i++) {
                varInfo[i].usType = getInt16(inBuffer, 16 + (4 * i));
                varInfo[i].usIndex = getInt16(inBuffer, 18 + (4 * i));
            }
            ret = mpGetVarData(varInfo, rData, num);
            setInt32(outBuffer, 0, 4 + num * 4);
            setInt32(outBuffer, 4, ret);
            for (i = 0; i < num; i++) {
                setInt32(outBuffer, 8 + i * 4, rData[i]);
            }
            sendRet = sendN(acceptHandle, outBuffer, 8 + num * 4, 0);
            if (sendRet != 8 + num * 4) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8 + num*4\n", sendRet);
                return -1;
            }
            break;

        case SYS1_PUT_VAR_DATA:
            num = getInt32(inBuffer, 12);
            if (num < 1 || num > 24) {
                fprintf(stderr, "tcpSvr: invalid num for mpPutVarData num = %ld\n", num);
                return -1;
            }
            if (msgSize != 12 + (num * 8)) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpPutVarData = %d != %ld for num = %ld\n", msgSize, 12 + (num * 8), num);
                return -1;
            }
            for (i = 0; i < num; i++) {
                varData[i].usType = getInt16(inBuffer, 16 + (8 * i));
                varData[i].usIndex = getInt16(inBuffer, 18 + (8 * i));
                varData[i].ulValue = getInt32(inBuffer, 20 + (8 * i));
            }
            ret = mpPutVarData(varData, num);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_CURRENT_CART_POS:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetClear = %d != 12\n", msgSize);
                return -1;
            }
            memset(&ctrlGrpSendData, 0, sizeof (ctrlGrpSendData));
            memset(&cartPosRspData, 0, sizeof (cartPosRspData));
            controlGroup = getInt32(inBuffer, 12);
            ctrlGrpSendData.sCtrlGrp = controlGroup;
            ret = mpGetCartPos(&ctrlGrpSendData, &cartPosRspData);
            setInt32(outBuffer, 0, 54);
            setInt32(outBuffer, 4, ret);
            for (i = 0; i < 6; i++) {
                setInt32(outBuffer, 8 + 4 * i, cartPosRspData.lPos[i]);
            }
            setInt16(outBuffer, 56, cartPosRspData.sConfig);
            sendRet = sendN(acceptHandle, outBuffer, 58, 0);
            if (sendRet != 58) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 58\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_CURRENT_PULSE_POS:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetClear = %d != 12\n", msgSize);
                return -1;
            }
            memset(&ctrlGrpSendData, 0, sizeof (ctrlGrpSendData));
            memset(&pulsePosRspData, 0, sizeof (pulsePosRspData));
            controlGroup = getInt32(inBuffer, 12);
            ctrlGrpSendData.sCtrlGrp = controlGroup;
            ret = mpGetPulsePos(&ctrlGrpSendData, &pulsePosRspData);
            setInt32(outBuffer, 0, 68);
            setInt32(outBuffer, 4, ret);
            for (i = 0; i < 8; i++) {
                setInt32(outBuffer, 8 + 4 * i, pulsePosRspData.lPos[i]);
            }
            sendRet = sendN(acceptHandle, outBuffer, 72, 0);
            if (sendRet != 72) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 72\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_CURRENT_FEEDBACK_PULSE_POS:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetClear = %d != 12\n", msgSize);
                return -1;
            }
            memset(&ctrlGrpSendData, 0, sizeof (ctrlGrpSendData));
            memset(&fbPulsePosRspData, 0, sizeof (fbPulsePosRspData));
            controlGroup = getInt32(inBuffer, 12);
            ctrlGrpSendData.sCtrlGrp = controlGroup;
            ret = mpGetFBPulsePos(&ctrlGrpSendData, &fbPulsePosRspData);
            setInt32(outBuffer, 0, 68);
            setInt32(outBuffer, 4, ret);
            for (i = 0; i < 8; i++) {
                setInt32(outBuffer, 8 + 4 * i, fbPulsePosRspData.lPos[i]);
            }
            sendRet = sendN(acceptHandle, outBuffer, 72, 0);
            if (sendRet != 72) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 72\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_DEG_POS_EX:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetClear = %d != 12\n", msgSize);
                return -1;
            }
            memset(&ctrlGrpSendData, 0, sizeof (ctrlGrpSendData));
            memset(&degPosRspDataEx, 0, sizeof (degPosRspDataEx));
            controlGroup = getInt32(inBuffer, 12);
            ctrlGrpSendData.sCtrlGrp = controlGroup;
            ret = mpGetDegPosEx(&ctrlGrpSendData, &degPosRspDataEx);
            setInt32(outBuffer, 0, 132);
            setInt32(outBuffer, 4, ret);
            for (i = 0; i < 8; i++) {
                setInt32(outBuffer, 8 + 4 * i, degPosRspDataEx.lDegPos[i]);
            }
            for (i = 0; i < 8; i++) {
                setInt32(outBuffer, 72 + 4 * i, degPosRspDataEx.lDegUnit[i]);
            }
            sendRet = sendN(acceptHandle, outBuffer, 136, 0);
            if (sendRet != 136) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 136\n", sendRet);
                return -1;
            }
            break;


        case SYS1_GET_SERVO_POWER:
            if (msgSize != 8) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetServoPower = %d != 8\n", msgSize);
                return -1;
            }
            memset(&servoPowerRspData, 0, sizeof (servoPowerRspData));
            ret = mpGetServoPower(&servoPowerRspData);
            setInt32(outBuffer, 0, 6);
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, servoPowerRspData.sServoPower);
            sendRet = sendN(acceptHandle, outBuffer, 10, 0);
            if (sendRet != 10) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 10\n", sendRet);
                return -1;
            }
            break;

        case SYS1_SET_SERVO_POWER:
            if (msgSize != 10) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpSetServoPower = %d != 12\n", msgSize);
                return -1;
            }
            memset(&servoPowerSendData, 0, sizeof (servoPowerSendData));
            memset(&stdRspData, 0, sizeof (stdRspData));
            servoPowerSendData.sServoPower = getInt16(inBuffer, 12);
            ret = mpSetServoPower(&servoPowerSendData, &stdRspData);
            setInt32(outBuffer, 0, 6);
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, stdRspData.err_no);
            sendRet = sendN(acceptHandle, outBuffer, 10, 0);
            if (sendRet != 10) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 136\n", sendRet);
                return -1;
            }
            break;


        case SYS1_READIO:
            num = getInt32(inBuffer, 12);
            if (num < 1 || num > 24) {
                fprintf(stderr, "tcpSvr: invalid num for mpReadIO num = %ld\n", num);
                return -1;
            }
            if (msgSize != 12 + (4 * num)) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpReadIO = %d != %ld for num = %ld\n", msgSize, 12 + (4 * num), num);
                return -1;
            }
            for (i = 0; i < num; i++) {
                ioInfo[i].ulAddr = getInt32(inBuffer, 16 + (4 * i));
            }
            memset(iorData, 0, sizeof (iorData));
            ret = mpReadIO(ioInfo, iorData, num);
            setInt32(outBuffer, 0, 4 + num * 2);
            setInt32(outBuffer, 4, ret);
            for (i = 0; i < num; i++) {
                setInt16(outBuffer, 8 + i * 2, iorData[i]);
            }
            sendRet = sendN(acceptHandle, outBuffer, 8 + num * 2, 0);
            if (sendRet != 8 + num * 2) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8 + num*4\n", sendRet);
                return -1;
            }
            break;

        case SYS1_WRITEIO:
            num = getInt32(inBuffer, 12);
            if (num < 1 || num > 24) {
                fprintf(stderr, "tcpSvr: invalid num for mpPutVarData num = %ld\n", num);
                return -1;
            }
            if (msgSize != 12 + (num * 8)) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpPutVarData = %d != %ld for num = %ld\n", msgSize, 12 + (num * 8), num);
                return -1;
            }
            for (i = 0; i < num; i++) {
                ioData[i].ulAddr = getInt32(inBuffer, 16 + (8 * i));
                ioData[i].ulValue = getInt32(inBuffer, 20 + (8 * i));
            }
            ret = mpWriteIO(ioData, num);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_MODE:
            if (msgSize != 8) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetMode = %d != 8\n", msgSize);
                return -1;
            }
            memset(&modeData, 0, sizeof (modeData));
            ret = mpGetMode(&modeData);
            setInt32(outBuffer, 0, 8);
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, modeData.sMode);
            setInt16(outBuffer, 10, modeData.sRemote);
            sendRet = sendN(acceptHandle, outBuffer, 12, 0);
            if (sendRet != 12) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 12\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_CYCLE:
            if (msgSize != 8) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetCycle = %d != 8\n", msgSize);
                return -1;
            }
            memset(&cycleData, 0, sizeof (cycleData));
            ret = mpGetCycle(&cycleData);
            setInt32(outBuffer, 0, 6);
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, cycleData.sCycle);
            sendRet = sendN(acceptHandle, outBuffer, 10, 0);
            if (sendRet != 10) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 10\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_ALARM_STATUS:
            if (msgSize != 8) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetAlarmStatus = %d != 8\n", msgSize);
                return -1;
            }
            memset(&alarmStatusData, 0, sizeof (alarmStatusData));
            ret = mpGetAlarmStatus(&alarmStatusData);
            setInt32(outBuffer, 0, 6);
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, alarmStatusData.sIsAlarm);
            sendRet = sendN(acceptHandle, outBuffer, 10, 0);
            if (sendRet != 10) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 10\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_ALARM_CODE:
            if (msgSize != 8) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetAlarmCode = %d != 8\n", msgSize);
                return -1;
            }
            memset(&alarmCodeData, 0, sizeof (alarmCodeData));
            ret = mpGetAlarmCode(&alarmCodeData);
            setInt32(outBuffer, 0, 10 + 4 * ((alarmCodeData.usAlarmNum > 4) ? 4 : alarmCodeData.usAlarmNum));
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, alarmCodeData.usErrorNo);
            setInt16(outBuffer, 10, alarmCodeData.usErrorData);
            setInt16(outBuffer, 12, alarmCodeData.usAlarmNum);
            for (i = 0; i < alarmCodeData.usAlarmNum && i < 4; i++) {
                setInt16(outBuffer, 14 + i * 4, alarmCodeData.AlarmData.usAlarmNo[i]);
                setInt16(outBuffer, 16 + i * 4, alarmCodeData.AlarmData.usAlarmData[i]);
            }
            sendRet = sendN(acceptHandle, outBuffer, 14 + 4 * ((alarmCodeData.usAlarmNum > 4) ? 4 : alarmCodeData.usAlarmNum), 0);
            if (sendRet != 14 + 4 * ((alarmCodeData.usAlarmNum > 4) ? 4 : alarmCodeData.usAlarmNum)) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 14 +4*((alarmCodeData.usAlarmNum>4)?4:alarmCodeData.usAlarmNum)\n", sendRet);
                return -1;
            }
            break;

        case SYS1_GET_RTC:
            if (msgSize != 8) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetAlarmCode = %d != 8\n", msgSize);
                return -1;
            }
            ret = mpGetRtc();
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;
        default:
            fprintf(stderr, "tcpSvr: invalid sys1 function type = %d\n", type);
            return -1;
    }
    return 0;
}

// Return 0 for success, anything else will be treated like a fatal error closing
// the connection.

int handleMotFunctionRequest(int acceptHandle, char *inBuffer, char *outBuffer, int type, int msgSize) {
    int32_t ret = -1;
    int32_t options = 0;
    int32_t controlGroup = 0;
    int32_t timeout = 0;
    int sendRet = 0;
    MP_TARGET target;
    MP_SPEED speed;
    int32_t grpNo = 0;
    int32_t aux = 0;
    int32_t coordType = 0;
    int32_t tool = 0;
    int32_t taskNo = 0;
    int recvId = 0;
    int i = 0;
    int millisPerTick = 0;
    int maxTimeout = 0;
    switch (type) {
        case MOT_START:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotStart = %d != 12\n", msgSize);
                return -1;
            }
            options = getInt32(inBuffer, 12);
            ret = mpMotStart(options);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_STOP:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotStop = %d != 12\n", msgSize);
                return -1;
            }
            options = getInt32(inBuffer, 12);
            ret = mpMotStop(options);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_TARGET_CLEAR:
            if (msgSize != 16) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetClear = %d != 16\n", msgSize);
                return -1;
            }
            controlGroup = getInt32(inBuffer, 12);
            options = getInt32(inBuffer, 16);
            ret = mpMotTargetClear(controlGroup, options);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_JOINT_TARGET_SEND:
            if (msgSize != 88) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetSend = %d != 88\n", msgSize);
                return -1;
            }
            memset(&target, 0, sizeof (target));
            controlGroup = getInt32(inBuffer, 12);
            target.id = getInt32(inBuffer, 16);
            target.intp = getInt32(inBuffer, 20);
            for (i = 0; i < 8 /* MP_GRP_AXES_NUM */; i++) {
                target.dst.joint[i] = getInt32(inBuffer, 24 + (i * 4));
            }
            for (i = 0; i < 8 /* MP_GRP_AXES_NUM */; i++) {
                target.aux.joint[i] = getInt32(inBuffer, 56 + (i * 4));
            }
            timeout = getInt32(inBuffer, 88);
            if (timeout == WAIT_FOREVER) {
                fprintf(stderr, "tcpSvr: invalid timeout for mpMotTargetSend = %d, WAIT_FOREVER not allowed.\n", timeout);
                return -1;
            }
            if (timeout != NO_WAIT) {
                millisPerTick = mpGetRtc();
                maxTimeout = 5000 / millisPerTick;
                if (timeout < 0 || timeout > maxTimeout) {
                    fprintf(stderr, "tcpSvr: invalid timeout for mpMotTargetSend = %d, millisPerTick = mpGetRtc() =%d, maxTimeout=5000/millisPerTick=%d\n",
                            timeout, millisPerTick, maxTimeout);
                    return -1;
                }
            }
            ret = mpMotTargetSend(controlGroup, &target, timeout);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_COORD_TARGET_SEND:
            if (msgSize != 88) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetSend = %d != 88\n", msgSize);
                return -1;
            }
            memset(&target, 0, sizeof (target));
            controlGroup = getInt32(inBuffer, 12);
            target.id = getInt32(inBuffer, 16);
            target.intp = getInt32(inBuffer, 20);
            target.dst.coord.x = getInt32(inBuffer, 24);
            target.dst.coord.y = getInt32(inBuffer, 28);
            target.dst.coord.z = getInt32(inBuffer, 32);
            target.dst.coord.rx = getInt32(inBuffer, 36);
            target.dst.coord.ry = getInt32(inBuffer, 40);
            target.dst.coord.rz = getInt32(inBuffer, 44);
            target.dst.coord.ex1 = getInt32(inBuffer, 48);
            target.dst.coord.ex2 = getInt32(inBuffer, 52);
            target.aux.coord.x = getInt32(inBuffer, 56);
            target.aux.coord.y = getInt32(inBuffer, 60);
            target.aux.coord.z = getInt32(inBuffer, 64);
            target.aux.coord.rx = getInt32(inBuffer, 68);
            target.aux.coord.ry = getInt32(inBuffer, 72);
            target.aux.coord.rz = getInt32(inBuffer, 76);
            target.aux.coord.ex1 = getInt32(inBuffer, 80);
            target.aux.coord.ex2 = getInt32(inBuffer, 84);
            timeout = getInt32(inBuffer, 88);
            if (timeout == WAIT_FOREVER) {
                fprintf(stderr, "tcpSvr: invalid timeout for mpMotTargetSend = %d, WAIT_FOREVER not allowed.\n", timeout);
                return -1;
            }
            if (timeout != NO_WAIT) {
                millisPerTick = mpGetRtc();
                maxTimeout = 5000 / millisPerTick;
                if (timeout < 0 || timeout > maxTimeout) {
                    fprintf(stderr, "tcpSvr: invalid timeout for mpMotTargetSend = %d, millisPerTick = mpGetRtc() =%d, maxTimeout=5000/millisPerTick=%d\n",
                            timeout, millisPerTick, maxTimeout);
                    return -1;
                }
            }
            ret = mpMotTargetSend(controlGroup, &target, timeout);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_TARGET_RECEIVE:
            if (msgSize != 24) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotTargetReceive = %d != 24\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            target.id = getInt32(inBuffer, 16);
            timeout = getInt32(inBuffer, 20);
            if (timeout == WAIT_FOREVER) {
                fprintf(stderr, "tcpSvr: invalid timeout for mpMotTargetReceive = %d, WAIT_FOREVER not allowed.\n", timeout);
                return -1;
            }
            if (timeout != NO_WAIT) {
                millisPerTick = mpGetRtc();
                maxTimeout = 5000 / millisPerTick;
                if (timeout < 0 || timeout > maxTimeout) {
                    fprintf(stderr, "tcpSvr: invalid timeout for mpMotTargetReceive = %d, millisPerTick = mpGetRtc() =%d, maxTimeout=5000/millisPerTick=%d\n",
                            timeout, millisPerTick, maxTimeout);
                    return -1;
                }
            }
            options = getInt32(inBuffer, 24);
            ret = mpMotTargetReceive(grpNo, target.id, &recvId, timeout, options);
            setInt32(outBuffer, 0, 8);
            setInt32(outBuffer, 4, ret);
            setInt32(outBuffer, 8, recvId);
            sendRet = sendN(acceptHandle, outBuffer, 12, 0);
            if (sendRet != 12) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 12\n", sendRet);
                return -1;
            }
            break;

        case MOT_SET_COORD:
            if (msgSize != 20) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotSetCoord = %d != 20\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            coordType = getInt32(inBuffer, 16);
            aux = getInt32(inBuffer, 20);
            ret = mpMotSetCoord(grpNo, coordType, aux);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_SET_TOOL:
            if (msgSize != 16) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotSetTool = %d != 16\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            tool = getInt32(inBuffer, 16);
            ret = mpMotSetTool(grpNo, tool);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_SET_SPEED:
            if (msgSize != 32) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotSetSpeed = %d != 32\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            speed.vj = getInt32(inBuffer, 16);
            speed.v = getInt32(inBuffer, 24);
            speed.vr = getInt32(inBuffer, 32);
            ret = mpMotSetSpeed(grpNo, &speed);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_SET_ORIGIN:
            if (msgSize != 16) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotSetOrigin = %d != 16\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            options = getInt32(inBuffer, 16);
            ret = mpMotSetOrigin(grpNo, options);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_SET_TASK:
            if (msgSize != 16) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotSetTask = %d != 16\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            taskNo = getInt32(inBuffer, 16);
            ret = mpMotSetTask(grpNo, taskNo);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_SET_SYNC:
            if (msgSize != 20) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotSetSync = %d != 20\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            aux = getInt32(inBuffer, 16);
            options = getInt32(inBuffer, 20);
            ret = mpMotSetSync(grpNo, aux, options);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case MOT_RESET_SYNC:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpMotResetSync = %d != 12\n", msgSize);
                return -1;
            }
            grpNo = getInt32(inBuffer, 12);
            ret = mpMotResetSync(grpNo);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        default:
            fprintf(stderr, "tcpSvr: invalid mot function type = %d\n", type);
            return -1;
    }
    return 0;
}

short lastExtensionId = -1;

int handleExFileFunctionRequest(int acceptHandle, char *inBuffer, char *outBuffer, int type, int msgSize) {
    int32_t ret = -1;
    int32_t index = -1;
    int32_t ramDriveId = -1;
    int32_t fileNameOffset = -1;
    int32_t fd = -1;

    int sendRet = 0;
    int namelen = 0;
    short extensionId = -1;
    MP_FILE_NAME_SEND_DATA fileNameSendData;
    MP_GET_JOBLIST_RSP_DATA jobListData;

    switch (type) {

        case EX_FILE_CTRL_GET_FILE_COUNT:
            if (msgSize != 10) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpGetFileCount = %d != 10\n", msgSize);
                return -1;
            }
            extensionId = getInt16(inBuffer, 12);
            if (extensionId < 1 || extensionId > 2) {
                fprintf(stderr, "tcpSvr: invalid extensionId for mpGetFileCount = %d  (must be 1 or 2)\n", extensionId);
                return -1;
            }
            lastExtensionId = -1;
            ret = mpRefreshFileList(extensionId);
            if (ret != 0) {
                setInt32(outBuffer, 0, 4);
                setInt32(outBuffer, 4, ret);
                sendRet = sendN(acceptHandle, outBuffer, 8, 0);
                if (sendRet != 8) {
                    fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                    return -1;
                }
                return 0;
            }
            lastExtensionId = extensionId;
            ret = mpGetFileCount();
            setInt32(outBuffer, 0, 8);
            setInt32(outBuffer, 4, 0);
            setInt32(outBuffer, 8, ret);
            sendRet = sendN(acceptHandle, outBuffer, 12, 0);
            if (sendRet != 12) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 12\n", sendRet);
                return -1;
            }
            break;

        case EX_FILE_CTRL_GET_FILE_NAME:
            extensionId = getInt16(inBuffer, 12);
            if (extensionId < 1 || extensionId > 2) {
                fprintf(stderr, "tcpSvr: invalid extensionId for mpGetFileName = %d  (must be 1 or 2)\n", extensionId);
                return -1;
            }
            if (extensionId != lastExtensionId) {
                lastExtensionId = -1;
                ret = mpRefreshFileList(extensionId);
                if (ret != 0) {
                    setInt32(outBuffer, 0, 4);
                    setInt32(outBuffer, 4, ret);
                    lastExtensionId = -1;
                    sendRet = sendN(acceptHandle, outBuffer, 8, 0);
                    if (sendRet != 8) {
                        fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                        return -1;
                    }
                    return 0;
                }
            }
            index = getInt32(inBuffer, 14);
            ret = mpGetFileName(index, outBuffer + 12);
            namelen = strlen(outBuffer + 12);
            setInt32(outBuffer, 0, 8 + namelen + 1);
            setInt32(outBuffer, 4, 0);
            setInt32(outBuffer, 8, ret);
            sendRet = sendN(acceptHandle, outBuffer, 12 + namelen + 1, 0);
            if (sendRet != 12 + namelen + 1) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 12+namelen+1\n", sendRet);
                return -1;
            }
            break;

        case EX_FILE_CTRL_LOAD_FILE:
            ramDriveId = getInt32(inBuffer, 12);
            if (ramDriveId < 1 || ramDriveId > 2) {
                fprintf(stderr, "tcpSvr: invalid ramDriveId for mpLoadFile = %d  (must be 1 or 2)\n", ramDriveId);
                return -1;
            }
            fileNameOffset = getInt32(inBuffer, 16);
            if (fileNameOffset < 20 || fileNameOffset > (BUFF_MAX - 21)) {
                fprintf(stderr, "tcpSvr: invalid fileNameOffset for mpLoadFile = %d  \n", fileNameOffset);
                return -1;
            }
            ret = mpLoadFile(ramDriveId, inBuffer + 20, inBuffer + fileNameOffset);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case EX_FILE_CTRL_SAVE_FILE:
            ramDriveId = getInt32(inBuffer, 12);
            if (ramDriveId < 1 || ramDriveId > 2) {
                fprintf(stderr, "tcpSvr: invalid ramDriveId for mpSaveFile = %d  (must be 1 or 2)\n", ramDriveId);
                return -1;
            }
            fileNameOffset = getInt32(inBuffer, 16);
            if (fileNameOffset < 20 || fileNameOffset > (BUFF_MAX - 21)) {
                fprintf(stderr, "tcpSvr: invalid fileNameOffset for mpSaveFile = %d  \n", fileNameOffset);
                return -1;
            }
            ret = mpSaveFile(ramDriveId, inBuffer + 20, inBuffer + fileNameOffset);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case EX_FILE_CTRL_FD_READ_FILE:
            fd = getInt32(inBuffer, 12);
            if (fd == -99) {
                fd = acceptHandle;
            }
            if (fd < 1) {
                fprintf(stderr, "tcpSvr: invalid fd for mpFdReadFile = %d\n", ramDriveId);
                return -1;
            }
            memset(&fileNameSendData, 0, sizeof (fileNameSendData));
            strcpy(fileNameSendData.cFileName, inBuffer + 16);
            ret = mpFdReadFile(fd, &fileNameSendData);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case EX_FILE_CTRL_FD_WRITE_FILE:
            fd = getInt32(inBuffer, 12);
            if (fd == -99) {
                fd = acceptHandle;
            }
            if (fd < 1) {
                fprintf(stderr, "tcpSvr: invalid fd for mpFdWriteFile = %d\n", ramDriveId);
                return -1;
            }
            memset(&fileNameSendData, 0, sizeof (fileNameSendData));
            strcpy(fileNameSendData.cFileName, inBuffer + 16);
            ret = mpFdWriteFile(fd, &fileNameSendData);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case EX_FILE_CTRL_FD_GET_JOB_LIST:
            fd = getInt32(inBuffer, 12);
            if (fd == -99) {
                fd = acceptHandle;
            }
            if (fd < 1) {
                fprintf(stderr, "tcpSvr: invalid fd for mpFdGetJobList = %d  (must be 1 or 2)\n", ramDriveId);
                return -1;
            }
            memset(&jobListData, 0, sizeof (jobListData));
            ret = mpFdGetJobList(fd, &jobListData);
            setInt32(outBuffer, 0, 10);
            setInt32(outBuffer, 4, ret);
            setInt16(outBuffer, 8, jobListData.err_no);
            setInt16(outBuffer, 10, jobListData.uIsEndFlag);
            setInt16(outBuffer, 12, jobListData.uListDataNum);
            sendRet = sendN(acceptHandle, outBuffer, 14, 0);
            if (sendRet != 14) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 14\n", sendRet);
                return -1;
            }
            break;

        default:
            fprintf(stderr, "tcpSvr: invalid file function type = %d\n", type);
            return -1;
    }
    return 0;
}

int handleFileFunctionRequest(int acceptHandle, char *inBuffer, char *outBuffer, int type, int msgSize) {
    int32_t ret = -1;
    int32_t mode = -1;
    int32_t flags = -1;
    int32_t fd = -1;
    int32_t maxBytes = -1;

    int sendRet = 0;
   
    switch (type) {

        case FILE_CTRL_OPEN:
            flags = getInt32(inBuffer, 12);
            mode = getInt32(inBuffer, 16);
            ret = mpOpen(inBuffer + 20, flags, mode);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case FILE_CTRL_CREATE:
            flags = getInt32(inBuffer, 12);
            ret = mpCreate(inBuffer + 16, flags);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case FILE_CTRL_CLOSE:
            if (msgSize != 12) {
                fprintf(stderr, "tcpSvr: invalid msgSize for mpClose = %d != 12\n", msgSize);
                return -1;
            }
            fd = getInt32(inBuffer, 12);
            if (fd < 1) {
                fprintf(stderr, "tcpSvr: invalid fd for mpRead = %d\n", fd);
                return -1;
            }
            ret = mpClose(fd);
            setInt32(outBuffer, 0, 4);
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8, 0);
            if (sendRet != 8) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8\n", sendRet);
                return -1;
            }
            break;

        case FILE_CTRL_READ:
            fd = getInt32(inBuffer, 12);
            if (fd < 1) {
                fprintf(stderr, "tcpSvr: invalid fd for mpRead = %d\n", fd);
                return -1;
            }
            maxBytes = getInt32(inBuffer, 16);
            if (maxBytes < 1 || maxBytes >= (BUFF_MAX - 8)) {
                fprintf(stderr, "tcpSvr: invalid maxBytes for mpRead = %d max = %d\n", maxBytes, (BUFF_MAX - 8));
                return -1;
            }
            ret = mpRead(fd, outBuffer + 8, maxBytes);
            setInt32(outBuffer, 0, 4 + (ret > 0 ? ret : 0));
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8 + (ret > 0 ? ret : 0), 0);
            if (sendRet != 8 + (ret > 0 ? ret : 0)) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8 + (ret > 0?ret:0)\n", sendRet);
                return -1;
            }
            break;


        case FILE_CTRL_WRITE:
            fd = getInt32(inBuffer, 12);
            if (fd < 1) {
                fprintf(stderr, "tcpSvr: invalid fd for mpRead = %d\n", fd);
                return -1;
            }
            maxBytes = getInt32(inBuffer, 16);
            if (maxBytes < 1 || maxBytes >= (BUFF_MAX - 8)) {
                fprintf(stderr, "tcpSvr: invalid maxBytes for mpRead = %d max = %d\n", maxBytes, (BUFF_MAX - 8));
                return -1;
            }
            ret = mpWrite(fd, inBuffer + 20, maxBytes);
            setInt32(outBuffer, 0, 4 + (ret > 0 ? ret : 0));
            setInt32(outBuffer, 4, ret);
            sendRet = sendN(acceptHandle, outBuffer, 8 + (ret > 0 ? ret : 0), 0);
            if (sendRet != 8 + (ret > 0 ? ret : 0)) {
                fprintf(stderr, "tcpSvr: sendRet = %d != 8 + (ret > 0?ret:0)\n", sendRet);
                return -1;
            }
            break;

        default:
            fprintf(stderr, "tcpSvr: invalid file function type = %d\n", type);
            return -1;
    }
    return 0;
}

static char inBuffer[BUFF_MAX + 1];
static char outBuffer[BUFF_MAX + 1];

int handleSingleConnection(int acceptHandle) {

    int32_t count = 0;
    int32_t group = 0;
    int32_t type = 0;
    int failed = 0;
    int bytesRecv;
    int32_t msgSize;

    memset(inBuffer, 0, BUFF_MAX + 1);
    memset(outBuffer, 0, BUFF_MAX + 1);
    bytesRecv = recvN(acceptHandle, inBuffer, 4, 0);
    if (bytesRecv != 4) {
        failed = 1;
        return failed;
    }

    msgSize = getInt32(inBuffer, 0);

    if (msgSize < 8 || msgSize >= (BUFF_MAX - 4)) {
        printf("tcpSvr: Invalid msgSize\n");
        failed = 1;
        return failed;
    }

    bytesRecv = recvN(acceptHandle, inBuffer + 4, (int) msgSize, 0);

    if (bytesRecv != msgSize) {
        failed = 1;
        return failed;
    }
    group = getInt32(inBuffer, 4);
    type = getInt32(inBuffer, 8);
    count++;

    switch (group) {
        case MOT_FUNCTION_GROUP:
            failed = handleMotFunctionRequest(acceptHandle, inBuffer, outBuffer, type, msgSize);
            break;

        case SYS1_FUNCTION_GROUP:
            failed = handleSys1FunctionRequest(acceptHandle, inBuffer, outBuffer, type, msgSize);
            break;

        case FILE_CTRL_FUNCTION_GROUP:
            failed = handleFileFunctionRequest(acceptHandle, inBuffer, outBuffer, type, msgSize);
            break;

        case EX_FILE_CTRL_FUNCTION_GROUP:
            failed = handleExFileFunctionRequest(acceptHandle, inBuffer, outBuffer, type, msgSize);
            break;

        default:
            fprintf(stderr, "tcpSvr: unrecognized group =%d\n", group);
            failed = 1;
            break;
    }
    return failed;
    /*
    printf("tcpSvr: msgSize=%d\n", msgSize);
    printf("tcpSvr: group=%d\n", group);
    printf("tcpSvr: type=%d\n", type);
    printf("tcpSvr: count=%d\n", count);
    printf("tcpSvr: Closing acceptHandle=%d\n", acceptHandle);
    mpClose(acceptHandle);
    free(inBuffer);
    free(outBuffer);
     */
}



