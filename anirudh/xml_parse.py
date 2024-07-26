import xml.etree.ElementTree as ET

tree = ET.parse("xml_files/crcl_programs/fanucStackMoveRepeat3.xml")
root = tree.getroot()

def parse_xml_file(xml_file_name, mode="raw"):
    print("enter q to quit")
    tree = ET.parse(xml_file_name)
    root = tree.getroot()

    cmd_num = -1

    print(f"\n\n\n len root: {len(root)}\n\n\n")


    if mode == "raw":
        for i in range(len(root)):
            cmd_num += 1
            if input(f"[press q to quit] --- command number: {cmd_num} --- ") == "q":
                print("---\nquitting\n---")
                break
            child = root[i]
            # print(child.tag, child.attrib)
            ET.dump(child)

    elif mode == "parsed":
        for i in range(len(root)):
            cmd_num += 1
            if input(f"[press q to quit] --- command number: {cmd_num} --- ") == "q":
                print("---\nquitting\n---")
                break
            child = root[i]
            print(f"command number: {cmd_num}")
            print(f"tag: {child.tag}")
            print(f"attrib: {child.attrib}")
            print(f"len: {len(child)}")
            for j in range(len(child)):
                grandchild = child[j]
                print(f"grandchild tag: {grandchild.tag}")
                print(f"grandchild attrib: {grandchild.attrib}")
                print(f"grandchild text: {grandchild.text}")
                print(f"grandchild tail: {grandchild.tail}")
                print(f"len grandchild: {len(grandchild)}")
    print("")


    

# print(root[0].__dir__())

# print(root[0].tag, root[0].tail)
# print(root[2].tag)
# print(root[2].attrib)

# print(root[3][2].tag, root[3][2].attrib)

# print(ET.dump(root[2]))


# for child in root:
#     print(child.tag, child.attrib)


# for v in dir(root):
#     print(v)

def test():
    x = input("> ")
    print("type:",type(x))
    print("value:",x)
    print("length:",len(x))
    if x == "":
        print("empty string")

if __name__ == "__main__":

    # mode = "raw"
    mode = "parsed"

    parse_xml_file("xml_files/crcl_programs/fanucStackMoveRepeat3.xml", mode=mode)
    
    # test()

