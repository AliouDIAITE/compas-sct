// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl.ied;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.lfenergy.compas.scl2007b4.model.TPredefinedCDCEnum;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateAdapter;
import org.lfenergy.compas.sct.commons.scl.dtt.LNodeTypeAdapter;

import java.util.List;
import java.util.Map;

/**
 * A representation of the model object
 * <em><b>{@link org.lfenergy.compas.sct.commons.scl.ied.DAITracker DAITracker}</b></em>.
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link DAITracker#search() <em>Compare Two <b>ObjectReference </b> sections </em>}</li>
 *   <li>{@link DAITracker#validateBoundedDAI() <em>Validate <b>ObjectReference</b> base <b>cdc </b> attribute </em>}</li>
 *   <li>{@link DAITracker#getDaiNumericValue(DaTypeName, double) <em>Returns value of the <b>bType </b> attribute By <b>DaTypeName </b> </em>}</li>
 * </ul>
 * <br/>
 * <pre>
 *     <b>ObjectReference</b>: LDName/LNName.DataName[.DataName[…]].DataAttributeName[.DAComponentName[ ….]]
 * </pre>
 * @see <a href="https://github.com/com-pas/compas-sct/issues/32" target="_blank">Issue !32</a>
 */
@Getter
public class DAITracker {
    /**
     * Value of indexDoType when DOI matching the DO is not instantiated
     */
    public static final int DOI_NOT_FOUND = -2;
    /**
     * When data has no BDA :   value of indexDaType when the DAI matching the DA is not instantiated
     * When data has BDA :      value of indexDaType when the SDI matching the DA is not instantiated
     */
    public static final int SDI_OR_DAI_NOT_FOUND_FOR_DA = -2; //
    private final AbstractLNAdapter<?> lnAdapter;
    private final DoTypeName doTypeName;
    private final DaTypeName daTypeName;

    private IDataParentAdapter doiOrSdoiAdapter;
    private int indexDoType = DOI_NOT_FOUND;
    private IDataAdapter bdaiOrDaiAdapter;
    private int indexDaType = SDI_OR_DAI_NOT_FOUND_FOR_DA;

    /**
     * Constructor
     * @param lnAdapter Parent container reference
     * @param doTypeName DOTypeName reference containing DO data leading to DA
     * @param daTypeName DATypeName reference containing linked DA to track data
     */
    public DAITracker(@NonNull AbstractLNAdapter<?> lnAdapter,
                      @NonNull DoTypeName doTypeName,
                      @NonNull DaTypeName daTypeName) {
        this.lnAdapter = lnAdapter;
        this.doTypeName = doTypeName;
        this.daTypeName = daTypeName;
    }

    /**
     * Checks DOI/SDI/DAI presence in LN (lnAdapter attribute) matching given DO/SDO (doTypeName attribute) and DA/BDA (daTypeName attribute)
     * As a reminder DOI/SDI/DAI chain matches DO/SDO/DA/BDA chain like this :
     * - DOI is equivalent to a DO
     * - SDI are equivalent to SDO and DA with bType="Struct" and BDA with bType="Struct". There can be no SDI when there are no SDO, nor DA(bType="Struct") , nor SDA(bType="Struct")
     * - DAI is the final element. It is equivalent to the final DA (when bType != "Struct") or the final BDA (BDA with bType != "Struct")
     *
     * @return one of <em>MatchResult</em> enum value :
     *      <ul>
     *          <li>FAILED</li> => no DOI found ( = nothing found)
     *          <li>PARTIAL_MATCH</li> => DOI found, and maybe some SDI found (matching SDO or DA), but DAI not found
     *          <li>FULL_MATCH</li> => DOI found, all intermediate SDI found and DAI found
     *      </ul>
     */
    public MatchResult search() {

        Pair<? extends IDataAdapter,Integer> matchResult;
        String doiName = doTypeName.getName();
        List<String> sdoiNames = doTypeName.getStructNames();
        try {
            doiOrSdoiAdapter = lnAdapter.getDOIAdapterByName(doiName);
            indexDoType = -1;
        } catch (ScdException e) {
            doiOrSdoiAdapter = null;
            return MatchResult.FAILED;
        }
        if(!sdoiNames.isEmpty()){
            matchResult = doiOrSdoiAdapter.findDeepestMatch(
                    sdoiNames,0,false
            );

            doiOrSdoiAdapter = matchResult.getLeft() != null ?
                    (IDataParentAdapter) matchResult.getLeft() : doiOrSdoiAdapter;
            indexDoType = matchResult.getRight();
            if( (indexDoType >= 0  && indexDoType < sdoiNames.size() - 1) ||
                    (indexDoType == -1 )){
                return MatchResult.PARTIAL_MATCH;
            }
        }

        if (!daTypeName.getStructNames().isEmpty()) {
            IDataParentAdapter firstDAIAdapter;
            try {
                firstDAIAdapter = doiOrSdoiAdapter.getStructuredDataAdapterByName(daTypeName.getName());
                indexDaType = -1;
            } catch (ScdException e) {
                return MatchResult.PARTIAL_MATCH;
            }
            matchResult = firstDAIAdapter.findDeepestMatch(
                    daTypeName.getStructNames(), 0, true
            );
            bdaiOrDaiAdapter = matchResult.getLeft();
            indexDaType = matchResult.getRight();
            if ( (indexDaType >= 0 && indexDaType < daTypeName.getStructNames().size() - 1) ||
                    (indexDaType == -1)){
                return MatchResult.PARTIAL_MATCH;
            }
        } else {
            try {
                bdaiOrDaiAdapter = doiOrSdoiAdapter.getDataAdapterByName(daTypeName.getName());
                indexDaType = -1;
            } catch (ScdException e) {
                return MatchResult.PARTIAL_MATCH;
            }
        }
        return MatchResult.FULL_MATCH;
    }

