/*
 * // SPDX-FileCopyrightText: 2023 RTE FRANCE
 * //
 * // SPDX-License-Identifier: Apache-2.0
 */

package org.lfenergy.compas.sct.commons.scl.ied;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.SclElementAdapter;
import org.lfenergy.compas.sct.commons.util.ServicesConfigEnum;
import org.lfenergy.compas.sct.commons.util.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lfenergy.compas.sct.commons.scl.ExtRefService.filterDuplicatedExtRefs;

/**
 * A representation of the model object
 * <em><b>{@link org.lfenergy.compas.scl2007b4.model.TAccessPoint AccessPoint}</b></em>.
 * <p>
 * The following features are supported:
 * </p>
 * <ol>
 *   <li>Functions</li>
 *    <ul>
 *      <li>{@link AccessPointAdapter#checkFCDALimitations <em>Returns the value of the <b>name </b>attribute</em>}</li>
 *      <li>{@link AccessPointAdapter#checkControlsLimitation Returns the value of the <b>Service </b>object</em>}</li>
 *    </ul>
 * </ol>
 */

public class AccessPointAdapter extends SclElementAdapter<IEDAdapter, TAccessPoint> {

    public static final long MAX_OCCURRENCE_NO_LIMIT_VALUE = -1L;

    /**
     * Constructor
     *
     * @param parentAdapter Parent container reference
     * @param tAccessPoint  Current reference
     */
    public AccessPointAdapter(IEDAdapter parentAdapter, TAccessPoint tAccessPoint) {
        super(parentAdapter, tAccessPoint);
    }

    /**
     * Check if current element is a child of the parent element
     *
     * @return true if the currentElem is part of the parentAdapter children
     */
    @Override
    protected boolean amChildElementRef() {
        return parentAdapter.getCurrentElem().getAccessPoint().stream()
                .anyMatch(tAccessPoint -> tAccessPoint.getName().equals(currentElem.getName()));
    }

    @Override
    protected String elementXPath() {
        return String.format("AccessPoint[%s]", Utils.xpathAttributeFilter("name", currentElem.isSetName() ? currentElem.getName() : null));
    }

    /**
     * Gets all LDevice from AccessPoint
     *
     * @return Stream of <em>LDeviceAdapter</em> object as IEDs of SCL
     */
    private Stream<LDeviceAdapter> streamLDeviceAdapters() {
        if (!currentElem.isSetServer()) return Stream.empty();
        return currentElem.getServer().getLDevice().stream()
                .map(tlDevice -> new LDeviceAdapter(getParentAdapter(), tlDevice));
    }

    /**
     * Checks FCDA number limitation into each LDevice of AccessPoint
     *
     * @return List of errors encountered for LDevices
     */
    public List<SclReportItem> checkFCDALimitations() {
        long max = getMaxInstanceAuthorized(ServicesConfigEnum.FCDA);
        if (currentElem.getServer() == null || max == MAX_OCCURRENCE_NO_LIMIT_VALUE) return Collections.emptyList();
        return currentElem.getServer().getLDevice().stream()
                .map(tlDevice -> new LDeviceAdapter(parentAdapter, tlDevice))
                .map(lDeviceAdapter ->
                        lDeviceAdapter.getLNAdaptersIncludingLN0().stream()
                                .map(abstractLNAdapter -> abstractLNAdapter.getCurrentElem().getDataSet())
                                .flatMap(Collection::stream)
                                .filter(tDataSet -> tDataSet.getFCDA().size() > max)
                                .map(tDataSet -> SclReportItem.fatal(getXPath(), String.format("There are too much FCDA for the DataSet %s for the LDevice %s"
                                        + " in IED %s: %d > %d max", tDataSet.getName(), lDeviceAdapter.getInst(), parentAdapter.getName(),
                                    tDataSet.getFCDA().size(), max))
                                ).toList()
                ).flatMap(Collection::stream).toList();
    }

    /**
     * Checks if occurrences of specified tpe (DataSet, Controls) exceeds config limitation
     *
     * @param servicesConfigEnum type of element for which limitation is checked
     * @return Optional of encountered error or empty
     */
    public Optional<SclReportItem> checkControlsLimitation(ServicesConfigEnum servicesConfigEnum) {
        long max = getMaxInstanceAuthorized(servicesConfigEnum);
        long value = getNumberOfItems(servicesConfigEnum);
        return max == MAX_OCCURRENCE_NO_LIMIT_VALUE || value <= max ? Optional.empty() : Optional.of(SclReportItem.fatal(getXPath(),
            String.format("There are too much %ss for the IED %s: %d > %d max", servicesConfigEnum.getDisplayName(), parentAdapter.getName(), value, max)));
    }

