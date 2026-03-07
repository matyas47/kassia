// FormantVowel.sc
// Revised for more audible but still musical vowel/body filtering.
// Changes:
// - Stronger band gains
// - Slight makeup gain
// - Intended to be placed AFTER drive in Drone8Synth.sc

FormantVowel {
	*process { |in, rootHz=110, mix=0.35, pos=1.5, rq=0.22, scaleRef=110|
		var vPos, vMix, vRQ, vScale, f1, f2, f3, f4, formant;

		vPos = pos.clip(0, 4);
		vMix = mix.clip(0, 1);
		vRQ = rq.clip(0.05, 0.45);
		vScale = (rootHz / scaleRef).clip(0.25, 4.0);

		f1 = SelectX.kr(vPos, [800, 400, 300, 450, 325]) * vScale;
		f2 = SelectX.kr(vPos, [1150, 1700, 2200, 800, 700]) * vScale;
		f3 = SelectX.kr(vPos, [2900, 2600, 3000, 2830, 2530]) * vScale;
		f4 = SelectX.kr(vPos, [3900, 3200, 3600, 3800, 3500]) * vScale;

		f1 = f1.clip(20, 20000);
		f2 = f2.clip(20, 20000);
		f3 = f3.clip(20, 20000);
		f4 = f4.clip(20, 20000);

		formant =
			(BPF.ar(in, f1, vRQ) * 1.00) +
			(BPF.ar(in, f2, vRQ) * 0.85) +
			(BPF.ar(in, f3, vRQ) * 0.65) +
			(BPF.ar(in, f4, vRQ) * 0.50);

		// Slight makeup gain so the wet branch is actually audible at moderate mix
		formant = formant * 1.35;

		^XFade2.ar(in, formant, vMix.linlin(0, 1, -1, 1));
	}
}
