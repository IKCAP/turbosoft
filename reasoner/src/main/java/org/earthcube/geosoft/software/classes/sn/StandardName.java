package org.earthcube.geosoft.software.classes.sn;

import java.util.ArrayList;

import org.earthcube.geosoft.util.URIEntity;

public class StandardName extends URIEntity {
  private static final long serialVersionUID = 1L;

  private String objectId;
  private String quantityId;
  private ArrayList<String> operatorIds;
  private String internalVariable;
  private String note;
  
  public StandardName(String id) {
    super(id);
    operatorIds = new ArrayList<String>();
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public String getQuantityId() {
    return quantityId;
  }

  public void setQuantityId(String quantityId) {
    this.quantityId = quantityId;
  }

  public ArrayList<String> getOperatorIds() {
    return operatorIds;
  }

  public void addOperatorId(String operatorId) {
    this.operatorIds.add(operatorId);
  }

  public String getInternalVariable() {
    return internalVariable;
  }

  public void setInternalVariable(String internalVariable) {
    this.internalVariable = internalVariable;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

}
