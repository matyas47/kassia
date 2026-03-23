// KassiaController.sc
// Controller layer for Kassia.
// Owns the spectral model, render engine, morph routine,
// and listener/callback system.
//
// The view registers callbacks here and calls controller methods
// in response to user input. The controller never touches the view directly.
//
// Requires: KassiaSpectralModel.sc, KassiaSynth.sc

KassiaController {

	var <model;       // KassiaSpectralModel
	var <synth;       // KassiaSynth
	var listeners;    // Dictionary of Arrays of callbacks
	var morphRoutine; // active ratio morph routine, or nil

	*new { |model, synth|
		^super.new.init(model, synth)
	}

	init { |m, s|
		model     = m;
		synth     = s;
		listeners = Dictionary.new;
		^this
	}

	// ------------------------------------------------------------------
	// Listener system
	// ------------------------------------------------------------------

	addListener { |key, func|
		listeners[key] = listeners[key].add(func);
	}

	removeListener { |key, func|
		if(func.isNil) {
			// Remove all listeners for this key
			listeners.removeAt(key);
		} {
			// Remove specific func only
			listeners[key] = listeners[key].select({ |f| f != func });
		};
	}

	notify { |key ...args|
		listeners[key].do({ |f| f.valueArray(args) });
	}

	// ------------------------------------------------------------------
	// Spectral model parameters
	// These update the model, recompute partials, push to synth,
	// and notify listeners.
	// ------------------------------------------------------------------

	setCarrier { |hz|
		model.setCarrier(hz);
		synth.set(\root, model.carrier);
		this.prPushPartials;
		this.notify(\carrier,  model.carrier);
		this.notify(\modHz,    model.modHz);
		this.notify(\partials, model.absFreqs, model.amps);
	}

	setRatio { |r|
		model.setRatio(r);
		this.prPushPartials;
		this.notify(\ratio,    model.ratio);
		this.notify(\modHz,    model.modHz);
		this.notify(\partials, model.absFreqs, model.amps);
	}

	setIndex { |i|
		model.setIndex(i);
		this.prPushPartials;
		this.notify(\index,    model.index);
		this.notify(\partials, model.absFreqs, model.amps);
	}

	// Recompute and push partials without changing parameters.
	// Useful for init and after preset load.
	refreshPartials {
		this.prPushPartials;
		this.notify(\carrier,  model.carrier);
		this.notify(\ratio,    model.ratio);
		this.notify(\index,    model.index);
		this.notify(\modHz,    model.modHz);
		this.notify(\partials, model.absFreqs, model.amps);
	}

	// Reinitialise level sliders from FM amplitudes only
	// (ratios unchanged — mirrors old "Init levels" button)
	initLevels {
		this.prPushPartials;
		this.notify(\partials, model.absFreqs, model.amps);
	}

	// ------------------------------------------------------------------
	// Synth parameters
	// These pass through to the synth and notify listeners.
	// ------------------------------------------------------------------

	set { |key, val|
		synth.set(key, val);
		this.notify(key, val);
	}

	// Per-partial array param (pans, amRate, amDepth, fmRate, fmDepthCents)
	setPartialParam { |key, values|
		synth.setPartialParam(key, values);
		this.notify(key, values);
	}

	// Single value within a per-partial array
	setPartialParamAt { |key, index, val|
		synth.setPartialParamAt(key, index, val);
		this.notify(key, synth.getState(key));
	}

	// Randomise partial phases
	randomisePhases {
		var phases;
		phases = Array.fill(synth.num, { 1.0.rand });
		synth.setPartialParam(\phase, phases);
		this.notify(\phase, phases);
	}

	// ------------------------------------------------------------------
	// Ratio morphing
	// ------------------------------------------------------------------

	morphRatioTo { |targetRatio, dur=10.0, updatesPerSec=20|
		var startRatio, safeStart, safeTarget, steps, waitTime;

		this.stopMorph;

		safeTarget  = targetRatio.clip(0.125, 8.0);
		startRatio  = model.ratio.clip(0.125, 8.0);

		if(dur <= 0.0) {
			this.setRatio(safeTarget);
			^this
		};

		safeStart       = startRatio.max(0.000001);
		safeTarget      = safeTarget.max(0.000001);
		updatesPerSec   = updatesPerSec.clip(1, 40);
		steps           = (dur * updatesPerSec).round(1).asInteger.max(2);
		waitTime        = dur / (steps - 1);

		morphRoutine = Routine({
			steps.do { |i|
				var x, r;
				x = i / (steps - 1);
				r = safeStart * ((safeTarget / safeStart) ** x);
				{ this.setRatio(r) }.defer;
				waitTime.wait;
			};
			{ this.setRatio(safeTarget) }.defer;
			morphRoutine = nil;
		}).play(AppClock);
	}

	stopMorph {
		if(morphRoutine.notNil) {
			morphRoutine.stop;
			morphRoutine = nil;
		};
	}

	isMorphing { ^morphRoutine.notNil }

	// ------------------------------------------------------------------
	// Playback
	// ------------------------------------------------------------------

	play { |server, root=55, master=0.12|
		synth.play(server, root, master);
		this.refreshPartials;
	}

	free {
		this.stopMorph;
		synth.free;
	}

	// ------------------------------------------------------------------
	// Private
	// ------------------------------------------------------------------

	prPushPartials {
		synth.setPartials(model.freqs, model.amps);
	}

}
