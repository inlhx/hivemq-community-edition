/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.packets.pubrec;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;
import com.hivemq.extension.sdk.api.packets.pubrec.PubrecPacket;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class ModifiablePubrecPacketImpl implements ModifiablePubrecPacket {

    private final @NotNull FullConfigurationService configurationService;

    private final int packetIdentifier;
    private @NotNull AckReasonCode reasonCode;
    private @Nullable String reasonString;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;

    private boolean modified = false;

    public ModifiablePubrecPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PUBREC pubrec) {

        this.configurationService = configurationService;
        packetIdentifier = pubrec.getPacketIdentifier();
        reasonCode = pubrec.getReasonCode().toAckReasonCode();
        reasonString = pubrec.getReasonString();
        userProperties = new ModifiableUserPropertiesImpl(
                pubrec.getUserProperties().getPluginUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    public ModifiablePubrecPacketImpl(
            final @NotNull FullConfigurationService configurationService,
            final @NotNull PubrecPacket pubrecPacket) {

        this.configurationService = configurationService;
        packetIdentifier = pubrecPacket.getPacketIdentifier();
        reasonCode = pubrecPacket.getReasonCode();
        reasonString = pubrecPacket.getReasonString().orElse(null);
        userProperties = new ModifiableUserPropertiesImpl(
                (InternalUserProperties) pubrecPacket.getUserProperties(),
                configurationService.securityConfiguration().validateUTF8());
    }

    @Override
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    @Override
    public @NotNull AckReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public void setReasonCode(final @NotNull AckReasonCode reasonCode) {
        Preconditions.checkNotNull(reasonCode, "Reason code must never be null");
        if (this.reasonCode == reasonCode) {
            return;
        }
        final Mqtt5PubRecReasonCode newReasonCode = Mqtt5PubRecReasonCode.from(reasonCode);
        final Mqtt5PubRecReasonCode oldReasonCode = Mqtt5PubRecReasonCode.from(this.reasonCode);
        final boolean switched = newReasonCode.isError() != oldReasonCode.isError();
        Preconditions.checkState(
                !switched, "Reason code must not switch from successful to unsuccessful or vice versa");
        this.reasonCode = reasonCode;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getReasonString() {
        return Optional.ofNullable(reasonString);
    }

    @Override
    public void setReasonString(final @Nullable String reasonString) {
        PluginBuilderUtil.checkReasonString(reasonString, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.reasonString, reasonString)) {
            return;
        }
        this.reasonString = reasonString;
        modified = true;
    }

    @Override
    public @NotNull ModifiableUserProperties getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
