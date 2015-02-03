The Turbosoft portal provides access to Geosoft's Turbosoft system (http://www.geosoft-earthcube.org/) by allowing you to browse, and add models to the repository. 

Demo here:
http://www.geosoft-earthcube.org/turbosoft-portal/

Installation
=============
Requirements
------------
1. Java JDK 1.6+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
2. Maven 2/3 (http://maven.apache.org/)
3. Tomcat 6+ (http://tomcat.apache.org/)

Installation
-------------
1. $ mvn clean install
	- This will create a turbosoft-portal-[version].war file in portal/target

2. Move the war file to a Servlet container (Tomcat)
	- $ mv /path/to/turbosoft-portal-<version>.war /path/to/tomcat/webapps/turbosoft-portal.war

3. Setup users in /path/to/tomcat/conf/tomcat-users.xml
	- Turbosoft users need to have the "TurbosoftUser" role

4. Start tomcat
	- $ /path/to/tomcat/bin/startup.sh

5. Login to http://[your-server-name]:8080/turbosoft-portal

6. After first Login, go to $HOME/.turbosoft directory and open portal.properties
	- Check that the server name is set correctly
	- (Optional) Change any other settings if needed


Use Cases
=========
1. I just finished my thesis, where do I put my software so it does not get lost?
2. I am looking for a model for reaeration, how do I find one?
3. I have my code on GitHub, what else do I need to do to help people find and use it?
4. I found code for a model, does it work as advertised?  Is it the best model/code I can use?
5. I found code for a model, how do people usually visualize the results?  Is there any software out there to put the data in the format the model uses?
6. How do I track who uses my code?  How do I get credit for this code?
7. I found a model, how do I find data for the model to complement the data I have?
8. I am writing a paper, how do I document my experiments, make it reproducible, etc
9. My organization would like to have an inventory of its geosciences software
10. I run a data center, what models/communities use my data
