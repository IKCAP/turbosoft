package org.earthcube.geosoft.software.classes;

import java.util.ArrayList;

public class SWPropertyValue {
  String propertyId;
  Object value;
  
  ArrayList<SWPropertyValue> provenance;
  
  public SWPropertyValue(String propertyId, Object value) {
    this.propertyId = propertyId;
    this.value = value;
    this.provenance = new ArrayList<SWPropertyValue>();
  }
  
  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public ArrayList<SWPropertyValue> getProvenance() {
    return provenance;
  }

  public void setProvenance(ArrayList<SWPropertyValue> provenance) {
    this.provenance = provenance;
  }
  
  public String toString() {
    return propertyId+"="+value;
  }
}
