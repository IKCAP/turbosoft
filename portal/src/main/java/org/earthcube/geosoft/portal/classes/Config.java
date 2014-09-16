package org.earthcube.geosoft.portal.classes;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;

public class Config {
  // The Portal configuration properties file. Order of checking:
  // 1. Check "config.file" servlet context parameter
  // 2. Check ${user.home}/.turbosoft/portal.properties if ${user.home} is
  // present
  // 3. Check /etc/wings/portal.properties
  private String configFile;

  // The following are loaded from the config file
  private String storageDirectory;
  private String tdbDirectory;
  private String dotFile;
  private String serverUrl;
  private String softwareOntologyUrl;
  private String dataOntologyUrl;
  private String communityOntologyUrl;
  private String resourceOntologyUrl;
  private HashMap<String, String> miscProperties;
  
  private String defaultStorageDir = ".turbosoft";
  private String ontdirurl = "http://www.isi.edu/ikcap/geosoft/ontology";
  
  private HashMap<String, String> standardNamesOntologies;

  // Some hardcoded values (TODO: override from config file)
  private String usersRelativeDir = "users";
  private String exportServletPath = "/export";
  private boolean isSandboxed = false;

  // The following are set from the "request" variable
  private String userId;
  private String sessionId;
  private String contextRootPath;
  private String scriptPath;

  // This following are user/domain specific properties
  private String userUrl;
  private String userDir;

  static String fsep = File.separator;
  static String usep = "/";

  public Config(HttpServletRequest request) {
    // Initialize portal config
    this.initializePortalConfig(request);

    // Initialize user config
    this.initializeUserConfig(request);
  }

  private void initializeUserConfig(HttpServletRequest request) {
    this.userId = request.getRemoteUser();
    if (this.userId == null)
      return;

    this.sessionId = request.getSession().getId();
    this.userUrl = serverUrl + contextRootPath + exportServletPath; 
    //+ "/" + usersRelativeDir + "/" + userId;
    this.userDir = storageDirectory;
    // + File.separator + usersRelativeDir + File.separator + userId;

    // Create userDir (if it doesn't exist)
    File uf = new File(this.userDir);
    if (!uf.exists() && !uf.mkdirs())
      System.err.println("Cannot create user directory : "
          + uf.getAbsolutePath());
  }

  private void initializePortalConfig(HttpServletRequest request) {
    this.contextRootPath = request.getContextPath();
    this.scriptPath = this.contextRootPath + request.getServletPath();

    PropertyListConfiguration serverConfig = getPortalConfiguration(request);
    this.storageDirectory = serverConfig.getString("storage.local");
    this.tdbDirectory = serverConfig.getString("storage.tdb");
    this.serverUrl = serverConfig.getString("server");
    this.softwareOntologyUrl = serverConfig.getString("ontology.software");
    this.dataOntologyUrl = serverConfig.getString("ontology.data");
    this.communityOntologyUrl = serverConfig.getString("ontology.community");
    this.resourceOntologyUrl = serverConfig.getString("ontology.resource");
    if(this.communityOntologyUrl == null)
      this.communityOntologyUrl = ontdirurl + "/community.owl";
    
    this.standardNamesOntologies = new HashMap<String, String>();
    SubnodeConfiguration snconfig = serverConfig.configurationAt("ontology.standard_names");
    
    for(@SuppressWarnings("unchecked")
    Iterator<String> it = snconfig.getKeys(); it.hasNext(); ) {
      String snrepo = it.next();
      String snurl = snconfig.getString(snrepo);
      this.standardNamesOntologies.put(snrepo,  snurl);
    }
    
    this.miscProperties = new HashMap<String, String>();
    if(serverConfig.containsKey("misc")) {
    SubnodeConfiguration misc = serverConfig.configurationAt("misc");
      for(@SuppressWarnings("unchecked")
      Iterator<String> it = misc.getKeys(); it.hasNext(); ) {
        String key = it.next();
        String value = misc.getString(key);
        this.miscProperties.put(key,  value);
      }
    }
  }

