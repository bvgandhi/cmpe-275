# cmpe-275
Dropbox prototype project.

Installation Instructions:
1.	Extract the code form zip folder.
2.	Copy the code onto different machines (nodes).
3.	Connect all the nodes to a switch.
4.	Find the IP address of all the nodes.
5.	Change the IP address of all the nodes in route.conf file in runtime folder.
6.	Go to build.xml location from cmd and execute following command:
ant build
7.	Repeat step 6 on all nodes and client machine.
8.	Execute ant server1 from node 1, server2, server5, server6 and repeat same step for other nodes.
9.	Execute ant client form client machine.(Change the below params in build .xml)
To Store file in System ( arg1:PUT, arg2:filename, arg3:path from where to read file)
<arg value="PUT" />
<arg value="protoc-2.6.1-win32.zip" />
<arg value="C:\Users\BG\Desktop\protoc-2.6.1-win32.zip" />

To Retrieve file from system (arg1:GET, arg2:filename, arg3:path where the retrieved file will be stored)
<arg value="GET" />
<arg value="protoc-2.6.1-win32.zip"/>
<arg value="F:\softwares\protoc-2.6.1-win32.zip"/>



