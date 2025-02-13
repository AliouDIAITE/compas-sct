// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl.ied;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DTO;
import org.lfenergy.compas.sct.commons.dto.ExtRefSignalInfo;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.ObjectReference;
import org.lfenergy.compas.sct.commons.scl.PrivateService;
import org.lfenergy.compas.sct.commons.scl.SclRootAdapter;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;
import org.lfenergy.compas.sct.commons.util.MonitoringLnClassEnum;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.lfenergy.compas.sct.commons.testhelpers.SclHelper.*;
import static org.mockito.Mockito.mock;

class IEDAdapterTest {

    private static final String SCD_IED_U_TEST = "/ied-test-schema-conf/ied_unit_test.xml";


    @Test
    void testAmChildElementRef() throws ScdException {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hID","hVersion","hRevision");
        TIED tied = new TIED();
        tied.setName(DTO.HOLDER_IED_NAME);

        tied.setServices(new TServices());
        sclRootAdapter.getCurrentElem().getIED().add(tied);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName(DTO.HOLDER_IED_NAME);
        assertTrue(iAdapter.amChildElementRef());
        assertNotNull(iAdapter.getServices());
        assertEquals(DTO.HOLDER_IED_NAME,iAdapter.getName());

        IEDAdapter fAdapter = new IEDAdapter(sclRootAdapter);
        TIED tied1 = new TIED();
        assertThrows(IllegalArgumentException.class,
                () ->fAdapter.setCurrentElem(tied1));

        assertThrows(ScdException.class,
                () -> sclRootAdapter.getIEDAdapterByName(DTO.HOLDER_IED_NAME + "1"));

    }

