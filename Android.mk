LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES += libcore
LOCAL_STATIC_JAVA_LIBRARIES += libdom
LOCAL_STATIC_JAVA_LIBRARIES += libio
LOCAL_STATIC_JAVA_LIBRARIES += libjutf
LOCAL_STATIC_JAVA_LIBRARIES += libjzlib
LOCAL_STATIC_JAVA_LIBRARIES += libhtmlcleaner

LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := Email

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libcore:libs/apache-mime4j-core-0.7-SNAPSHOT.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libdom:libs/apache-mime4j-dom-0.7-SNAPSHOT.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libio:libs/commons-io-2.0.1.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libjutf:libs/jutf7-1.0.1-SNAPSHOT.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libjzlib:libs/jzlib-1.0.7.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libhtmlcleaner:libs/htmlcleaner-2.2.jar

include $(BUILD_MULTI_PREBUILT)


# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