    /**
     * Validate if DAI Setting Group value is between boundaries (of DA BType)
     * @throws ScdException throws when value inconsistancy
     */
    public void validateBoundedDAI() throws ScdException {
        if(TPredefinedCDCEnum.ING != doTypeName.getCdc() && TPredefinedCDCEnum.ASG != doTypeName.getCdc() ){
            return;
        }
        Long sGroup = daTypeName.getDaiValues().keySet().stream().findFirst().orElse(-1L);
        String value = sGroup < 0 ? null : daTypeName.getDaiValues().get(sGroup);
        double val;
        try {
            val = Double.parseDouble(value);
        }  catch (NumberFormatException | NullPointerException e){
            throw new ScdException("Invalid DAI value :" + e.getMessage());
        }
        DataTypeTemplateAdapter dttAdapter = lnAdapter.getDataTypeTemplateAdapter();
        LNodeTypeAdapter lNodeTypeAdapter = dttAdapter.getLNodeTypeAdapterById(lnAdapter.getLnType())
                .orElseThrow(() -> new ScdException("Unknown LNodeType : " + lnAdapter.getLnType()));

        List<DataAttributeRef> dataAttributeRefs =  lNodeTypeAdapter.getDataAttributeRefByDoName(doTypeName);
        try{
            DataAttributeRef tempDataAttributeRef =  dataAttributeRefs.stream()
                    .filter(rData -> rData.getDaName().getName().equals("minVal")).findFirst().orElse(null);
            if(tempDataAttributeRef != null) {
                Map<Long, String> daiValues = lnAdapter.getDAIValues(tempDataAttributeRef);
                if (!daiValues.isEmpty()) {
                    tempDataAttributeRef.getDaName().setDaiValues(daiValues);
                }

                double min = getDaiNumericValue(tempDataAttributeRef.getDaName(), Double.MIN_VALUE);
                if (val < min) {
                    throw new ScdException(
                            String.format("The DA(%s) value(%f) must be greater than(%f)",daTypeName, val,min)
                    );
                }
            }

            tempDataAttributeRef =  dataAttributeRefs.stream()
                    .filter(rData -> rData.getDaName().getName().equals("maxVal")).findFirst().orElse(null);
            if(tempDataAttributeRef != null) {
                Map<Long, String> daiValues = lnAdapter.getDAIValues(tempDataAttributeRef);
                if (!daiValues.isEmpty()) {
                    tempDataAttributeRef.getDaName().setDaiValues(daiValues);
                }

                double max = getDaiNumericValue(tempDataAttributeRef.getDaName(), Double.MAX_VALUE);
                if (val > max) {
                    throw new ScdException(
                            String.format("The DA(%s) value(%f) must be less than(%f)",daTypeName, val,max)
                    );
                }
            }

            tempDataAttributeRef =  dataAttributeRefs.stream()
                    .filter(rData -> rData.getDaName().getName().equals("stepSize")).findFirst().orElse(null);
            if(tempDataAttributeRef != null) {
                Map<Long, String> daiValues = lnAdapter.getDAIValues(tempDataAttributeRef);
                if (!daiValues.isEmpty()) {
                    tempDataAttributeRef.getDaName().setDaiValues(daiValues);
                }

                double step =  getDaiNumericValue(tempDataAttributeRef.getDaName(),val);
                if (Math.abs((int)val)% Math.abs((int)step) > Math.pow(10,-9)) {
                    throw new ScdException(
                            String.format("The DA(%s) value(%f) divisible by (%f)",daTypeName,val,step)
                    );
                }
            }
        } catch (NumberFormatException e){
            throw new ScdException("Invalid DO(minVal or maxVal or stepSize) :" + e.getMessage());
        }
    }

    /**
     * Gets DAI value from DaTypeName for specific known types and throws exception if unknown type
     * @param daTypeName contains DA information
     * @param defaultValue default init value
     * @return <em>Double</em> corresponding value for DAI val
     */
    protected double getDaiNumericValue(DaTypeName daTypeName, double defaultValue) {
        String value = daTypeName.getDaiValues().values().stream().findFirst().orElse(null);
        if(value == null){
            return defaultValue;
        }

        if(daTypeName.getBType() == null){
            throw new NumberFormatException(String.format("Undefined DAI(%s) basic Type", daTypeName));
        }
        switch (daTypeName.getBType()){
            case INT_8:
            case INT_8_U:
            case INT_16:
            case INT_16_U:
            case INT_32:
            case INT_32_U:
            case INT_64:
            case FLOAT_32:
            case FLOAT_64:
                return Double.valueOf(value);
            default:
                throw new NumberFormatException("Unknown numeric format");

        }
    }

    /**
     * Enumeration of three states for matching check (<em>FAILED</em>, <em>PARTIAL_MATCH</em>, <em>FULL_MATCH</em>)
     */
    public enum MatchResult {
        FAILED("FAILED"),
        PARTIAL_MATCH("PARTIAL_MATCH"),
        FULL_MATCH("FULL_MATCH");

        private final String value;
        MatchResult(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static MatchResult fromValue(String text) {

            for (MatchResult b : MatchResult.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }
}
