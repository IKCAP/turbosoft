package org.earthcube.geosoft.software.classes;

import java.util.ArrayList;
import java.util.HashMap;

import org.earthcube.geosoft.software.classes.sn.SNAssumption;
import org.earthcube.geosoft.software.classes.sn.StandardName;
import org.earthcube.geosoft.util.URIEntity;

public class Software extends URIEntity {
  private static final long serialVersionUID = 1L;

  String classId;
  ArrayList<SWPropertyValue> propertyValues;
  ArrayList<SoftwareRole> inputs;
  ArrayList<SoftwareRole> outputs;
  ArrayList<String> explanations;
  HashMap<String, String> labels;
  
  ArrayList<SNAssumption> assumptions;
  ArrayList<StandardName> standardnames;
  
  public Software(String id, String classId) {
    super(id);
    this.classId = classId;
    propertyValues = new ArrayList<SWPropertyValue>();
    inputs = new ArrayList<SoftwareRole>();
    outputs = new ArrayList<SoftwareRole>();
    explanations = new ArrayList<String>();
    assumptions = new ArrayList<SNAssumption>();
    standardnames = new ArrayList<StandardName>();
    labels = new HashMap<String, String>();
  }

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classId) {
    this.classId = classId;
  }

  public ArrayList<SWPropertyValue> getPropertyValues() {
    return propertyValues;
  }

  public void addPropertyValue(SWPropertyValue propertyValue) {
    this.propertyValues.add(propertyValue);
  }

  public ArrayList<SoftwareRole> getInputs() {
    return inputs;
  }

  public void addInput(SoftwareRole input) {
    this.inputs.add(input);
  }

  public ArrayList<SoftwareRole> getOutputs() {
    return outputs;
  }

  public void addOutput(SoftwareRole output) {
    this.outputs.add(output);
  }
  
  public ArrayList<String> getExplanations() {
    return explanations;
  }

  public void addExplanation(String explanation) {
    this.explanations.add(explanation);
  }
  
  public void setExplanations(ArrayList<String> explanations) {
    this.explanations = explanations;
  }

  public HashMap<String, String> getLabels() {
    return labels;
  }

  public void setLabels(HashMap<String, String> labels) {
    this.labels = labels;
  }

  public ArrayList<SNAssumption> getAssumptions() {
    return assumptions;
  }

  public void addAssumption(SNAssumption assumption) {
    this.assumptions.add(assumption);
  }

  public ArrayList<StandardName> getStandardNames() {
    return standardnames;
  }

  public void addStandardName(StandardName standardname) {
    this.standardnames.add(standardname);
  }
}