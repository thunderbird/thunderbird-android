package com.fsck.k9.helper;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HorizontalRuleHelperTest {

	@Test
	public void replaceHorizontalRule_withDashes_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("--------------");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withEquals_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("==============");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withFancyDashEquals_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("-=-=-=-=-=-=-=-");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withUnderscores_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("_______________");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withScissorsLeft_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("- - - >8 - - -");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withScissorsLeft2_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("--- >8 ---");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withScissorsRight_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("- - - 8< - - -");

		assertEquals("<hr />", result);
	}

	@Test
	public void replaceHorizontalRule_withScissorsRight2_shouldReturnReplacementWithHR() {
		String result = HorizontalRuleHelper.replaceHorizontalRule("--- 8< ---");

		assertEquals("<hr />", result);
	}

}
