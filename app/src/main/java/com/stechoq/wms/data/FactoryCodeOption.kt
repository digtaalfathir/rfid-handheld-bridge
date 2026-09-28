package com.stechoq.wms.data

/** One entry from /api/v1/master/warehouse-factory/. [code] is what the regular scan payload's
 * "factory_code" sends; [id] is the numeric id the RFID-replacement endpoint's "factory_id" wants
 * instead — same fetched entry, two different downstream fields. [name] only exists to make the
 * dropdown list readable when picking one. */
data class FactoryCodeOption(val id: Int, val code: String, val name: String)
