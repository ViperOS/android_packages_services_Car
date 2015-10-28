/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.support.car.test;

import com.android.car.VehicleHalEmulator.VehicleHalPropertyHandler;
import com.android.car.vehiclenetwork.VehicleNetworkConsts;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehicleAppContextFlag;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehicleAudioRoutingPolicyIndex;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehiclePermissionModel;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehiclePropAccess;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehiclePropChangeMode;
import com.android.car.vehiclenetwork.VehicleNetworkConsts.VehicleValueType;
import com.android.car.vehiclenetwork.VehicleNetworkProto.VehiclePropValue;
import com.android.car.vehiclenetwork.VehiclePropConfigUtil;
import com.android.car.vehiclenetwork.VehiclePropValueUtil;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AudioRoutingPolicyTest extends MockedCarTestBase {

    private static final long TIMEOUT_MS = 3000;

    private VehicleHalPropertyHandler mAudioRoutingPolicyHandler = new VehicleHalPropertyHandler() {

        @Override
        public void onPropertySet(VehiclePropValue value) {
            handlePropertySetEvent(value);
        }

        @Override
        public VehiclePropValue onPropertyGet(VehiclePropValue value) {
            fail("cannot get");
            return null;
        }

        @Override
        public void onPropertySubscribe(int property, int sampleRate) {
            fail("cannot subscribe");
        }

        @Override
        public void onPropertyUnsubscribe(int property) {
            fail("cannot unsubscribe");
        }
    };

    private final Semaphore mWaitSemaphore = new Semaphore(0);
    private final LinkedList<VehiclePropValue> mEvents = new LinkedList<VehiclePropValue>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        getVehicleHalEmulator().addProperty(
                VehiclePropConfigUtil.getBuilder(
                        VehicleNetworkConsts.VEHICLE_PROPERTY_AUDIO_ROUTING_POLICY,
                        VehiclePropAccess.VEHICLE_PROP_ACCESS_WRITE,
                        VehiclePropChangeMode.VEHICLE_PROP_CHANGE_MODE_ON_CHANGE,
                        VehicleValueType.VEHICLE_VALUE_TYPE_INT32_VEC2,
                        VehiclePermissionModel.VEHICLE_PERMISSION_SYSTEM_APP_ONLY,
                        0 /*configFlags*/, 0 /*sampleRateMax*/, 0 /*sampleRateMin*/).build(),
                mAudioRoutingPolicyHandler);
    }

    public void testNoHwVaraint() throws Exception {
        getVehicleHalEmulator().removeProperty(
                VehicleNetworkConsts.VEHICLE_PROPERTY_AUDIO_HW_VARIANT);
        getVehicleHalEmulator().start();
        checkPolicy0();
    }

    public void testHwVariant0() throws Exception {
        getVehicleHalEmulator().addStaticProperty(
                VehiclePropConfigUtil.createStaticStringProperty(
                        VehicleNetworkConsts.VEHICLE_PROPERTY_AUDIO_HW_VARIANT),
                VehiclePropValueUtil.createIntValue(
                        VehicleNetworkConsts.VEHICLE_PROPERTY_AUDIO_HW_VARIANT, 0, 0));
        getVehicleHalEmulator().start();
        checkPolicy0();
    }

    public void testHwVariant1() throws Exception {
        getVehicleHalEmulator().addStaticProperty(
                VehiclePropConfigUtil.createStaticStringProperty(
                        VehicleNetworkConsts.VEHICLE_PROPERTY_AUDIO_HW_VARIANT),
                VehiclePropValueUtil.createIntValue(
                        VehicleNetworkConsts.VEHICLE_PROPERTY_AUDIO_HW_VARIANT, 1, 0));
        getVehicleHalEmulator().start();
        checkPolicy1();
    }

    private void checkPolicy0() throws Exception {
        assertTrue(mWaitSemaphore.tryAcquire(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        VehiclePropValue v = mEvents.get(0);
        assertEquals(0, v.getInt32Values(
                VehicleAudioRoutingPolicyIndex.VEHICLE_AUDIO_ROUTING_POLICY_INDEX_STREAM));
        assertEquals(
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_ALARM_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_CALL_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_MUSIC_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_NAVIGATION_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_NOTIFICATION_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_UNKNOWN_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_VOICE_COMMAND_FLAG,
                v.getInt32Values(
                        VehicleAudioRoutingPolicyIndex.VEHICLE_AUDIO_ROUTING_POLICY_INDEX_CONTEXTS)
                        );
    }

    private void checkPolicy1() throws Exception {
        // write should be twice.
        assertTrue(mWaitSemaphore.tryAcquire(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mWaitSemaphore.tryAcquire(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        VehiclePropValue v = mEvents.get(0);
        assertEquals(0, v.getInt32Values(
                VehicleAudioRoutingPolicyIndex.VEHICLE_AUDIO_ROUTING_POLICY_INDEX_STREAM));
        assertEquals(
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_CALL_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_MUSIC_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_UNKNOWN_FLAG,
                v.getInt32Values(
                        VehicleAudioRoutingPolicyIndex.VEHICLE_AUDIO_ROUTING_POLICY_INDEX_CONTEXTS)
                        );
        v = mEvents.get(1);
        assertEquals(1, v.getInt32Values(
                VehicleAudioRoutingPolicyIndex.VEHICLE_AUDIO_ROUTING_POLICY_INDEX_STREAM));
        assertEquals(
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_ALARM_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_NAVIGATION_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_NOTIFICATION_FLAG |
                VehicleAppContextFlag.VEHICLE_APP_CONTEXT_VOICE_COMMAND_FLAG,
                v.getInt32Values(
                        VehicleAudioRoutingPolicyIndex.VEHICLE_AUDIO_ROUTING_POLICY_INDEX_CONTEXTS)
                        );
    }

    private void handlePropertySetEvent(VehiclePropValue value) {
        mEvents.add(value);
        mWaitSemaphore.release();
    }
}