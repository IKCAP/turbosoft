package org.earthcube.geosoft.software.api;

import java.util.ArrayList;

import org.earthcube.geosoft.software.api.SoftwareAPI;
import org.earthcube.geosoft.software.classes.SWProperty;
import org.earthcube.geosoft.software.classes.Software;
import org.earthcube.geosoft.software.classes.sn.SNAssumption;
import org.earthcube.geosoft.software.classes.sn.SNObject;
import org.earthcube.geosoft.software.classes.sn.SNOperator;
import org.earthcube.geosoft.software.classes.sn.SNQuantity;
import org.earthcube.geosoft.software.classes.sn.StandardName;
import org.earthcube.geosoft.util.URIEntity;

public interface SoftwareAPI {
  
  /*
   * General Software Metadata Part
   */
  // Query
  ArrayList<String> getSoftwareTypes();
  
  ArrayList<Software> getSoftwares(boolean details);

  ArrayList<SWProperty> getAllSoftwareProperties(boolean rangeValues);
  
  Software getSoftware(String softwareid, boolean details);
  
  // Update
  boolean addSoftware(Software software);

  boolean updateSoftware(Software software);
  
  boolean renameSoftware(String oldid, String newid);

  boolean removeSoftware(String softwareid);
  
  /*
   * Standard Names Part
   */
  // Load
  public void loadStandardNamesOntology(String id, String url);
  
  // Query
  public ArrayList<SNObject> getObjects();
  
  public ArrayList<SNQuantity> getQuantities();
  
  public ArrayList<SNOperator> getOperators();
  
  public ArrayList<StandardName> getStandardNames();
  
  public ArrayList<URIEntity> getAssumptionCategories();
  
  public ArrayList<SNAssumption> getAssumptions();
  
  // Update
  public boolean addObject(SNObject object);
  
  public boolean addQuantity(SNQuantity quantity);
  
  public boolean addOperator(SNOperator operator);
  
  public boolean addStandardName(StandardName sname);
  
  public boolean addAssumption(SNAssumption assumption);
  
  // Reasoning
  Software getInferredSoftware(Software software);
  
  // Saving
  boolean save();

  void end();
  
  void delete();
}
