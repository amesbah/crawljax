//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.09.16 at 12:27:14 AM PDT 
//

package com.crawljax.core.plugin.descriptor.jaxb.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for version-list complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <pre>
 * &lt;complexType name="version-list"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "version-list", propOrder = {"version"})
public class VersionList {

  @XmlElement(required = true)
  protected List<String> version;

  /**
   * Gets the value of the version property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a
   * <CODE>set</CODE> method for the version property.
   * <p>
   * For example, to add a new item, do as follows:
   * <pre>
   * getVersion().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link String }
   */
  public List<String> getVersion() {
    if (version == null) {
      version = new ArrayList<>();
    }
    return this.version;
  }

}
