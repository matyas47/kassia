// Drone8Synth_vowelSmooth.sc
// Simplified vowel smoothing:
// - Keeps server-side smoothing for vowel controls
// - Removes exposed lag params from the public control set
// - Signal flow: partials -> drive -> formant -> MoogFF

Drone8Synth {

	classvar <>defName = \drone8_core;

	var <>node, <>num, <>state;

	*new { |num=8| ^super.new.init(num) }

	init { |n|
		num = n.asInteger.max(1);
		state = (
			ratios:        (1..num),
			detuneCents:   Array.fill(num, 0),
			levels:        Array.fill(num, 0.0),
			pans:          Array.fill(num, 0.0),
			phase:         Array.fill(num, 0.0),
			amRate:        Array.fill(num, 0.05),
			amDepth:       Array.fill(num, 0.2),
			fmRate:        Array.fill(num, 0.03),
			fmDepthCents:  Array.fill(num, 0.0)
		);
		^this
	}

	*addDef { |num=8, defName=\drone8_core|
		var n;
		n = num.asInteger.max(1);

		SynthDef(defName, { |out=0, gate=1,
			root=55, master=0.15,
			vcfFreq=1200, vcfRQ=0.35,
			vcfModRate=0.07, vcfModDepth=0.25,
			partLPF=6000, partLPFRQ=0.5,
			vowelMix=0.35, vowelPos=1.5, vowelRQ=0.22,
			// smoothing for manual moves
			masterLag=0.5, vcfLag=1.5, vcfRQLag=1.0, driveLag=1.0,
			partLevelLag=2.0, panLag=1.0, fmDepthLag=2.0, amRateLag=2.0, amDepthLag=2.0, fmRateLag=2.0,
			drive=1.0, drivePost=1.0
			|

			var ratios, detuneCents, levels, pans, phase, amRate, amDepth, fmRate, fmDepthCents;
			var env, vcfLFO, vcfCut, baseF, fm, f, ph, osc, am, per, sig;
			var vmix, vpos, vrq, mstr, vcfFreqSm, vcfRQSm, driveSm, levelsSm, pansSm, fmDepthSm, amRateSm, amDepthSm, fmRateSm;

			ratios        = NamedControl.kr(\ratios,       (1..n));
			detuneCents   = NamedControl.kr(\detuneCents,  Array.fill(n, 0));
			levels        = NamedControl.kr(\levels,       Array.fill(n, 0.0));
			pans          = NamedControl.kr(\pans,         Array.fill(n, 0.0));
			phase         = NamedControl.kr(\phase,        Array.fill(n, 0.0));
			amRate        = NamedControl.kr(\amRate,       Array.fill(n, 0.05));
			amDepth       = NamedControl.kr(\amDepth,      Array.fill(n, 0.2));
			fmRate        = NamedControl.kr(\fmRate,       Array.fill(n, 0.03));
			fmDepthCents  = NamedControl.kr(\fmDepthCents, Array.fill(n, 0.0));

			env = EnvGen.kr(Env.asr(2.0, 1.0, 3.0), gate, doneAction: 2);

			mstr      = Lag.kr(master,                    masterLag.max(0.001));
			vcfFreqSm = Lag.kr(vcfFreq.clip(20, 20000),  vcfLag.max(0.001));
			vcfRQSm   = Lag.kr(vcfRQ.clip(0.05, 0.95),   vcfRQLag.max(0.001));
			driveSm   = Lag.kr(drive.clip(0.25, 8.0),    driveLag.max(0.001));
			levelsSm  = Lag.kr(levels,                    partLevelLag.max(0.001));
			pansSm    = Lag.kr(pans,                      panLag.max(0.001));
			fmDepthSm = Lag.kr(fmDepthCents,              fmDepthLag.max(0.001));
			amRateSm  = Lag.kr(amRate.clip(0.0001, 2.0), amRateLag.max(0.001));
			amDepthSm = Lag.kr(amDepth.clip(0.0, 1.0),   amDepthLag.max(0.001));
			fmRateSm  = Lag.kr(fmRate.clip(0.0001, 2.0), fmRateLag.max(0.001));

			vcfLFO = SinOsc.kr(vcfModRate).range(-1, 1);
			vcfCut = (vcfFreqSm * (vcfLFO * vcfModDepth + 1)).clip(20, 20000);

			baseF = (root * ratios * (detuneCents/100).midiratio).clip(0.1, 20000);
			fm    = SinOsc.kr(fmRateSm.max(0.000001)).bipolar(fmDepthSm/100).midiratio;
			f     = (baseF * fm).clip(0.1, 20000);

			ph  = phase.wrap(0, 1);
			osc = VarSaw.ar(f, iphase: ph, width: 0.5);
			osc = RLPF.ar(osc, partLPF.clip(50, 20000), partLPFRQ.clip(0.05, 1.0));

			am  = SinOsc.kr(amRateSm.max(0.000001)).range(1 - amDepthSm, 1).clip(0, 1);
			per = Pan2.ar(osc * am * levelsSm, pansSm);
			sig = Mix(per);

			sig = (sig * driveSm).tanh * drivePost;

			vmix = Lag.kr(vowelMix.clip(0, 1), 2.5);
			vpos = Lag.kr(vowelPos.clip(0, 4), 3.5);
			vrq  = Lag.kr(vowelRQ.clip(0.05, 0.45), 1.5);

			sig = FormantVowel.process(
				sig,
				rootHz:   root,
				mix:      vmix,
				pos:      vpos,
				rq:       vrq,
				scaleRef: 110
			);

			// FIX: use vcfRQSm (smoothed) instead of raw vcfRQ for resonance
			sig = MoogFF.ar(sig, vcfCut, vcfRQSm.linlin(0, 1, 0.0, 4.0));

			sig = LeakDC.ar(sig);
			sig = sig * mstr * env;
			sig = Limiter.ar(sig, 0.95, 0.01);

			Out.ar(out, sig);
		}).add;
	}

	play { |server, defName=\drone8_core, root=55, master=0.12|
		server = server ? Server.default;
		node = Synth(defName, [\root, root, \master, 0.0], server);
		this.pushAll;
		SystemClock.sched(0.05, { node.set(\master, master); nil });
		^node
	}

	free {
		// FIX: capture node reference locally before clearing it, so the
		// envelope release completes cleanly without a race condition if
		// anything else tries to call node.set() during the fade.
		var n = node;
		node = nil;
		if(n.notNil) {
			n.set(\master, 0.0);
			n.set(\gate, 0);
		};
	}

	push { |keys|
		keys.do { |k|
			var v;
			v = state[k];
			if(node.notNil) {
				if(v.isArray) { node.setn(k, v) } { node.set(k, v) };
			};
		};
	}

	pushAll {
		if(node.notNil) {
			node.setn(\ratios,       state[\ratios]);
			node.setn(\detuneCents,  state[\detuneCents]);
			node.setn(\levels,       state[\levels]);
			node.setn(\pans,         state[\pans]);
			node.setn(\phase,        state[\phase]);
			node.setn(\amRate,       state[\amRate]);
			node.setn(\amDepth,      state[\amDepth]);
			node.setn(\fmRate,       state[\fmRate]);
			node.setn(\fmDepthCents, state[\fmDepthCents]);
		};
	}

	stateAt  { |key|      ^state[key]    }
	statePut { |key, val| state[key] = val; ^val }

}
