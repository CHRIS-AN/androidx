/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** RouteItem helps keep track of the current app controlled routes. */
public final class RouteItem {

    private String mId;
    private String mName;
    private String mDescription;
    private ControlFilter mControlFilter;
    private PlaybackStream mPlaybackStream;
    private PlaybackType mPlaybackType;
    private boolean mCanDisconnect;
    private VolumeHandling mVolumeHandling;
    private int mVolume;
    private int mVolumeMax;
    private DeviceType mDeviceType;
    private List<String> mGroupMemberIds;

    public RouteItem() {
        this.mId = UUID.randomUUID().toString();
        this.mName = "";
        this.mDescription = "";
        this.mControlFilter = ControlFilter.BASIC;
        this.mPlaybackStream = PlaybackStream.MUSIC;
        this.mPlaybackType = PlaybackType.REMOTE;
        this.mVolumeHandling = VolumeHandling.FIXED;
        this.mVolume = 5;
        this.mVolumeMax = 25;
        this.mDeviceType = DeviceType.UNKNOWN;
        this.mCanDisconnect = false;
        this.mGroupMemberIds = new ArrayList<>();
    }

    public enum ControlFilter {
        BASIC,
        QUEUE,
        SESSION
    }

    public enum PlaybackStream {
        ACCESSIBILITY,
        ALARM,
        DTMF,
        MUSIC,
        NOTIFICATION,
        RING,
        SYSTEM,
        VOICE_CALL
    }

    public enum PlaybackType {
        LOCAL,
        REMOTE
    }

    public enum VolumeHandling {
        FIXED,
        VARIABLE
    }

    public enum DeviceType {
        TV,
        SPEAKER,
        BLUETOOTH,
        UNKNOWN
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public void setId(@NonNull String id) {
        mId = id;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String description) {
        mDescription = description;
    }

    @NonNull
    public ControlFilter getControlFilter() {
        return mControlFilter;
    }

    public void setControlFilter(@NonNull ControlFilter controlFilter) {
        mControlFilter = controlFilter;
    }

    @NonNull
    public PlaybackStream getPlaybackStream() {
        return mPlaybackStream;
    }

    public void setPlaybackStream(@NonNull PlaybackStream playbackStream) {
        mPlaybackStream = playbackStream;
    }

    @NonNull
    public PlaybackType getPlaybackType() {
        return mPlaybackType;
    }

    public void setPlaybackType(@NonNull PlaybackType playbackType) {
        mPlaybackType = playbackType;
    }

    public boolean isCanDisconnect() {
        return mCanDisconnect;
    }

    public void setCanDisconnect(boolean canDisconnect) {
        mCanDisconnect = canDisconnect;
    }

    @NonNull
    public VolumeHandling getVolumeHandling() {
        return mVolumeHandling;
    }

    public void setVolumeHandling(@NonNull VolumeHandling volumeHandling) {
        mVolumeHandling = volumeHandling;
    }

    public int getVolume() {
        return mVolume;
    }

    public void setVolume(int volume) {
        mVolume = volume;
    }

    public int getVolumeMax() {
        return mVolumeMax;
    }

    public void setVolumeMax(int volumeMax) {
        mVolumeMax = volumeMax;
    }

    @NonNull
    public DeviceType getDeviceType() {
        return mDeviceType;
    }

    public void setDeviceType(@NonNull DeviceType deviceType) {
        mDeviceType = deviceType;
    }

    @NonNull
    public List<String> getGroupMemberIds() {
        return mGroupMemberIds;
    }

    public void setGroupMemberIds(@NonNull List<String> groupMemberIds) {
        mGroupMemberIds = groupMemberIds;
    }
}
