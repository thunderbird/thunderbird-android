package com.fsck.k9.helper;

/**
 * HorizontalRuleHelper
 * author - Sayan Goswami (goswami.sayan47@gmail.com)
 * date - 02/28/2017
 * issue - https://github.com/k9mail/k-9/issues/2259
 */

public class HorizontalRuleHelper {

	/*
	pattern checks for dashes, equals, underscores, combination of dashes and equals,
	snips aligned in different directions and replaces them with <hr />

	Usage - 
	String replaced = HorizontalRuleHelper.replaceHorizontalRule(stringToReplaceFrom);
	*/

	private String pattern = "[-=_]{4,}|(-|=){2,}-|(-| )+ *>8 *(-| )+|(-| )+ *8< *(-| )+";

	public void HorizontalRuleHelper(){
		// Empty constructor
	}

	public String replaceHorizontalRule(String text){
		return text.replaceAll(pattern, "<hr />");
	}
}