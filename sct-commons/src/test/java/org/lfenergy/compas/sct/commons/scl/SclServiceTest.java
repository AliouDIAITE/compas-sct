// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.*;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.ied.*;
import org.lfenergy.compas.sct.commons.testhelpers.MarshallerWrapper;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.lfenergy.compas.sct.commons.testhelpers.DataTypeUtils.createDa;
import static org.lfenergy.compas.sct.commons.testhelpers.DataTypeUtils.createDo;
import static org.lfenergy.compas.sct.commons.testhelpers.SclHelper.LD_SUIED;
import static org.lfenergy.compas.sct.commons.testhelpers.SclHelper.getDAIAdapters;
import static org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller.assertIsMarshallable;
import static org.lfenergy.compas.sct.commons.util.PrivateEnum.COMPAS_SCL_FILE_TYPE;

class SclServiceTest {

    private static Stream<Arguments> sclProviderMissingRequiredObjects() {
        SCL scl1 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_KO_MissingBeh.scd");
        SCL scl2 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_KO_MissingLDevicePrivate.scd");
        SCL scl3 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_KO_MissingLDevicePrivateAttribute.scd");
        SCL scl4 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_KO_MissingMod.scd");
        Tuple[] scl1Errors = new Tuple[]{Tuple.tuple("The LDevice doesn't have a DO @name='Beh' OR its associated DA@fc='ST' AND DA@name='stVal'",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0")};
        Tuple[] scl2Errors = new Tuple[]{Tuple.tuple("The LDevice doesn't have a Private compas:LDevice.",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0")};
        Tuple[] scl3Errors = new Tuple[]{Tuple.tuple("The Private compas:LDevice doesn't have the attribute 'LDeviceStatus'",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0")};
        Tuple[] scl4Errors = new Tuple[]{Tuple.tuple("The LDevice doesn't have a DO @name='Mod'",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0")};
        return Stream.of(
                Arguments.of("MissingDOBeh", scl1, scl1Errors),
                Arguments.of("MissingLDevicePrivate", scl2, scl2Errors),
                Arguments.of("MissingLDevicePrivateAttribute", scl3, scl3Errors),
                Arguments.of("MissingDOMod", scl4, scl4Errors)
        );
    }

    private static Stream<Arguments> sclProviderBasedLDeviceStatus() {
        SCL scl1 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_LD_STATUS_ACTIVE.scd");
        SCL scl2 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_LD_STATUS_UNTESTED.scd");
        SCL scl3 = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test1_LD_STATUS_INACTIVE.scd");
        Tuple[] scl1Errors = new Tuple[]{Tuple.tuple("The LDevice cannot be set to 'off' but has not been selected into SSD.",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                Tuple.tuple("The LDevice cannot be set to 'on' but has been selected into SSD.",
                        "/SCL/IED[@name=\"IedName2\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                Tuple.tuple("The LDevice cannot be activated or desactivated because its BehaviourKind Enum contains NOT 'on' AND NOT 'off'.",
                        "/SCL/IED[@name=\"IedName3\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"
                )};
        Tuple[] scl2Errors = new Tuple[]{Tuple.tuple("The LDevice cannot be set to 'off' but has not been selected into SSD.",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                Tuple.tuple("The LDevice cannot be set to 'on' but has been selected into SSD.",
                        "/SCL/IED[@name=\"IedName2\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                Tuple.tuple("The LDevice cannot be activated or desactivated because its BehaviourKind Enum contains NOT 'on' AND NOT 'off'.",
                        "/SCL/IED[@name=\"IedName3\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"
                )};
        Tuple[] scl3Errors = new Tuple[]{Tuple.tuple("The LDevice is not qualified into STD but has been selected into SSD.",
                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                Tuple.tuple("The LDevice cannot be set to 'on' but has been selected into SSD.",
                        "/SCL/IED[@name=\"IedName2\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                Tuple.tuple("The LDevice cannot be activated or desactivated because its BehaviourKind Enum contains NOT 'on' AND NOT 'off'.",
                        "/SCL/IED[@name=\"IedName3\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"
                )};
        return Stream.of(
                Arguments.of("ACTIVE", scl1, scl1Errors),
                Arguments.of("UNTESTED", scl2, scl2Errors),
                Arguments.of("INACTIVE", scl3, scl3Errors)
        );
    }

    @Test
    void testAddHistoryItem() throws ScdException {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();

        SclService.addHistoryItem(scd, "who", "what", "why");

        assertNotNull(scd.getHeader());
        THeader.History history = scd.getHeader().getHistory();
        assertNotNull(history);
        assertEquals(1, history.getHitem().size());
        THitem tHitem = history.getHitem().get(0);
        assertEquals("who", tHitem.getWho());
        assertEquals("what", tHitem.getWhat());
        assertEquals("why", tHitem.getWhy());
        assertEquals(SclRootAdapter.REVISION, tHitem.getRevision());
        assertEquals(SclRootAdapter.VERSION, tHitem.getVersion());
        assertIsMarshallable(scd);
    }

    @Test
    void testAddIED() {

        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        IEDAdapter iedAdapter = assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));
        assertEquals("IED_NAME1", iedAdapter.getName());
        assertNotNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());

        assertIsMarshallable(scd);
    }

    @Test
    void testAddSubnetworks() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));

        SubNetworkDTO subNetworkDTO = new SubNetworkDTO();
        subNetworkDTO.setName("sName1");
        subNetworkDTO.setType("IP");
        ConnectedApDTO connectedApDTO = new ConnectedApDTO();
        connectedApDTO.setApName("AP_NAME");
        connectedApDTO.setIedName("IED_NAME1");
        subNetworkDTO.addConnectedAP(connectedApDTO);

        assertDoesNotThrow(() -> SclService.addSubnetworks(scd, Set.of(subNetworkDTO), Optional.of(icd)).get());
        assertIsMarshallable(scd);
    }

    @Test
    void testAddSubnetworksWithoutCommunicationTagInIcd() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));

        assertDoesNotThrow(() -> SclService.addSubnetworks(scd, new HashSet<>(), Optional.of(icd)));
        String marshalledScd = assertIsMarshallable(scd);
        assertThat(marshalledScd).doesNotContain("<Communication");
    }

    @Test
    void testAddSubnetworksWithFilledCommunication() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_with_filled_communication.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));

        Set<SubNetworkDTO> subNetworkDTOSet = new HashSet<>(SclService.getSubnetwork(icd));
        assertDoesNotThrow(() -> SclService.addSubnetworks(scd, subNetworkDTOSet, Optional.of(icd)).get());

