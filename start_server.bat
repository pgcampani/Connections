@echo off
javac -cp "gson-2.13.2.jar" -d out server/game/* server/*.java messages/*.java messages/responses/*.java messages/requests/*.java
java -cp "out;gson-2.13.2.jar" server.ServerMain