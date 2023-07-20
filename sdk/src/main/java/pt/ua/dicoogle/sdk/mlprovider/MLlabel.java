package pt.ua.dicoogle.sdk.mlprovider;

import java.io.Serializable;
import java.util.Objects;

/**
 * A label object that belongs to a model.
 * Labels must be unique within a model.
 * The label definition proposed here follows the DICOM standard guidelines for segmentation objects.
 * @see C.8.20.2 Segmentation Image Module for more information.
 */
public class MLlabel implements Comparable<MLlabel>, Serializable {

    public enum CodingSchemeDesignator{
        DCM, // DICOM scheme code designator
        SRT, // SNOMED scheme code designator
        LN // LOINC scheme code designator
    }

    /**
     * DICOM Segment Label (0062, 0005) is a user defined label.
     */
    private String label;

    /**
     * DICOM Segment Description (0062, 0007) is a user defined description.
     */
    private String description;

    /**
     * A hex color string that specifies the color this label should have.
     */
    private String color;

    /**
     * DICOM Code Value (0008,0100) is an identifier that is unambiguous within the Coding Scheme denoted by Coding Scheme Designator (0008,0102) and Coding Scheme Version (0008,0103).
     */
    private String codeValue;

    /**
     * DICOM Code Meaning (0008,0104), a human-readable description of the label, <br>
     * given by the combination of Code Value and Coding Scheme Designator.
     */
    private String codeMeaning;

    /**
     * DICOM attribute Coding Scheme Designator (0008,0102) defines the coding scheme in which the code for a term is defined.
     * Typical values: "DCM" for DICOM defined codes, "SRT" for SNOMED and "LN" for LOINC
     */
    private CodingSchemeDesignator codingSchemeDesignator;

    public MLlabel(String label) {
        this.label = label;
        this.description = "unknown";
        this.codingSchemeDesignator = CodingSchemeDesignator.DCM;
        this.codeValue = "333333";
        this.codeMeaning = "unknown";
        this.color = "#000000";
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(String codeValue) {
        this.codeValue = codeValue;
    }

    public String getCodeMeaning() {
        return codeMeaning;
    }

    public void setCodeMeaning(String codeMeaning) {
        this.codeMeaning = codeMeaning;
    }

    public CodingSchemeDesignator getCodingSchemeDesignator() {
        return codingSchemeDesignator;
    }

    public void setCodingSchemeDesignator(CodingSchemeDesignator codingSchemeDesignator) {
        this.codingSchemeDesignator = codingSchemeDesignator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MLlabel mLlabel = (MLlabel) o;
        return label.equals(mLlabel.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public int compareTo(MLlabel o) {
        return o.getLabel().compareTo(this.getLabel());
    }
}
