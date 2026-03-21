// Drone8GUI_FMPartials_clean_v25_2.scd
// v25.2:
// - screen-aware startup sizing
// - dynamic top/mid width
// - dynamic spectrum width
// - tighter ratio-morph control layout
// - renamed morph stop button to "Stop morph"
// - vowelRQ slider updated to bandwidth multiplier range (0.5–3.0)
//
// Requires:
// Drone8Synth_vowelSmooth_v3.sc (installed as Drone8Synth.sc or compatible)
// FMRatioPartials.sc

(

s.waitForBoot {

var w, top, mid, scope;
var sb, winW, winH;
var carrierNb, carrierPitchTxt, masterSl;
var vcfSl, vcfNb, vcfRQSl, vcfRQNb;
var driveSl, driveNb;
var ratioSl, ratioNb, modHzNb, modPitchTxt, idxSl, idxNb;
var initBtn, randBtn, stopBtn;
var vowelMixSl, vowelPosSl, vowelRQSl;
var filterModRateSl, filterModRateNb, filterModDepthSl, filterModDepthNb;
var levelSl, levelNb, panSl, freqNb, freqPitchTxt, fmCtSl, fmCtNb, amRtSl, amRtNb, amDpSl, amDpNb, fmRtSl, fmRtNb;
var stripViews;
var applyFMPartials, initFMLevels, syncCarrierRoot, updateModHz, updateFreqReadouts;
var stripW, startX, dark, stripBg, txtCol, amCol, fmCol, panCol, lvlCol, uiFont;
var ratioTargetNb, ratioTimeNb, ratioGoBtn, ratioStopBtn;
var ratioMorphRoutine, stopRatioMorph, setRatioValue, morphRatioTo;

Drone8Synth.addDef(8, \drone8_core);
s.sync;

~dr = Drone8Synth.new(8);
~dr.play(s, \drone8_core, root: 55, master: 0.12);

levelSl = Array.newClear(~dr.num);
levelNb = Array.newClear(~dr.num);
panSl = Array.newClear(~dr.num);
freqNb = Array.newClear(~dr.num);
fmCtSl = Array.newClear(~dr.num);
fmCtNb = Array.newClear(~dr.num);
amRtSl = Array.newClear(~dr.num);
amRtNb = Array.newClear(~dr.num);
amDpSl = Array.newClear(~dr.num);
amDpNb = Array.newClear(~dr.num);
fmRtSl = Array.newClear(~dr.num);
fmRtNb = Array.newClear(~dr.num);
freqPitchTxt = Array.newClear(~dr.num);
stripViews = Array.newClear(~dr.num);

uiFont = Font("Sans", 10);
dark = Color.grey(0.92);
stripBg = Color.grey(0.82);
txtCol = Color.black;
lvlCol = Color.grey(0.93);
panCol = Color.grey(0.80);
amCol = Color.grey(0.80);
fmCol = Color.grey(0.80);

sb = Window.screenBounds;
// Guard against Qt reporting portrait dimensions on a landscape monitor
// (e.g. a disconnected or rotated secondary display)
winW = min(1760, sb.width.max(sb.height) - 40);
winH = min(760,  sb.width.min(sb.height) - 80);

w = Window("Kassia", Rect(20, 20, winW, winH)).front;
w.background_(dark);

w.onClose_({
	if(scope.notNil) { scope.active_(false) };
	if(ratioMorphRoutine.notNil) { ratioMorphRoutine.stop };
	~dr.free;
});

top = CompositeView(w, Rect(0, 0, winW, 250));
top.background_(dark);

mid = CompositeView(w, Rect(0, 250, winW, winH - 269));
mid.background_(dark);

updateModHz = {
	var cf, mr, mhz;
	cf = carrierNb.value.max(0.1);
	mr = ratioNb.value.clip(0.125, 8.0);
	mhz = (cf * mr);
	modHzNb.value = mhz.round(0.001);
	if(modPitchTxt.notNil) { modPitchTxt.string = PitchView.hzToPitchString(mhz, 0.1) };
};

updateFreqReadouts = {
	var cf, ratios, hz;
	cf = carrierNb.value.max(0.1);
	ratios = ~dr.state[\ratios];
	~dr.num.do { |i|
		if(freqNb[i].notNil and: { ratios[i].notNil }) {
			hz = (cf * ratios[i]);
			freqNb[i].value = hz.round(0.001);
			if(freqPitchTxt[i].notNil) { freqPitchTxt[i].string = PitchView.hzToPitchString(hz, 0.1) };
		};
	};
};

syncCarrierRoot = {
	var cf;
	cf = carrierNb.value.max(0.1);
	~dr.node.set(\root, cf);
	if(carrierPitchTxt.notNil) { carrierPitchTxt.string = PitchView.hzToPitchString(cf, 0.1) };
	updateModHz.();
	updateFreqReadouts.();
};

applyFMPartials = {
	var cf, mr, idx, obj, freqs, amps, mx;
	cf = carrierNb.value.max(0.1);
	mr = ratioNb.value.clip(0.125, 8.0);
	idx = idxNb.value.clip(0.0, 10.0);
	updateModHz.();
	obj = FMRatioPartials.new(cf, mr, idx, 200);
	freqs = obj.freqs.asArray.collect(_.asFloat).copyRange(0, 7);
	amps = obj.amps.asArray.collect({ |a| a.asFloat.abs }).copyRange(0, 7);
	if(freqs.notEmpty) {
		~dr.state[\ratios] = freqs.collect({ |f| (f / cf).asFloat });
		~dr.push([\ratios]);
		updateFreqReadouts.();
	};
	if(amps.notEmpty) {
		mx = amps.maxItem.max(1e-12);
		amps = amps / mx;
		~dr.state[\levels] = amps.collect({ |a| (a * 0.14).asFloat });
		~dr.push([\levels]);
		~dr.num.do { |i|
			var lv;
			lv = (~dr.state[\levels][i] / 0.14).clip(0, 1);
			if(levelSl[i].notNil) { levelSl[i].value_(lv) };
			if(levelNb[i].notNil) { levelNb[i].value = ~dr.state[\levels][i].round(0.001) };
		};
	};
};

initFMLevels = {
	var cf, mr, idx, obj, amps, mx;
	cf = carrierNb.value.max(0.1);
	mr = ratioNb.value.clip(0.125, 8.0);
	idx = idxNb.value.clip(0.0, 10.0);
	obj = FMRatioPartials.new(cf, mr, idx, 200);
	amps = obj.amps.asArray.collect({ |a| a.asFloat.abs }).copyRange(0, 7);
	if(amps.notEmpty) {
		mx = amps.maxItem.max(1e-12);
		amps = amps / mx;
		~dr.state[\levels] = amps.collect({ |a| (a * 0.14).asFloat });
		~dr.push([\levels]);
		~dr.num.do { |i|
			var v;
			v = (~dr.state[\levels][i] / 0.14).clip(0, 1);
			if(levelSl[i].notNil) { levelSl[i].value_(v) };
			if(levelNb[i].notNil) { levelNb[i].value = ~dr.state[\levels][i].round(0.001) };
		};
	};
};

stopRatioMorph = {
	if(ratioMorphRoutine.notNil) {
		ratioMorphRoutine.stop;
		ratioMorphRoutine = nil;
	};
};

setRatioValue = { |r|
	r = r.clip(0.125, 8.0);
	ratioNb.value = r.round(0.001);
	ratioSl.value = r.explin(0.125, 8.0, 0, 1);
	applyFMPartials.();
};

morphRatioTo = { |targetRatio, dur = 10.0, updatesPerSecond = 20|
	var startRatio, steps, waitTime, safeStart, safeTarget;
	stopRatioMorph.();
	safeTarget = targetRatio.clip(0.125, 8.0);
	startRatio = ratioNb.value.clip(0.125, 8.0);
	if(dur <= 0.0) {
		setRatioValue.(safeTarget);
		^nil;
	};
	safeStart = startRatio.max(0.000001);
	safeTarget = safeTarget.max(0.000001);
	updatesPerSecond = updatesPerSecond.clip(1, 40);
	steps = (dur * updatesPerSecond).round(1).asInteger.max(2);
	waitTime = dur / (steps - 1);
	ratioMorphRoutine = Routine({
		steps.do { |i|
			var x, r;
			x = i / (steps - 1);
			r = safeStart * ((safeTarget / safeStart) ** x);
			{
				setRatioValue.(r);
			}.defer;
			waitTime.wait;
		};
		{
			setRatioValue.(safeTarget);
		}.defer;
		ratioMorphRoutine = nil;
	}).play(AppClock);
};

// ---------- top control area ----------

StaticText(top, Rect(14, 10, 110, 20)).string_("carrier/root Hz").stringColor_(txtCol).font_(uiFont);
carrierNb = NumberBox(top, Rect(120, 8, 95, 22))
	.value_(55)
	.decimals_(3)
	.step_(0.001)
	.action_({
		syncCarrierRoot.();
		applyFMPartials.();
	});
carrierPitchTxt = StaticText(top, Rect(120, 30, 132, 14)).string_("A3 +0.0c").stringColor_(txtCol).font_(uiFont);

StaticText(top, Rect(235, 10, 60, 20)).string_("master").stringColor_(txtCol).font_(uiFont);
masterSl = Slider(top, Rect(290, 12, 180, 16))
	.value_(0.12 / 0.5)
	.background_(lvlCol)
	.action_({ |sl| ~dr.node.set(\master, sl.value * 0.5); });

StaticText(top, Rect(490, 10, 60, 20)).string_("vcf").stringColor_(txtCol).font_(uiFont);
vcfSl = Slider(top, Rect(525, 12, 220, 16))
	.value_(1200.explin(20, 20000, 0, 1))
	.background_(panCol)
	.action_({ |sl|
		var hz;
		hz = sl.value.linexp(0, 1, 20, 20000);
		vcfNb.value = hz.round(0.001);
		~dr.node.set(\vcfFreq, hz);
	});
vcfNb = NumberBox(top, Rect(750, 8, 82, 22))
	.value_(1200)
	.decimals_(3)
	.step_(0.001)
	.action_({ |nb|
		var hz;
		hz = nb.value.clip(20, 20000);
		vcfSl.value = hz.explin(20, 20000, 0, 1);
		~dr.node.set(\vcfFreq, hz);
	});

StaticText(top, Rect(845, 10, 40, 20)).string_("Q").stringColor_(txtCol).font_(uiFont);
vcfRQSl = Slider(top, Rect(875, 12, 110, 16))
	.value_(0.35.linlin(0.05, 0.95, 0, 1))
	.background_(panCol)
	.action_({ |sl|
		var q;
		q = sl.value.linlin(0, 1, 0.05, 0.95);
		vcfRQNb.value = q.round(0.001);
		~dr.node.set(\vcfRQ, q);
	});
vcfRQNb = NumberBox(top, Rect(990, 8, 68, 22))
	.value_(0.35)
	.decimals_(3)
	.step_(0.001)
	.action_({ |nb|
		var q;
		q = nb.value.clip(0.05, 0.95);
		vcfRQSl.value = q.linlin(0.05, 0.95, 0, 1);
		~dr.node.set(\vcfRQ, q);
	});

randBtn = Button(top, Rect(1075, 8, 130, 24))
	.states_([["Rand phases"]])
	.action_({
		~dr.statePut(\phase, Array.fill(~dr.num, { 1.0.rand }));
		~dr.push([\phase]);
	});

stopBtn = Button(top, Rect(1215, 8, 140, 24))
	.states_([["Stop (fade)"]])
	.action_({
		stopRatioMorph.();
		~dr.free;
	});

// modulation row
StaticText(top, Rect(14, 48, 70, 20)).string_("mod ratio").stringColor_(txtCol).font_(uiFont);
ratioSl = Slider(top, Rect(80, 50, 220, 16))
	.value_(1.0.explin(0.125, 8.0, 0, 1))
	.background_(fmCol)
	.action_({ |sl|
		var r;
		stopRatioMorph.();
		r = sl.value.linexp(0, 1, 0.125, 8.0);
		ratioNb.value = r.round(0.001);
		applyFMPartials.();
	});
ratioNb = NumberBox(top, Rect(305, 46, 82, 22))
	.value_(1.0)
	.decimals_(3)
	.step_(0.0001)
	.action_({ |nb|
		var r;
		stopRatioMorph.();
		r = nb.value.clip(0.125, 8.0);
		ratioSl.value = r.explin(0.125, 8.0, 0, 1);
		applyFMPartials.();
	});

StaticText(top, Rect(405, 48, 60, 20)).string_("mod Hz").stringColor_(txtCol).font_(uiFont);
modHzNb = NumberBox(top, Rect(455, 46, 95, 22))
	.value_(55)
	.decimals_(3)
	.step_(0.0001)
	.enabled_(false);
modPitchTxt = StaticText(top, Rect(455, 68, 132, 14)).string_("A1 +0.0c").stringColor_(txtCol).font_(uiFont);

StaticText(top, Rect(570, 48, 70, 20)).string_("index").stringColor_(txtCol).font_(uiFont);
idxSl = Slider(top, Rect(615, 50, 150, 16))
	.value_(3.0.linlin(0.0, 10.0, 0, 1))
	.background_(fmCol)
	.action_({ |sl|
		var x;
		x = sl.value.linlin(0, 1, 0.0, 10.0);
		idxNb.value = x.round(0.001);
		applyFMPartials.();
	});
idxNb = NumberBox(top, Rect(770, 46, 70, 22))
	.value_(3.0)
	.decimals_(3)
	.step_(0.001)
	.action_({ |nb|
		var x;
		x = nb.value.clip(0.0, 10.0);
		idxSl.value = x.linlin(0.0, 10.0, 0, 1);
		applyFMPartials.();
	});

initBtn = Button(top, Rect(850, 46, 110, 24))
	.states_([["Init levels"]])
	.action_({ initFMLevels.(); });

StaticText(top, Rect(975, 48, 42, 20)).string_("target").stringColor_(txtCol).font_(uiFont);
ratioTargetNb = NumberBox(top, Rect(1018, 46, 72, 22))
	.value_(1.0)
	.decimals_(3)
	.step_(0.0001);

StaticText(top, Rect(1100, 48, 34, 20)).string_("time").stringColor_(txtCol).font_(uiFont);
ratioTimeNb = NumberBox(top, Rect(1134, 46, 66, 22))
	.value_(10.0)
	.decimals_(3)
	.step_(0.001);

StaticText(top, Rect(1204, 48, 14, 20)).string_("s").stringColor_(txtCol).font_(uiFont);
ratioGoBtn = Button(top, Rect(1222, 46, 72, 24))
	.states_([["Morph"]])
	.action_({
		morphRatioTo.(ratioTargetNb.value, ratioTimeNb.value);
	});
ratioStopBtn = Button(top, Rect(1300, 46, 96, 24))
	.states_([["Stop morph"]])
	.action_({
		stopRatioMorph.();
	});

// drive + vowels + filter modulation
StaticText(top, Rect(14, 82, 50, 20)).string_("drive").stringColor_(txtCol).font_(uiFont);
driveSl = Slider(top, Rect(60, 84, 140, 16))
	.value_(1.0.explin(0.25, 8.0, 0, 1))
	.background_(panCol)
	.action_({ |sl|
		var d;
		d = sl.value.linexp(0, 1, 0.25, 8.0);
		driveNb.value = d.round(0.001);
		~dr.node.set(\drive, d);
	});
driveNb = NumberBox(top, Rect(205, 80, 70, 22))
	.value_(1.0)
	.decimals_(3)
	.step_(0.0001)
	.action_({ |nb|
		var d;
		d = nb.value.clip(0.25, 8.0);
		driveSl.value = d.explin(0.25, 8.0, 0, 1);
		~dr.node.set(\drive, d);
	});

StaticText(top, Rect(290, 82, 70, 20)).string_("vMix").stringColor_(txtCol).font_(uiFont);
vowelMixSl = Slider(top, Rect(330, 84, 120, 16))
	.value_(0.35.linexp(0.001, 1.0, 0, 1))
	.background_(amCol)
	.action_({ |sl|
		var x;
		x = sl.value.linexp(0, 1, 0.001, 1.0);
		~dr.node.set(\vowelMix, x);
	});

StaticText(top, Rect(470, 82, 70, 20)).string_("vPos").stringColor_(txtCol).font_(uiFont);
vowelPosSl = Slider(top, Rect(510, 84, 120, 16))
	.value_(1.5 / 4)
	.background_(amCol)
	.action_({ |sl| ~dr.node.set(\vowelPos, sl.value * 4); });

// vRQ is now a bandwidth multiplier: 1.0 = natural, >1 = wider, <1 = tighter
StaticText(top, Rect(650, 82, 70, 20)).string_("vRQ").stringColor_(txtCol).font_(uiFont);
vowelRQSl = Slider(top, Rect(685, 84, 120, 16))
	.value_(1.0.linlin(0.5, 3.0, 0, 1))
	.background_(amCol)
	.action_({ |sl| ~dr.node.set(\vowelRQ, sl.value.linlin(0, 1, 0.5, 3.0)); });

StaticText(top, Rect(825, 82, 54, 20)).string_("fModHz").stringColor_(txtCol).font_(uiFont);
filterModRateSl = Slider(top, Rect(875, 84, 120, 16))
	.value_(0.03.explin(0.0001, 2.0, 0, 1))
	.background_(panCol)
	.action_({ |sl|
		var r;
		r = sl.value.linexp(0, 1, 0.0001, 2.0);
		filterModRateNb.value = r.round(0.001);
		~dr.node.set(\filterModRate, r);
	});
filterModRateNb = NumberBox(top, Rect(1000, 80, 62, 22))
	.value_(0.03)
	.decimals_(3)
	.step_(0.0001)
	.action_({ |nb|
		var r;
		r = nb.value.clip(0.0001, 2.0);
		filterModRateSl.value = r.explin(0.0001, 2.0, 0, 1);
		~dr.node.set(\filterModRate, r);
	});

StaticText(top, Rect(1078, 82, 54, 20)).string_("fModDp").stringColor_(txtCol).font_(uiFont);
filterModDepthSl = Slider(top, Rect(1128, 84, 120, 16))
	.value_(0.0)
	.background_(panCol)
	.action_({ |sl|
		var d;
		d = sl.value.clip(0, 0.99);
		filterModDepthNb.value = d.round(0.001);
		~dr.node.set(\filterModDepth, d);
	});
filterModDepthNb = NumberBox(top, Rect(1253, 80, 62, 22))
	.value_(0.0)
	.decimals_(3)
	.step_(0.001)
	.action_({ |nb|
		var d;
		d = nb.value.clip(0.0, 0.99);
		filterModDepthSl.value = d;
		~dr.node.set(\filterModDepth, d);
	});

// spectrum display
StaticText(top, Rect(14, 116, 120, 20)).string_("spectrum").stringColor_(txtCol).font_(uiFont);
scope = FreqScopeView(top, Rect(14, 136, winW - 28, 88));
scope.active_(true);
scope.background_(Color.black);
scope.freqMode_(1);
scope.inBus_(0);
scope.dbRange_(90);

// ---------- channel strips ----------

stripW = 172;
startX = 8;

~dr.num.do { |i|
	var x0, strip;
	x0 = startX + (i * stripW);
	strip = CompositeView(mid, Rect(x0, 6, stripW - 8, 360));
	strip.background_(stripBg);
	stripViews[i] = strip;

	StaticText(strip, Rect(8, 6, 22, 14)).string_("P" ++ (i + 1)).stringColor_(txtCol).font_(uiFont);
	freqNb[i] = NumberBox(strip, Rect(28, 4, 68, 20))
		.enabled_(false)
		.decimals_(3)
		.value_(0);
	freqPitchTxt[i] = StaticText(strip, Rect(98, 6, 62, 14)).string_("A3").stringColor_(txtCol).font_(uiFont);

	levelSl[i] = Slider(strip, Rect(10, 36, 18, 298))
		.value_(0.0)
		.background_(lvlCol)
		.action_({ |sl|
			var lv;
			lv = sl.value.linlin(0, 1, 0, 0.14);
			~dr.state[\levels][i] = lv;
			if(levelNb[i].notNil) { levelNb[i].value = lv.round(0.001) };
			~dr.push([\levels]);
		});
	levelNb[i] = NumberBox(strip, Rect(34, 36, 64, 20))
		.value_(0.000)
		.decimals_(3)
		.step_(0.001)
		.action_({ |nb|
			var lv;
			lv = nb.value.clip(0.0, 0.14);
			levelSl[i].value = lv.linlin(0, 0.14, 0, 1);
			~dr.state[\levels][i] = lv;
			~dr.push([\levels]);
		});

	StaticText(strip, Rect(34, 58, 60, 14)).string_("pan").stringColor_(txtCol).font_(uiFont);
	panSl[i] = Slider(strip, Rect(34, 72, 118, 16))
		.value_(0.5)
		.background_(panCol)
		.action_({ |sl|
			~dr.state[\pans][i] = sl.value.linlin(0, 1, -1, 1);
			~dr.push([\pans]);
		});

	StaticText(strip, Rect(34, 96, 60, 14)).string_("fmCt").stringColor_(txtCol).font_(uiFont);
	fmCtSl[i] = Slider(strip, Rect(34, 110, 58, 16))
		.value_(0.0)
		.background_(fmCol)
		.action_({ |sl|
			var cents;
			cents = sl.value.linlin(0, 1, 0, 60);
			fmCtNb[i].value = cents.round(0.001);
			~dr.state[\fmDepthCents][i] = cents;
			~dr.push([\fmDepthCents]);
		});
	fmCtNb[i] = NumberBox(strip, Rect(96, 108, 60, 20))
		.value_(0.000)
		.decimals_(3)
		.step_(0.001)
		.action_({ |nb|
			var cents;
			cents = nb.value.clip(0, 60);
			fmCtSl[i].value = cents.linlin(0, 60, 0, 1);
			~dr.state[\fmDepthCents][i] = cents;
			~dr.push([\fmDepthCents]);
		});

	StaticText(strip, Rect(34, 134, 60, 14)).string_("amHz").stringColor_(txtCol).font_(uiFont);
	amRtSl[i] = Slider(strip, Rect(34, 148, 58, 16))
		.value_(0.05.explin(0.0001, 2.0, 0, 1))
		.background_(amCol)
		.action_({ |sl|
			var r;
			r = sl.value.linexp(0, 1, 0.0001, 2.0);
			amRtNb[i].value = r.round(0.001);
			~dr.state[\amRate][i] = r;
			~dr.push([\amRate]);
		});
	amRtNb[i] = NumberBox(strip, Rect(96, 146, 60, 20))
		.value_(0.050)
		.decimals_(3)
		.step_(0.0001)
		.action_({ |nb|
			var r;
			r = nb.value.clip(0.0001, 2.0);
			amRtSl[i].value = r.explin(0.0001, 2.0, 0, 1);
			~dr.state[\amRate][i] = r;
			~dr.push([\amRate]);
		});

	StaticText(strip, Rect(34, 172, 60, 14)).string_("amDp").stringColor_(txtCol).font_(uiFont);
	amDpSl[i] = Slider(strip, Rect(34, 186, 58, 16))
		.value_(0.2)
		.background_(amCol)
		.action_({ |sl|
			var d;
			d = sl.value.clip(0, 1);
			amDpNb[i].value = d.round(0.001);
			~dr.state[\amDepth][i] = d;
			~dr.push([\amDepth]);
		});
	amDpNb[i] = NumberBox(strip, Rect(96, 184, 60, 20))
		.value_(0.200)
		.decimals_(3)
		.step_(0.001)
		.action_({ |nb|
			var d;
			d = nb.value.clip(0, 1);
			amDpSl[i].value = d;
			~dr.state[\amDepth][i] = d;
			~dr.push([\amDepth]);
		});

	StaticText(strip, Rect(34, 210, 60, 14)).string_("fmHz").stringColor_(txtCol).font_(uiFont);
	fmRtSl[i] = Slider(strip, Rect(34, 224, 58, 16))
		.value_(0.03.explin(0.0001, 2.0, 0, 1))
		.background_(fmCol)
		.action_({ |sl|
			var r;
			r = sl.value.linexp(0, 1, 0.0001, 2.0);
			fmRtNb[i].value = r.round(0.001);
			~dr.state[\fmRate][i] = r;
			~dr.push([\fmRate]);
		});
	fmRtNb[i] = NumberBox(strip, Rect(96, 222, 60, 20))
		.value_(0.030)
		.decimals_(3)
		.step_(0.0001)
		.action_({ |nb|
			var r;
			r = nb.value.clip(0.0001, 2.0);
			fmRtSl[i].value = r.explin(0.0001, 2.0, 0, 1);
			~dr.state[\fmRate][i] = r;
			~dr.push([\fmRate]);
		});
};

syncCarrierRoot.();
applyFMPartials.();
~dr.node.set(\filterModRate, 0.03);
~dr.node.set(\filterModDepth, 0.0);

};

)
