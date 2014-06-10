package org.earthcube.geosoft.community.classes;

public class UserContribution {
  String softwareId;
  String propertyId;
  Object value;
  long timestamp;

  public String getSoftwareId() {
    return softwareId;
  }

  public void setSoftwareId(String softwareId) {
    this.softwareId = softwareId;
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

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }
}
