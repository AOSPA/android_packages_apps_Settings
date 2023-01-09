/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony;

import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoTdscdma;
import android.telephony.CellInfoWcdma;
import android.telephony.ServiceState;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;

import com.android.internal.telephony.OperatorInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Add static Utility functions to get information from the CellInfo object.
 * TODO: Modify {@link CellInfo} for simplify those functions
 */
public final class CellInfoUtil {
    private static final String TAG = "NetworkSelectSetting";

    private CellInfoUtil() {
    }

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * @param cellId contains the identity of the network.
     * @param networkMccMnc contains the MCCMNC string of the network
     * @return Long Name if not null/empty, otherwise Short Name if not null/empty,
     * else MCCMNC string.
     */
    public static String getNetworkTitle(CellIdentity cellId, String networkMccMnc) {
        if (cellId != null) {
            String title = Objects.toString(cellId.getOperatorAlphaLong(), "");
            if (TextUtils.isEmpty(title)) {
                title = Objects.toString(cellId.getOperatorAlphaShort(), "");
            }
            if (!TextUtils.isEmpty(title)) {
                return title;
            }
        }
        if (TextUtils.isEmpty(networkMccMnc)) {
            return "";
        }
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        return bidiFormatter.unicodeWrap(networkMccMnc, TextDirectionHeuristics.LTR);
    }

    /**
     * Returns the network info obtained in the manual search.
     *
     * @param cellId contains the identity of the network.
     * @return SNPN network Id if not null/empty, otherwise CAG name if not null/empty,
     * else CAG Id.
     */
    public static String getNetworkInfo(CellIdentityNr cellId) {
        String info = "";
        if (cellId != null) {
            if (cellId.getSnpnInfo() != null) {
                info += "SNPN: ";
                for (byte id : cellId.getSnpnInfo().getNid()) {
                    info += String.format("%02X", id);
                }
            } else if (cellId.getCagInfo() != null) {
                if (cellId.getCagInfo().getCagOnlyAccess() == false) {
                    if (cellId.getCagInfo().getCagName() != null &&
                            !(cellId.getCagInfo().getCagName().isEmpty())) {
                        info += "CAG: " + cellId.getCagInfo().getCagName();
                    } else {
                        info += "CAG: " + cellId.getCagInfo().getCagId();
                    }
                } else {
                    info += "CAG Only";
                }
            }
        }
        return info;
    }