  private PropertyListConfiguration getPortalConfiguration(
      HttpServletRequest request) {
    ServletContext app = request.getSession().getServletContext();
    this.configFile = app.getInitParameter("config.file");
    if (this.configFile == null) {
      String home = System.getProperty("user.home");
      if (home != null && !home.equals(""))
        this.configFile = home + File.separator + this.defaultStorageDir
            + File.separator + "portal.properties";
      else
        this.configFile = "/etc/wings/portal.properties";
    }
    // Create configFile if it doesn't exist (portal.properties)
    File cfile = new File(this.configFile);
    if (!cfile.exists()) {
      if (!cfile.getParentFile().mkdirs()) {
        System.err.println("Cannot create config file directory : "
            + cfile.getParent());
        return null;
      }
      createDefaultPortalConfig(request);
    }

    // Load properties from configFile
    PropertyListConfiguration props = new PropertyListConfiguration();
    try {
      props.load(this.configFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return props;
  }

  private void createDefaultPortalConfig(HttpServletRequest request) {
    String server = request.getScheme() + "://" + request.getServerName() + ":"
        + request.getServerPort();
    String storageDir = null;
    String home = System.getProperty("user.home");
    if (home != null && !home.equals(""))
      storageDir = home + File.separator + this.defaultStorageDir
          + File.separator + "storage";
    else
      storageDir = System.getProperty("java.io.tmpdir") + File.separator
          + "wings" + File.separator + "storage";
    if (!new File(storageDir).mkdirs())
      System.err.println("Cannot create storage directory: " + storageDir);

    PropertyListConfiguration config = new PropertyListConfiguration();
    config.addProperty("storage.local", storageDir);
    config.addProperty("storage.tdb", storageDir + File.separator + "TDB");
    config.addProperty("server", server);
    
    config.addProperty("ontology.software", ontdirurl + "/software.owl");
    config.addProperty("ontology.data", ontdirurl + "/data.owl");
    config.addProperty("ontology.resource", ontdirurl + "/resource.owl");
    config.addProperty("ontology.community", ontdirurl + "/community.owl");
    
    config.addProperty("misc.tikaUrl", "http://[Tika Server]/detect/stream");
    config.addProperty("misc.solrUrl", "http://[Solr Server]/solr");
    config.addProperty("misc.dratHome", "/path/to/drat");
    
    config.addProperty("ontology.standard_names.CSDMS", ontdirurl + "/CSDMS.owl");

    try {
      config.save(this.configFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Return Properties that are currently used by catalogs & planners
  public Properties getProperties() {
    Properties props = new Properties();
    props.setProperty("lib.software.url", this.userUrl + usep + "software/library.owl");
    props.setProperty("lib.community.url", this.userUrl + usep + "community/library.owl");
    props.setProperty("lib.domain.data.url", this.userUrl + usep + "data/library.owl");
    props.setProperty("ont.domain.data.url", this.userUrl + usep + "data/ontology.owl");
    props.setProperty("lib.domain.data.storage", this.userDir + fsep + "data");
    
    props.setProperty("rules.software.url", ontdirurl + "/software.rules");
    props.setProperty("ont.software.url", this.softwareOntologyUrl);
    props.setProperty("ont.data.url", this.dataOntologyUrl);
    if(this.resourceOntologyUrl != null)
      props.setProperty("ont.resource.url", this.resourceOntologyUrl);
    props.setProperty("ont.community.url", this.communityOntologyUrl);

    props.setProperty("tdb.repository.dir", this.getTripleStoreDir());
    return props;
  }

  public String getSoftwareOntologyUrl() {
    return this.softwareOntologyUrl;
  }

  public void setSoftwareOntologyUrl(String softwareOntologyUrl) {
    this.softwareOntologyUrl = softwareOntologyUrl;
  }

  public String getDataOntologyUrl() {
    return this.dataOntologyUrl;
  }

  public void setDataOntologyUrl(String dataOntologyUrl) {
    this.dataOntologyUrl = dataOntologyUrl;
  }

  public String getCommunityOnologyUrl() {
    return this.communityOntologyUrl;
  }

  public void setCommunityOnologyUrl(String communityOnologyUrl) {
    this.communityOntologyUrl = communityOnologyUrl;
  }
  
  public String getResourceOntologyUrl() {
    return resourceOntologyUrl;
  }

  public void setResourceOntologyUrl(String resourceOntologyUrl) {
    this.resourceOntologyUrl = resourceOntologyUrl;
  }

  public String getConfigFile() {
    return configFile;
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public String getUsersRelativeDir() {
    return usersRelativeDir;
  }

  public void setUsersRelativeDir(String usersRelativeDir) {
    this.usersRelativeDir = usersRelativeDir;
  }

  public String getExportServletPath() {
    return exportServletPath;
  }

  public void setExportServletPath(String exportServletPath) {
    this.exportServletPath = exportServletPath;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserDir() {
    return userDir;
  }

  public void setUserDir(String userDir) {
    this.userDir = userDir;
  }

  public void setUserUrl(String userUrl) {
    this.userUrl = userUrl;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getContextRootPath() {
    return contextRootPath;
  }

  public void setContextRootPath(String root) {
    this.contextRootPath = root;
  }

  public String getScriptPath() {
    return scriptPath;
  }

  public void setScriptPath(String scriptPath) {
    this.scriptPath = scriptPath;
  }

  public String getUserUrl() {
    return userUrl;
  }

  public String getTripleStoreDir() {
    return tdbDirectory;
  }

  public void setTripleStoreDir(String tripleStoreDir) {
    this.tdbDirectory = tripleStoreDir;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public boolean isSandboxed() {
    return isSandboxed;
  }

  public void setSandboxed(boolean isSandboxed) {
    this.isSandboxed = isSandboxed;
  }

  public String getDotFile() {
    return dotFile;
  }

  public void setDotFile(String dotFile) {
    this.dotFile = dotFile;
  }

  public String getStorageDirectory() {
    return storageDirectory;
  }

  public void setStorageDirectory(String storageDirectory) {
    this.storageDirectory = storageDirectory;
  }

  public HashMap<String, String> getStandardNamesOntologies() {
    return standardNamesOntologies;
  }

  public void setStandardNamesOntologies(HashMap<String, String> standardNamesOntologies) {
    this.standardNamesOntologies = standardNamesOntologies;
  }

  public HashMap<String, String> getMiscProperties() {
    return miscProperties;
  }

  public String getMiscPropertyValue(String key) {
    return miscProperties.get(key);
  }
  
  public void setMiscProperties(HashMap<String, String> miscProperties) {
    this.miscProperties = miscProperties;
  }
  
  public void addMiscPropertyValue(String key, String value) {
    this.miscProperties.put(key, value);
  }
}
