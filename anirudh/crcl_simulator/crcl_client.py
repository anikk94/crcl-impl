#!/usr/bin/env python3
### 2024-07-24
## doc
# this is a crcl client to the crcl server that is part of a crcl python simulation
# git repository of the crcl python simulator: https://github.com/anikk94/crcl-impl
## 

import getopt
import sys
from configparser import ConfigParser
import socket
import threading
import xml.etree.ElementTree as ET

from crcl_python3.crcl import *

INIFILE = ""
ROBOT_PORT = ""
ROBOT_HOST = ""
GRIPPER_PORT = ""
GRIPPER_HOST = ""
DEBUG = False

robotcommandid = 0
robotstatusid = 0
robotcommandstate = 0
robotpose = 0


def printHelp():
    print("HELP!")

def printStatus():
    global robotcommandid, robotstatusid, robotcommandstate, robotpose
    print(f"robot: {robotcommandid}, {robotstatusid}, {robotcommandstate}, {robotpose}")
    print("gripper: ")

def xmlPretty(xmlET: ET.ElementTree):
    ET.indent(xmlET)
    xmlstr = ET.tostring(xmlET.getroot(), encoding='utf-8')
    print(xmlstr.decode('utf-8'))

def except_info():
    exc_type, exc_value = sys.exc_info()[:2]
    return str(exc_type.__name__) + ": " + str(exc_value)

def robot_reader(conn):
    global DEBUG, robotcommandid, robotstatusid, robotcommandstate, robotpose
    size = 1024
    while True:
        try:
            data = conn.recv(size)
        except: 
            break
        if not data: 
            break
        try:
            # print(f"robot_reader: {data}")
            pass
        except:
            print(f"crcl_client: robot_reader: {except_info()}")
    print("robot_reader: connection closed")
    conn.close()

def gripper_reader(conn):
    global DEBUG, grippercommandid, gripperstatusid, grippercommandstate
    global gripperstatus
    size = 1024
    while True:
        try:
            data = conn.recv(size)
        except: 
            break
        if not data:
            break
        # try:
        #     tree = ET.parse(StringIO(data.rstrip(' \t\n\r\0')))
        #     root = tree.getroot()
        #     if root.tag == "CRCLStatus":
        #         for child in root:
        #             if child.tag == "CommandStatus":
        #             elif


if __name__ == "__main__":
    print("crcl_client")



    try:
        opts, args = getopt.getopt(sys.argv[1:], "i:r:R:g:G:t:Xd?",["infile=", "robot="])
    except getopt.GetoptError as err:
        print("crcl_client:", str(err))
        sys.exit(1)

    # option flags (o) and flag values (a)
    for o, a in opts:
        if o in ("-i", "--inifile"):
            INIFILE = a
        elif o in ("-r", "--robot"):
            ROBOT_PORT = a
        elif o in ("-R", "--robothost"):
            ROBOT_HOST = a
        elif o in ("-g", "--gripper"):
            GRIPPER_PORT = a
        elif o in ("-G", "--gripperhost"):
            GRIPPER_HOST = a
        elif o in ("-d", "--debug"):
            DEBUG = True
        elif o in ("-?", "--help"):
            printHelp()
            sys.exit(0)
        
    INIFILE = ""

    if ROBOT_PORT == "":
        print("crcl_client: no robot port provided")
        sys.exit(1)
    
    if ROBOT_HOST == "":
        ROBOT_HOST = "localhost"

    if GRIPPER_PORT == "":
        print("crcl_client: no gripper port provided")
        sys.exit(1)

    if GRIPPER_HOST == "":
        GRIPPER_HOST = "localhost"

    if DEBUG:
        print("crcl_client: robot_host {}, port {}".format(ROBOT_HOST, ROBOT_PORT))
        print("crcl_client: gripper_host {}, port {}".format(GRIPPER_HOST, GRIPPER_PORT))

    try:
        robot_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        robot_socket.connect((ROBOT_HOST, int(ROBOT_PORT)))
    except IOError as err:
        print(f"crcl_client: cant connect to robot conroller {ROBOT_HOST}:{ROBOT_PORT} - {err}")
        sys.exit(1)

    rt = threading.Thread(target=robot_reader, args=(robot_socket,))
    rt.daemon = True
    rt.start()

    try:
        gripper_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        gripper_socket.connect((GRIPPER_HOST, int(GRIPPER_PORT)))
    except IOError as err:
        print(f"crcl_client: cant connect to gripper conroller {GRIPPER_HOST}:{GRIPPER_PORT} - {err}")
        sys.exit(1)

    gt = threading.Thread(target=gripper_reader, args=(gripper_socket,))
    gt.daemon = True
    gt.start()

    # cid -> command_id?
    robot_cid = 100
    gripper_cid = 200
    ## not needed
    # done = False

    # not needed
    # while not done:
    while True:
        try:
            sys.stdout.write("> ")
            line = sys.stdin.readline()
            
            # this version doesn't work for blank inputs 
            # line = input("> ")
        except KeyboardInterrupt:
            break
        if not line:
            break

        toks = line.split()
        if len(toks) == 0:
            printStatus()
            continue

        cmd = toks[0]
        print(cmd)

        if cmd == "q": break

        elif cmd == "?": printHelp()

        elif cmd == "#": continue

        elif cmd == "init":
            print("user_input: init")
            gripper_cid += 1
            m = InitCanonType(gripper_cid)
            gripper_socket.send(str(m).encode('utf-8'))
            robot_cid += 1
            m = InitCanonType(robot_cid)
            xmlPretty(m.tree())
            robot_socket.send(str(m).encode('utf-8'))

        elif cmd == "end":
            gripper_cid += 1
            m = EndCanonType(gripper_cid)
            gripper_socket.send(str(m).encode('utf-8'))
            robot_cid += 1
            m = EndCanonType(robot_cid)
            robot_socket.send(str(m).encode('utf-8'))

        elif cmd == "close":
            try:
                name = toks[1]
            except:
                print("need a gripper name")
                continue
            gripper_cid += 1
            m = CloseToolChangerType(gripper_cid, Name=name)
            gripper_socket.send(str(m).encode('utf-8'))

        elif cmd == "move":
            try:
                x, y, z, xi, xj, xk, zi, zj, zk = map(float, toks[1:10])
            except:
                print("need x y z xi xj xk zi zj zk")
                continue
            robot_cid += 1
            m = MoveToType(robot_cid, False, PoseType(x, y, z), unit(VectorType(xi, xj, xk)), unit(VectorType(zi, zj, zk)))
            robot_socket.send(str(m).encode('utf-8'))

        else: print(f"? : {line}")
    