        String marshalledScd = assertIsMarshallable(scd);
        assertThat(marshalledScd).contains("<Address>", "PhysConn");
    }

    @Test
    void testAddSubnetworksWithoutImportingIcdAddressAndPhysConn() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_with_filled_communication.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));

        Set<SubNetworkDTO> subNetworkDTOSet = new HashSet<>(SclService.getSubnetwork(icd));
        assertDoesNotThrow(() -> SclService.addSubnetworks(scd, subNetworkDTOSet, Optional.empty()).get());

        String marshalledScd = assertIsMarshallable(scd);
        assertThat(marshalledScd).doesNotContain("<Address>", "PhysConn");
    }

    @Test
    void testGetSubnetwork() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));

        SubNetworkDTO subNetworkDTO = new SubNetworkDTO();
        subNetworkDTO.setName("sName1");
        subNetworkDTO.setType("IP");
        ConnectedApDTO connectedApDTO = new ConnectedApDTO();
        connectedApDTO.setApName("AP_NAME");
        connectedApDTO.setIedName("IED_NAME1");
        subNetworkDTO.addConnectedAP(connectedApDTO);

        assertDoesNotThrow(() -> SclService.addSubnetworks(scd, Set.of(subNetworkDTO), Optional.of(icd)).get());

        List<SubNetworkDTO> subNetworkDTOS = assertDoesNotThrow(() -> SclService.getSubnetwork(scd));
        assertEquals(1, subNetworkDTOS.size());
    }

    @Test
    void testGetExtRefInfo() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd));
        var extRefInfos = assertDoesNotThrow(() -> SclService.getExtRefInfo(scd, "IED_NAME1", "LD_INST11"));
        assertEquals(1, extRefInfos.size());

        assertEquals("IED_NAME1", extRefInfos.get(0).getHolderIEDName());

        assertThrows(ScdException.class, () -> SclService.getExtRefInfo(scd, "IED_NAME1", "UNKNOWN_LD"));
    }

    @Test
    void getExtRefBinders_shouldThowScdException_whenExtRefNotExist() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_binders_test.xml");

        ExtRefSignalInfo signalInfo = createSignalInfo("Do11.sdo11", "da11.bda111.bda112.bda113", "INT_ADDR11");
        signalInfo.setPLN("ANCR");
        //When Then
        assertThatThrownBy(
                () -> SclService.getExtRefBinders(scd, "IED_NAME1", "UNKNOWN_LD", "LLN0", "", "", signalInfo))
                .isInstanceOf(ScdException.class);
    }

    @Test
    void getExtRefBinders_shouldReturnSortedListBindingInfo_whenExtRefAndDOExist() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_binders_test.xml");

        ExtRefSignalInfo signalInfo = createSignalInfo(
                "Do11.sdo11", "da11.bda111.bda112.bda113", "INT_ADDR11"
        );
        signalInfo.setPLN("ANCR");

        // When
        List<ExtRefBindingInfo> potentialBinders = SclService.getExtRefBinders(scd, "IED_NAME1", "LD_INST11", "LLN0", "", "", signalInfo);

        // Then
        assertThat(potentialBinders).hasSize(4);
        assertThat(potentialBinders)
                .extracting(ExtRefBindingInfo::getIedName)
                .containsExactly("IED_NAME1", "IED_NAME1", "IED_NAME2", "IED_NAME3");
        assertThat(potentialBinders)
                .extracting(ExtRefBindingInfo::getLdInst)
                .containsExactly("LD_INST11", "LD_INST12", "LD_INST22", "LD_INST31");
        assertThat(potentialBinders)
                .extracting(ExtRefBindingInfo::getLnClass)
                .containsExactly("ANCR", "ANCR", "ANCR", "ANCR");
        assertThat(potentialBinders)
                .extracting(ExtRefBindingInfo::getLnInst)
                .containsExactly("1", "1", "2", "3");
    }

    @Test
    void testUpdateExtRefBinders() {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hId", SclRootAdapter.VERSION, SclRootAdapter.REVISION);
        SCL scd = sclRootAdapter.getCurrentElem();
        assertNull(sclRootAdapter.getCurrentElem().getDataTypeTemplates());
        SCL icd1 = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");
        SCL icd2 = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_2_test.xml");

        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME1", icd1));
        assertDoesNotThrow(() -> SclService.addIED(scd, "IED_NAME2", icd2));

        ExtRefSignalInfo signalInfo = createSignalInfo(
                "Do11.sdo11", "da11.bda111.bda112.bda113", "INT_ADDR11"
        );
        signalInfo.setPServT(null);
        signalInfo.setPLN(null);
        signalInfo.setDesc(null);
        // Signal for external binding (in IED 2 LD_INST22 - PIOC)
        ExtRefBindingInfo bindingInfo = new ExtRefBindingInfo();
        bindingInfo.setIedName("IED_NAME2");
        bindingInfo.setLdInst("LD_INST22");
        bindingInfo.setLnClass("PIOC");
        bindingInfo.setLnInst("1");
        bindingInfo.setLnType("LN2");
        bindingInfo.setDoName(new DoTypeName(signalInfo.getPDO()));
        bindingInfo.setDaName(new DaTypeName(signalInfo.getPDA()));
        bindingInfo.setServiceType(signalInfo.getPServT());
        LNodeDTO lNodeDTO = new LNodeDTO();
        lNodeDTO.setNodeClass(TLLN0Enum.LLN_0.value());
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME1");
        extRefInfo.setHolderLDInst("LD_INST11");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());
        extRefInfo.setSignalInfo(signalInfo);
        extRefInfo.setBindingInfo(bindingInfo);
        lNodeDTO.getExtRefs().add(extRefInfo);

        assertDoesNotThrow(
                () -> SclService.updateExtRefBinders(scd, extRefInfo)
        );

        extRefInfo.setHolderLDInst("UNKNOWN_LD");
        assertThrows(
                ScdException.class,
                () -> SclService.updateExtRefBinders(scd, extRefInfo)
        );
        assertIsMarshallable(scd);
    }

    @Test
    void getExtRefSourceInfo_shouldReturnEmptyList_whenExtRefMatchNoFCDA() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        String iedName = "IED_NAME2";
        String ldInst = "LD_INST21";
        String lnClass = TLLN0Enum.LLN_0.value();
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName(iedName);
        LDeviceAdapter lDeviceAdapter = assertDoesNotThrow(() -> iedAdapter.findLDeviceAdapterByLdInst(ldInst).get());
        LN0Adapter ln0Adapter = lDeviceAdapter.getLN0Adapter();
        List<TExtRef> extRefs = ln0Adapter.getExtRefs(null);
        assertFalse(extRefs.isEmpty());

        ExtRefInfo extRefInfo = new ExtRefInfo(extRefs.get(0));

        extRefInfo.setHolderIEDName(iedName);
        extRefInfo.setHolderLDInst(ldInst);
        extRefInfo.setHolderLnClass(lnClass);

        //When
        List<ControlBlock> controlBlocks = SclService.getExtRefSourceInfo(scd, extRefInfo);

        //Then
        assertThat(controlBlocks).isEmpty();
    }

    @Test
    void getExtRefSourceInfo_shouldReturnListOfControlBlocks_whenExtRefMatchFCDA() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/issue_175_scd_get_cbs_test.xml");
        String iedName = "IED_NAME2";
        String ldInst = "LD_INST21";
        String lnClass = TLLN0Enum.LLN_0.value();
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName(iedName);
        LDeviceAdapter lDeviceAdapter = assertDoesNotThrow(() -> iedAdapter.findLDeviceAdapterByLdInst(ldInst).get());
        LN0Adapter ln0Adapter = lDeviceAdapter.getLN0Adapter();
        List<TExtRef> extRefs = ln0Adapter.getExtRefs(null);
        assertFalse(extRefs.isEmpty());

        ExtRefInfo extRefInfo = new ExtRefInfo(extRefs.get(0));

        extRefInfo.setHolderIEDName(iedName);
        extRefInfo.setHolderLDInst(ldInst);
        extRefInfo.setHolderLnClass(lnClass);

        //When
        List<ControlBlock> controlBlocks = SclService.getExtRefSourceInfo(scd, extRefInfo);

        //Then
        assertThat(controlBlocks).hasSize(1);
        assertThat(controlBlocks.get(0).getName()).isEqualTo("goose2");
    }

    @Test
    void updateExtRefSource_shouldThrowScdException_whenSignalInfoNullOrInvalid() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME2");
        extRefInfo.setHolderLDInst("LD_INST21");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());

        //When Then
        assertThat(extRefInfo.getSignalInfo()).isNull();
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class); // signal = null
        extRefInfo.setSignalInfo(new ExtRefSignalInfo());
        assertThat(extRefInfo.getSignalInfo()).isNotNull();
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class);// signal invalid
    }

    @Test
    void updateExtRefSource_shouldThrowScdException_whenBindingInfoNullOrInvalid() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME2");
        extRefInfo.setHolderLDInst("LD_INST21");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());

        ExtRefSignalInfo extRefSignalInfo = new ExtRefSignalInfo();
        extRefSignalInfo.setIntAddr("INT_ADDR21");
        extRefSignalInfo.setPDA("da21.bda211.bda212.bda213");
        extRefSignalInfo.setPDO("Do21.sdo21");
        extRefInfo.setSignalInfo(extRefSignalInfo);
        //When Then
        assertThat(extRefInfo.getBindingInfo()).isNull();
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class); // binding = null
        extRefInfo.setBindingInfo(new ExtRefBindingInfo());
        assertThat(extRefInfo.getBindingInfo()).isNotNull();
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class);// binding invalid
    }

    @Test
    void updateExtRefSource_shouldThrowScdException_whenBindingInternalByIedName() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME2");
        extRefInfo.setHolderLDInst("LD_INST21");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());

        ExtRefSignalInfo extRefSignalInfo = new ExtRefSignalInfo();
        extRefSignalInfo.setIntAddr("INT_ADDR21");
        extRefSignalInfo.setPDA("da21.bda211.bda212.bda213");
        extRefSignalInfo.setPDO("Do21.sdo21");
        extRefInfo.setSignalInfo(extRefSignalInfo);

        ExtRefBindingInfo extRefBindingInfo = new ExtRefBindingInfo();
        extRefBindingInfo.setIedName("IED_NAME2"); // internal binding
        extRefBindingInfo.setLdInst("LD_INST12");
        extRefBindingInfo.setLnClass(TLLN0Enum.LLN_0.value());
        extRefInfo.setBindingInfo(new ExtRefBindingInfo());
        //When Then
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class); // CB not allowed
    }

    @Test
    void updateExtRefSource_shouldThrowScdException_whenBindingInternaByServiceType() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME2");
        extRefInfo.setHolderLDInst("LD_INST21");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());

        ExtRefSignalInfo extRefSignalInfo = new ExtRefSignalInfo();
        extRefSignalInfo.setIntAddr("INT_ADDR21");
        extRefSignalInfo.setPDA("da21.bda211.bda212.bda213");
        extRefSignalInfo.setPDO("Do21.sdo21");
        extRefInfo.setSignalInfo(extRefSignalInfo);

        ExtRefBindingInfo extRefBindingInfo = new ExtRefBindingInfo();
        extRefBindingInfo.setIedName("IED_NAME2"); // internal binding
        extRefBindingInfo.setLdInst("LD_INST12");
        extRefBindingInfo.setLnClass(TLLN0Enum.LLN_0.value());
        extRefBindingInfo.setServiceType(TServiceType.POLL);
        extRefInfo.setBindingInfo(new ExtRefBindingInfo());
        //When Then
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class); // CB not allowed
    }

    @Test
    void updateExtRefSource_shouldThrowScdException_whenSourceInfoNullOrInvalid() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME2");
        extRefInfo.setHolderLDInst("LD_INST21");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());

        ExtRefSignalInfo extRefSignalInfo = new ExtRefSignalInfo();
        extRefSignalInfo.setIntAddr("INT_ADDR21");
        extRefSignalInfo.setPDA("da21.bda211.bda212.bda213");
        extRefSignalInfo.setPDO("Do21.sdo21");
        extRefInfo.setSignalInfo(extRefSignalInfo);

        ExtRefBindingInfo extRefBindingInfo = new ExtRefBindingInfo();
        extRefBindingInfo.setIedName("IED_NAME1"); // internal binding
        extRefBindingInfo.setLdInst("LD_INST12");
        extRefBindingInfo.setLnClass(TLLN0Enum.LLN_0.value());
        extRefInfo.setBindingInfo(new ExtRefBindingInfo());

        //When Then
        assertThat(extRefInfo.getSourceInfo()).isNull();
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class); // signal = null
        extRefInfo.setSourceInfo(new ExtRefSourceInfo());
        assertThat(extRefInfo.getSourceInfo()).isNotNull();
        assertThatThrownBy(() -> SclService.updateExtRefSource(scd, extRefInfo)).isInstanceOf(ScdException.class);// signal invalid
    }

    @Test
    void updateExtRefSource_shouldThrowScdException_whenBindingExternalBinding() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-scd-extref-cb/scd_get_cbs_test.xml");
        ExtRefInfo extRefInfo = new ExtRefInfo();
        extRefInfo.setHolderIEDName("IED_NAME2");
        extRefInfo.setHolderLDInst("LD_INST21");
        extRefInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());

        ExtRefSignalInfo extRefSignalInfo = new ExtRefSignalInfo();
        extRefSignalInfo.setIntAddr("INT_ADDR21");
        extRefSignalInfo.setPDA("da21.bda211.bda212.bda213");
        extRefSignalInfo.setPDO("Do21.sdo21");
        extRefInfo.setSignalInfo(extRefSignalInfo);

        ExtRefBindingInfo extRefBindingInfo = new ExtRefBindingInfo();
        extRefBindingInfo.setIedName("IED_NAME1");
        extRefBindingInfo.setLdInst("LD_INST12");
        extRefBindingInfo.setLnClass(TLLN0Enum.LLN_0.value());
        extRefInfo.setBindingInfo(extRefBindingInfo);

        ExtRefSourceInfo sourceInfo = new ExtRefSourceInfo();
        sourceInfo.setSrcLDInst(extRefInfo.getBindingInfo().getLdInst());
        sourceInfo.setSrcLNClass(extRefInfo.getBindingInfo().getLnClass());
        sourceInfo.setSrcCBName("goose1");
        extRefInfo.setSourceInfo(sourceInfo);

        //When
        TExtRef extRef = assertDoesNotThrow(() -> SclService.updateExtRefSource(scd, extRefInfo));
        //Then
        assertThat(extRef.getSrcCBName()).isEqualTo(extRefInfo.getSourceInfo().getSrcCBName());
        assertThat(extRef.getSrcLDInst()).isEqualTo(extRefInfo.getBindingInfo().getLdInst());
        assertThat(extRef.getSrcLNClass()).contains(extRefInfo.getBindingInfo().getLnClass());
    }

    private ExtRefSignalInfo createSignalInfo(String pDO, String pDA, String intAddr) {

        final String DESC = "DESC";
        final String P_LN = TLLN0Enum.LLN_0.value();
        final String P_SERV_T = "Report";

        ExtRefSignalInfo signalInfo = new ExtRefSignalInfo();
        signalInfo.setDesc(DESC);
        signalInfo.setPDA(pDA);
        signalInfo.setPDO(pDO);
        signalInfo.setPLN(P_LN);
        signalInfo.setPServT(TServiceType.fromValue(P_SERV_T));
        signalInfo.setIntAddr(intAddr);

        return signalInfo;
    }

    @Test
    void getDAI_should_return_all_dai() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        // when
        Set<DataAttributeRef> allResults = SclService.getDAI(scd, "IED_NAME1", "LD_INST12", new DataAttributeRef(), true);

        // then
        assertThat(allResults).hasSize(733);

        List<DataAttributeRef> resultsWithDa = allResults.stream().filter(rdt -> StringUtils.isNotBlank(rdt.getDaRef())).collect(Collectors.toList());
        assertThat(resultsWithDa).hasSize(733);

        List<DataAttributeRef> resultsWithNoBda = allResults.stream().filter(rdt -> rdt.getBdaNames().isEmpty()).collect(Collectors.toList());
        assertThat(resultsWithNoBda).hasSize(3);
        List<DataAttributeRef> resultsWithBdaDepth1 = allResults.stream().filter(rdt -> rdt.getBdaNames().size() == 1).collect(Collectors.toList());
        assertThat(resultsWithBdaDepth1).isEmpty();
        List<DataAttributeRef> resultsWithBdaDepth2 = allResults.stream().filter(rdt -> rdt.getBdaNames().size() == 2).collect(Collectors.toList());
        assertThat(resultsWithBdaDepth2).hasSize(1);
        List<DataAttributeRef> resultsWithBdaDepth3 = allResults.stream().filter(rdt -> rdt.getBdaNames().size() == 3).collect(Collectors.toList());
        assertThat(resultsWithBdaDepth3).hasSize(729);


        List<DataAttributeRef> resultsWithDo = allResults.stream().filter(rdt -> StringUtils.isNotBlank(rdt.getDoRef())).collect(Collectors.toList());
        assertThat(resultsWithDo).hasSize(733);

        List<DataAttributeRef> resultsWithNoSdo = allResults.stream().filter(rdt -> rdt.getSdoNames().isEmpty()).collect(Collectors.toList());
        assertThat(resultsWithNoSdo).hasSize(3);
        List<DataAttributeRef> resultsWithSdoDepth1 = allResults.stream().filter(rdt -> rdt.getSdoNames().size() == 1).collect(Collectors.toList());
        assertThat(resultsWithSdoDepth1).isEmpty();
        List<DataAttributeRef> resultsWithSdoDepth2 = allResults.stream().filter(rdt -> rdt.getSdoNames().size() == 2).collect(Collectors.toList());
        assertThat(resultsWithSdoDepth2).hasSize(730);
        List<DataAttributeRef> resultsWithSdoDepth3 = allResults.stream().filter(rdt -> rdt.getSdoNames().size() == 3).collect(Collectors.toList());
        assertThat(resultsWithSdoDepth3).isEmpty();
    }

    @Test
    void getDAI_should_aggregate_attribute_from_DAI() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_test_aggregate_DAI.xml");

        // when
        Set<DataAttributeRef> dais = SclService.getDAI(scd, "VirtualBCU", "LDMODEXPF", new DataAttributeRef(), false);

        // then
        DataAttributeRef lln0 = DataAttributeRef.builder().prefix("").lnType("lntype1").lnClass("LLN0").lnInst("").build();
        DataAttributeRef lln0DoA = lln0.toBuilder().doName(createDo("DoA", TPredefinedCDCEnum.DPL)).build();
        DataAttributeRef lln0DoB = lln0.toBuilder().doName(createDo("DoB", TPredefinedCDCEnum.ACD)).build();

        assertThat(dais).containsExactlyInAnyOrder(
                lln0DoA.toBuilder().daName(createDa("daNotInDai", TFCEnum.CF, false, Map.of(0L, "0"))).build(),
                lln0DoA.toBuilder().daName(createDa("daNotInDai2", TFCEnum.CF, true, Map.of())).build(),
                lln0DoA.toBuilder().daName(createDa("daiOverrideVal", TFCEnum.CF, false, Map.of(0L, "1"))).build(),
                lln0DoA.toBuilder().daName(createDa("daiOverrideValImport", TFCEnum.CF, true, Map.of())).build(),
                lln0DoA.toBuilder().daName(createDa("daiOverrideValImport2", TFCEnum.CF, false, Map.of())).build(),

                lln0DoB.toBuilder().daName(createDa("structDa.daNotInDai", TFCEnum.ST, false, Map.of(0L, "0"))).build(),
                lln0DoB.toBuilder().daName(createDa("structDa.daNotInDai2", TFCEnum.ST, true, Map.of())).build(),
                lln0DoB.toBuilder().daName(createDa("structDa.daiOverrideVal", TFCEnum.ST, false, Map.of(0L, "1"))).build(),
                lln0DoB.toBuilder().daName(createDa("structDa.daiOverrideValImport", TFCEnum.ST, true, Map.of())).build(),
                lln0DoB.toBuilder().daName(createDa("structDa.daiOverrideValImport2", TFCEnum.ST, false, Map.of())).build(),

                DataAttributeRef.builder().prefix("").lnType("lntype2").lnClass("LPHD").lnInst("0")
                        .doName(createDo("PhyNam", TPredefinedCDCEnum.DPS))
                        .daName(createDa("aDa", TFCEnum.BL, false, Map.of())).build()
        );
    }

    @Test
    void getDAI_when_LDevice_not_found_should_throw_exception() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");

        // when & then
        DataAttributeRef dataAttributeRef = new DataAttributeRef();
        assertThrows(ScdException.class,
                () -> SclService.getDAI(scd, "IED_NAME1", "UNKNOWNLD", dataAttributeRef, true));
    }

    @Test
    void getDAI_should_filter_updatable_DA() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_test_updatable_DAI.xml");

        // when
        Set<DataAttributeRef> dais = SclService.getDAI(scd, "VirtualBCU", "LDMODEXPF", new DataAttributeRef(), true);

        // then
        assertThat(dais).isNotNull();
        List<String> resultSimpleDa = dais.stream()
                .filter(dataAttributeRef -> dataAttributeRef.getBdaNames().isEmpty()) // test only simple DA
                .map(DataAttributeRef::getLNRef).collect(Collectors.toList());
        assertThat(resultSimpleDa).containsExactlyInAnyOrder(
                // ...AndTrueInDai : If ValImport is True in DAI, DA is updatable
                "LLN0.DoA.valImportNotSetAndTrueInDai",
                "LLN0.DoA.valImportTrueAndTrueInDai",
                "LLN0.DoA.valImportFalseAndTrueInDai",
                // valImportTrue : If ValImport is True in DA and DAI does not exist, DA is updatable
                "LLN0.DoA.valImportTrue",
                // valImportTrueAndNotSetInDai : If ValImport is True in DA and DAI exists but DAI ValImport is not set, DA is updatable
                "LLN0.DoA.valImportTrueAndNotSetInDai",
                // Only these FC are updatable
                "LLN0.DoA.fcCF",
                "LLN0.DoA.fcDC",
                "LLN0.DoA.fcSG",
                "LLN0.DoA.fcSP",
                "LLN0.DoA.fcST",
                "LLN0.DoA.fcSE"
        );
    }

    @Test
    void getDAI_should_filter_updatable_BDA() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_test_updatable_DAI.xml");

        // when
        Set<DataAttributeRef> dais = SclService.getDAI(scd, "VirtualBCU", "LDMODEXPF", new DataAttributeRef(), true);

        // then
        assertThat(dais).isNotNull();
        List<String> resultStructDa = dais.stream()
                .filter(dataAttributeRef -> !dataAttributeRef.getBdaNames().isEmpty()) // test only struct DA
                .map(DataAttributeRef::getLNRef).collect(Collectors.toList());
        assertThat(resultStructDa).containsExactlyInAnyOrder(
                // ...AndTrueInDai : If ValImport is True in DAI, BDA is updatable
                "LLN0.DoB.structValImportNotSet.bValImportFalseAndTrueInDai",
                "LLN0.DoB.structValImportNotSet.bValImportNotSetAndTrueInDai",
                "LLN0.DoB.structValImportNotSet.bValImportTrueAndTrueInDai",
                "LLN0.DoB.structValImportTrue.bValImportFalseAndTrueInDai",
                "LLN0.DoB.structValImportTrue.bValImportNotSetAndTrueInDai",
                "LLN0.DoB.structValImportTrue.bValImportTrueAndTrueInDai",
                "LLN0.DoB.structValImportFalse.bValImportFalseAndTrueInDai",
                "LLN0.DoB.structValImportFalse.bValImportNotSetAndTrueInDai",
                "LLN0.DoB.structValImportFalse.bValImportTrueAndTrueInDai",
                // bValImportTrue : If ValImport is True in BDA and DAI does not exist, BDA is updatable
                "LLN0.DoB.structValImportFalse.bValImportTrue",
                "LLN0.DoB.structValImportTrue.bValImportTrue",
                "LLN0.DoB.structValImportNotSet.bValImportTrue",
                // bValImportTrueAndNotSetInDai : If ValImport is True in BDA and DAI exists but DAI ValImport is not set, BDA is updatable
                "LLN0.DoB.structValImportTrue.bValImportTrueAndNotSetInDai",
                "LLN0.DoB.structValImportNotSet.bValImportTrueAndNotSetInDai",
                "LLN0.DoB.structValImportFalse.bValImportTrueAndNotSetInDai",
                // Only these FC are updatable
                "LLN0.DoB.structWithFcCF.bda1",
                "LLN0.DoB.structWithFcDC.bda1",
                "LLN0.DoB.structWithFcSG.bda1",
                "LLN0.DoB.structWithFcSP.bda1",
                "LLN0.DoB.structWithFcST.bda1",
                "LLN0.DoB.structWithFcSE.bda1"
        );
    }

    @Test
    void getDAI_should_filter_updatable_DA_with_sGroup_Val() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_test_updatable_DAI.xml");

        // when
        Set<DataAttributeRef> dais = SclService.getDAI(scd, "VirtualBCU", "LDCAP", new DataAttributeRef(), true);

        // then
        assertThat(dais).isNotNull();
        List<String> resultSimpleDa = dais.stream()
                .filter(dataAttributeRef -> dataAttributeRef.getBdaNames().isEmpty()) // test only simple DA
                .map(DataAttributeRef::getLNRef).collect(Collectors.toList());
        assertThat(resultSimpleDa).containsExactlyInAnyOrder(
                "LLN0.DoD.sGroupValImportNotSet",
                "LLN0.DoD.sGroupValImportTrue"
        );
    }

    @Test
    void getDAI_should_filter_updatable_DA_with_sGroup_Val_without_ConfSg() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_test_updatable_DAI.xml");

        // when
        Set<DataAttributeRef> dais = SclService.getDAI(scd, "VirtualBCU", "LDMOD", new DataAttributeRef(), true);

        // then
        assertThat(dais)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void testInitScl() {
        SclRootAdapter sclRootAdapter = assertDoesNotThrow(
                () -> SclService.initScl(Optional.empty(), "hVersion", "hRevision")
        );
        assertIsMarshallable(sclRootAdapter.getCurrentElem());
    }

    @Test
    void testInitScl_With_hId_shouldNotThrowError() {
        UUID hid = UUID.randomUUID();
        SclRootAdapter sclRootAdapter = assertDoesNotThrow(
                () -> SclService.initScl(Optional.of(hid), "hVersion", "hRevision")
        );
        assertIsMarshallable(sclRootAdapter.getCurrentElem());
    }

    @Test
    void testInitScl_Create_Private_SCL_FILETYPE() {
        UUID hid = UUID.randomUUID();
        SclRootAdapter rootAdapter = assertDoesNotThrow(
                () -> SclService.initScl(Optional.of(hid), "hVersion", "hRevision")
        );
        assertThat(rootAdapter.getCurrentElem().getPrivate()).isNotEmpty();
        assertThat(rootAdapter.getCurrentElem().getPrivate().get(0).getType()).isEqualTo(COMPAS_SCL_FILE_TYPE.getPrivateType());
        assertIsMarshallable(rootAdapter.getCurrentElem());
    }

    @Test
    void testUpdateHeader() {

        SclRootAdapter sclRootAdapter = assertDoesNotThrow(
                () -> SclService.initScl(Optional.empty(), "hVersion", "hRevision")
        );
        UUID hId = UUID.fromString(sclRootAdapter.getHeaderAdapter().getHeaderId());
        HeaderDTO headerDTO = DTO.createHeaderDTO(hId);
        SclService.updateHeader(sclRootAdapter.getCurrentElem(), headerDTO);
        assertIsMarshallable(sclRootAdapter.getCurrentElem());
    }

    @Test
    void testUpdateDAI() {
        DataAttributeRef dataAttributeRef = new DataAttributeRef();
        dataAttributeRef.setLnType("unknownID");
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");

        assertThrows(ScdException.class, () -> SclService.updateDAI(
                scd, "IED", "LD", dataAttributeRef
        ));
        dataAttributeRef.setLnType("LNO1");
        dataAttributeRef.setLnClass(TLLN0Enum.LLN_0.value());
        DoTypeName doTypeName = new DoTypeName("Do.sdo1.d");
        dataAttributeRef.setDoName(doTypeName);
        dataAttributeRef.setDaName(new DaTypeName("antRef.bda1.bda2.bda3"));
        TVal tVal = new TVal();
        tVal.setValue("newValue");
        dataAttributeRef.setDaiValues(List.of(tVal));
        assertDoesNotThrow(() -> SclService.updateDAI(scd, "IED_NAME", "LD_INS1", dataAttributeRef));
        assertIsMarshallable(scd);
    }

    @Test
    void testGetEnumTypeElements() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");
        assertThrows(ScdException.class, () -> SclService.getEnumTypeElements(scd, "unknwnID"));

        var enumList = assertDoesNotThrow(
                () -> SclService.getEnumTypeElements(scd, "RecCycModKind")
        );
        assertFalse(enumList.isEmpty());
    }

    @Test
    void testImportSTDElementsInSCD() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/scd.xml");
        SCL std = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std.xml");
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);

        SclRootAdapter expectedScdAdapter = assertDoesNotThrow(() -> SclService.importSTDElementsInSCD(
                scdRootAdapter, Set.of(std), DTO.comMap));
        assertThat(expectedScdAdapter.getCurrentElem().getIED()).hasSize(1);
        assertThat(expectedScdAdapter.getCurrentElem().getDataTypeTemplates()).hasNoNullFieldsOrProperties();
        assertThat(expectedScdAdapter.getCurrentElem().getCommunication().getSubNetwork()).hasSize(2);
        assertIsMarshallable(scd);
    }

    @Test
    void testImportSTDElementsInSCD_with_Multiple_STD() {
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/scd_lnode_with_many_compas_icdheader.xml");
        SCL std0 = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std.xml");
        SCL std1 = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std_SITESITE1SCU1.xml");
        SCL std2 = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std_SITESITE1SCU2.xml");
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);

        SclRootAdapter expectedScdAdapter = assertDoesNotThrow(() -> SclService.importSTDElementsInSCD(
                scdRootAdapter, Set.of(std0, std1, std2), DTO.comMap));
        assertThat(expectedScdAdapter.getCurrentElem().getIED()).hasSize(3);
        assertThat(expectedScdAdapter.getCurrentElem().getDataTypeTemplates()).hasNoNullFieldsOrProperties();
        assertThat(expectedScdAdapter.getCurrentElem().getCommunication().getSubNetwork()).hasSize(2);
        assertThat(expectedScdAdapter.getCurrentElem().getCommunication().getSubNetwork().get(0).getConnectedAP()).hasSizeBetween(1, 3);
        assertThat(expectedScdAdapter.getCurrentElem().getCommunication().getSubNetwork().get(1).getConnectedAP()).hasSizeBetween(1, 3);
        assertIsMarshallable(scd);
    }

    @Test
    void testImportSTDElementsInSCD_Several_STD_Match_Compas_ICDHeader() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/scd.xml");
        SCL std = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std.xml");
        SCL std1 = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std.xml");
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);
        //When Then
        Set<SCL> stds = Set.of(std, std1);
        assertThrows(ScdException.class, () -> SclService.importSTDElementsInSCD(scdRootAdapter, stds, DTO.comMap));
        assertIsMarshallable(scd);
    }

    @Test
    void test_importSTDElementsInSCD_should_not_throw_exception_when_SCD_file_contains_same_ICDHeader_in_two_different_functions() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/scd_with_same_compas_icd_header_in_different_functions.xml");
        SCL std = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std.xml");
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);
        //When Then
        assertDoesNotThrow(() -> SclService.importSTDElementsInSCD(scdRootAdapter, Set.of(std), DTO.comMap));
        assertIsMarshallable(scd);
    }

    @Test
    void testImportSTDElementsInSCD_Compas_ICDHeader_Not_Match() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/scd.xml");
        SCL std = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/std_with_same_ICDSystemVersionUUID.xml");
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);
        //When Then
        Set<SCL> stds = Set.of(std);
        assertThrows(ScdException.class, () -> SclService.importSTDElementsInSCD(scdRootAdapter, stds, DTO.comMap));
        assertIsMarshallable(scd);
    }

    @Test
    void testImportSTDElementsInSCD_No_STD_Match() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-ied-dtt-com-import-stds/ssd.xml");
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);
        //When Then
        Set<SCL> stds = new HashSet<>();
        assertThrows(ScdException.class, () -> SclService.importSTDElementsInSCD(scdRootAdapter, stds, DTO.comMap));
        assertIsMarshallable(scd);
    }

    @Test
    void removeControlBlocksAndDatasetAndExtRefSrc_should_remove_controlBlocks_and_Dataset_on_ln0() {
        // Given
        SCL scl = SclTestMarshaller.getSCLFromFile("/scl-remove-controlBlocks-dataSet-extRefSrc/scl-with-control-blocks.xml");
        // When
        SclService.removeAllControlBlocksAndDatasetsAndExtRefSrcBindings(scl);
        // Then
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scl);
        List<LDeviceAdapter> lDevices = scdRootAdapter.streamIEDAdapters().flatMap(IEDAdapter::streamLDeviceAdapters).toList();
        List<LN0> ln0s = lDevices.stream().map(LDeviceAdapter::getLN0Adapter).map(LN0Adapter::getCurrentElem).toList();
        assertThat(ln0s)
                .isNotEmpty()
                .noneMatch(TAnyLN::isSetDataSet)
                .noneMatch(TAnyLN::isSetLogControl)
                .noneMatch(TAnyLN::isSetReportControl)
                .noneMatch(LN0::isSetGSEControl)
                .noneMatch(LN0::isSetSampledValueControl);
        assertIsMarshallable(scl);
    }

    @Test
    void removeControlBlocksAndDatasetAndExtRefSrc_should_remove_controlBlocks_and_Dataset_on_ln() {
        // Given
        SCL scl = SclTestMarshaller.getSCLFromFile("/scl-remove-controlBlocks-dataSet-extRefSrc/scl-with-control-blocks.xml");
        // When
        SclService.removeAllControlBlocksAndDatasetsAndExtRefSrcBindings(scl);
        // Then
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scl);
        List<TLN> lns = scdRootAdapter.streamIEDAdapters()
                .flatMap(IEDAdapter::streamLDeviceAdapters)
                .map(LDeviceAdapter::getLNAdapters).flatMap(List::stream)
                .map(LNAdapter::getCurrentElem).collect(Collectors.toList());
        assertThat(lns)
                .isNotEmpty()
                .noneMatch(TAnyLN::isSetDataSet)
                .noneMatch(TAnyLN::isSetLogControl)
                .noneMatch(TAnyLN::isSetReportControl);
        assertIsMarshallable(scl);
    }

    @Test
    void removeControlBlocksAndDatasetAndExtRefSrc_should_remove_srcXXX_attributes_on_ExtRef() {
        // Given
        SCL scl = SclTestMarshaller.getSCLFromFile("/scl-remove-controlBlocks-dataSet-extRefSrc/scl-with-control-blocks.xml");
        // When
        SclService.removeAllControlBlocksAndDatasetsAndExtRefSrcBindings(scl);
        // Then
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scl);
        List<TExtRef> extRefs = scdRootAdapter
                .streamIEDAdapters()
                .flatMap(IEDAdapter::streamLDeviceAdapters)
                .map(LDeviceAdapter::getLN0Adapter)
                .map(AbstractLNAdapter::getExtRefs).flatMap(List::stream)
                .collect(Collectors.toList());
        assertThat(extRefs)
                .isNotEmpty()
                .noneMatch(TExtRef::isSetSrcLDInst)
                .noneMatch(TExtRef::isSetSrcPrefix)
                .noneMatch(TExtRef::isSetSrcLNInst)
                .noneMatch(TExtRef::isSetSrcCBName)
                .noneMatch(TExtRef::isSetSrcLNClass);
        assertIsMarshallable(scl);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sclProviderMissingRequiredObjects")
    void updateLDeviceStatus_shouldReturnReportWithError_MissingRequiredObject(String testCase, SCL scl, Tuple... errors) {
        // Given
        assertTrue(getLDeviceStatusValue(scl, "IedName1", "LDSUIED").isPresent());
        assertEquals("off", getLDeviceStatusValue(scl, "IedName1", "LDSUIED").get().getValue());
        String before = MarshallerWrapper.marshall(scl);
        // When
        SclReport sclReport = SclService.updateLDeviceStatus(scl);
        // Then
        String after = MarshallerWrapper.marshall(sclReport.getSclRootAdapter().getCurrentElem());
        assertFalse(sclReport.isSuccess());
        assertThat(sclReport.getSclReportItems())
                .hasSize(1)
                .extracting(SclReportItem::getMessage, SclReportItem::getXpath)
                .containsExactly(errors);
        assertEquals("off", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName1", "LDSUIED").get().getValue());
        assertEquals(before, after);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sclProviderBasedLDeviceStatus")
    void updateLDeviceStatus_shouldReturnReportWithError_WhenLDeviceStatusActiveOrUntestedOrInactive(String testCase, SCL scl, Tuple... errors) {
        // Given
        assertEquals("off", getLDeviceStatusValue(scl, "IedName1", "LDSUIED").get().getValue());
        assertEquals("on", getLDeviceStatusValue(scl, "IedName2", "LDSUIED").get().getValue());
        assertFalse(getLDeviceStatusValue(scl, "IedName3", "LDSUIED").isPresent());
        String before = MarshallerWrapper.marshall(scl);
        // When
        SclReport sclReport = SclService.updateLDeviceStatus(scl);
        // Then
        String after = MarshallerWrapper.marshall(sclReport.getSclRootAdapter().getCurrentElem());
        assertFalse(sclReport.isSuccess());
        assertThat(sclReport.getSclReportItems())
                .hasSize(3)
                .extracting(SclReportItem::getMessage, SclReportItem::getXpath)
                .containsExactly(errors);
        assertEquals("off", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName1", "LDSUIED").get().getValue());
        assertEquals("on", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName2", "LDSUIED").get().getValue());
        assertFalse(getLDeviceStatusValue(scl, "IedName3", "LDSUIED").isPresent());
        assertEquals(before, after);
    }

    @Test
    void updateLDeviceStatus_shouldReturnReportWithError_WhenAllLDeviceInactive_Test2() {
        // Given
        SCL scl = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test2_LD_STATUS_INACTIVE.scd");
        assertEquals("off", getLDeviceStatusValue(scl, "IedName1", "LDSUIED").get().getValue());
        assertEquals("on", getLDeviceStatusValue(scl, "IedName2", "LDSUIED").get().getValue());
        assertFalse(getLDeviceStatusValue(scl, "IedName3", "LDSUIED").isPresent());
        // When
        SclReport sclReport = SclService.updateLDeviceStatus(scl);
        // Then
        assertFalse(sclReport.isSuccess());
        assertThat(sclReport.getSclReportItems())
                .hasSize(2)
                .extracting(SclReportItem::getMessage, SclReportItem::getXpath)
                .containsExactly(Tuple.tuple("The LDevice cannot be set to 'off' but has not been selected into SSD.",
                                "/SCL/IED[@name=\"IedName1\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"),
                        Tuple.tuple("The LDevice is not qualified into STD but has been selected into SSD.",
                                "/SCL/IED[@name=\"IedName2\"]/AccessPoint/Server/LDevice[@inst=\"LDSUIED\"]/LN0"));
        assertEquals("off", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName1", "LDSUIED").get().getValue());
        assertEquals("on", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName2", "LDSUIED").get().getValue());
        assertTrue(getLDeviceStatusValue(scl, "IedName3", "LDSUIED").isPresent());
        assertEquals("off", getLDeviceStatusValue(scl, "IedName3", "LDSUIED").get().getValue());
    }


    @Test
    void updateLDeviceStatus_shouldReturnUpdatedFile() {
        // Given
        SCL givenScl = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue68_Test_Template.scd");
        assertTrue(getLDeviceStatusValue(givenScl, "IedName1", "LDSUIED").isPresent());
        assertEquals("off", getLDeviceStatusValue(givenScl, "IedName1", "LDSUIED").get().getValue());

        assertTrue(getLDeviceStatusValue(givenScl, "IedName2", "LDSUIED").isPresent());
        assertEquals("on", getLDeviceStatusValue(givenScl, "IedName2", "LDSUIED").get().getValue());

        assertFalse(getLDeviceStatusValue(givenScl, "IedName3", "LDSUIED").isPresent());

        // When
        SclReport sclReport = SclService.updateLDeviceStatus(givenScl);
        // Then
        assertTrue(sclReport.isSuccess());
        assertTrue(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName1", "LDSUIED").isPresent());
        assertEquals("on", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName1", "LDSUIED").get().getValue());

        assertTrue(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName2", "LDSUIED").isPresent());
        assertEquals("off", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName2", "LDSUIED").get().getValue());

        assertTrue(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName3", "LDSUIED").isPresent());
        assertEquals("off", getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName3", "LDSUIED").get().getValue());
    }

    @Test
    void updateLDeviceStatus_shouldReturnUpdatedFile_when_DAI_Mod_DO_stVal_whatever_it_is_updatable_or_not() {
        // Given
        SCL givenScl = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/issue_165_enhance_68_Test_Dai_Updatable.scd");
        assertThat(getLDeviceStatusValue(givenScl, "IedName1", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("off");
        assertThat(getLDeviceStatusValue(givenScl, "IedName2", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("on");
        assertThat(getLDeviceStatusValue(givenScl, "IedName3", "LDSUIED"))
                .map(TVal::getValue)
                .isNotPresent();
        assertThat(getLDeviceStatusValue(givenScl, "IedName4", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("on");
        assertThat(getLDeviceStatusValue(givenScl, "IedName5", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("on");

        // When
        SclReport sclReport = SclService.updateLDeviceStatus(givenScl);

        // Then
        assertThat(sclReport.isSuccess()).isTrue();
        assertThat(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName1", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("on");

        assertThat(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName2", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("off");

        assertThat(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName3", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("off");

        assertThat(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName4", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("off");

        assertThat(getLDeviceStatusValue(sclReport.getSclRootAdapter().getCurrentElem(), "IedName5", "LDSUIED"))
                .map(TVal::getValue)
                .hasValue("off");
    }

    private Optional<TVal> getLDeviceStatusValue(SCL scl, String iedName, String ldInst) {
        return getValFromDaiName(scl, iedName, ldInst, "Mod", "stVal");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Test update setSrcRef Value,LD_WITH_1_InRef,InRef2,setSrcRef,IED_NAME1LD_WITH_1_InRef/PRANCR1.Do11.sdo11",
            "Test update setSrcCB Value,LD_WITH_1_InRef,InRef2,setSrcCB,OLD_VAL",
            "Test update setSrcRef Value,LD_WITH_3_InRef,InRef3,setSrcRef,IED_NAME1LD_WITH_3_InRef/PRANCR1.Do11.sdo11",
            "Test update setSrcCB Value,LD_WITH_3_InRef,InRef3,setSrcCB,IED_NAME1LD_WITH_3_InRef/prefixANCR1.GSE1",
            "Test update setTstRef Value,LD_WITH_3_InRef,InRef3,setTstRef,IED_NAME1LD_WITH_3_InRef/PRANCR1.Do11.sdo11",
            "Test update setTstCB Value,LD_WITH_3_InRef,InRef3,setTstCB,IED_NAME1LD_WITH_3_InRef/prefixANCR3.GSE3"
    })
    void updateDoInRef_shouldReturnUpdatedFile(String testName, String ldInst, String doName, String daName, String expected) {
        // Given
        SCL givenScl = SclTestMarshaller.getSCLFromFile("/scd-test-update-inref/scd_update_inref_issue_231_test_ok.xml");

        // When
        SclReport sclReport = SclService.updateDoInRef(givenScl);

        // Then
        assertThat(sclReport.isSuccess()).isTrue();
        SclTestMarshaller.assertIsMarshallable(sclReport.getSclRootAdapter().currentElem);
        assertThat(getValFromDaiName(sclReport.getSclRootAdapter().getCurrentElem(), "IED_NAME1", ldInst, doName, daName)
                .map(TVal::getValue))
                .hasValue(expected);
        assertIsMarshallable(sclReport.getSclRootAdapter().getCurrentElem());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Test with only 1 ExtRef should not update srcTstCB,LD_WITH_1_InRef,InRef2,setTstRef",
            "Test with only 1 ExtRef should not update setTstCB Value,LD_WITH_1_InRef,InRef2,setTstCB",
            "Test with only 1 ExtRef should not update DO when IedName not present,LD_WITH_1_InRef_ExtRef_Without_IedName,InRef4,setSrcRef",
            "Test with only 1 ExtRef should not update DO when LdInst not present,LD_WITH_1_InRef_ExtRef_Without_LdInst,InRef5,setSrcRef",
            "Test with only 1 ExtRef should not update DO when lnClass not present,LD_WITH_1_InRef_ExtRef_Without_LnClass,InRef6,setSrcRef"
    })
    void updateDoInRef_should_not_update_DAI(String testName, String ldInst, String doName, String daName) {
        // Given
        SCL givenScl = SclTestMarshaller.getSCLFromFile("/scd-test-update-inref/scd_update_inref_issue_231_test_ok.xml");

        // When
        SclReport sclReport = SclService.updateDoInRef(givenScl);

        // Then
        assertThat(sclReport.isSuccess()).isTrue();
        assertThat(getValFromDaiName(sclReport.getSclRootAdapter().getCurrentElem(), "IED_NAME1", ldInst, doName, daName)).isNotPresent();
    }

    @Test
    void updateDoInRef_shouldReturnReportWithError_when_ExtRef_not_coherent() {
        // Given
        SCL givenScl = SclTestMarshaller.getSCLFromFile("/scd-test-update-inref/scd_update_inref_issue_231_test_ko.xml");

        // When
        SclReport sclReport = SclService.updateDoInRef(givenScl);

        // Then
        assertThat(sclReport.isSuccess()).isTrue();
        assertThat(sclReport.getSclReportItems()).isNotEmpty();
        assertThat(sclReport.getSclReportItems()).hasSize(4);
    }

    private Optional<TVal> getValFromDaiName(SCL scl, String iedName, String ldInst, String doiName, String daiName) {
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scl);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName(iedName);
        Optional<LDeviceAdapter> lDeviceAdapter = iedAdapter.findLDeviceAdapterByLdInst(ldInst);
        LN0Adapter ln0Adapter = lDeviceAdapter.get().getLN0Adapter();
        Optional<DOIAdapter> doiAdapter = ln0Adapter.getDOIAdapters().stream()
                .filter(doiAdapter1 -> doiAdapter1.getCurrentElem().getName().equals(doiName))
                .findFirst();
        return doiAdapter.flatMap(adapter -> adapter.getCurrentElem().getSDIOrDAI().stream()
                .filter(tUnNaming -> tUnNaming.getClass().equals(TDAI.class))
                .map(TDAI.class::cast)
                .filter(tdai -> tdai.getName().equals(daiName) && !tdai.getVal().isEmpty())
                .map(tdai -> tdai.getVal().get(0))
                .findFirst());
    }

    @Test
    void analyzeDataGroups_should_success() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/limitation_cb_dataset_fcda/scd_check_limitation_bound_ied_controls_fcda.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iedAdapter1 = sclRootAdapter.getIEDAdapterByName("IED_NAME1");
        iedAdapter1.getCurrentElem().getAccessPoint().get(0).getServices().getClientServices().setMaxAttributes(9L);
        iedAdapter1.getCurrentElem().getAccessPoint().get(0).getServices().getClientServices().setMaxGOOSE(3L);
        iedAdapter1.getCurrentElem().getAccessPoint().get(0).getServices().getClientServices().setMaxSMV(2L);
        iedAdapter1.getCurrentElem().getAccessPoint().get(0).getServices().getClientServices().setMaxReports(1L);
        // When
        SclReport sclReport = SclService.analyzeDataGroups(scd);
        //Then
        assertThat(sclReport.getSclReportItems()).isEmpty();

    }

    @Test
    void analyzeDataGroups_should_return_errors_messages() {

        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/limitation_cb_dataset_fcda/scd_check_limitation_bound_ied_controls_fcda.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME2");
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getConfDataSet().setMaxAttributes(1L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getConfDataSet().setMax(3L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getSMVsc().setMax(1L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getGOOSE().setMax(2L);
        iedAdapter.getCurrentElem().getAccessPoint().get(0).getServices().getConfReportControl().setMax(0L);
        // When
        SclReport sclReport = SclService.analyzeDataGroups(scd);
        //Then
        assertThat(sclReport.getSclReportItems()).hasSize(11)
                .extracting(SclReportItem::getMessage)
                .containsExactlyInAnyOrder(
                        "The Client IED IED_NAME1 subscribes to too much FCDA: 9 > 8 max",
                        "The Client IED IED_NAME1 subscribes to too much GOOSE Control Blocks: 3 > 2 max",
                        "The Client IED IED_NAME1 subscribes to too much Report Control Blocks: 1 > 0 max",
                        "The Client IED IED_NAME1 subscribes to too much SMV Control Blocks: 2 > 1 max",
                        "There are too much FCDA for the DataSet dataset6 for the LDevice LD_INST21 in IED IED_NAME2: 2 > 1 max",
                        "There are too much FCDA for the DataSet dataset6 for the LDevice LD_INST22 in IED IED_NAME2: 2 > 1 max",
                        "There are too much FCDA for the DataSet dataset5 for the LDevice LD_INST22 in IED IED_NAME2: 2 > 1 max",
                        "There are too much DataSets for the IED IED_NAME2: 6 > 3 max",
                        "There are too much Report Control Blocks for the IED IED_NAME2: 1 > 0 max",
                        "There are too much GOOSE Control Blocks for the IED IED_NAME2: 3 > 2 max",
                        "There are too much SMV Control Blocks for the IED IED_NAME2: 3 > 1 max");
    }

    @Test
    void manageMonitoringLns_should_update_and_create_lsvs_and_goose() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/monitoring_lns/scd_monitoring_lsvs_lgos.xml");
        // When
        SclReport sclReport = SclService.manageMonitoringLns(scd);
        //Then
        assertThat(sclReport.getSclReportItems()).isEmpty();
        LDeviceAdapter lDeviceAdapter = sclReport.getSclRootAdapter().getIEDAdapterByName("IED_NAME1").getLDeviceAdapterByLdInst(LD_SUIED);
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(4)
                .extracting(LNAdapter::getLNClass, LNAdapter::getLNInst).containsExactlyInAnyOrder(
                        Tuple.tuple("LGOS", "1"), Tuple.tuple("LGOS", "2"),
                        Tuple.tuple("LSVS", "1"), Tuple.tuple("LSVS", "2"));
        SclTestMarshaller.assertIsMarshallable(sclReport.getSclRootAdapter().currentElem);
    }

    @Test
    void manageMonitoringLns_should_not_update_and_not_create_lsvs_and_goose_when_no_extRef() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/monitoring_lns/scd_monitoring_lsvs_lgos.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        LDeviceAdapter lDeviceAdapter11 = sclRootAdapter.getIEDAdapterByName("IED_NAME1").getLDeviceAdapterByLdInst("LD_INST11");
        lDeviceAdapter11.getLN0Adapter().getCurrentElem().setInputs(null);
        LDeviceAdapter lDeviceAdapter21 = sclRootAdapter.getIEDAdapterByName("IED_NAME1").getLDeviceAdapterByLdInst("LD_INST21");
        lDeviceAdapter21.getLN0Adapter().getCurrentElem().setInputs(null);
        // When
        SclReport sclReport = SclService.manageMonitoringLns(scd);
        //Then
        assertThat(sclReport.getSclReportItems()).isEmpty();
        LDeviceAdapter lDeviceAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME1").getLDeviceAdapterByLdInst(LD_SUIED);
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(2)
                .extracting(LNAdapter::getLNClass, LNAdapter::getLNInst).containsExactlyInAnyOrder(
                        Tuple.tuple("LGOS", "3"), Tuple.tuple("LSVS", "9"));
        SclTestMarshaller.assertIsMarshallable(sclReport.getSclRootAdapter().currentElem);
    }

    @Test
    void manageMonitoringLns_should_not_update_and_not_create_lsvs_and_goose_when_dai_not_updatable() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/monitoring_lns/scd_monitoring_lsvs_lgos.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        LDeviceAdapter lDeviceAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME1").getLDeviceAdapterByLdInst(LD_SUIED);
        getDAIAdapters(lDeviceAdapter, "LGOS", "GoCBRef", "setSrcRef")
                .forEach(daiAdapter -> daiAdapter.getCurrentElem().setValImport(false));
        getDAIAdapters(lDeviceAdapter, "LSVS", "SvCBRef", "setSrcRef")
                .forEach(daiAdapter -> daiAdapter.getCurrentElem().setValImport(false));
        // When
        SclReport sclReport = SclService.manageMonitoringLns(scd);
        //Then
        assertThat(sclReport.getSclReportItems())
                .isNotEmpty()
                .hasSize(2)
                .extracting(SclReportItem::getMessage)
                .containsExactly("The DAI cannot be updated", "The DAI cannot be updated");
        assertThat(lDeviceAdapter.getLNAdapters())
                .hasSize(2)
                .extracting(LNAdapter::getLNClass, LNAdapter::getLNInst).containsExactlyInAnyOrder(
                        Tuple.tuple("LGOS", "3"), Tuple.tuple("LSVS", "9"));
        SclTestMarshaller.assertIsMarshallable(sclReport.getSclRootAdapter().currentElem);
    }

}
