import xml.etree.ElementTree as ET
tree = ET.parse("xml_files/fanucStackMoveRepeat3.xml")
root = tree.getroot()

# print(root[0].__dir__())

print(root[0].tag, root[0].tail)
print(root[2].tag)
print(root[2].attrib)

print(root[3][2].tag, root[3][2].attrib)

print(ET.dump(root[2]))


# for child in root:
#     print(child.tag, child.attrib)


# for v in dir(root):
#     print(v)