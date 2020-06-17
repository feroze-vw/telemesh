package com.w3engineers.unicef.telemesh.data.helper;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.w3engineers.mesh.util.Constant;
import com.w3engineers.unicef.telemesh.data.helper.constants.Constants;
import com.w3engineers.unicef.telemesh.data.local.grouptable.GroupDataSource;
import com.w3engineers.unicef.telemesh.data.local.grouptable.GroupEntity;
import com.w3engineers.unicef.telemesh.data.local.grouptable.GroupMembersInfo;
import com.w3engineers.unicef.telemesh.data.local.grouptable.GroupModel;
import com.w3engineers.unicef.telemesh.data.local.grouptable.GroupNameModel;
import com.w3engineers.unicef.telemesh.data.local.grouptable.GroupUserNameMap;
import com.w3engineers.unicef.telemesh.data.local.messagetable.MessageEntity;
import com.w3engineers.unicef.telemesh.data.local.messagetable.MessageSourceData;
import com.w3engineers.unicef.telemesh.data.local.usertable.UserEntity;
import com.w3engineers.unicef.util.helper.CommonUtil;
import com.w3engineers.unicef.util.helper.GsonBuilder;
import com.w3engineers.unicef.util.helper.uiutil.UIHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.reactivex.schedulers.Schedulers;

public class GroupDataHelper extends RmDataHelper {

    private static GroupDataHelper groupDataHelper = new GroupDataHelper();
    private GroupDataSource groupDataSource;

    private GroupDataHelper() {
        groupDataSource = GroupDataSource.getInstance();
    }

    @NonNull
    public static GroupDataHelper getInstance() {
        return groupDataHelper;
    }

    void groupDataObserver() {

        compositeDisposable.add(groupDataSource.getLastCreatedGroup()
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::sendGroupCreationInfo, Throwable::printStackTrace));

