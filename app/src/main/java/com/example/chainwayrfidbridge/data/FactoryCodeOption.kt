package com.example.chainwayrfidbridge.data

/** One entry from /api/v1/master/warehouse-factory/. [code] is what's stored and sent to the
 * server; [name] only exists to make the dropdown list readable when picking one. */
data class FactoryCodeOption(val code: String, val name: String)
