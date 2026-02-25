@echo off
javac -cp "gson-2.13.2.jar" -d out client/*.java messages/*.java messages/requests/*.java messages/responses/*.java 
java -cp "out;gson-2.13.2.jar" client.ClientMain