@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
"C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass="com.esprit.TestFxml"
