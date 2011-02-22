package org.miscwidgets.interpolator;

/*
 *
 * Open source under the BSD License.
 *
 * Copyright © 2001 Robert Penner
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * Neither the name of the author nor the names of contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.miscwidgets.interpolator.EasingType.Type;

import android.view.animation.Interpolator;


public class BounceInterpolator implements Interpolator {

	private Type type;

	public BounceInterpolator(Type type) {
		this.type = type;
	}

	public float getInterpolation(float t) {
		if (type == Type.IN) {
			return in(t);
		} else
		if (type == Type.OUT) {
			return out(t);
		} else
		if (type == Type.INOUT) {
			return inout(t);
		}
		return 0;
	}

	private float out(float t) {
		if (t < (1/2.75)) {
			return 7.5625f*t*t;
		} else
		if (t < 2/2.75) {
			return 7.5625f*(t-=(1.5/2.75))*t + .75f;
		} else
		if (t < 2.5/2.75) {
			return 7.5625f*(t-=(2.25/2.75))*t + .9375f;
		} else {
			return 7.5625f*(t-=(2.625/2.75))*t + .984375f;
		}
	}

	private float in(float t) {
		return 1 - out(1-t);
	}

	private float inout(float t) {
		if (t < 0.5f) {
			return in(t*2) * .5f;
		} else {
			return out(t*2-1) * .5f + .5f;
		}
	}
}