    @Test
    void streamLDeviceAdapters_should_return_all_lDevices() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        Stream<LDeviceAdapter> result = iAdapter.streamLDeviceAdapters();
        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    void findLDeviceAdapterByLdInst_should_return_LDevice() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        Optional<LDeviceAdapter> result = iAdapter.findLDeviceAdapterByLdInst("LD_INS1");
        // Then
        assertThat(result.map(LDeviceAdapter::getInst)).hasValue("LD_INS1");
    }

    @Test
    void findLDeviceAdapterByLdInst_when_not_found_should_return_empty_optional() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        Optional<LDeviceAdapter> result = iAdapter.findLDeviceAdapterByLdInst("NOT_EXISTING");
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getLDeviceAdapterByLdInst_should_return_LDevice() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        LDeviceAdapter result = iAdapter.getLDeviceAdapterByLdInst("LD_INS1");
        // Then
        assertThat(result.getInst()).isEqualTo("LD_INS1");
    }

    @Test
    void getLDeviceAdapterByLdInst_when_not_found_should_throw_exception() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When & Then
        assertThatThrownBy(() -> iAdapter.getLDeviceAdapterByLdInst("NOT_EXISTING"))
            .isInstanceOf(ScdException.class)
            .hasMessage("LDevice.inst 'NOT_EXISTING' not found in IED 'IED_NAME'");
    }

    @Test
    void testUpdateLDeviceNodesType() {

        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow( () -> sclRootAdapter.getIEDAdapterByName(DTO.HOLDER_IED_NAME));

        assertTrue(iAdapter.streamLDeviceAdapters().count() >= 2);
        Map<String,String> pairOldNewId = new HashMap<>();
        pairOldNewId.put("LNO1", DTO.HOLDER_IED_NAME + "_LNO1");
        pairOldNewId.put("LNO2", DTO.HOLDER_IED_NAME + "_LNO2");
        assertDoesNotThrow( () ->iAdapter.updateLDeviceNodesType(pairOldNewId));

        LDeviceAdapter lDeviceAdapter = iAdapter.streamLDeviceAdapters().findFirst().get();
        assertEquals(DTO.HOLDER_IED_NAME + "_LNO1",lDeviceAdapter.getLN0Adapter().getLnType());
    }

    @Test
    void testGetExtRefBinders() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));
        ExtRefSignalInfo signalInfo = DTO.createExtRefSignalInfo();
        signalInfo.setPDO("Do.sdo1");
        signalInfo.setPDA("da.bda1.bda2.bda3");
        assertDoesNotThrow(() ->iAdapter.getExtRefBinders(signalInfo));

        signalInfo.setPDO("Do.sdo1.errorSdo");
        assertThrows(ScdException.class, () ->iAdapter.getExtRefBinders(signalInfo));
    }

    @Test
    void TestIsSettingConfig() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        assertTrue(iAdapter.isSettingConfig("LD_INS1"));

        assertThrows(IllegalArgumentException.class,() -> iAdapter.isSettingConfig("UnknownLD"));
    }

    @Test
    void testMatches() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        ObjectReference objectReference = new ObjectReference("IED_NAMELD_INS3/LLN0.Do.da2");
        objectReference.init();
        assertTrue(iAdapter.matches(objectReference));

        objectReference = new ObjectReference("IED_NAMELD_INS2/ANCR1.dataSet");
        objectReference.init();
        assertTrue(iAdapter.matches(objectReference));
    }

    @Test
    void addPrivate() {
        SclRootAdapter sclRootAdapter = mock(SclRootAdapter.class);
        SCL scl = mock(SCL.class);
        Mockito.when(sclRootAdapter.getCurrentElem()).thenReturn(scl);
        TIED tied = new TIED();
        Mockito.when(scl.getIED()).thenReturn(List.of(tied));
        IEDAdapter iAdapter = new IEDAdapter(sclRootAdapter, tied);
        TPrivate tPrivate = new TPrivate();
        tPrivate.setType("Private Type");
        tPrivate.setSource("Private Source");
        assertTrue(iAdapter.getCurrentElem().getPrivate().isEmpty());
        iAdapter.addPrivate(tPrivate);
        assertEquals(1, iAdapter.getCurrentElem().getPrivate().size());
    }

    @Test
    void elementXPath() {
        // Given
        SclRootAdapter sclRootAdapter = mock(SclRootAdapter.class);
        SCL scl = mock(SCL.class);
        Mockito.when(sclRootAdapter.getCurrentElem()).thenReturn(scl);
        TIED tied = new TIED();
        tied.setName("iedName");
        Mockito.when(scl.getIED()).thenReturn(List.of(tied));
        IEDAdapter iAdapter = new IEDAdapter(sclRootAdapter, tied);
        // When
        String result = iAdapter.elementXPath();
        // Then
        assertThat(result).isEqualTo("IED[@name=\"iedName\"]");
    }

    @Test
    void checkDataGroupCoherence_should_succeed_no_error_message() {
        //Given
        IEDAdapter iedAdapter = provideIEDForCheckLimitationForIED();
        //When
        List<SclReportItem> sclReportItems = iedAdapter.checkDataGroupCoherence();
        //Then
        assertThat(sclReportItems).isEmpty();
    }

    @Test
    void checkDataGroupCoherence_should_fail_five_error_message() {
        //Given
        IEDAdapter iedAdapter = provideIEDForCheckLimitationForIED();
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getConfDataSet().setMaxAttributes(2L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getConfDataSet().setMax(5L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getSMVsc().setMax(2L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getGOOSE().setMax(2L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getConfReportControl().setMax(0L);
        //When
        List<SclReportItem> sclReportItems = iedAdapter.checkDataGroupCoherence();
        //Then
        assertThat(sclReportItems)
                .extracting(SclReportItem::getMessage)
                .containsExactlyInAnyOrder("There are too much FCDA for the DataSet dataset6 for the LDevice LD_INST21 in IED IED_NAME: 3 > 2 max",
                        "There are too much DataSets for the IED IED_NAME: 6 > 5 max",
                        "There are too much Report Control Blocks for the IED IED_NAME: 1 > 0 max",
                        "There are too much GOOSE Control Blocks for the IED IED_NAME: 3 > 2 max",
                        "There are too much SMV Control Blocks for the IED IED_NAME: 3 > 2 max");
    }

    @Test
    void checkBindingDataGroupCoherence_should_succeed_no_error_message() {
        //Given
        IEDAdapter iedAdapter = provideIEDForCheckLimitationForBoundIED();
        TClientServices tClientServices = iedAdapter.getParentAdapter().getIEDAdapterByName("IED_NAME1").getCurrentElem().getAccessPoint().get(0).getServices().getClientServices();
        tClientServices.setMaxAttributes(11L);
        tClientServices.setMaxGOOSE(5L);
        tClientServices.setMaxReports(2L);
        tClientServices.setMaxSMV(2L);
        //When
        List<SclReportItem> sclReportItems = iedAdapter.checkBindingDataGroupCoherence();
        //Then
        assertThat(sclReportItems).isEmpty();
    }

    @Test
    void checkBindingDataGroupCoherence_should_fail_five_error_message() {
        //Given
        IEDAdapter iedAdapter = provideIEDForCheckLimitationForBoundIED();
        //When
        List<SclReportItem> sclReportItems = iedAdapter.checkBindingDataGroupCoherence();
        //Then
        assertThat(sclReportItems).hasSize(4)
                .extracting(SclReportItem::getMessage)
                .containsExactlyInAnyOrder("The Client IED IED_NAME1 subscribes to too much FCDA: 9 > 8 max",
                    "The Client IED IED_NAME1 subscribes to too much Report Control Blocks: 1 > 0 max",
                    "The Client IED IED_NAME1 subscribes to too much SMV Control Blocks: 2 > 1 max",
                    "The Client IED IED_NAME1 subscribes to too much GOOSE Control Blocks: 3 > 2 max");

    }

    public static IEDAdapter provideIEDForCheckLimitationForBoundIED() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/limitation_cb_dataset_fcda/scd_check_limitation_bound_ied_controls_fcda.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        return sclRootAdapter.getIEDAdapterByName("IED_NAME1");
    }

    public static IEDAdapter provideIEDForCheckLimitationForIED() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/limitation_cb_dataset_fcda/scd_check_limitation_ied_controls_dataset.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        return sclRootAdapter.getIEDAdapterByName("IED_NAME");
    }

    @Test
    void getCompasICDHeader_should_return_compas_icd_header(){
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        TCompasICDHeader tCompasICDHeader = new TCompasICDHeader();
        tCompasICDHeader.setHeaderId("HEADER_ID");
        iedAdapter.getCurrentElem().getPrivate().add(PrivateService.createPrivate(tCompasICDHeader));

        // When
        Optional<TCompasICDHeader> compasICDHeader = iedAdapter.getCompasICDHeader();
        // Then
        assertThat(compasICDHeader).map(TCompasICDHeader::getHeaderId).hasValue("HEADER_ID");
    }

    @Test
    void getCompasSystemVersion_should_return_compas_icd_header(){
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        TCompasSystemVersion tCompasSystemVersion = new TCompasSystemVersion();
        tCompasSystemVersion.setMainSystemVersion("01.00");
        iedAdapter.getCurrentElem().getPrivate().add(PrivateService.createPrivate(tCompasSystemVersion));

        // When
        Optional<TCompasSystemVersion> compasSystemVersion = iedAdapter.getCompasSystemVersion();
        // Then
        assertThat(compasSystemVersion).map(TCompasSystemVersion::getMainSystemVersion).hasValue("01.00");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLnClassAndDoType")
    void manageMonitoringLns_should_not_update_ln_when_no_extRef(String testCase, MonitoringLnClassEnum lnClassEnum, String doName) {
        // Given
        IEDAdapter iedAdapter = createIedsInScl(lnClassEnum.value(), doName).getIEDAdapterByName(IED_NAME_1);
        // When
        List<SclReportItem> sclReportItems = iedAdapter.manageMonitoringLns();
        // Then
        LDeviceAdapter lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(LD_SUIED);
        assertThat(sclReportItems).isEmpty();
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(1);
        assertThat(lDeviceAdapter.getLNAdapters().get(0).getLNInst()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLnClassAndDoType")
    void manageMonitoringLns_should_not_create_ln_when_no_init_ln(String testCase, MonitoringLnClassEnum lnClassEnum, String doName, TServiceType tServiceType) {
        // Given
        IEDAdapter iedAdapter = createIedsInScl(lnClassEnum.value(), doName).getIEDAdapterByName(IED_NAME_1);
        TExtRef tExtRef = createExtRefExample("CB_Name", tServiceType);
        LDeviceAdapter lDAdapter = iedAdapter.getLDeviceAdapterByLdInst("LD_ADD");
        lDAdapter.getLN0Adapter().getCurrentElem().getInputs().getExtRef().add(tExtRef);
        LDeviceAdapter lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(LD_SUIED);
        lDeviceAdapter.getCurrentElem().unsetLN();
        // When
        List<SclReportItem> sclReportItems = iedAdapter.manageMonitoringLns();
        // Then
        assertThat(sclReportItems).isNotEmpty()
                .extracting(SclReportItem::getMessage)
                .containsExactly("There is no LN " + lnClassEnum.value() + " present in LDevice");
        assertThat(lDeviceAdapter.getLNAdapters()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLnClassAndDoType")
    void manageMonitoringLns_should_update_ln_when_one_extRef_and_dai_updatable(String testCase, MonitoringLnClassEnum lnClassEnum, String doName, TServiceType tServiceType) {
        // Given
        IEDAdapter iedAdapter = createIedsInScl(lnClassEnum.value(), doName).getIEDAdapterByName(IED_NAME_1);
        TExtRef tExtRef = createExtRefExample("CB_Name", tServiceType);
        LDeviceAdapter lDAdapter = iedAdapter.getLDeviceAdapterByLdInst("LD_ADD");
        lDAdapter.getLN0Adapter().getCurrentElem().getInputs().getExtRef().add(tExtRef);
        // When
        List<SclReportItem> sclReportItems = iedAdapter.manageMonitoringLns();
        // Then
        LDeviceAdapter lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(LD_SUIED);
        assertThat(sclReportItems).isEmpty();
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(1)
                .map(LNAdapter::getLNInst)
                .isEqualTo(List.of("1"));
        assertThat(getDaiValues(lDeviceAdapter, lnClassEnum.value(), doName, "setSrcRef"))
                .hasSize(1)
                .extracting(TVal::getValue)
                .containsExactly("LD_Name/LLN0.CB_Name");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLnClassAndDoType")
    void manageMonitoringLns_should_not_update_ln_when_one_extRef_and_dai_not_updatable(String testCase, MonitoringLnClassEnum lnClassEnum, String doName, TServiceType tServiceType) {
        // Given
        SclRootAdapter sclRootAdapter = createIedsInScl(lnClassEnum.value(), doName);
        sclRootAdapter.getDataTypeTemplateAdapter().getDOTypeAdapterById("REF").get().getDAAdapterByName("setSrcRef")
                .get().getCurrentElem().setValImport(false);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName(IED_NAME_1);
        TExtRef tExtRef = createExtRefExample("CB_Name", tServiceType);
        LDeviceAdapter lDAdapter = iedAdapter.getLDeviceAdapterByLdInst("LD_ADD");
        lDAdapter.getLN0Adapter().getCurrentElem().getInputs().getExtRef().add(tExtRef);
        // When
        List<SclReportItem> sclReportItems = iedAdapter.manageMonitoringLns();
        // Then
        LDeviceAdapter lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(LD_SUIED);
        assertThat(sclReportItems).isNotEmpty()
                .extracting(SclReportItem::getMessage)
                .containsExactly("The DAI cannot be updated");
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(1);
        assertThat(lDeviceAdapter.getLNAdapters().get(0).getLNInst()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLnClassAndDoType")
    void manageMonitoringLns_should_update_ln_when_2_extRef_and_dai_updatable(String testCase, MonitoringLnClassEnum lnClassEnum, String doName, TServiceType tServiceType) {
        // Given
        IEDAdapter iedAdapter = createIedsInScl(lnClassEnum.value(), doName).getIEDAdapterByName(IED_NAME_1);
        TExtRef tExtRef1 = createExtRefExample("CB_Name_1", tServiceType);
        TExtRef tExtRef2 = createExtRefExample("CB_Name_2", tServiceType);
        LDeviceAdapter lDAdapter = iedAdapter.getLDeviceAdapterByLdInst("LD_ADD");
        lDAdapter.getLN0Adapter().getCurrentElem().getInputs().getExtRef().addAll(List.of(tExtRef1, tExtRef2));
        // When
        List<SclReportItem> sclReportItems = iedAdapter.manageMonitoringLns();
        // Then
        LDeviceAdapter lDeviceAdapter = iedAdapter.getLDeviceAdapterByLdInst(LD_SUIED);
        assertThat(sclReportItems).isEmpty();
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(2)
                .map(LNAdapter::getLNInst)
                .isEqualTo(List.of("1", "2"));
        assertThat(getDaiValues(lDeviceAdapter, lnClassEnum.value(), doName, "setSrcRef"))
                .hasSize(2)
                .extracting(TVal::getValue)
                .containsExactly("LD_Name/LLN0.CB_Name_1", "LD_Name/LLN0.CB_Name_2");
    }

    private static Stream<Arguments> provideLnClassAndDoType() {
        return Stream.of(
                Arguments.of("Case GOOSE : ln LGOS", MonitoringLnClassEnum.LGOS, DO_GOCBREF, TServiceType.GOOSE),
                Arguments.of("Case SMV : ln LSVS", MonitoringLnClassEnum.LSVS, DO_SVCBREF, TServiceType.SMV)
        );
    }
}
