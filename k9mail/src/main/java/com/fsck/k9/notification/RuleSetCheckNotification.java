package com.fsck.k9.notification;

import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.Account;

class RuleSetCheckNotification {
    private final NotificationController controller;
    private final NotificationContentCreator contentCreator;

    RuleSetCheckNotification(NotificationController controller, NotificationContentCreator contentCreator) {
        this.controller = controller;
        this.contentCreator = contentCreator;
    }

    public static RuleSetCheckNotification newInstance(NotificationController controller) {
        NotificationContentCreator contentCreator = new NotificationContentCreator(controller.getContext());
        return new RuleSetCheckNotification(controller, contentCreator);
    }

    /**
     * check
     * Will check if any rule set applies to message received
     * if so, the rule set will define whether or not the notification should appear
     * if multiple rule set can apply, only the first one will apply
     * @param account
     * @param senderAddress
     * @param message
     * @return
     */
    public boolean check(Account account, String senderAddress, LocalMessage message) {
       boolean shouldNotify = true;
       if ( account.getNotificationSetting().getNotificationRuleSets().size() > 0) {
            NotificationContent notificationContent = contentCreator.createFromMessage(account, message);

            for  (NotificationRuleSet ruleSet : account.getNotificationSetting().getNotificationRuleSets() ) {
                if (ruleSet.matches(senderAddress,notificationContent) ) {
                    shouldNotify = ruleSet.getShouldNotify();
                    break;
                }
            }
        }

        return shouldNotify;
    }
};