    /**
     * Counts all occurrence of Control into AccessPoint
     *
     * @param servicesConfigEnum type (GOOSE, Report, SampledValue, DataSet)
     * @return number of occurrence
     */
    private long getNumberOfItems(ServicesConfigEnum servicesConfigEnum) {
        if (!currentElem.isSetServer()) return 0L;
        return currentElem.getServer().getLDevice().stream()
                .map(tlDevice -> new LDeviceAdapter(parentAdapter, tlDevice))
                .map(lDeviceAdapter -> {
                    List<AbstractLNAdapter<?>> list = new ArrayList<>();
                    if (servicesConfigEnum == ServicesConfigEnum.GSE || servicesConfigEnum == ServicesConfigEnum.SMV)
                        list.add(lDeviceAdapter.getLN0Adapter());
                    else list.addAll(lDeviceAdapter.getLNAdaptersIncludingLN0());
                    return list;
                })
                .flatMap(Collection::stream)
                .map(abstractLNAdapter -> {
                    if (servicesConfigEnum == ServicesConfigEnum.DATASET) {
                        return abstractLNAdapter.getCurrentElem().getDataSet();
                    } else {
                        return abstractLNAdapter.getTControlsByType(getControlTypeClass(servicesConfigEnum));
                    }
                })
                .mapToLong(Collection::size).sum();
    }

    private Class<? extends TControl> getControlTypeClass(ServicesConfigEnum servicesConfigEnum) {
        return switch (servicesConfigEnum) {
            case REPORT -> TReportControl.class;
            case GSE -> TGSEControl.class;
            case SMV -> TSampledValueControl.class;
            default -> throw new ScdException("Unknown Control Block Type: " + servicesConfigEnum);
        };
    }

    /**
     * Gets max number authorized in configuration of each element (DataSets, FCDAs, Control Blocks) into an AccessPoint
     *
     * @param servicesConfigEnum element type
     * @return max number authorized by config
     */
    private long getMaxInstanceAuthorized(ServicesConfigEnum servicesConfigEnum) {
        if (currentElem.getServices() == null)
            return MAX_OCCURRENCE_NO_LIMIT_VALUE;
        TServices tServices = currentElem.getServices();

        return switch (servicesConfigEnum) {
            case DATASET ->
                    tServices.isSetConfDataSet() && tServices.getConfDataSet().isSetMax() ?
                        tServices.getConfDataSet().getMax() : MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case FCDA ->
                    tServices.isSetConfDataSet() && tServices.getConfDataSet().isSetMaxAttributes() ?
                        tServices.getConfDataSet().getMaxAttributes() : MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case REPORT ->
                tServices.isSetConfReportControl() && tServices.getConfReportControl().isSetMax() ?
                    tServices.getConfReportControl().getMax() : MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case GSE -> tServices.isSetGOOSE() && tServices.getGOOSE().isSetMax() ? tServices.getGOOSE().getMax() : MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case SMV -> tServices.isSetSMVsc() && tServices.getSMVsc().isSetMax() ? tServices.getSMVsc().getMax() : MAX_OCCURRENCE_NO_LIMIT_VALUE;
        };
    }


    /**
     * Checks FCDA number limitation for bound IED
     *
     * @return Optional of encountered error or empty
     */
    public Optional<SclReportItem> checkLimitationForBoundIedFcdas(List<TExtRef> tExtRefs) {
        long max;
        if (currentElem.getServices() == null) {
            max = MAX_OCCURRENCE_NO_LIMIT_VALUE;
        } else {
            TClientServices tClientServices = currentElem.getServices().getClientServices();
            max = tClientServices != null && tClientServices.isSetMaxAttributes() ? tClientServices.getMaxAttributes() : MAX_OCCURRENCE_NO_LIMIT_VALUE;
        }
        if (max == MAX_OCCURRENCE_NO_LIMIT_VALUE){
            return Optional.empty();
        }
        long value = tExtRefs.stream()
                .map(tExtRef -> {
                    IEDAdapter iedAdapter = getParentAdapter().getParentAdapter().getIEDAdapterByName(tExtRef.getIedName());
                    LDeviceAdapter lDeviceAdapter;
                    if (tExtRef.getSrcLDInst() != null) {
                        lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(tExtRef.getSrcLDInst());
                    } else {
                        lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(tExtRef.getLdInst());
                    }
                    AbstractLNAdapter<?> abstractLNAdapter;
                    if (!tExtRef.isSetSrcLNClass() || tExtRef.getSrcLNClass().contains(TLLN0Enum.LLN_0.value())) {
                        abstractLNAdapter = lDeviceAdapter.getLN0Adapter();
                    } else {
                        abstractLNAdapter = lDeviceAdapter.getLNAdapter(tExtRef.getSrcLNClass().get(0), tExtRef.getSrcLNInst(), tExtRef.getSrcPrefix());
                    }
                    return abstractLNAdapter.getFCDAs(tExtRef);
                })
                .flatMap(Collection::stream)
                .toList()
                .size();

        return value <= max ? Optional.empty() :
                Optional.of(SclReportItem.fatal(getParentAdapter().getXPath(),
                    "The Client IED %s subscribes to too much FCDA: %d > %d max".formatted(getParentAdapter().getName(), value, max)));
    }

    /**
     * @param sclReportItems
     * @param tExtRefs
     */
    public record ExtRefAnalyzeRecord(List<SclReportItem> sclReportItems, List<TExtRef> tExtRefs) {
    }

