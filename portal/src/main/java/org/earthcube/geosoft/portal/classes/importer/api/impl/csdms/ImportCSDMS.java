package org.earthcube.geosoft.portal.classes.importer.api.impl.csdms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.earthcube.geosoft.portal.classes.importer.api.ImportAPI;
import org.earthcube.geosoft.software.SoftwareFactory;
import org.earthcube.geosoft.software.api.SoftwareAPI;
import org.earthcube.geosoft.software.classes.SWProperty;
import org.earthcube.geosoft.software.classes.SWPropertyValue;
import org.earthcube.geosoft.software.classes.Software;
import org.earthcube.geosoft.util.KBUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImportCSDMS implements ImportAPI {

  private String url_prefix = "http://csdms.colorado.edu/wiki/Special:Browse?article=Model:";
  
  private HashMap<String, String> propertyMap = new HashMap<String, String>();
  private HashMap<String, SWProperty> softwareProps = new HashMap<String, SWProperty>();
  
  private String ontns;
  
  public ImportCSDMS(Properties props) {
    this.initPropertyMap();
    this.ontns = props.getProperty("ont.software.url") + "#";
    SoftwareAPI api = SoftwareFactory.getAPI(props);
    for(SWProperty mprop : api.getAllSoftwareProperties(false)) {
      softwareProps.put(mprop.getId(), mprop);
    }
  }
  
  @Override
  public Software importSoftware(String softwareid, String typeid, String repository_softwareid) {
    String url = url_prefix + repository_softwareid;
    try {
      HashMap<String, ArrayList<String>> propVals = new HashMap<String, ArrayList<String>>();
      Document doc = Jsoup.connect(url).get();
      Elements elements = doc.select("tr.smwb-propvalue");
      for (Element el : elements) {
        String prop = el.select("th a").attr("title");
        prop = prop.replace("(page does not exist)", "");
        prop = prop.trim();
        for (Element valel : el.select("td span.smwb-value")) {
          String val = valel.ownText();
          val = val.replace("\u00a0", ""); // Remove &nbsp;
          if (val.equals("--"))
            val = null;
          else
            val = val.trim();
          if (val == null || val.equals("")) {
            for (Element ael : valel.select("a")) {
              if(ael.ownText().equals("+")) 
                continue;
              val = ael.ownText();
              val = val.trim();
            }
          }            
          if (!propVals.containsKey(prop))
            propVals.put(prop, new ArrayList<String>());
          propVals.get(prop).add(val);
        }
      }
      return createSoftware(softwareid, typeid, propVals);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  private Software createSoftware(String id, String typeid, HashMap<String, ArrayList<String>> pvs) {
    Software m = new Software(id, typeid);
    for(String r_prop : pvs.keySet()) {
      ArrayList<String> vals = pvs.get(r_prop);
      if(vals == null) 
        continue;
      r_prop = r_prop.replace("Property:", "");
      // Get Property Mapping
      if(this.propertyMap.containsKey(r_prop)) {
        String propid = this.ontns + this.propertyMap.get(r_prop);
        SWProperty mprop = softwareProps.get(propid);
        if(mprop != null) {
          for(String val : vals) {
            if(mprop.isObjectProperty()) {
              val = val.replace(" ", "");
              if(val.startsWith("Apache"))
                val = "ALv2";
              val = this.ontns + val;
            }
            else if(mprop.getRange() != null &&
                mprop.getRange().equals(KBUtils.XSD+"boolean")) {
              if(val.toLowerCase().startsWith("yes"))
                val = "true";
              else if(val.toLowerCase().startsWith("through"))
                val = "true";
              else if(val.toLowerCase().startsWith("no"))
                val = "false";
            }
            else if(mprop.getRange() != null &&
                mprop.getRange().equals(KBUtils.XSD+"integer")) {
              val = val.replace("," , "");
            }
            SWPropertyValue mpv = new SWPropertyValue(mprop.getId(), val);
            
            mpv.getProvenance().add(new SWPropertyValue(
                this.ontns+"importedFrom", "CSDMS"));
            mpv.getProvenance().add(new SWPropertyValue(
                this.ontns+"timestamp", new Date().getTime()));
            m.addPropertyValue(mpv);
          }
        }
      }
    }
    return m;
  }
  
  private void initPropertyMap() {
    propertyMap.put("Extended model description", "ExtendedDescription"); 
    propertyMap.put("Last name", "LastName"); 
    propertyMap.put("Describe numerical limitations", "NumericalLimitations"); 
    propertyMap.put("Manual model available", "IsManualAvailable"); 
    propertyMap.put("City", "City"); 
    propertyMap.put("Code optimized", "IsCodeOptimized");
    propertyMap.put("One-line model description", "OneLineDescription"); 
    propertyMap.put("Type of contact", "ContactType"); 
    propertyMap.put("Spatial dimensions", "SpatialDimension"); 
    propertyMap.put("Describe key physical parameters", ""); 
    propertyMap.put("Development still active", "isActivelyDeveloped"); 
    propertyMap.put("Supported platforms", "SupportedPlatform"); 
    propertyMap.put("Postal address1", "PostalAddress1"); 
    propertyMap.put("Model website", "SoftwareWebsite"); 
    propertyMap.put("State", "State"); 
    propertyMap.put("Describe time scale and resolution", "TimeScaleDescription"); 
    propertyMap.put("DOI model", "DOI"); 
    propertyMap.put("Programming language", "ProgrammingLanguage"); 
    propertyMap.put("Describe length scale and resolution", "LengthScaleDescription"); 
    propertyMap.put("Memory requirements", "MemoryRequirement"); 
    propertyMap.put("Model type", "SoftwareType"); 

    propertyMap.put("Describe processes", "ProcessDescription"); 
    propertyMap.put("Country", "Country"); 
    
    propertyMap.put("Program license type", "ProgramLicense");
    propertyMap.put("Program license type other", "ProgramLicenseOther");
    
    propertyMap.put("Pre processing software", "NeedsPreProcessing");
    propertyMap.put("Describe pre-processing software", "PreProcessingSoftware");
    propertyMap.put("Post processing software", "NeedsPostProcessing");
    propertyMap.put("Describe post-processing software", "PostProcessingSoftware");
    propertyMap.put("Visualization software needed", "NeedsVisualization");
    propertyMap.put("Visualization software", "VisualizationSoftware");
    propertyMap.put("Visualization software other", "VisualizationSoftware");
    propertyMap.put("Institute", "Institute"); 
    
    propertyMap.put("Code IRF or not", "IRFInterface");
    propertyMap.put("Code CMT compliant or not", "IsCMTCompliant"); 
    propertyMap.put("Code openmi compliant or not", "IsOpenMICompliant");
    propertyMap.put("Code compliant or not", "isCCACompliant");
    
    propertyMap.put("Source code availability", "SourceCodeIsAvailable");
    propertyMap.put("Source web address", "SourceCode");
    
    propertyMap.put("First name", "FirstName"); 
    propertyMap.put("Model forum", "SoftwareForum"); 
    propertyMap.put("Phone", "Phone"); 
    propertyMap.put("Fax", "Fax");
    
    propertyMap.put("Start year development", "DevelopmentStartYear"); 
    propertyMap.put("End year development", "DevelopmentEndYear"); 
    
    propertyMap.put("Email address", "EmailAddress");  
    propertyMap.put("Postal code", "PostalCode"); 
    propertyMap.put("Spatialscale", "SpatialScale");  
    propertyMap.put("Model keywords", "Keywords");
    
    propertyMap.put("Special:Categories", "SoftwareDomain");
    propertyMap.put("DOI assigned to model version", "DOIforVersion");

    propertyMap.put("Describe ideal data", "IdealTestDataDescription"); 
    propertyMap.put("Describe available test data", "TestDataDescription");
    propertyMap.put("Describe available calibration data", "CalibrationDataSetDescription"); 
    
    propertyMap.put("Model availability", "SoftwareAvailability"); 
    
    propertyMap.put("Current future collaborators", "HaveCollaborationPlans"); 
    
    propertyMap.put("ViewVC web address", "SourceCode"); 
    propertyMap.put("Run time model", "TypicalRuntime");
    propertyMap.put("Additional comments model", "Comments"); 
    
    /*
    propertyMap.put("Describe output parameters model", "");
    propertyMap.put("Describe input parameters model", "");  
    propertyMap.put("Input format model", ""); 
    propertyMap.put("Output format model", ""); 
    */
  }
}
