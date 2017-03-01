package com.fsck.k9.helper;


public class HorizontalRuleHelper {

	/*
	pattern checks for dashes, equals, underscores, combination of dashes and equals,
	snips aligned in different directions and replaces them with <hr />

	Usage - 
	String replaced = HorizontalRuleHelper.replaceHorizontalRule(stringToReplaceFrom);
	*/

	private String pattern;

	public HorizontalRuleHelper(){
        pattern = "[-=_]{4,}|(-=){2,}-|(-| )+ *>8 *(-| )+|(-| )+ *8< *(-| )+";
    }

	public String replaceHorizontalRule(String text){
		return text.replaceAll(pattern, "<hr />");
	}
}
