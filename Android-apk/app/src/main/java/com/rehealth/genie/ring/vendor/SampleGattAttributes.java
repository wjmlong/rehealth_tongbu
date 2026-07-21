/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.rehealth.genie.ring.vendor;

import java.util.UUID;

/**
 * Vendor GATT attributes copied verbatim from the MRD SDK demo.
 * Service/characteristic UUIDs must match the ring firmware exactly.
 */
public class SampleGattAttributes {
    public static UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    /**
     * 写
     */
    public final static UUID WriteServiceUUID = UUID.fromString("f000efe0-0451-4000-0000-00000000b000");
    public final static UUID WriteCharacteristicUUID = UUID.fromString("f000efe1-0451-4000-0000-00000000b000");
    /**
     * 读
     */
    public final static UUID NotifyServiceUUID = UUID.fromString("f000efe0-0451-4000-0000-00000000b000");
    public final static UUID NotifyCharacteristicUUID = UUID.fromString("f000efe3-0451-4000-0000-00000000b000");
}
