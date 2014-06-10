package org.earthcube.geosoft.community.api.impl.kb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.earthcube.geosoft.community.api.CommunityAPI;
import org.earthcube.geosoft.community.classes.User;
import org.earthcube.geosoft.community.classes.UserContribution;
import org.earthcube.geosoft.util.KBUtils;

import edu.isi.wings.ontapi.KBAPI;
import edu.isi.wings.ontapi.KBObject;
import edu.isi.wings.ontapi.OntFactory;
import edu.isi.wings.ontapi.OntSpec;
import edu.isi.wings.ontapi.SparqlQuerySolution;

public class CommunityKB implements CommunityAPI {
  protected String swonturl, comonturl;
  protected String swliburl, comliburl;
  
  protected String comns, swns, comlibns;
  
  protected String tdbRepository;
  protected OntFactory ontologyFactory;
  
  protected KBAPI kb, swkb, writerkb;
  
  protected HashMap<String, KBObject> objPropMap;
  protected HashMap<String, KBObject> dataPropMap;
  
  public CommunityKB(Properties props) {
    this.swonturl = props.getProperty("ont.software.url");
    this.comonturl = props.getProperty("ont.community.url");
    this.swliburl = props.getProperty("lib.software.url");
    this.comliburl = props.getProperty("lib.community.url");

    String hash = "#";
    this.comns = comonturl + hash;
    this.swns = swonturl + hash;
    this.comlibns = comliburl + hash;

    this.tdbRepository = props.getProperty("tdb.repository.dir");
    this.ontologyFactory = new OntFactory(OntFactory.JENA, this.tdbRepository);
    
    this.initializeAPI();
  }
  