    /**
     * Returns the CellIdentity from CellInfo
     *
     * @param cellInfo contains the information of the network.
     * @return CellIdentity within CellInfo
     */
    public static CellIdentity getCellIdentity(CellInfo cellInfo) {
        if (cellInfo == null) {
            return null;
        }
        CellIdentity cellId = null;
        if (cellInfo instanceof CellInfoGsm) {
            cellId = ((CellInfoGsm) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoCdma) {
            cellId = ((CellInfoCdma) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoWcdma) {
            cellId = ((CellInfoWcdma) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoTdscdma) {
            cellId = ((CellInfoTdscdma) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoLte) {
            cellId = ((CellInfoLte) cellInfo).getCellIdentity();
        } else if (cellInfo instanceof CellInfoNr) {
            cellId = ((CellInfoNr) cellInfo).getCellIdentity();
        }
        return cellId;
    }

    /**
     * Creates a CellInfo object from OperatorInfo. GsmCellInfo is used here only because
     * operatorInfo does not contain technology type while CellInfo is an abstract object that
     * requires to specify technology type. It doesn't matter which CellInfo type to use here, since
     * we only want to wrap the operator info and PLMN to a CellInfo object.
     */
    public static CellInfo convertOperatorInfoToCellInfo(OperatorInfo operatorInfo) {
        final String operatorNumeric = operatorInfo.getOperatorNumeric();
        String mcc = null;
        String mnc = null;
        if (operatorNumeric != null && operatorNumeric.matches("^[0-9]{5,6}$")) {
            mcc = operatorNumeric.substring(0, 3);
            mnc = operatorNumeric.substring(3);
        }
        final CellIdentityGsm cig = new CellIdentityGsm(
                Integer.MAX_VALUE /* lac */,
                Integer.MAX_VALUE /* cid */,
                Integer.MAX_VALUE /* arfcn */,
                Integer.MAX_VALUE /* bsic */,
                mcc,
                mnc,
                operatorInfo.getOperatorAlphaLong(),
                operatorInfo.getOperatorAlphaShort(),
                Collections.emptyList());

        final CellInfoGsm ci = new CellInfoGsm();
        ci.setCellIdentity(cig);
        return ci;
    }

    /**
     * Creates a CellInfo object from OperatorInfo for Legacy Incremental Scan results.
     */
    public static CellInfo convertLegacyIncrScanOperatorInfoToCellInfo(OperatorInfo operatorInfo) {
        final String operatorNumeric = operatorInfo.getOperatorNumeric();
        String mcc = null;
        String mnc = null;
        String ran = String.valueOf(AccessNetworkType.UNKNOWN);

        if (operatorNumeric != null) {
            if (operatorNumeric.matches("^[0-9]{5,6}$")) {
                mcc = operatorNumeric.substring(0, 3);
                mnc = operatorNumeric.substring(3);
            } else if (operatorNumeric.matches("^[0-9]{5,6}[+][0-9]{1,2}$")) {
                // If the operator numeric contains the RAN, then parse the MCC-MNC accordingly
                String values[] = operatorNumeric.split("\\+");
                mcc = values[0].substring(0, 3);
                mnc = values[0].substring(3);
                ran = values[1];
            }
        }

        CellInfoNr cellInfoNr = null;
        CellInfoLte cellInfoLte = null;
        CellInfoWcdma cellInfoWcdma = null;
        CellInfoGsm cellInfoGsm = null;
        CellInfoGsm cellInfoDefault = null;

        // Convert RadioAccessNetwork(ran) to AccessNetworkType
        int accessNetworkType = AccessNetworkType.convertRanToAnt(Integer.parseInt(ran));

        switch(accessNetworkType) {
            case AccessNetworkType.NGRAN:
                // 5G
                CellIdentityNr cellIdentityNr = new CellIdentityNr(
                    Integer.MAX_VALUE /* pci */,
                    Integer.MAX_VALUE /* tac */,
                    Integer.MAX_VALUE /* nrArfcn */,
                    null /* bands */,
                    mcc,
                    mnc,
                    Integer.MAX_VALUE /* nci */,
                    operatorInfo.getOperatorAlphaLong() + " 5G",
                    operatorInfo.getOperatorAlphaShort() + " 5G",
                    Collections.emptyList());
                cellInfoNr = new CellInfoNr();
                cellInfoNr.setCellIdentity(cellIdentityNr);
                break;

            case AccessNetworkType.EUTRAN:
                // 4G
                CellIdentityLte cellIdentityLte = new CellIdentityLte(
                    Integer.MAX_VALUE /* ci */,
                    Integer.MAX_VALUE /* pci */,
                    Integer.MAX_VALUE /* tac */,
                    Integer.MAX_VALUE /* earfcn */,
                    null /* bands */,
                    Integer.MAX_VALUE /* bandwidth */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong() + " 4G",
                    operatorInfo.getOperatorAlphaShort() + " 4G",
                    Collections.emptyList(),
                    null /* csgInfo */);
                cellInfoLte = new CellInfoLte();
                cellInfoLte.setCellIdentity(cellIdentityLte);
                break;

            case AccessNetworkType.UTRAN:
                CellIdentityWcdma cellIdentityWcdma = new CellIdentityWcdma(
                    Integer.MAX_VALUE /* lac */,
                    Integer.MAX_VALUE /* cid */,
                    Integer.MAX_VALUE /* psc */,
                    Integer.MAX_VALUE /* uarfcn */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong() + " 3G",
                    operatorInfo.getOperatorAlphaShort() + " 3G",
                    Collections.emptyList(),
                    null /* csgInfo */);
                cellInfoWcdma = new CellInfoWcdma();
                cellInfoWcdma.setCellIdentity(cellIdentityWcdma);
                break;

            case AccessNetworkType.GERAN:
                // 2G
                CellIdentityGsm cellIdentityGsm = new CellIdentityGsm(
                    Integer.MAX_VALUE /* lac */,
                    Integer.MAX_VALUE /* cid */,
                    Integer.MAX_VALUE /* arfcn */,
                    Integer.MAX_VALUE /* bsic */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong() + " 2G",
                    operatorInfo.getOperatorAlphaShort() + " 2G",
                    Collections.emptyList());
                cellInfoGsm = new CellInfoGsm();
                cellInfoGsm.setCellIdentity(cellIdentityGsm);
                break;

            default:
                // This is when RAT info is not present with the PLMN.
                // Do not add any network class to the operator name.
                CellIdentityGsm cellIdentityDefault = new CellIdentityGsm(
                    Integer.MAX_VALUE /* lac */,
                    Integer.MAX_VALUE /* cid */,
                    Integer.MAX_VALUE /* arfcn */,
                    Integer.MAX_VALUE /* bsic */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong(),
                    operatorInfo.getOperatorAlphaShort(),
                    Collections.emptyList());
                cellInfoDefault = new CellInfoGsm();
                cellInfoDefault.setCellIdentity(cellIdentityDefault);
                break;
        }

        CellInfo cellInfo = null;
        if (cellInfoNr != null) cellInfo = cellInfoNr;
        else if (cellInfoLte != null) cellInfo = cellInfoLte;
        else if (cellInfoWcdma != null) cellInfo = cellInfoWcdma;
        else if (cellInfoGsm != null) cellInfo = cellInfoGsm;
        else cellInfo = cellInfoDefault;

        if (operatorInfo.getState() == OperatorInfo.State.CURRENT) {
            // Unlike the legacy full scan, legacy incremental scanning using qcril hooks
            // sends the results containing the info about the currently registered operator.
            cellInfo.setRegistered(true);
        }
        return cellInfo;
    }

    /** Convert a list of cellInfos to readable string without sensitive info. */
    public static String cellInfoListToString(List<CellInfo> cellInfos) {
        return cellInfos.stream()
                .map(cellInfo -> cellInfoToString(cellInfo))
                .collect(Collectors.joining(", "));
    }

    /** Convert {@code cellInfo} to a readable string without sensitive info. */
    public static String cellInfoToString(CellInfo cellInfo) {
        final String cellType = cellInfo.getClass().getSimpleName();
        final CellIdentity cid = getCellIdentity(cellInfo);
        String mcc = getCellIdentityMcc(cid);
        String mnc = getCellIdentityMnc(cid);
        CharSequence alphaLong = null;
        CharSequence alphaShort = null;
        if (cid != null) {
            alphaLong = cid.getOperatorAlphaLong();
            alphaShort = cid.getOperatorAlphaShort();
        }
        return String.format(
                "{CellType = %s, isRegistered = %b, mcc = %s, mnc = %s, alphaL = %s, alphaS = %s}",
                cellType, cellInfo.isRegistered(), mcc, mnc,
                alphaLong, alphaShort);
    }

    /**
     * Returns the MccMnc.
     *
     * @param cid contains the identity of the network.
     * @return MccMnc string.
     */
    public static String getCellIdentityMccMnc(CellIdentity cid) {
        String mcc = getCellIdentityMcc(cid);
        String mnc = getCellIdentityMnc(cid);
        return (mcc == null || mnc == null) ? null : mcc + mnc;
    }

    /**
     * Returns the Mcc.
     *
     * @param cid contains the identity of the network.
     * @return Mcc string.
     */
    public static String getCellIdentityMcc(CellIdentity cid) {
        String mcc = null;
        if (cid != null) {
            if (cid instanceof CellIdentityGsm) {
                mcc = ((CellIdentityGsm) cid).getMccString();
            } else if (cid instanceof CellIdentityWcdma) {
                mcc = ((CellIdentityWcdma) cid).getMccString();
            } else if (cid instanceof CellIdentityTdscdma) {
                mcc = ((CellIdentityTdscdma) cid).getMccString();
            } else if (cid instanceof CellIdentityLte) {
                mcc = ((CellIdentityLte) cid).getMccString();
            } else if (cid instanceof CellIdentityNr) {
                mcc = ((CellIdentityNr) cid).getMccString();
            }
        }
        return (mcc == null) ? null : mcc;
    }

    /**
     * Returns the Mnc.
     *
     * @param cid contains the identity of the network.
     * @return Mcc string.
     */
    public static String getCellIdentityMnc(CellIdentity cid) {
        String mnc = null;
        if (cid != null) {
            if (cid instanceof CellIdentityGsm) {
                mnc = ((CellIdentityGsm) cid).getMncString();
            } else if (cid instanceof CellIdentityWcdma) {
                mnc = ((CellIdentityWcdma) cid).getMncString();
            } else if (cid instanceof CellIdentityTdscdma) {
                mnc = ((CellIdentityTdscdma) cid).getMncString();
            } else if (cid instanceof CellIdentityLte) {
                mnc = ((CellIdentityLte) cid).getMncString();
            } else if (cid instanceof CellIdentityNr) {
                mnc = ((CellIdentityNr) cid).getMncString();
            }
        }
        return (mnc == null) ? null : mnc;
    }
}
