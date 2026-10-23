/*
  listen.js: reads the scripture readings and saints' lives aloud.

  It uses the browser's built-in "Web Speech API" (speechSynthesis), which
  speaks with the reading voices installed on the phone or computer, the same
  voices screen readers use. We only use voices stored ON the device
  (voice.localService is true): some browsers also offer online "natural"
  voices that send the text to a cloud AI service, and those are skipped.

  Long texts are split into sentences and spoken one chunk at a time:
  browsers stop very long single utterances part-way, and it lets Pause and
  Resume pick up where they left off.

  Use:  Listen.play('Galatians 1:11-19', 'The reading from ... Brethren, ...');
  A small player bar appears at the bottom of the page with Pause, Stop,
  speed and voice.
*/
const Listen = (() => {
    const VOICE_KEY = 'orthodoxDaily.voice';
    const RATE_KEY = 'orthodoxDaily.rate';
    const supported = 'speechSynthesis' in window;
    const synth = supported ? window.speechSynthesis : null;

    let chunks = [];      // the text, split into sentence-sized pieces
    let index = 0;        // which piece is being spoken
    let playing = false;
    let paused = false;
    let turn = 0;         // counts spoken pieces, so events from a cancelled piece are ignored
    let player;           // the player bar element

    // ---- Settings remembered in this browser ----
    function load(key, fallback) {
        try { return localStorage.getItem(key) ?? fallback; } catch { return fallback; }
    }
    function save(key, value) {
        try { localStorage.setItem(key, value); } catch { /* storage switched off */ }
    }

    // English voices stored on this device.
    function localVoices() {
        return synth.getVoices().filter(v => v.localService && v.lang.toLowerCase().startsWith('en'));
    }

    function chosenVoice() {
        const voices = localVoices();
        return voices.find(v => v.name === load(VOICE_KEY, ''))
            || voices.find(v => v.default)
            || voices.find(v => v.lang === 'en-US')
            || voices[0];
    }

    // ---- Preparing the text ----
    // Voices may read "St." as "Street", so spell out Saint/Saints.
    function forSpeaking(text) {
        return text
            .replace(/\bSts\.?\s+(?=[A-Z])/g, 'Saints ')
            .replace(/\bSt\.?\s+(?=[A-Z])/g, 'Saint ')
            .replace(/\s+/g, ' ')
            .trim();
    }

    // Split into sentences, then join short ones so each piece is up to ~200 characters.
    function toChunks(text) {
        const sentences = forSpeaking(text).match(/[^.!?;:]+[.!?;:]*["”’)]*\s*/g) || [];
        const pieces = [];
        let current = '';
        for (const sentence of sentences) {
            if ((current + sentence).length > 200 && current) {
                pieces.push(current.trim());
                current = '';
            }
            current += sentence;
        }
        if (current.trim()) pieces.push(current.trim());
        return pieces;
    }

    // ---- Speaking ----
    function speakNext() {
        if (!playing || paused) return;
        if (index >= chunks.length) {
            stop();
            return;
        }
        const utterance = new SpeechSynthesisUtterance(chunks[index]);
        const voice = chosenVoice();
        if (voice) {
            utterance.voice = voice;
            utterance.lang = voice.lang;
        }
        utterance.rate = Number(load(RATE_KEY, '1'));

        // When this piece finishes, move on, unless it was cancelled (Pause,
        // Stop, a new voice), which some browsers also report as "finished".
        const myTurn = ++turn;
        const next = () => {
            if (myTurn === turn && playing && !paused) {
                index++;
                speakNext();
            }
        };
        utterance.onend = next;
        utterance.onerror = next;
        synth.speak(utterance);
    }

    // Cancels whatever is being said, so its events are ignored.
    function silence() {
        turn++;
        if (synth) synth.cancel();
    }

    function play(title, text) {
        if (!supported) {
            alert("Sorry, this browser can't read text aloud.");
            return;
        }
        silence();
        chunks = toChunks(text);
        index = 0;
        playing = true;
        paused = false;
        showPlayer(title);
        if (localVoices().length === 0) {
            player.querySelector('.player-note').hidden = false;
        }
        speakNext();
    }

    // Pausing cancels the current sentence; resuming starts that sentence again.
    // (This works more reliably across browsers than speechSynthesis.pause().)
    function togglePause() {
        paused = !paused;
        silence();
        updateButtons();
        if (!paused) speakNext();
    }

    function stop() {
        playing = false;
        paused = false;
        silence();
        if (player) player.hidden = true;
    }

    // ---- The player bar ----
    function showPlayer(title) {
        if (!player) buildPlayer();
        player.querySelector('.player-title').textContent = title;
        player.querySelector('.player-note').hidden = true;
        player.hidden = false;
        fillVoices();
        updateButtons();
    }

    function updateButtons() {
        const button = player.querySelector('[data-action=pause]');
        button.textContent = paused ? '▶ Resume' : '❚❚ Pause';
    }

    function fillVoices() {
        const select = player.querySelector('select[name=voice]');
        const chosen = chosenVoice();
        select.innerHTML = '';
        for (const voice of localVoices()) {
            const option = document.createElement('option');
            option.value = voice.name;
            option.textContent = voice.name.replace(/^(Microsoft|Google|Apple)\s+/, '');
            option.selected = voice === chosen;
            select.append(option);
        }
        select.closest('label').hidden = select.options.length < 2;
    }

    function buildPlayer() {
        player = document.createElement('div');
        player.className = 'player';
        player.setAttribute('role', 'region');
        player.setAttribute('aria-label', 'Reading aloud');
        player.innerHTML = `
            <div class="player-title"></div>
            <div class="player-controls">
                <button type="button" data-action="pause"></button>
                <button type="button" data-action="stop">■ Stop</button>
                <label>Speed
                    <select name="rate">
                        <option value="0.8">Slower</option>
                        <option value="0.9">Slow</option>
                        <option value="1">Normal</option>
                        <option value="1.15">Fast</option>
                    </select>
                </label>
                <label>Voice <select name="voice"></select></label>
            </div>
            <div class="player-note" hidden>This device doesn't have an English reading voice installed.
                On Windows: Settings › Time &amp; language › Speech › Add voices.
                On iPhone: Settings › Accessibility › Spoken Content › Voices.</div>`;
        document.body.append(player);

        player.querySelector('[data-action=pause]').addEventListener('click', togglePause);
        player.querySelector('[data-action=stop]').addEventListener('click', stop);

        const rate = player.querySelector('select[name=rate]');
        rate.value = load(RATE_KEY, '1');
        rate.addEventListener('change', () => save(RATE_KEY, rate.value)); // used from the next sentence

        player.querySelector('select[name=voice]').addEventListener('change', event => {
            save(VOICE_KEY, event.target.value);
            if (playing && !paused) {   // switch voice from the current sentence
                silence();
                speakNext();
            }
        });
    }

    // Some browsers load their list of voices a moment after the page.
    if (supported) {
        synth.addEventListener('voiceschanged', () => { if (player && !player.hidden) fillVoices(); });
        // Stop talking if the person leaves the page.
        window.addEventListener('pagehide', stop);
    }

    return { play, stop, supported };
})();
