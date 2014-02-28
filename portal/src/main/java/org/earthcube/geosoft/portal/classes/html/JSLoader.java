package org.earthcube.geosoft.portal.classes.html;

import java.io.PrintWriter;
import java.util.HashMap;

import org.earthcube.geosoft.portal.classes.Config;

public class JSLoader {
  static String[] common_scripts = { "lib/extjs/ext-all.js",
      "js/util/common.js" };
  static String[] software_scripts = { "js/gui/SoftwareViewer.js" };
  static String[] data_scripts = { "js/gui/DataViewer.js" };

  static String[] plupload_scripts = { "js/util/pluploadPanel.js",
      "lib/plupload/plupload.full.min.js" };

  public static void setContextInformation(PrintWriter out, Config config) {
    HashMap<String, Object> jsvars = new HashMap<String, Object>();
    jsvars.put("CONTEXT_ROOT", "'" + config.getContextRootPath() + "'");
    jsvars.put("USER_ID", "'" + config.getUserId() + "'");
    JSLoader.showScriptKeyVals(out, jsvars);
  }

  public static void loadLoginViewer(PrintWriter out, String path) {
    showScriptTags(out, path, common_scripts);
  }

  public static void loadSoftwareViewer(PrintWriter out, String path) {
    showScriptTags(out, path, common_scripts);
    showScriptTags(out, path, software_scripts);
    // showScriptTags(out, path, rule_scripts);
    showScriptTags(out, path, plupload_scripts);
  }

  public static void loadDataViewer(PrintWriter out, String path) {
    showScriptTags(out, path, common_scripts);
    showScriptTags(out, path, data_scripts);
    showScriptTags(out, path, plupload_scripts);
  }

  private static void showScriptTags(PrintWriter out, String path,
      String[] scripts) {
    for (String script : scripts) {
      out.println("<script src=\"" + path + "/" + script + "\"></script>");
    }
  }

  public static void showScriptKeyVals(PrintWriter out,
      HashMap<String, Object> map) {
    out.println("<script>");
    for (String key : map.keySet()) {
      Object val = map.get(key);
      out.println("var " + key + " = " + val + ";");
    }
    out.println("</script>");
  }
}