  protected void initializeAPI() {
    try {
      this.kb = this.ontologyFactory.getKB(comliburl, OntSpec.PLAIN, true);
      this.kb.importFrom(this.ontologyFactory.getKB(comonturl, OntSpec.PLAIN, true, true));

      this.writerkb = this.ontologyFactory.getKB(comliburl, OntSpec.PLAIN, true);
      
      this.swkb = this.ontologyFactory.getKB(swliburl, OntSpec.PLAIN, true);
      this.swkb.importFrom(this.ontologyFactory.getKB(swonturl, OntSpec.PLAIN, true, true));
      this.initializeMaps();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initializeMaps() {
    this.objPropMap = new HashMap<String, KBObject>();
    this.dataPropMap = new HashMap<String, KBObject>();

    for (KBObject prop : this.kb.getAllObjectProperties()) {
      this.objPropMap.put(prop.getName(), prop);
    }
    for (KBObject prop : this.kb.getAllDatatypeProperties()) {
      this.dataPropMap.put(prop.getName(), prop);
    }
  }

  @Override
  public User getUser(String userid) {
    KBObject userobj = this.kb.getIndividual(userid);
    if(userobj == null)
      return null;
    
    User user = new User(userobj.getID());

    KBObject nameobj = this.kb.getPropertyValue(userobj, dataPropMap.get("hasName"));
    KBObject affobj = this.kb.getPropertyValue(userobj, dataPropMap.get("hasAffiliation"));
    KBObject expobj = this.kb.getPropertyValue(userobj, dataPropMap.get("hasExpertise"));
    KBObject picobj = this.kb.getPropertyValue(userobj, dataPropMap.get("hasPicture"));
    KBObject siteobj = this.kb.getPropertyValue(userobj, dataPropMap.get("hasWebsite"));
    
    if(nameobj != null && nameobj.getValue() != null)
      user.setUsername(nameobj.getValue().toString());
    if(affobj != null && affobj.getValue() != null)
      user.setAffiliation(affobj.getValue().toString());
    if(expobj != null && expobj.getValue() != null)
      user.setExpertise(expobj.getValue().toString());
    if(picobj != null && picobj.getValue() != null)
      user.setPicture(picobj.getValue().toString());
    if(siteobj != null && siteobj.getValue() != null)
      user.setSite(siteobj.getValue().toString());
    
    return user;
  }

  @Override
  public void initializeUser(String username) {
    KBObject userobj = this.kb.getIndividual(this.comlibns + username);
    if(userobj == null) {
      KBObject usercls = this.kb.getConcept(this.comns + "User");
      if(usercls == null)
        return;
      userobj = this.writerkb.createObjectOfClass(this.comlibns + username, usercls);
    }
  }
    
  @Override
  public boolean saveUser(User user) {
    KBObject userobj = this.kb.getIndividual(user.getID());
    if(userobj == null) {
      KBObject usercls = this.kb.getConcept(this.comns + "User");
      userobj = this.writerkb.createObjectOfClass(user.getID(), usercls);
    }
    if(user.getUsername() != null)
      this.writerkb.setPropertyValue(userobj, dataPropMap.get("hasName"), 
          ontologyFactory.getDataObject(user.getUsername()));
    if(user.getAffiliation() != null)
      this.writerkb.setPropertyValue(userobj, dataPropMap.get("hasAffiliation"),
          ontologyFactory.getDataObject(user.getAffiliation()));
    if(user.getExpertise() != null)
      this.writerkb.setPropertyValue(userobj, dataPropMap.get("hasExpertise"),
          ontologyFactory.getDataObject(user.getExpertise()));
    if(user.getPicture() != null)
      this.writerkb.setPropertyValue(userobj, dataPropMap.get("hasPicture"),
          ontologyFactory.getDataObject(user.getPicture()));
    if(user.getSite() != null)
      this.writerkb.setPropertyValue(userobj, dataPropMap.get("hasWebsite"),
          ontologyFactory.getDataObject(user.getSite()));
    return true;
  }

  @Override
  public ArrayList<String> listUserIds() {
    ArrayList<String> userids = new ArrayList<String>();
    KBObject usercls = this.kb.getConcept(this.comns + "User");
    ArrayList<KBObject> userobjs = this.kb.getInstancesOfClass(usercls, true);
    for(KBObject userobj : userobjs) {
      userids.add(userobj.getID());
    }
    return userids;
  }

  @Override
  public ArrayList<User> getAllUsers() {
    ArrayList<User> users = new ArrayList<User>();
    for(String userid : this.listUserIds()) {
      users.add(this.getUser(userid));
    }
    return users;
  }
  
  @Override
  public ArrayList<UserContribution> getUserContributions(String userid) {
    KBObject userobj = this.kb.getIndividual(userid);
    if(userobj == null)
      return null;

    String query = "SELECT DISTINCT ?s ?p ?ts WHERE\n"
        + "{\n"
        +   "{ \n"
        +     "?s <"+this.swns+"hasValueHolder> ?vh .\n"
        +     "?vh <"+this.swns+"editedBy> '"+ userobj.getName()+"'^^<"+KBUtils.XSD+"string> .\n"
        +     "?vh <"+this.swns+"timestamp> ?ts .\n"
        +     "?vh ?p ?v .\n"
        +     "?p <"+KBUtils.RDFS+"subPropertyOf> ?pp .\n"
        +     "FILTER ( ?pp != <"+this.swns+"hasProvenance> )\n"
        +   "}\n"
        +   "UNION \n"
        +   "{ \n"
        +     "?sp <"+KBUtils.RDF+"type> <"+this.swns+"SoftwareParameter> .\n"
        +     "?sp <"+this.swns+"editedBy> '"+ userobj.getName()+"'^^<"+KBUtils.XSD+"string> .\n"
        +     "?sp <"+this.swns+"timestamp> ?ts .\n"
        +     "?s ?p ?sp \n"
        +   "}\n"
        + "}\n";
    
    
    ArrayList<UserContribution> contributions = new ArrayList<UserContribution>();
    
    for(ArrayList<SparqlQuerySolution> sols : this.swkb.sparqlQuery(query)) {
      UserContribution contrib = new UserContribution();
      for(SparqlQuerySolution sol : sols) {
        KBObject obj = sol.getObject();
        String var = sol.getVariable();
        if(var.equals("s"))
          contrib.setSoftwareId(obj.getID());
        else if(var.equals("p"))
          contrib.setPropertyId(obj.getID());
        else if(var.equals("v")) {
          if(obj.isLiteral())
            contrib.setValue(obj.getValue());
          else
            contrib.setValue(obj.getID());
        }
        else if(var.equals("ts"))
          contrib.setTimestamp((Long)obj.getValue());
      }
      contributions.add(contrib);
    }
    return contributions;
  }
  
  @Override
  public boolean save() {
    if(this.writerkb != null)
      return this.writerkb.save();
    return false;
  }

  @Override
  public void end() {
    if(this.kb != null)
      this.kb.end();
    if(this.swkb != null)
      this.swkb.end();
    if(this.writerkb != null)
      this.writerkb.end();
  }

  @Override
  public void delete() {
    this.writerkb.delete();
  }

}
