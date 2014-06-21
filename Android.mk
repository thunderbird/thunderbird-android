LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES += libcore
LOCAL_STATIC_JAVA_LIBRARIES += libdom
LOCAL_STATIC_JAVA_LIBRARIES += libio
LOCAL_STATIC_JAVA_LIBRARIES += libjutf
LOCAL_STATIC_JAVA_LIBRARIES += libjzlib
LOCAL_STATIC_JAVA_LIBRARIES += libhtmlcleaner
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4

LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, plugins/Android-PullToRefresh/library/src)
LOCAL_SRC_FILES += $(call all-java-files-under, plugins/ckChangeLog/library/src)
LOCAL_SRC_FILES += $(call all-java-files-under, plugins/HoloColorPicker/src)

res_dir := res plugins/Android-PullToRefresh/library/res plugins/ckChangeLog/library/res plugins/HoloColorPicker/res
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dir))

LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := Email

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages de.cketti.library.changelog
LOCAL_AAPT_FLAGS += --extra-packages android.support.v4.app
LOCAL_AAPT_FLAGS += --extra-packages com.handmark.pulltorefresh.library
LOCAL_AAPT_FLAGS += --extra-packages com.larswerkman.colorpicker

LOCAL_PROGUARD_FLAG_FILES := proguard.cfg

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libcore:libs/apache-mime4j-core-0.7.2.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libdom:libs/apache-mime4j-dom-0.7.2.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libio:libs/commons-io-2.0.1.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libjutf:libs/jutf7-1.0.1-SNAPSHOT.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libjzlib:libs/jzlib-1.0.7.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libhtmlcleaner:libs/htmlcleaner-2.2.jar

include $(BUILD_MULTI_PREBUILT)

