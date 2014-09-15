package org.earthcube.geosoft.portal.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.earthcube.geosoft.data.DataFactory;
import org.earthcube.geosoft.data.api.DataAPI;
import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.classes.JsonHandler;
import org.earthcube.geosoft.portal.classes.StorageHandler;
import org.earthcube.geosoft.portal.classes.html.CSSLoader;
import org.earthcube.geosoft.portal.classes.html.JSLoader;
import org.earthcube.geosoft.portal.classes.importer.ImporterFactory;
import org.earthcube.geosoft.portal.classes.importer.api.ImportAPI;
import org.earthcube.geosoft.software.SoftwareFactory;
import org.earthcube.geosoft.software.api.SoftwareAPI;
import org.earthcube.geosoft.software.classes.SWPropertyValue;
import org.earthcube.geosoft.software.classes.Software;
import org.earthcube.geosoft.software.classes.SoftwareType;

@SuppressWarnings("unused")
public class SoftwareController {
  private int guid;
  private String ontns;
  private String libns;
  private String liburl;

  private String uploadScript;

  private SoftwareAPI api;
  private DataAPI dapi;
  
  private boolean isSandboxed;
  private Config config;
  private Properties props;
  private Gson json;

  public SoftwareController(int guid, Config config) {
    this.guid = guid;
    this.config = config;
    this.isSandboxed = config.isSandboxed();
    json = JsonHandler.createGson();

    this.props = config.getProperties();
    api = SoftwareFactory.getAPI(props);
    dapi = DataFactory.getAPI(props);
    
    HashMap<String, String> snprops = config.getStandardNamesOntologies();
    for(String snrepo : snprops.keySet()) {
      api.loadStandardNamesOntology(snrepo, snprops.get(snrepo));
    }
    
    this.ontns = (String) props.get("ont.software.url") + "#";
    this.liburl = (String) props.get("lib.software.url");
    this.libns = this.liburl + "#";

    this.uploadScript = config.getContextRootPath() + "/upload";
  }

  public void show(PrintWriter out) {
    // Get Hierarchy
    try {
      SoftwareType typeroot = api.getSoftwareTypesTree();
      String list = json.toJson(api.getSoftwares(typeroot, false));
      String software_types = json.toJson(typeroot);
      String data_types = json.toJson(dapi.getAllDatatypeIds());
      String props = json.toJson(api.getAllSoftwareProperties(true));
      
      HashMap<String, Object> sninfo = new HashMap<String, Object>();
      sninfo.put("repos", config.getStandardNamesOntologies());
      sninfo.put("objects", api.getObjects());
      sninfo.put("quantities", api.getQuantities());
      sninfo.put("operators", api.getOperators());
      sninfo.put("categories", api.getAssumptionCategories());
      sninfo.put("assumptions", api.getAssumptions());
      sninfo.put("standardnames", api.getStandardNames());
      
      out.println("<html>");
      out.println("<head>");
      out.println("<title>Describe Software Components</title>");
      JSLoader.setContextInformation(out, config);
      CSSLoader.loadSoftwareViewer(out, config.getContextRootPath());
      JSLoader.loadSoftwareViewer(out, config.getContextRootPath());
      out.println("</head>");

      out.println("<script>");
      out.println("var compViewer_" + guid + ";");
      out.println("Ext.onReady(function() {"
          + "compViewer_" + guid + " = new SoftwareViewer('" + guid + "', { "
          + " softwares:" + list + ", "
          + " software_types:" + software_types + ", "
          + " data_types:" + data_types + ", "
          + " properties:" + props + ", "
          + " sninfo: " + json.toJson(sninfo)
          + " }, "
          + "'" + config.getScriptPath() + "', "
          + "'" + this.uploadScript + "', "
          + "'" + this.ontns + "', "
          + "'" + this.liburl + "', "
          + !isSandboxed
          + ");\n"
          + "compViewer_" + guid + ".initialize();\n" + "});\n");
      out.println("</script>");

      out.println("</html>");
    } finally {
      api.end();
    }
  }

  public String getSoftwareJSON(String softwareid) {
    try {
      return json.toJson(api.getSoftware(softwareid, true));
    } finally {
      api.end();
    }
  }
  
  public String getSoftwareTypeJSON(String typeid) {
    try {
      return json.toJson(api.getSoftwareType(typeid, false));
    } finally {
      api.end();
    }
  }

  public String getInferredSoftware(String softwareid, String software_json) {
    if (this.api == null)
      return null;

    try {
      Software software = json.fromJson(software_json, Software.class);
      Software nsoftware = this.api.getInferredSoftware(software);
      // Check for Tika detected mimetypes
      String tikaUrl = this.config.getMiscPropertyValue("tikaUrl");
      if(tikaUrl != null)
        nsoftware = this.api.getTikaInferredSoftware(nsoftware, tikaUrl);
      return json.toJson(nsoftware);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }
  
  public String checkCode(String id) {
    if (this.api == null) 
      return null;
    
    try {
      String results = this.api.checkCode(id);
      return json.toJson(results);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      api.end();
    }
  }
  
  /*
   * Writing Methods
   */
  public synchronized boolean saveSoftwareJSON(String softwareid, String software_json) {
    if (this.api == null)
      return false;

    try {
      Software software = json.fromJson(software_json, Software.class);
      return this.api.updateSoftware(software) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  public synchronized boolean saveSoftwareTypeJSON(String typeid, String type_json) {
    if (this.api == null)
      return false;

    try {
      SoftwareType stype = json.fromJson(type_json, SoftwareType.class);
      return this.api.updateSoftwareType(stype) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  public synchronized boolean addSoftware(String softwareid, String typeid) {
    try {
      Software software = new Software(softwareid, typeid);
      return this.api.addSoftware(software) && this.api.save();

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public synchronized boolean importSoftware(String softwareid, String typeid, 
      String repo_softwareid, String repo_id) {
    try {
      ImportAPI iapi = ImporterFactory.getAPI(repo_id, this.props);
      if (iapi == null)
        return false;

      Software software = iapi.importSoftware(softwareid, typeid, repo_softwareid);
      if (software == null)
        return false;

      return this.api.addSoftware(software) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public synchronized boolean delSoftware(String softwareid) {
    try {
      return this.api.removeSoftware(softwareid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public synchronized boolean setSoftwareType(String softwareid, String typeid) {
    try {
      return this.api.setSoftwareType(softwareid, typeid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public synchronized boolean setSoftwareTypeParent(String typeid, String parentid) {
    try {
      SoftwareType type = this.api.getSoftwareType(typeid, false);
      type.setParentid(parentid);
      return this.api.updateSoftwareType(type) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }

  public synchronized boolean renameSoftware(String softwareid, String newid) {
    try {
      return this.api.renameSoftware(softwareid, newid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  public synchronized boolean renameSoftwareType(String typeid, String newid) {
    try {
      return this.api.renameSoftwareType(typeid, newid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  public synchronized boolean addSoftwareType(String typeid, String parentid) {
    try {
      return this.api.addSoftwareType(typeid, parentid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  public synchronized boolean delSoftwareType(String typeid) {
    try {
      return this.api.removeSoftwareType(typeid) && this.api.save();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      api.end();
    }
  }
  
  public boolean runAuditTool(String softwareid) {
    Software sw = this.api.getSoftware(softwareid, true);
    // Run DRAT to analyze licenses
    String dratHome = this.config.getMiscPropertyValue("dratHome");
    if(dratHome != null) {
      return this.api.runAuditTool(sw, dratHome);
    }
    return false;
  }
}
