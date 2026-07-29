package com.rehealth.genie.ring.provider

/** Domain-level vendor identifier. Vendor SDK types must remain behind RingRepository. */
enum class WearableVendor {
    MOCK,
    MRD,
    RWFIT,
    HBAND,

    /** 云米/MiwiTracker 4G 云平台手表（S8 等）：数据经厂商云回调进入后端，App 通过 IMEI 绑定。 */
    MIWI4G,
}
