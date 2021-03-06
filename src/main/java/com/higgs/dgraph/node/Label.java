package com.higgs.dgraph.node;

import java.util.List;

/**
 * User: JerryYou
 *
 * Date: 2018-05-10
 *
 * Copyright (c) 2018 devops
 *
 * <<licensetext>>
 */
public class Label extends EntityNode {

  private School school;

  private Company company;

  private Industry industry;  // 已测试通过，可建对应边

  private Major major;

  private List<School> schools; // 已测试通过，可建对应边

  public List<School> getSchools() {
    return schools;
  }

  public void setSchools(List<School> schools) {
    this.schools = schools;
  }

  public Major getMajor() {
    return major;
  }

  public void setMajor(Major major) {
    this.major = major;
  }

  public School getSchool() {
    return school;
  }

  public void setSchool(School school) {
    this.school = school;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public Industry getIndustry() {
    return industry;
  }

  public void setIndustry(Industry industry) {
    this.industry = industry;
  }
}