        compositeDisposable.add(Objects.requireNonNull(dataSource.getGroupUserEvent())
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::sendGroupForEvent, Throwable::printStackTrace));
    }

    void groupDataReceive(int dataType, String userId, byte[] rawData, boolean isNewMessage) {
        if (!isNewMessage)
            return;

        switch (dataType) {
            case Constants.DataType.EVENT_GROUP_CREATION:
                receiveGroupCreationInfo(rawData);
                break;
            case Constants.DataType.EVENT_GROUP_JOIN:
                receiveGroupForJoinEvent(rawData, userId);
                break;
            case Constants.DataType.EVENT_GROUP_LEAVE:
                receiveGroupForLeaveEvent(rawData, userId);
                break;
        }
    }

    private void sendGroupCreationInfo(GroupEntity groupEntity) {
        GroupModel groupModel = groupEntity.toGroupModel();
        String groupModelText = GsonBuilder.getInstance().getGroupModelJson(groupModel);

        ArrayList<GroupMembersInfo> groupMembersInfos = GsonBuilder.getInstance()
                .getGroupMemberInfoObj(groupEntity.getMembersInfo());

        if (groupMembersInfos != null) {
            for (GroupMembersInfo groupMembersInfo : groupMembersInfos) {
                String userId = groupMembersInfo.getMemberId();
                if (!userId.equals(getMyMeshId())) {
                    dataSend(groupModelText.getBytes(), Constants.DataType.EVENT_GROUP_CREATION, userId, false);
                }
            }
        }

        setGroupJoined(groupMembersInfos);

        String groupMemberInfoText = GsonBuilder.getInstance().getGroupMemberInfoJson(groupMembersInfos);

        groupEntity.setOwnStatus(Constants.GroupUserOwnState.GROUP_JOINED);
        groupEntity.setMembersInfo(groupMemberInfoText);

        groupDataSource.insertOrUpdateGroup(groupEntity);
    }

    private void receiveGroupCreationInfo(byte[] rawData) {
        try {

            GsonBuilder gsonBuilder = GsonBuilder.getInstance();

            String groupModelText = new String(rawData);
            GroupModel groupModel = gsonBuilder.getGroupModelObj(groupModelText);

            GroupEntity groupEntity = groupDataSource.getGroupById(groupModel.getGroupId());

            if (groupEntity == null) {
                groupEntity = new GroupEntity().toGroupEntity(groupModel);
            } else {

                groupEntity.setGroupName(groupModel.getGroupName());
                groupEntity.setAvatarIndex(groupModel.getAvatar());

                groupEntity.setAdminInfo(groupModel.getAdminInfo());

                ArrayList<GroupMembersInfo> groupMembersInfos = gsonBuilder.getGroupMemberInfoObj(groupModel.getMemberInfo());

                String storedMemberInfoText = groupEntity.getMembersInfo();

                if (TextUtils.isEmpty(storedMemberInfoText)) {
                    storedMemberInfoText = gsonBuilder.getGroupMemberInfoJson(groupMembersInfos);

                } else {
                    ArrayList<GroupMembersInfo> storedMembersInfos = gsonBuilder
                            .getGroupMemberInfoObj(storedMemberInfoText);

                    HashMap<String, GroupMembersInfo> groupMembersInfoHashMap = new HashMap<>();
                    for (GroupMembersInfo groupMembersInfo : storedMembersInfos) {
                        groupMembersInfoHashMap.put(groupMembersInfo.getMemberId(), groupMembersInfo);
                    }

                    for (int i = (groupMembersInfos.size() - 1); i >= 0; i--) {
                        GroupMembersInfo groupMembersInfo = groupMembersInfos.get(i);

                        if (groupMembersInfoHashMap.containsKey(groupMembersInfo.getMemberId())) {

                            GroupMembersInfo storedMembersInfo = groupMembersInfoHashMap
                                    .get(groupMembersInfo.getMemberId());

                            if (storedMembersInfo != null) {
                                if (storedMembersInfo.getMemberStatus() == Constants.GroupUserEvent.EVENT_LEAVE) {
                                    groupMembersInfos.remove(groupMembersInfo);
                                } else {
                                    groupMembersInfos.set(i, storedMembersInfo);
                                }
                            }
                        }
                    }

                    storedMemberInfoText = gsonBuilder.getGroupMemberInfoJson(groupMembersInfos);
                }

                groupEntity.setMembersInfo(storedMemberInfoText);
            }


            groupEntity.setOwnStatus(Constants.GroupUserOwnState.GROUP_PENDING);

            groupDataSource.insertOrUpdateGroup(groupEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGroupForEvent(GroupEntity groupEntity) {

        ArrayList<GroupMembersInfo> groupMembersInfos = GsonBuilder.getInstance()
                .getGroupMemberInfoObj(groupEntity.getMembersInfo());

        byte groupEvent = -1;

        if (groupEntity.getOwnStatus() == Constants.GroupUserOwnState.GROUP_JOINED) {

            groupEvent = Constants.DataType.EVENT_GROUP_JOIN;
            setGroupJoined(groupMembersInfos);

            String groupMemberInfoText = GsonBuilder.getInstance().getGroupMemberInfoJson(groupMembersInfos);
            groupEntity.setMembersInfo(groupMemberInfoText);

            groupDataSource.insertOrUpdateGroup(groupEntity);

        } else if (groupEntity.getOwnStatus() == Constants.GroupUserOwnState.GROUP_LEAVE) {
            groupEvent = Constants.DataType.EVENT_GROUP_LEAVE;

            groupDataSource.deleteGroupById(groupEntity.groupId);
        }

        if (groupMembersInfos != null) {
            for (GroupMembersInfo groupMembersInfo : groupMembersInfos) {
                String userId = groupMembersInfo.getMemberId();
                if (!userId.equals(getMyMeshId())) {
                    dataSend(groupEntity.getGroupId().getBytes(),
                            groupEvent, userId, false);
                }
            }
        }
    }

    private void receiveGroupForJoinEvent(byte[] rawData, String userId) {
        try {
            String groupId = new String(rawData);
            GroupEntity groupEntity = groupDataSource.getGroupById(groupId);

            if (groupEntity == null) {
                groupEntity = new GroupEntity()
                        .setGroupId(groupId);

                ArrayList<GroupMembersInfo> groupMembersInfos = new ArrayList<>();
                GroupMembersInfo groupMembersInfo = new GroupMembersInfo().setMemberId(userId)
                        .setMemberStatus(Constants.GroupUserEvent.EVENT_JOINED);
                groupMembersInfos.add(groupMembersInfo);

                String groupMemberInfoText = GsonBuilder.getInstance()
                        .getGroupMemberInfoJson(groupMembersInfos);
                groupEntity.setMembersInfo(groupMemberInfoText);

            } else {
                String groupMemberInfoText = groupEntity.getMembersInfo();
                ArrayList<GroupMembersInfo> groupMembersInfos = GsonBuilder.getInstance()
                        .getGroupMemberInfoObj(groupMemberInfoText);

                boolean isMemberUpdate = false;
                for (int i = 0; i < groupMembersInfos.size(); i++) {
                    GroupMembersInfo groupMembersInfo = groupMembersInfos.get(i);

                    if (groupMembersInfo.getMemberId().equals(userId)) {
                        groupMembersInfo.setMemberStatus(Constants.GroupUserEvent.EVENT_JOINED);
                        groupMembersInfos.set(i, groupMembersInfo);
                        isMemberUpdate = true;
                    }
                }

                if (!isMemberUpdate) {
                    GroupMembersInfo groupMembersInfo = new GroupMembersInfo().setMemberId(userId)
                            .setMemberStatus(Constants.GroupUserEvent.EVENT_JOINED);
                    groupMembersInfos.add(groupMembersInfo);
                }

                groupMemberInfoText = GsonBuilder.getInstance()
                        .getGroupMemberInfoJson(groupMembersInfos);
                groupEntity.setMembersInfo(groupMemberInfoText);
            }

            groupDataSource.insertOrUpdateGroup(groupEntity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveGroupForLeaveEvent(byte[] rawData, String userId) {
        try {
            String groupId = new String(rawData);
            GroupEntity groupEntity = groupDataSource.getGroupById(groupId);

            if (groupEntity == null) {
                groupEntity = new GroupEntity()
                        .setGroupId(groupId);

                ArrayList<GroupMembersInfo> groupMembersInfos = new ArrayList<>();
                GroupMembersInfo groupMembersInfo = new GroupMembersInfo().setMemberId(userId)
                        .setMemberStatus(Constants.GroupUserEvent.EVENT_LEAVE);
                groupMembersInfos.add(groupMembersInfo);

                String groupMemberInfoText = GsonBuilder.getInstance()
                        .getGroupMemberInfoJson(groupMembersInfos);
                groupEntity.setMembersInfo(groupMemberInfoText);

            } else {
                String groupMemberInfoText = groupEntity.getMembersInfo();
                ArrayList<GroupMembersInfo> groupMembersInfos = GsonBuilder.getInstance()
                        .getGroupMemberInfoObj(groupMemberInfoText);

                boolean isMemberLeaved = false;

                for (int i = (groupMembersInfos.size() - 1); i >= 0; i--) {
                    GroupMembersInfo groupMembersInfo = groupMembersInfos.get(i);

                    if (groupMembersInfo.getMemberId().equals(userId)) {
                        if (!TextUtils.isEmpty(groupEntity.getGroupName())) {
                            groupMembersInfos.remove(groupMembersInfo);
                        } else {
                            groupMembersInfo.setMemberStatus(Constants.GroupUserEvent.EVENT_LEAVE);
                            groupMembersInfos.set(i, groupMembersInfo);
                        }
                        isMemberLeaved = true;
                    }
                }

                if (!isMemberLeaved) {
                    GroupMembersInfo groupMembersInfo = new GroupMembersInfo().setMemberId(userId)
                            .setMemberStatus(Constants.GroupUserEvent.EVENT_LEAVE);
                    groupMembersInfos.add(groupMembersInfo);
                }

                groupMemberInfoText = GsonBuilder.getInstance()
                        .getGroupMemberInfoJson(groupMembersInfos);
                groupEntity.setMembersInfo(groupMemberInfoText);
            }

            groupDataSource.insertOrUpdateGroup(groupEntity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateGroupUserInfo(UserEntity userEntity) {
        String updatedUserId = "%" + userEntity.getMeshId() + "%";
        List<GroupEntity> groupEntities = groupDataSource.getGroupByUserId(updatedUserId);

        for (GroupEntity groupEntity : groupEntities) {

            GroupNameModel groupNameModel = GsonBuilder.getInstance()
                    .getGroupNameModelObj(groupEntity.getGroupName());
            List<GroupUserNameMap> groupUserNameMaps = groupNameModel.getGroupUserMap();

            for (int i = 0; i < groupUserNameMaps.size(); i++) {

                GroupUserNameMap groupUserNameMap = groupUserNameMaps.get(i);
                if (groupUserNameMap.getUserId().equals(userEntity.getMeshId())) {
                    groupUserNameMap.setUserName(userEntity.getUserName());

                    groupUserNameMaps.set(i, groupUserNameMap);
                }
            }

            groupNameModel.setGroupUserMap(groupUserNameMaps);
            groupNameModel.setGroupName(CommonUtil.getGroupName(groupUserNameMaps));
            String groupNameText = GsonBuilder.getInstance().getGroupNameModelJson(groupNameModel);

            groupEntity.setGroupName(groupNameText);
            groupDataSource.insertOrUpdateGroup(groupEntity);
        }
    }

    private void setGroupJoined(ArrayList<GroupMembersInfo> groupMembersInfos) {
        if (groupMembersInfos != null) {
            for (int i = 0; i < groupMembersInfos.size(); i++) {
                GroupMembersInfo groupMembersInfo = groupMembersInfos.get(i);

                if (groupMembersInfo.getMemberId().equals(getMyMeshId())) {
                    groupMembersInfo.setMemberStatus(Constants.GroupUserEvent.EVENT_JOINED);

                    groupMembersInfos.set(i, groupMembersInfo);
                }
            }
        }
    }

}