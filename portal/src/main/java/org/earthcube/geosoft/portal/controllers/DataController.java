package org.earthcube.geosoft.portal.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.earthcube.geosoft.data.DataFactory;
import org.earthcube.geosoft.data.api.DataAPI;
import org.earthcube.geosoft.data.classes.DataItem;
import org.earthcube.geosoft.data.classes.DataTree;
import org.earthcube.geosoft.data.classes.MetadataProperty;
import org.earthcube.geosoft.data.classes.MetadataValue;
import org.earthcube.geosoft.util.KBUtils;
import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.classes.JsonHandler;
import org.earthcube.geosoft.portal.classes.html.CSSLoader;
import org.earthcube.geosoft.portal.classes.html.JSLoader;

public class DataController {
	private int guid;

	private String dcns;
	private String domns;
	private String libns;
	
	private String uploadScript;

	private DataAPI dc;
	private boolean isSandboxed;
	private Config config;
	private Properties props;

	private Gson json;

	public DataController(int guid, Config config) {
		this.guid = guid;
		this.config = config;
		this.isSandboxed = config.isSandboxed();
		json = JsonHandler.createDataGson();
		this.props = config.getProperties();

		dc = DataFactory.getAPI(props);

		this.dcns = (String) props.get("ont.data.url") + "#";
		this.domns = (String) props.get("ont.domain.data.url") + "#";
		this.libns = (String) props.get("lib.domain.data.url") + "#";
		
		this.uploadScript = config.getContextRootPath() + "/upload";
	}

	public void show(PrintWriter out) {
		// Get Hierarchy
		try {
			String tree = this.getDataHierarchyJSON();
			String metrics = this.getMetricsHierarchyJSON();
	
			out.println("<html>");
			out.println("<head>");
			out.println("<title>Describe File Types</title>");
			JSLoader.setContextInformation(out, config);
			CSSLoader.loadDataViewer(out, config.getContextRootPath());
			JSLoader.loadDataViewer(out, config.getContextRootPath());
			out.println("</head>");
	
			out.println("<script>");
			out.println("var dataViewer_" + guid + ";");
			out.println("Ext.onReady(function() {"
					+ "dataViewer_" + guid + " = new DataViewer('" + guid + "', { "
							+ "tree: " + tree + ", "
							+ "metrics: " + metrics 
						+ " }, " 
						+ "'" + config.getScriptPath() + "', "
						+ "'" + this.uploadScript + "', "
						+ "'" + this.dcns + "', " 
						+ "'" + this.domns + "', " 
						+ "'" + this.libns + "', " 
						+ !this.isSandboxed + ", "
						+ false
						+ ");\n"
						+ "dataViewer_" + guid + ".initialize();\n"
					+ "});");
			out.println("</script>");
	
			out.println("</html>");
		}
		finally {
			if(dc != null)
				dc.end();
		}
	}

