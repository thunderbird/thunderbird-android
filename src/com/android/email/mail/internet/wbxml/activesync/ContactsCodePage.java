package com.android.email.mail.internet.wbxml.activesync;

import com.android.email.mail.internet.wbxml.CodePage;

/**
 * This class is the specific code page for Contacts in the ActiveSync protocol.
 * The code page number is 1.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class ContactsCodePage extends CodePage {
    /**
     * Constructor for ContactsCodePage.  Initializes all of the code page values.
     */
    public ContactsCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("Anniversary", 0x05);
        codepageTokens.put("AssistantName", 0x06);
        codepageTokens.put("AssistantTelephoneNumber", 0x07);
        codepageTokens.put("Birthday", 0x08);
        codepageTokens.put("Body", 0x09);
        codepageTokens.put("BodySize", 0x0a);
        codepageTokens.put("BodyTruncated", 0x0b);
        codepageTokens.put("Business2TelephoneNumber", 0x0c);
        codepageTokens.put("BusinessAddressCity", 0x0d);
        codepageTokens.put("BusinessAddressCountry", 0x0e);
        codepageTokens.put("BusinessAddressPostalCode", 0x0f);
        codepageTokens.put("BusinessAddressState", 0x10);
        codepageTokens.put("BusinessAddressStreet", 0x11);
        codepageTokens.put("BusinessFaxNumber", 0x12);
        codepageTokens.put("BusinessTelephoneNumber", 0x13);
        codepageTokens.put("CarTelephoneNumber", 0x14);
        codepageTokens.put("Categories", 0x15);
        codepageTokens.put("Category", 0x16);
        codepageTokens.put("Children", 0x17);
        codepageTokens.put("Child", 0x18);
        codepageTokens.put("CompanyName", 0x19);
        codepageTokens.put("Department", 0x1a);
        codepageTokens.put("Email1Address", 0x1b);
        codepageTokens.put("Email2Address", 0x1c);
        codepageTokens.put("Email3Address", 0x1d);
        codepageTokens.put("FileAs", 0x1e);
        codepageTokens.put("FirstName", 0x1f);
        codepageTokens.put("Home2TelephoneNumber", 0x20);
        codepageTokens.put("HomeAddressCity", 0x21);
        codepageTokens.put("HomeAddressCountry", 0x22);
        codepageTokens.put("HomeAddressPostalCode", 0x23);
        codepageTokens.put("HomeAddressState", 0x24);
        codepageTokens.put("HomeAddressStreet", 0x25);
        codepageTokens.put("HomeFaxNumber", 0x26);
        codepageTokens.put("HomeTelephoneNumber", 0x27);
        codepageTokens.put("JobTitle", 0x28);
        codepageTokens.put("LastName", 0x29);
        codepageTokens.put("MiddleName", 0x2a);
        codepageTokens.put("MobileTelephoneNumber", 0x2b);
        codepageTokens.put("OfficeLocation", 0x2c);
        codepageTokens.put("OtherAddressCity", 0x2d);
        codepageTokens.put("OtherAddressCountry", 0x2e);
        codepageTokens.put("OtherAddressPostalCode", 0x2f);
        codepageTokens.put("OtherAddressState", 0x30);
        codepageTokens.put("OtherAddressStreet", 0x31);
        codepageTokens.put("PagerNumber", 0x32);
        codepageTokens.put("RadioTelephoneNumber", 0x33);
        codepageTokens.put("Spouse", 0x34);
        codepageTokens.put("Suffix", 0x35);
        codepageTokens.put("Title", 0x36);
        codepageTokens.put("Webpage", 0x37);
        codepageTokens.put("YomiCompanyName", 0x38);
        codepageTokens.put("YomiFirstName", 0x39);
        codepageTokens.put("YomiLastName", 0x3a);
        codepageTokens.put("CompressedRTF", 0x3b);
        codepageTokens.put("Picture", 0x3c);
        /* Maps token to string for the code page */
        codepageStrings.put(0x05, "Anniversary");
        codepageStrings.put(0x06, "AssistantName");
        codepageStrings.put(0x07, "AssistantTelephoneNumber");
        codepageStrings.put(0x08, "Birthday");
        codepageStrings.put(0x09, "Body");
        codepageStrings.put(0x0a, "BodySize");
        codepageStrings.put(0x0b, "BodyTruncated");
        codepageStrings.put(0x0c, "Business2TelephoneNumber");
        codepageStrings.put(0x0d, "BusinessAddressCity");
        codepageStrings.put(0x0e, "BusinessAddressCountry");
        codepageStrings.put(0x0f, "BusinessAddressPostalCode");
        codepageStrings.put(0x10, "BusinessAddressState");
        codepageStrings.put(0x11, "BusinessAddressStreet");
        codepageStrings.put(0x12, "BusinessFaxNumber");
        codepageStrings.put(0x13, "BusinessTelephoneNumber");
        codepageStrings.put(0x14, "CarTelephoneNumber");
        codepageStrings.put(0x15, "Categories");
        codepageStrings.put(0x16, "Category");
        codepageStrings.put(0x17, "Children");
        codepageStrings.put(0x18, "Child");
        codepageStrings.put(0x19, "CompanyName");
        codepageStrings.put(0x1a, "Department");
        codepageStrings.put(0x1b, "Email1Address");
        codepageStrings.put(0x1c, "Email2Address");
        codepageStrings.put(0x1d, "Email3Address");
        codepageStrings.put(0x1e, "FileAs");
        codepageStrings.put(0x1f, "FirstName");
        codepageStrings.put(0x20, "Home2TelephoneNumber");
        codepageStrings.put(0x21, "HomeAddressCity");
        codepageStrings.put(0x22, "HomeAddressCountry");
        codepageStrings.put(0x23, "HomeAddressPostalCode");
        codepageStrings.put(0x24, "HomeAddressState");
        codepageStrings.put(0x25, "HomeAddressStreet");
        codepageStrings.put(0x26, "HomeFaxNumber");
        codepageStrings.put(0x27, "HomeTelephoneNumber");
        codepageStrings.put(0x28, "JobTitle");
        codepageStrings.put(0x29, "LastName");
        codepageStrings.put(0x2a, "MiddleName");
        codepageStrings.put(0x2b, "MobileTelephoneNumber");
        codepageStrings.put(0x2c, "OfficeLocation");
        codepageStrings.put(0x2d, "OtherAddressCity");
        codepageStrings.put(0x2e, "OtherAddressCountry");
        codepageStrings.put(0x2f, "OtherAddressPostalCode");
        codepageStrings.put(0x30, "OtherAddressState");
        codepageStrings.put(0x31, "OtherAddressStreet");
        codepageStrings.put(0x32, "PagerNumber");
        codepageStrings.put(0x33, "RadioTelephoneNumber");
        codepageStrings.put(0x34, "Spouse");
        codepageStrings.put(0x35, "Suffix");
        codepageStrings.put(0x36, "Title");
        codepageStrings.put(0x37, "Webpage");
        codepageStrings.put(0x38, "YomiCompanyName");
        codepageStrings.put(0x39, "YomiFirstName");
        codepageStrings.put(0x3a, "YomiLastName");
        codepageStrings.put(0x3b, "CompressedRTF");
        codepageStrings.put(0x3c, "Picture");
        
        codePageIndex = 0x01;
        codePageName = "POOMCONTACTS";
    }
}
