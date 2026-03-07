// Drone8GUI_FMPartials_clean_v16.scd

// Requires:
//   Drone8Synth_vowelSmooth_v3.sc (installed as Drone8Synth.sc or compatible)
//   FMRatioPartials.sc

(
s.waitForBoot {

	var w, top, mid, scope;
	var carrierNb, masterSl;
	var vcfSl, vcfNb, vcfRQSl, vcfRQNb;
	var driveSl, driveNb;
	var ratioSl, ratioNb, modHzNb, idxSl, idxNb;
	var initBtn, randBtn, stopBtn;
	var vowelMixSl, vowelPosSl, vowelRQSl;

	var levelSl, levelNb, panSl, freqNb, fmCtSl, fmCtNb, amRtSl, amRtNb, amDpSl, amDpNb, fmRtSl, fmRtNb;
	var stripViews;

	var applyFMPartials, initFMLevels, syncCarrierRoot, updateModHz, updateFreqReadouts;
	var stripW, startX, dark, stripBg, txtCol, amCol, fmCol, panCol, lvlCol;

	Drone8Synth.addDef(8, \drone8_core);
	s.sync;

	~dr = Drone8Synth.new(8);
	~dr.play(s, \drone8_core, root: 55, master: 0.12);

	levelSl = Array.newClear(~dr.num);
	levelNb = Array.newClear(~dr.num);
	panSl   = Array.newClear(~dr.num);
	freqNb  = Array.newClear(~dr.num);
	fmCtSl  = Array.newClear(~dr.num);
	fmCtNb  = Array.newClear(~dr.num);
	amRtSl  = Array.newClear(~dr.num);
	amRtNb  = Array.newClear(~dr.num);
	amDpSl  = Array.newClear(~dr.num);
	amDpNb  = Array.newClear(~dr.num);
	fmRtSl  = Array.newClear(~dr.num);
	fmRtNb  = Array.newClear(~dr.num);
	stripViews = Array.newClear(~dr.num);

	dark = Color.grey(0.92);
	stripBg = Color.grey(0.82);
	txtCol = Color.black;
	lvlCol = Color.grey(0.93);
	panCol = Color.grey(0.80);
	amCol = Color.grey(0.80);
	fmCol = Color.grey(0.80);

	w = Window("Kassia", Rect(80, 80, 1360, 640)).front;
	w.alwaysOnTop_(true);
	w.view.minSize = Size(1350, 620);
	w.background_(dark);
	w.onClose_({
		if(scope.notNil) { scope.active_(false) };
		~dr.free;
	});

	top = CompositeView(w, Rect(0, 0, 1380, 260));
	top.background_(dark);
	mid = CompositeView(w, Rect(0, 260, 1440, 480));
	mid.background_(dark);

	updateModHz = {
		var cf, mr;
		cf = carrierNb.value.max(0.1);
		mr = ratioNb.value.clip(0.125, 8.0);
		modHzNb.value = (cf * mr).round(0.001);
	};

	updateFreqReadouts = {
		var cf, ratios;
		cf = carrierNb.value.max(0.1);
		ratios = ~dr.state[\ratios];
		~dr.num.do { |i|
			if(freqNb[i].notNil and: { ratios[i].notNil }) {
				freqNb[i].value = (cf * ratios[i]).round(0.001);
			};
		};
	};

	syncCarrierRoot = {
		var cf;
		cf = carrierNb.value.max(0.1);
		~dr.node.set(\root, cf);
		updateModHz.();
		updateFreqReadouts.();
	};

	applyFMPartials = {
		var cf, mr, idx, obj, freqs, amps, mx;

		cf  = carrierNb.value.max(0.1);
		mr  = ratioNb.value.clip(0.125, 8.0);
		idx = idxNb.value.clip(0.0, 10.0);

		updateModHz.();

		obj = FMRatioPartials.new(cf, mr, idx, 200);
		freqs = obj.freqs.asArray.collect(_.asFloat).copyRange(0, 7);
		amps  = obj.amps.asArray.collect({ |a| a.asFloat.abs }).copyRange(0, 7);

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

		cf  = carrierNb.value.max(0.1);
		mr  = ratioNb.value.clip(0.125, 8.0);
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

	// ---------- top control area ----------
	StaticText(top, Rect(14, 10, 110, 20)).string_("carrier/root Hz").stringColor_(txtCol);
	carrierNb = NumberBox(top, Rect(120, 8, 95, 22))
		.value_(55)
		.decimals_(3)
		.step_(0.001)
		.action_({
			syncCarrierRoot.();
			applyFMPartials.();
		});

	StaticText(top, Rect(235, 10, 60, 20)).string_("master").stringColor_(txtCol);
	masterSl = Slider(top, Rect(290, 12, 180, 16))
		.value_(0.12 / 0.5)
		.background_(lvlCol)
		.action_({ |sl| ~dr.node.set(\master, sl.value * 0.5); });

	StaticText(top, Rect(490, 10, 60, 20)).string_("vcf").stringColor_(txtCol);
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

	StaticText(top, Rect(845, 10, 40, 20)).string_("Q").stringColor_(txtCol);
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

	stopBtn = Button(top, Rect(1215, 8, 130, 24))
		.states_([["Stop (fade)"]])
		.action_({ ~dr.free; });

	// modulation row
	StaticText(top, Rect(14, 48, 70, 20)).string_("mod ratio").stringColor_(txtCol);
	ratioSl = Slider(top, Rect(80, 50, 220, 16))
		.value_(1.0.explin(0.125, 8.0, 0, 1))
		.background_(fmCol)
		.action_({ |sl|
			var r;
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
			r = nb.value.clip(0.125, 8.0);
			ratioSl.value = r.explin(0.125, 8.0, 0, 1);
			applyFMPartials.();
		});

	StaticText(top, Rect(405, 48, 60, 20)).string_("mod Hz").stringColor_(txtCol);
	modHzNb = NumberBox(top, Rect(455, 46, 95, 22))
		.value_(55)
		.decimals_(3)
		.step_(0.0001)
		.enabled_(false);

	StaticText(top, Rect(570, 48, 70, 20)).string_("index").stringColor_(txtCol);
	idxSl = Slider(top, Rect(615, 50, 220, 16))
		.value_(3.0.linlin(0.0, 10.0, 0, 1))
		.background_(fmCol)
		.action_({ |sl|
			var x;
			x = sl.value.linlin(0, 1, 0.0, 10.0);
			idxNb.value = x.round(0.001);
			applyFMPartials.();
		});
	idxNb = NumberBox(top, Rect(840, 46, 82, 22))
		.value_(3.0)
		.decimals_(3)
		.step_(0.001)
		.action_({ |nb|
			var x;
			x = nb.value.clip(0.0, 10.0);
			idxSl.value = x.linlin(0.0, 10.0, 0, 1);
			applyFMPartials.();
		});

	initBtn = Button(top, Rect(940, 46, 130, 24))
		.states_([["Init levels"]])
		.action_({ initFMLevels.(); });

	// drive + vowels
	StaticText(top, Rect(14, 86, 50, 20)).string_("drive").stringColor_(txtCol);
	driveSl = Slider(top, Rect(60, 88, 220, 16))
		.value_(1.0.explin(0.25, 8.0, 0, 1))
		.background_(panCol)
		.action_({ |sl|
			var d;
			d = sl.value.linexp(0, 1, 0.25, 8.0);
			driveNb.value = d.round(0.001);
			~dr.node.set(\drive, d);
		});
	driveNb = NumberBox(top, Rect(285, 84, 82, 22))
		.value_(1.0)
		.decimals_(3)
		.step_(0.0001)
		.action_({ |nb|
			var d;
			d = nb.value.clip(0.25, 8.0);
			driveSl.value = d.explin(0.25, 8.0, 0, 1);
			~dr.node.set(\drive, d);
		});

	StaticText(top, Rect(390, 86, 70, 20)).string_("vMix").stringColor_(txtCol);
	vowelMixSl = Slider(top, Rect(430, 88, 170, 16))
		.value_(0.35.linexp(0.001, 1.0, 0, 1))
		.background_(amCol)
		.action_({ |sl|
			var x;
			x = sl.value.linexp(0, 1, 0.001, 1.0);
			~dr.node.set(\vowelMix, x);
		});

	StaticText(top, Rect(620, 86, 70, 20)).string_("vPos").stringColor_(txtCol);
	vowelPosSl = Slider(top, Rect(660, 88, 170, 16))
		.value_(1.5 / 4)
		.background_(amCol)
		.action_({ |sl| ~dr.node.set(\vowelPos, sl.value * 4); });

	StaticText(top, Rect(850, 86, 70, 20)).string_("vRQ").stringColor_(txtCol);
	vowelRQSl = Slider(top, Rect(885, 88, 170, 16))
		.value_(0.22.linlin(0.05, 0.45, 0, 1))
		.background_(amCol)
		.action_({ |sl| ~dr.node.set(\vowelRQ, sl.value.linlin(0, 1, 0.05, 0.45)); });

	// spectrum display
	StaticText(top, Rect(14, 126, 120, 20)).string_("spectrum").stringColor_(txtCol);
	scope = FreqScopeView(top, Rect(14, 148, 1332, 96));
	scope.active_(true);
	scope.background_(Color.black);
	scope.freqMode_(1); // log-ish frequency axis
	scope.inBus_(0);
	scope.dbRange_(90);

	// ---------- channel strips ----------
	stripW = 168;
	startX = 12;

	~dr.num.do { |i|
		var x0, strip;
		x0 = startX + (i * stripW);

		strip = CompositeView(mid, Rect(x0, 8, stripW - 8, 448));
		strip.background_(stripBg);
		stripViews[i] = strip;

		StaticText(strip, Rect(8, 6, 28, 16)).string_("P" ++ (i + 1)).stringColor_(txtCol);
		freqNb[i] = NumberBox(strip, Rect(34, 4, 118, 22))
			.enabled_(false)
			.decimals_(3)
			.value_(0);

		levelSl[i] = Slider(strip, Rect(10, 34, 18, 322))
			.value_(0.0)
			.background_(lvlCol)
			.action_({ |sl|
				var lv;
				lv = sl.value.linlin(0, 1, 0, 0.14);
				~dr.state[\levels][i] = lv;
				if(levelNb[i].notNil) { levelNb[i].value = lv.round(0.001) };
				~dr.push([\levels]);
			});

		levelNb[i] = NumberBox(strip, Rect(34, 34, 64, 22))
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

		StaticText(strip, Rect(34, 60, 60, 14)).string_("pan").stringColor_(txtCol);
		panSl[i] = Slider(strip, Rect(34, 76, 118, 16))
			.value_(0.5)
			.background_(panCol)
			.action_({ |sl|
				~dr.state[\pans][i] = sl.value.linlin(0, 1, -1, 1);
				~dr.push([\pans]);
			});

		StaticText(strip, Rect(34, 102, 60, 14)).string_("fmCt").stringColor_(txtCol);
		fmCtSl[i] = Slider(strip, Rect(34, 118, 58, 16))
			.value_(0.0)
			.background_(fmCol)
			.action_({ |sl|
				var cents;
				cents = sl.value.linlin(0, 1, 0, 60);
				fmCtNb[i].value = cents.round(0.001);
				~dr.state[\fmDepthCents][i] = cents;
				~dr.push([\fmDepthCents]);
			});
		fmCtNb[i] = NumberBox(strip, Rect(96, 116, 60, 22))
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

		StaticText(strip, Rect(34, 146, 60, 14)).string_("amHz").stringColor_(txtCol);
		amRtSl[i] = Slider(strip, Rect(34, 162, 58, 16))
			.value_(0.05.explin(0.0001, 2.0, 0, 1))
			.background_(amCol)
			.action_({ |sl|
				var r;
				r = sl.value.linexp(0, 1, 0.0001, 2.0);
				amRtNb[i].value = r.round(0.001);
				~dr.state[\amRate][i] = r;
				~dr.push([\amRate]);
			});
		amRtNb[i] = NumberBox(strip, Rect(96, 160, 60, 22))
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

		StaticText(strip, Rect(34, 190, 60, 14)).string_("amDp").stringColor_(txtCol);
		amDpSl[i] = Slider(strip, Rect(34, 206, 58, 16))
			.value_(0.2)
			.background_(amCol)
			.action_({ |sl|
				var d;
				d = sl.value.clip(0, 1);
				amDpNb[i].value = d.round(0.001);
				~dr.state[\amDepth][i] = d;
				~dr.push([\amDepth]);
			});
		amDpNb[i] = NumberBox(strip, Rect(96, 204, 60, 22))
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

		StaticText(strip, Rect(34, 234, 60, 14)).string_("fmHz").stringColor_(txtCol);
		fmRtSl[i] = Slider(strip, Rect(34, 250, 58, 16))
			.value_(0.03.explin(0.0001, 2.0, 0, 1))
			.background_(fmCol)
			.action_({ |sl|
				var r;
				r = sl.value.linexp(0, 1, 0.0001, 2.0);
				fmRtNb[i].value = r.round(0.001);
				~dr.state[\fmRate][i] = r;
				~dr.push([\fmRate]);
			});
		fmRtNb[i] = NumberBox(strip, Rect(96, 248, 60, 22))
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

};
)