The Turbosoft portal provides access to Geosoft's Turbosoft system (http://www.isi.edu/ikcap/geosoft/) by allowing you to browse, and add models to the repository. 

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

