/*
 * Copyright (C) 2024 The Android Open Source Project
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
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.settings.network.telephony

import android.content.Context
import android.telephony.AccessNetworkConstants.AccessNetworkType
import android.telephony.CellIdentity
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellIdentityGsm
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoGsm
import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import com.android.internal.telephony.OperatorInfo

import java.util.Collections

/**
 * Add static Utility functions to get information from the CellInfo object.
 * TODO: Modify [CellInfo] for simplify those functions
 */
object CellInfoUtil {

    var context: Context? = null

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * By the following order,
     * 1. Long Name if not null/empty
     * 2. Short Name if not null/empty
     * 3. OperatorNumeric (MCCMNC) string
     */
    @JvmStatic
    fun CellIdentity.getNetworkTitle(): String? {
        operatorAlphaLong?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        operatorAlphaShort?.takeIf { it.isNotBlank() }?.let { return it.toString() }
        val operatorNumeric = getOperatorNumeric() ?: return null
        val bidiFormatter = BidiFormatter.getInstance()
        return bidiFormatter.unicodeWrap(operatorNumeric, TextDirectionHeuristics.LTR)
    }

    /**
     * Creates a CellInfo object from OperatorInfo for Legacy Incremental Scan results.
     */
    @JvmStatic
    fun convertLegacyIncrScanOperatorInfoToCellInfo(operatorInfo: OperatorInfo): CellInfo? {
        val operatorNumeric: String = operatorInfo.getOperatorNumeric()
        var mcc: String? = null
        var mnc: String? = null
        var ran: String = java.lang.String.valueOf(AccessNetworkType.UNKNOWN)
        if (operatorNumeric != null) {
            if (operatorNumeric.matches("^[0-9]{5,6}$".toRegex())) {
                mcc = operatorNumeric.substring(0, 3)
                mnc = operatorNumeric.substring(3)
            } else if (operatorNumeric.matches("^[0-9]{5,6}[+][0-9]{1,2}$".toRegex())) {
                // If the operator numeric contains the RAN, then parse the MCC-MNC accordingly
                val values = operatorNumeric.split("\\+".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                mcc = values[0].substring(0, 3)
                mnc = values[0].substring(3)
                ran = values[1]
            }
        }

        var cellInfoNr: CellInfoNr? = null
        var cellInfoLte: CellInfoLte? = null
        var cellInfoWcdma: CellInfoWcdma? = null
        var cellInfoGsm: CellInfoGsm? = null
        var cellInfoDefault: CellInfoGsm? = null

        // Convert RadioAccessNetwork(ran) to AccessNetworkType
        val accessNetworkType: Int = AccessNetworkType.convertRanToAnt(ran.toInt())
        when (accessNetworkType) {
            AccessNetworkType.NGRAN -> {
                // 5G
                val cellIdentityNr = CellIdentityNr(
                    Int.MAX_VALUE /* pci */,
                    Int.MAX_VALUE /* tac */,
                    Int.MAX_VALUE /* nrArfcn */,
                    intArrayOf() /* bands */,
                    mcc,
                    mnc, Long.MAX_VALUE /* nci */,
                    operatorInfo.getOperatorAlphaLong() + " 5G",
                    operatorInfo.getOperatorAlphaShort() + " 5G",
                    Collections.emptyList()
                )
                cellInfoNr = CellInfoNr()
                cellInfoNr.setCellIdentity(cellIdentityNr)
            }

            AccessNetworkType.EUTRAN -> {
                // 4G
                val cellIdentityLte = CellIdentityLte(
                    Int.MAX_VALUE /* ci */,
                    Int.MAX_VALUE /* pci */,
                    Int.MAX_VALUE /* tac */,
                    Int.MAX_VALUE /* earfcn */,
                    intArrayOf() /* bands */,
                    Int.MAX_VALUE /* bandwidth */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong() + " 4G",
                    operatorInfo.getOperatorAlphaShort() + " 4G",
                    Collections.emptyList(),
                    null /* csgInfo */
                )
                cellInfoLte = CellInfoLte()
                cellInfoLte.setCellIdentity(cellIdentityLte)
            }

            AccessNetworkType.UTRAN -> {
                // 3G
                val cellIdentityWcdma = CellIdentityWcdma(
                    Int.MAX_VALUE /* lac */,
                    Int.MAX_VALUE /* cid */,
                    Int.MAX_VALUE /* psc */,
                    Int.MAX_VALUE /* uarfcn */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong() + " 3G",
                    operatorInfo.getOperatorAlphaShort() + " 3G",
                    Collections.emptyList(),
                    null /* csgInfo */
                )
                cellInfoWcdma = CellInfoWcdma()
                cellInfoWcdma.setCellIdentity(cellIdentityWcdma)
            }

            AccessNetworkType.GERAN -> {
                // 2G
                val cellIdentityGsm = CellIdentityGsm(
                    Int.MAX_VALUE /* lac */,
                    Int.MAX_VALUE /* cid */,
                    Int.MAX_VALUE /* arfcn */,
                    Int.MAX_VALUE /* bsic */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong() + " 2G",
                    operatorInfo.getOperatorAlphaShort() + " 2G",
                    Collections.emptyList()
                )
                cellInfoGsm = CellInfoGsm()
                cellInfoGsm.setCellIdentity(cellIdentityGsm)
            }

            else -> {
                // This is when RAT info is not present with the PLMN.
                // Do not add any network class to the operator name.
                val cellIdentityDefault = CellIdentityGsm(
                    Int.MAX_VALUE /* lac */,
                    Int.MAX_VALUE /* cid */,
                    Int.MAX_VALUE /* arfcn */,
                    Int.MAX_VALUE /* bsic */,
                    mcc,
                    mnc,
                    operatorInfo.getOperatorAlphaLong(),
                    operatorInfo.getOperatorAlphaShort(),
                    Collections.emptyList()
                )
                cellInfoDefault = CellInfoGsm()
                cellInfoDefault.setCellIdentity(cellIdentityDefault)
            }
        }
        var cellInfo: CellInfo? = null
        cellInfo = if (cellInfoNr != null) cellInfoNr
            else if (cellInfoLte != null) cellInfoLte
            else if (cellInfoWcdma != null) cellInfoWcdma
            else if (cellInfoGsm != null) cellInfoGsm
            else cellInfoDefault
        if (operatorInfo.getState() === OperatorInfo.State.CURRENT) {
            // Unlike the legacy full scan, legacy incremental scan using qcril hooks
            // sends the results containing the info about the currently registered operator.
            cellInfo?.setRegistered(true)
        }
        return cellInfo
    }

    /**
     * Convert a list of cellInfos to readable string without sensitive info.
     */
    @JvmStatic
    fun cellInfoListToString(cellInfos: List<CellInfo>): String =
        cellInfos.joinToString(System.lineSeparator()) { cellInfo -> cellInfo.readableString() }

    /**
     * Convert [CellInfo] to a readable string without sensitive info.
     */
    private fun CellInfo.readableString(): String = buildString {
        append("{CellType = ${this@readableString::class.simpleName}, ")
        append("isRegistered = $isRegistered, ")
        append(cellIdentity.readableString())
        append("}")
    }

    private fun CellIdentity.readableString(): String = buildString {
        append("mcc = $mccString, ")
        append("mnc = $mncString, ")
        append("alphaL = $operatorAlphaLong, ")
        append("alphaS = $operatorAlphaShort")
    }

    /**
     * Returns the MccMnc.
     */
    @JvmStatic
    fun CellIdentity.getOperatorNumeric(): String? {
        if (this is CellIdentityNr) {
            if (MobileNetworkUtils.isCagSnpnEnabled(context)) {
                if (snpnInfo != null) {
                    return snpnInfo.operatorNumeric
                }
            }
        }
        val mcc = mccString
        val mnc = mncString
        return if (mcc == null || mnc == null) null else mcc + mnc
    }

    /**
     * Returns the network info obtained in the manual search.
     *
     * @param cellId contains the identity of the network.
     * @return SNPN network Id if not null/empty, otherwise CAG name if not null/empty,
     * else CAG Id.
     */
    fun getNetworkInfo(cellId: CellIdentityNr?): String {
        var info = ""
        if (cellId != null) {
            if (cellId.snpnInfo != null) {
                info += "SNPN: "
                for (id in cellId.snpnInfo.nid) {
                    info += String.format("%02X", id)
                }
            } else if (cellId.cagInfo != null) {
                info += if (cellId.cagInfo.cagOnlyAccess == false) {
                    if (cellId.cagInfo.cagName != null && !cellId.cagInfo.cagName.isEmpty()) {
                        "CAG: " + cellId.cagInfo.cagName
                    } else {
                        "CAG: " + cellId.cagInfo.cagId
                    }
                } else {
                    "CAG Only"
                }
            }
        }
        return info
    }
}