	/*
	 * Querying Methods
	 */
	public String getDataJSON(String dataid) {
		try {
			if (this.dc == null)
				return "{}";
			String location = this.dc.getDataLocation(dataid);
			DataItem dtype = this.dc.getDatatypeForData(dataid);
			ArrayList<MetadataProperty> props = this.dc.getMetadataProperties(dtype.getID(), false);
			ArrayList<String> propids = new ArrayList<String>();
			for (MetadataProperty prop : props)
				propids.add(prop.getID());
			ArrayList<MetadataValue> vals = this.dc.getMetadataValues(dataid, propids);
			
			HashMap<String, Object> details = new HashMap<String, Object>();
			details.put("dtype", dtype.getID());
			details.put("location", location);
			details.put("props", props);
			details.put("vals", vals);
			return json.toJson(details);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		finally {
			dc.end();
		}
	}

	public String getDatatypeJSON(String dtype) {
		try {
			if (this.dc == null)
				return "{}";

			ArrayList<MetadataProperty> props = this.dc.getMetadataProperties(dtype, false);
			String format = this.dc.getTypeNameFormat(dtype);
	
			HashMap<String, Object> details = new HashMap<String, Object>();
			details.put("properties", props);
			details.put("name_format", format);
			return json.toJson(details);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		finally {
			dc.end();
		}
	}

	public String getDataHierarchyJSON() {
		try {
			if(dc == null)
				return null;
			DataTree tree = dc.getDataHierarchy();
			String dtree = null;
			if(tree != null) 
				dtree = json.toJson(tree.getRoot());
			return dtree;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getMetricsHierarchyJSON() {
		try {
			if(dc == null)
				return null;
			DataTree tree = dc.getMetricsHierarchy();
			String mtree = null;
			if(tree != null) 
				mtree = json.toJson(tree.getRoot());
			return mtree;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void streamData(String dataid, HttpServletResponse response, ServletContext context) {
		try {
			String location = dc.getDataLocation(dataid);
			if(location != null) {
				// Check if this is a file url
				File f = null;
				try {
					URL url = new URL(location);
					f = new File(url.getPath());
				} catch (MalformedURLException e) {
					// Do nothing
				}
				// Else assume it's a file path
				if(f == null)
					f = new File(location);

				if(f.canRead()) {
					try {
						String mimeType = context.getMimeType(location);
						response.setContentType(mimeType);
						response.setHeader("Content-Disposition", "attachment; filename=\"" + f.getName() + "\"");
						FileInputStream fin = new FileInputStream(f);
						IOUtils.copyLarge(fin, response.getOutputStream());
						fin.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					try {
						PrintWriter out = response.getWriter();
						out.println("File not on server\nLocation: "+location);
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		finally {
			dc.end();
		}
	}

	/*
	 * Writing Methods
	 */
	public synchronized boolean saveDataJSON(String dataid, String propvals_json) {
		if (dc == null)
			return false;
		try {
			JsonParser parser = new JsonParser();
			JsonElement propvals = parser.parse(propvals_json);

			DataItem dtype = dc.getDatatypeForData(dataid);
			ArrayList<MetadataProperty> props = dc.getMetadataProperties(dtype.getID(), false);

			ArrayList<String> propids = new ArrayList<String>();
			HashMap<String, MetadataProperty> pinfos = new HashMap<String, MetadataProperty>();
			for (MetadataProperty prop : props) {
				propids.add(prop.getID());
				pinfos.put(prop.getID(), prop);
			}

			dc.removeAllPropertyValues(dataid, propids);
			for (JsonElement propval : propvals.getAsJsonArray()) {
				JsonObject pval = propval.getAsJsonObject();
				String propid = pval.get("name").getAsString();
				String value = pval.get("value").getAsString();
				MetadataProperty pinfo = pinfos.get(propid);
				if (pinfo.isDatatypeProperty()) {
					if (value.equals("") && !pinfo.getRange().contains("string"))
						continue;
					dc.addDatatypePropertyValue(dataid, propid, value, pinfo.getRange());
				} else {
					dc.addObjectPropertyValue(dataid, propid, this.domns + value.toString());
				}
			}
			dc.save();
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
	
	public synchronized String registerData(String dataid, String newname, String metadata_json) {
    if (dc == null)
      return "No Data Catalog";
    try {
      JsonParser parser = new JsonParser();
      JsonElement propvals = parser.parse(metadata_json);
      JsonObject pvals = propvals.getAsJsonObject();
      if(pvals.get("type") == null)
        return "Datatype not known";
      
      String dtypeid = pvals.get("type").getAsString();
      
      String dloc = dc.getDataLocation(dataid);
      if(dloc == null)
        return "Existing data not found on server";
      
      String newid = this.libns + newname;
      String newloc = dc.getDataLocation(newid);
      if(!dc.addData(newid, dtypeid))
        return "Could not add data";

      if(!dataid.equals(newid)) {
        File origf = new File(dloc);
        File newf = new File(origf.getParentFile().getAbsolutePath()+File.separator+newname);
        newloc = newf.getAbsolutePath();
        if(origf.exists() && !newf.exists())
          FileUtils.copyFile(origf, newf);
      }
      if(newloc == null)
        return "Cannot find location for new data";
      
      if(!dc.setDataLocation(newid, newloc))
         return "Could not set data location";
      
      ArrayList<MetadataProperty> props = dc.getMetadataProperties(dtypeid, false);
      HashMap<String, MetadataProperty> pinfos = new HashMap<String, MetadataProperty>();
      for (MetadataProperty prop : props) {
        pinfos.put(prop.getName(), prop);
      }

      for(Entry<String, JsonElement> entry : pvals.entrySet()) {
        String pname = entry.getKey();
        String value = entry.getValue().getAsString();
        MetadataProperty pinfo = pinfos.get(pname);
        if(pinfo != null) {
          if (pinfo.isDatatypeProperty()) {
            if (value.equals("") && !pinfo.getRange().contains("string"))
              continue;
            dc.addDatatypePropertyValue(newid, pinfo.getID(), value, pinfo.getRange());
          } else {
            dc.addObjectPropertyValue(newid, pinfo.getID(), this.domns + value.toString());
          }
        }
      }
      dc.save();
      return "OK";
    }
    catch(Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
    finally {
      dc.end();
    }
	}

	public synchronized String saveDatatypeJSON(String dtypeid, String props_json) {
		ArrayList<String> errors = new ArrayList<String>();
		ArrayList<String> warnings = new ArrayList<String>();
		
		if(dtypeid == null || props_json == null)
			return "[\"Null inputs\"]";

		try {
			JsonParser parser = new JsonParser();
			JsonElement jsonel = parser.parse(props_json);
			JsonObject ops = jsonel.getAsJsonObject();

			// Same datatype's name format
			if (ops.get("format") != null && !ops.get("format").isJsonNull()) {
				String fmt = ops.get("format").getAsString();
				this.dc.setTypeNameFormat(dtypeid, fmt);
			}

			// Check all properties being added
			JsonObject addops = ops.get("add").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : addops.entrySet()) {
				String propid = entry.getKey();
				JsonObject prop = entry.getValue().getAsJsonObject();
				if (prop.get("range") == null) {
					errors.add("No range specified for property propid");
					continue;
				}
				String range = prop.get("range").getAsString();
				MetadataProperty eprop = dc.getMetadataProperty(propid);
				if (eprop != null) {
					// A property with this id already exists. 
					// Add dtypeid as a domain of the property
					this.dc.addMetadataPropertyDomain(propid, dtypeid);
				}
				else {
					this.dc.addMetadataProperty(propid, dtypeid, range);
				}
			}

			// Check all properties being deleted
			JsonObject delops = ops.get("del").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : delops.entrySet()) {
				String propid = entry.getKey();
				MetadataProperty eprop = dc.getMetadataProperty(propid);
				if(eprop.getDomains().size() > 1) {
					// There are more than one datatypes using this property
					// just remove the property from this datatype
					this.dc.removeMetadataPropertyDomain(propid, dtypeid);
					eprop.getDomains().remove(dtypeid);
					warnings.add("Note that the property you deleted currently also "
							+ "exists for other datatypes: " 
							+ eprop.getDomains());
				}
				else {
					this.dc.removeMetadataProperty(propid);
				}
			}

			// Check all properties being modified
			JsonObject modops = ops.get("mod").getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : modops.entrySet()) {
				String propid = entry.getKey();
				JsonObject prop = entry.getValue().getAsJsonObject();
				if (prop.get("range") == null) {
					errors.add("No range specified for property propid");
					continue;
				}
				String range = prop.get("range").getAsString();
				String npropid = prop.get("pid").getAsString();
				MetadataProperty eprop = this.dc.getMetadataProperty(propid);
				if (!eprop.getRange().equals(range)) {
					this.dc.removeMetadataProperty(propid);
					this.dc.addMetadataProperty(npropid, dtypeid, range);
					// Re-add any other domains that the property might have had
					for(String domid : eprop.getDomains()) {
						if(!domid.equals(dtypeid))
							this.dc.addMetadataPropertyDomain(npropid, domid);
					}
				} else if (!propid.equals(npropid)) {
					this.dc.renameMetadataProperty(propid, npropid);
				}
				if(eprop.getDomains().size() > 1) {
					eprop.getDomains().remove(dtypeid);
					warnings.add("Note that the property you modified also "
							+ "exists for other datatypes, and would have been modified for them as well : " 
							+ eprop.getDomains());
				}
			}
			if(errors.size() == 0)
				dc.save();
		}
		catch(Exception e) {
			e.printStackTrace();
			errors.add(e.getMessage());
		}
		finally {
			dc.end();
		}
		HashMap<String, Object> retobj = new HashMap<String, Object>();
		retobj.put("errors", errors);
		retobj.put("warnings", warnings);
		return json.toJson(retobj);
	}

	public synchronized boolean addDatatype(String ptype, String dtype) {
		if(ptype == null || dtype == null)
			return false;
		try {
			return dc.addDatatype(dtype, ptype)
				&& dc.save();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}

	public synchronized boolean moveDatatypeTo(String dtype, String fromtype, String totype) {
		if(dtype == null || dtype == null || totype == null)
			return false;
		try {
			return dc.moveDatatypeParent(dtype, fromtype, totype) 
					&& dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}

	public synchronized boolean delDatatypes(String[] dtypes) {
		try {
			for (String dtypeid : dtypes) {
				if(!dc.removeDatatype(dtypeid)) {
					return false;
				}
			}
			dc.save();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}

	public synchronized boolean addDataForDatatype(String dataid, String dtypeid) {
		try {
			return dc.addData(dataid, dtypeid)
				&& dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
	
	public synchronized boolean addBatchData(String dtypeid, String[] dids, String[] locations) {
		try {
			for(int i=0; i<dids.length; i++) {
				if(!dc.addData(dids[i], dtypeid))
					return false;
				if(locations.length > i && locations[i] != null)
					if(!dc.setDataLocation(dids[i], locations[i]))
						return false;
			}
			return dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}

	public synchronized boolean delData(String dataid) {
		try {
			return dc.removeData(dataid)
				&& dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
	
	public synchronized boolean renameData(String dataid, String newid) {
		try {
			return dc.renameData(newid, dataid)
				&& dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
	
	public synchronized boolean setDataLocation(String dataid, String location) {
		try {
			return dc.setDataLocation(dataid, location)
				&& dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
	
	public synchronized boolean renameDataType(String dtypeid, String newid) {
		try {
			return dc.renameDatatype(newid, dtypeid)
				&& dc.save();
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
	
	public synchronized boolean importFromExternalCatalog(String dataid, String dtypeid, 
			String propvals_json, String location) {
		if (dc == null)
			return false;
		try {
			JsonParser parser = new JsonParser();
			JsonElement propvals = parser.parse(propvals_json);

			DataItem dtype = dc.getDatatypeForData(dataid);
			if(dtype == null) {
				dc.addData(dataid, dtypeid);
			}
			for (Map.Entry<String, JsonElement> entry : propvals.getAsJsonObject().entrySet()) {
				String propid = entry.getKey();
				String value = entry.getValue().getAsString();
				//FIXME: Just using range as string for now
				String range = KBUtils.XSD + "string";
				MetadataProperty eprop = dc.getMetadataProperty(propid);
				if (eprop == null) {
					// Property doesn't exist
					this.dc.addMetadataProperty(propid, dtypeid, range);
				}
				else if(!eprop.getDomains().contains(dtypeid)) {
					// Property exists for another class. Add this class as domain
					this.dc.addMetadataPropertyDomain(propid, dtypeid);
				}
				this.dc.addDatatypePropertyValue(dataid, propid, value, range);
			}
			dc.setDataLocation(dataid, location);
			return dc.save();
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		finally {
			dc.end();
		}
	}
}
