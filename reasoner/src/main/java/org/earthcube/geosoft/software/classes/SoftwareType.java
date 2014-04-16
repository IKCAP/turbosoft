package org.earthcube.geosoft.software.classes;

import java.util.ArrayList;

public class SoftwareType {
  String id;
  String annotation;
  ArrayList<SoftwareType> subtypes;
  
  public SoftwareType(String id) {
    this.id = id;
    this.annotation = "";
    this.subtypes = new ArrayList<SoftwareType>();
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getAnnotation() {
    return annotation;
  }

  public void setAnnotation(String annotation) {
    this.annotation = annotation;
  }

  public ArrayList<SoftwareType> getSubtypes() {
    return subtypes;
  }
  
  public void setSubtypes(ArrayList<SoftwareType> subtypes) {
    this.subtypes = subtypes;
  }
  
  public void addSubtype(SoftwareType subtype) {
    this.subtypes.add(subtype);
  }
}
