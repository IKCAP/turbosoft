package org.earthcube.geosoft.community.api;

import java.util.ArrayList;

import org.earthcube.geosoft.community.classes.User;
import org.earthcube.geosoft.community.classes.UserContribution;

public interface CommunityAPI {
  void initializeUser(String username);
  
  ArrayList<String> listUserIds();
  
  ArrayList<User> getAllUsers();
  
  User getUser(String userid);
  
  boolean saveUser(User user);
  
  ArrayList<UserContribution> getUserContributions(String userid);
  
  // Sync/Save
  boolean save();
  
  void end();
  
  void delete();
}
