package org.earthcube.geosoft.community.classes;

import org.earthcube.geosoft.util.URIEntity;

public class User extends URIEntity{
  private static final long serialVersionUID = 1L;
  String username, picture, site, expertise, affiliation;

  public User(String id) {
    super(id);
  }
  
  public String getUsername() {
    return this.username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }

  public String getPicture() {
    return picture;
  }

  public void setPicture(String picture) {
    this.picture = picture;
  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getExpertise() {
    return expertise;
  }

  public void setExpertise(String expertise) {
    this.expertise = expertise;
  }

  public String getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(String affiliation) {
    this.affiliation = affiliation;
  }
}