    /**
     * Returns all ExtRef of the AccessPoint which have SrcCBName set and ServiceType provided
     *
     * @return ExtRefAnalyzeRecord object containing Set of ExtRefs and errors list for ExtRefs with SrcCBName and without ServiceType provided
     */
    public ExtRefAnalyzeRecord getAllCoherentExtRefForAnalyze() {
        List<SclReportItem> sclReportItems = new ArrayList<>();
        List<TExtRef> tExtRefList = streamLDeviceAdapters()
                .map(LDeviceAdapter::getLN0Adapter)
                .map(ln0Adapter -> {
                    List<TExtRef> extRefs = new ArrayList<>();
                    if (ln0Adapter.hasInputs()) {
                        extRefs.addAll(ln0Adapter.getInputsAdapter().filterDuplicatedExtRefs()
                                .stream().filter(TExtRef::isSetSrcCBName).collect(Collectors.toCollection(ArrayList::new)));
                        sclReportItems.addAll(checkExtRefWithoutServiceType(extRefs, ln0Adapter.getXPath()));
                        extRefs.removeIf(tExtRef -> !tExtRef.isSetServiceType());
                    }
                    return extRefs;
                }).flatMap(Collection::stream)
                .toList();
        return new ExtRefAnalyzeRecord(sclReportItems, filterDuplicatedExtRefs(tExtRefList));
    }

    /**
     * Checks all ExtRefs with SrcCBName and without ServiceType provided
     *
     * @param tExtRefs Set of ExtRefs to check
     * @return errors list
     */
    private List<SclReportItem> checkExtRefWithoutServiceType(List<TExtRef> tExtRefs, String xPath) {
        return tExtRefs.stream()
                .filter(tExtRef -> !tExtRef.isSetServiceType())
                .map(tExtRef ->
                        SclReportItem.fatal("%s/Inputs/ExtRef[%s]".formatted(xPath,
                            Utils.xpathAttributeFilter("desc", tExtRef.getDesc())),
                            "ExtRef is missing ServiceType attribute"))
                .toList();
    }

    /**
     * Checks Control Blocks (Report, Goose, SMV) number limitation for bound IED
     *
     * @return List of errors encountered
     */
    public List<SclReportItem> checkLimitationForBoundIEDControls(List<TExtRef> tExtRefs) {
        Map<TServiceType, Set<TExtRef>> extRefsByServiceType = tExtRefs.stream()
                .filter(TExtRef::isSetServiceType)
                .collect(Collectors.groupingBy(TExtRef::getServiceType, Collectors.toSet()));
        return extRefsByServiceType.entrySet().stream()
                .map(entry -> checkLimitationForOneControlType(entry.getValue(), ServicesConfigEnum.from(entry.getKey())))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Checks Control Block number limitation for bound IED
     *
     * @param tExtRefs           list of ExtRefs referenced same ied
     * @param servicesConfigEnum type of Control Block for which check is done
     * @return Optional of encountered error or empty
     */
    private Optional<SclReportItem> checkLimitationForOneControlType(Set<TExtRef> tExtRefs, ServicesConfigEnum servicesConfigEnum) {
        long max = getMaxInstanceAuthorizedForBoundIED(servicesConfigEnum);
        long value = tExtRefs.size();
        return max == AccessPointAdapter.MAX_OCCURRENCE_NO_LIMIT_VALUE || value <= max ? Optional.empty() :
            Optional.of(SclReportItem.fatal(getParentAdapter().getXPath(),
                "The Client IED %s subscribes to too much %ss: %d > %d max".formatted(getParentAdapter().getName(), servicesConfigEnum.getDisplayName(),
                    value, max)));
    }

    /**
     * Gets max number authorized in configuration of each element ( DataSets, FCDAs, Control Blocks) into an AccessPoint
     *
     * @param servicesConfigEnum element type
     * @return max number authorized by config
     */
    private long getMaxInstanceAuthorizedForBoundIED(ServicesConfigEnum servicesConfigEnum) {
        if (currentElem.getServices() == null || currentElem.getServices().getClientServices() == null) {
            return AccessPointAdapter.MAX_OCCURRENCE_NO_LIMIT_VALUE;
        }
        TClientServices tClientServices = currentElem.getServices().getClientServices();
        return switch (servicesConfigEnum) {
            case FCDA ->
                    tClientServices.isSetMaxAttributes() ? tClientServices.getMaxAttributes() : AccessPointAdapter.MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case REPORT ->
                    tClientServices.isSetMaxReports() ? tClientServices.getMaxReports() : AccessPointAdapter.MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case GSE ->
                    tClientServices.isSetMaxGOOSE() ? tClientServices.getMaxGOOSE() : AccessPointAdapter.MAX_OCCURRENCE_NO_LIMIT_VALUE;
            case SMV ->
                    tClientServices.isSetMaxSMV() ? tClientServices.getMaxSMV() : AccessPointAdapter.MAX_OCCURRENCE_NO_LIMIT_VALUE;
            default -> throw new ScdException("Unsupported value: " + servicesConfigEnum);
        };

    }


}
