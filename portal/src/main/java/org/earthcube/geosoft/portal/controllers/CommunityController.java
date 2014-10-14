package org.earthcube.geosoft.portal.controllers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.earthcube.geosoft.community.CommunityFactory;
import org.earthcube.geosoft.community.api.CommunityAPI;
import org.earthcube.geosoft.community.classes.User;
import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.classes.JsonHandler;
import org.earthcube.geosoft.portal.classes.html.CSSLoader;
import org.earthcube.geosoft.portal.classes.html.JSLoader;

import com.google.gson.Gson;

public class CommunityController {
  private int guid;

  private String ontns;
  
  private CommunityAPI api;
  private boolean isSandboxed;
  private Config config;
  private Properties props;

  private Gson json;

  public CommunityController(int guid, Config config) {
    this.guid = guid;
    this.config = config;
    this.isSandboxed = config.isSandboxed();
    json = JsonHandler.createDataGson();
    this.props = config.getProperties();
    this.api = CommunityFactory.getAPI(this.props);

    this.ontns = (String) props.get("ont.software.url") + "#";
  }

  public void show(PrintWriter out) {
    try {
      api.initializeUser(config.getUserId());
      api.save();
      ArrayList<User> users = api.getAllUsers();
      
      out.println("<html>");
      out.println("<head>");
      out.println("<title>Community</title>");
      JSLoader.setContextInformation(out, config);
      CSSLoader.loadCommunityViewer(out, config.getContextRootPath());
      JSLoader.loadCommunityViewer(out, config.getContextRootPath());
      out.println("</head>");
  
      out.println("<script>");
      out.println("var communityViewer_" + guid + ";");
      out.println("Ext.onReady(function() {"
          + "communityViewer_" + guid + " = new CommunityViewer('" + guid + "', { "
              + "users: " + json.toJson(users)
            + " }, " 
            + "'" + config.getScriptPath() + "', "
            + "'" + this.ontns + "', " 
            + !this.isSandboxed
            + ");\n"
            + "communityViewer_" + guid + ".initialize();\n"
          + "});");
      out.println("</script>");
      out.println("</html>");
    }
    catch (Exception e) {
      api.end();
    }
  }

  public String getUserJSON(String userid) {
    try {
      HashMap<String, Object> map = new HashMap<String, Object>();
      map.put("user", api.getUser(userid));
      map.put("user_contributions", api.getUserContributions(userid));
      return json.toJson(map);
    }
    catch (Exception e) {
      e.printStackTrace();
      api.end();
    }
    return null;
  }

  public boolean saveUserJSON(String userid, String uservals_json) {
    if (this.api == null)
      return false;

    try {
      User user = json.fromJson(uservals_json, User.class);
      if(!this.config.getUserId().equals(user.getName()))
        return false;
      return this.api.saveUser(user) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  
}