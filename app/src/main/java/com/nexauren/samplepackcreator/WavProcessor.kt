package com.nexauren.samplepackcreator

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/** Offline 16-bit PCM processor. Each enabled effect has its own 0..100 intensity. */
object WavProcessor {
    private data class WavInfo(val channels: Int, val sampleRate: Int, val dataOffset: Int, val dataLength: Int)

    fun effectName(index: Int): String {
        val name = EffectCatalog.all[index.mod(EffectCatalog.all.size)].name.replace(Regex("[^A-Za-z0-9]+"), "")
        return if (index >= EffectCatalog.all.size) name + "_Alt" + (index / EffectCatalog.all.size + 1) else name
    }

    fun process(input: ByteArray, variationIndex: Int, effects: List<EffectSetting> = emptyList()): ByteArray? {
        val wav = parse(input) ?: return null
        val samples = FloatArray(wav.dataLength / 2)
        var cursor = wav.dataOffset
        for (i in samples.indices) { samples[i] = readShort(input, cursor) / 32768f; cursor += 2 }
        val frames = samples.size / wav.channels
        val active = effects.filter { it.enabled && it.intensity > 0f }
        if (active.isEmpty()) {
            val def = EffectCatalog.all[variationIndex.mod(EffectCatalog.all.size)]
            applyEffect(samples, wav.channels, wav.sampleRate, def.id, 38f + (variationIndex % 5) * 9f, variationIndex)
        } else {
            active.forEachIndexed { effectIndex, setting ->
                // Keep the selected effect chain, but nudge the strength per
                // render so a collection made from one source is not a stack
                // of byte-identical clones.
                val offset = (((variationIndex * 7 + effectIndex * 3) % 9) - 4) * 2.0f
                val variedIntensity = (setting.intensity + offset).coerceIn(0f, 100f)
                applyEffect(samples, wav.channels, wav.sampleRate, setting.id, variedIntensity, variationIndex)
            }
        }
        // Gentle deterministic variation prevents byte-identical repeated renders.
        val variationGain = 0.965f + (variationIndex % 29) * 0.0024f
        val seed = variationIndex + 1
        for (frame in 0 until frames) {
            val ripple = 1f + sin(frame * (0.00071f + seed * 0.000013f)) * (0.0008f + (seed % 7) * 0.0001f)
            for (channel in 0 until wav.channels) {
                val i = frame * wav.channels + channel
                samples[i] = (samples[i] * variationGain * ripple).coerceIn(-0.999f,0.999f)
            }
        }
        val output = input.copyOf()
        cursor = wav.dataOffset
        for (sample in samples) { writeShort(output,cursor,(sample.coerceIn(-1f,1f)*32767f).toInt()); cursor += 2 }
        return output
    }

    private fun applyEffect(x: FloatArray, channels: Int, rate: Int, id: String, amount: Float, seed: Int) {
        if (x.isEmpty()) return
        val mix = (amount / 100f).coerceIn(0f,1f)
        val frames = x.size / channels
        val original = x.copyOf()
        val speed = 0.15f + mix * 3.1f
        val delayFrames = max(1,(rate * (0.025f + mix * 0.28f)).toInt())
        when (id) {
            "reverse","reverse_gate","reverse_swell" -> {
                for (frame in 0 until frames) for (ch in 0 until channels) {
                    val i=frame*channels+ch
                    val reversed=original[(frames-frame-1)*channels+ch]
                    val gate=when(id) { "reverse_gate" -> if ((frame/max(1,rate/12))%2==0) 1f else 0.22f; "reverse_swell" -> frame.toFloat()/frames; else -> 1f }
                    x[i]=original[i]*(1f-mix)+reversed*gate*mix
                }
            }
            "reverb_room","reverb_hall","ambience","shimmer" -> {
                val taps=if(id=="reverb_room") intArrayOf(29,43,61) else if(id=="ambience") intArrayOf(47,73) else intArrayOf(71,113,157,211)
                val feedback=if(id=="reverb_hall") 0.62f else if(id=="shimmer") 0.42f else 0.34f
                taps.forEachIndexed { index,tap ->
                    val d=max(1,rate*tap/1000)
                    for(frame in d until frames) for(ch in 0 until channels) {
                        val i=frame*channels+ch
                        x[i]=(x[i]+x[(frame-d)*channels+ch]*(feedback/(index+1))*mix*0.55f).coerceIn(-1f,1f)
                    }
                }
                if(id=="shimmer") addPitchLayer(x,original,channels,mix*0.32f,2f)
            }
            "echo","delay","ping_pong","slapback","tape_echo","dub_delay","feedback_delay" -> {
                val d=when(id) { "slapback"->max(1,rate/13); "tape_echo"->max(1,rate/4); "dub_delay"->max(1,rate*3/10); else->delayFrames }
                val repeats=if(id=="slapback") 1 else if(id=="dub_delay") 5 else 3
                for(frame in 0 until frames) for(ch in 0 until channels) {
                    var delayed=0f
                    for(repeat in 1..repeats) {
                        val from=frame-d*repeat
                        if(from>=0) {
                            val sourceCh=if(id=="ping_pong"&&channels>1) 1-ch else ch
                            delayed += original[from*channels+sourceCh] * (0.55f.pow(repeat))
                        }
                    }
                    val i=frame*channels+ch
                    val coloured=if(id=="tape_echo") tanh(delayed*1.7f) else delayed
                    val feedback=if(id=="feedback_delay"||id=="dub_delay") (0.24f+mix*0.52f) else 0.42f
                    x[i]=(original[i]*(1f-mix*0.18f)+coloured*feedback*mix).coerceIn(-1f,1f)
                }
            }
            "stutter","repeater","glitch","scratch","granular" -> {
                val block=when(id) { "stutter"->max(32,rate/12); "repeater"->max(64,rate/4); "glitch"->max(24,rate/45); "scratch"->max(48,rate/9); else->max(32,rate/30) }
                for(frame in 0 until frames) {
                    val local=frame%block
                    val sourceFrame=when(id) {
                        "scratch" -> (frame+(sin(frame*0.0007f)*block*mix).toInt()).coerceIn(0,frames-1)
                        "granular" -> (frame-local+(seed%4)*block/4+local).coerceIn(0,frames-1)
                        else -> (frame-local+local%max(1,block/(1+(seed%3)))).coerceIn(0,frames-1)
                    }
                    val gate=if(id=="glitch"&&(frame/block)%3==1) 0.25f else 1f
                    for(ch in 0 until channels) { val i=frame*channels+ch; x[i]=original[i]*(1f-mix)+original[sourceFrame*channels+ch]*mix*gate }
                }
            }
            "tremolo","pan_motion","wow_flutter" -> {
                for(frame in 0 until frames) {
                    val t=frame.toFloat()/rate
                    val lfo=sin(2f*PI.toFloat()*t*(if(id=="tremolo") 4f+speed*2f else 0.35f+speed*0.2f)+seed)
                    for(ch in 0 until channels) {
                        val i=frame*channels+ch
                        val mod=when(id) {
                            "wow_flutter" -> 1f+lfo*mix*0.12f
                            "pan_motion" -> if(channels>1) (if(ch==0) 1f-max(0f,lfo)*mix else 1f-max(0f,-lfo)*mix) else 1f
                            else -> 1f-mix*0.75f+(lfo*0.5f+0.5f)*mix*0.75f
                        }
                        x[i]=original[i]*mod
                    }
                }
            }
            "vibrato","chorus","flanger","phaser" -> {
                val baseDelay=max(1,rate/if(id=="flanger") 180 else 90)
                for(frame in 0 until frames) {
                    val lfo=(sin(frame*2f*PI.toFloat()*(0.12f+mix*0.8f)/rate)*0.5f+0.5f)
                    val modDelay=max(1,baseDelay+(lfo*baseDelay*(if(id=="chorus") 5 else 2)).toInt())
                    for(ch in 0 until channels) {
                        val i=frame*channels+ch
                        val delayed=if(frame>=modDelay) original[(frame-modDelay)*channels+ch] else 0f
                        val wet=when(id) { "phaser"->original[i]-delayed*(0.5f+mix*0.45f); "flanger"->original[i]+delayed*(0.7f+mix*0.8f); "vibrato"->delayed; else->original[i]*0.65f+delayed*0.75f }
                        x[i]=original[i]*(1f-mix*0.65f)+wet*mix*0.65f
                    }
                }
            }
            "ring_mod","robot","metallic","formant" -> {
                val low=FloatArray(channels)
                val filteredState=FloatArray(channels)
                for(frame in 0 until frames) {
                    val carrier=sin(2f*PI.toFloat()*(if(id=="metallic") 720f else 85f+mix*210f)*frame/rate)
                    for(ch in 0 until channels) {
                        val i=frame*channels+ch
                        val filtered=original[i]-low[ch]*0.82f
                        filteredState[ch]+=(filtered-filteredState[ch])*(0.025f+mix*0.28f)
                        low[ch]+=(original[i]-low[ch])*0.12f
                        val wet=when(id) { "formant"->filteredState[ch]*1.8f; "metallic"->filtered*carrier; else->original[i]*carrier }
                        x[i]=original[i]*(1f-mix)+wet*mix
                    }
                }
            }
            "bitcrush","downsample","vinyl","noise" -> {
                val bits=if(id=="bitcrush") (16-mix*12).toInt().coerceAtLeast(3) else 16
                val levels=((1 shl bits)-1).toFloat()
                val hold=if(id=="downsample") max(1,(1+mix*30).toInt()) else 1
                val held=FloatArray(channels)
                val random=java.util.Random(seed.toLong()*173L+31L)
                for(frame in 0 until frames) {
                    if(frame%hold==0) for(ch in 0 until channels) held[ch]=original[frame*channels+ch]
                    for(ch in 0 until channels) {
                        val i=frame*channels+ch
                        val quantized=(held[ch]*levels).toInt()/levels
                        val hiss=if(id=="noise"||id=="vinyl") (random.nextFloat()-0.5f)*mix*(if(id=="vinyl") 0.07f else 0.025f) else 0f
                        x[i]=if(id=="vinyl") original[i]*(1f-mix*0.18f)+(quantized+hiss)*mix*0.18f else original[i]*(1f-mix)+(quantized+hiss)*mix
                    }
                }
            }
            "pitch_up","pitch_down","octave","fifth","chord","detune" -> {
                val semitones=when(id) { "pitch_up"->2f+mix*10f; "pitch_down"->-2f-mix*10f; "octave"->12f; "fifth"->7f; "detune"->0.2f+mix*1.8f; else->12f }
                addPitchLayer(x,original,channels,mix*if(id=="chord") 0.48f else 0.65f,2f.pow(semitones/12f))
                if(id=="chord") addPitchLayer(x,original,channels,mix*0.32f,2f.pow(7f/12f))
                if(id=="detune") addPitchLayer(x,original,channels,mix*0.5f,1.01f+mix*0.01f)
            }
            "distortion","overdrive","fuzz","saturation","hard_clip","soft_clip","waveshaper" -> {
                val drive=when(id) { "fuzz"->3f+mix*16f; "hard_clip"->1.2f+mix*3f; "soft_clip"->1.5f+mix*7f; else->1.8f+mix*9f }
                val norm=tanh(drive).coerceAtLeast(0.001f)
                for(i in x.indices) {
                    val value=original[i]*drive
                    val wet=when(id) { "hard_clip"->value.coerceIn(-0.55f,0.55f)/0.55f; "fuzz"->tanh(value*1.8f)/norm; "waveshaper"->(value-value.pow(3)*0.22f).coerceIn(-1f,1f); else->tanh(value)/norm }
                    x[i]=original[i]*(1f-mix)+wet*mix
                }
            }
            "low_pass","dark","warm","telephone","radio","high_pass","band_pass","bright","bass_boost" -> {
                val low=FloatArray(channels)
                val alpha=when(id) { "low_pass","dark","warm"->0.025f+(1f-mix)*0.42f; "telephone","radio"->0.16f; else->0.12f+mix*0.4f }
                for(i in x.indices) {
                    val ch=i%channels; val input=original[i]
                    low[ch]+=(input-low[ch])*alpha
                    val high=input-low[ch]
                    val wet=when(id) { "low_pass","dark"->low[ch]; "warm"->low[ch]*0.82f+input*0.18f; "telephone","radio"->high*0.45f+low[ch]*0.75f; "high_pass","bright"->high; "band_pass"->high*0.55f+low[ch]*0.22f; "bass_boost"->input+low[ch]*(0.35f+mix*1.5f); else->input }
                    x[i]=original[i]*(1f-mix*0.75f)+wet*mix*0.75f
                }
                if(id=="radio"||id=="telephone") for(i in x.indices) x[i]*=0.88f
            }
            "gate","expander","compressor","limiter","transient","punch" -> {
                for(i in x.indices) {
                    val value=original[i]; val magnitude=abs(value)
                    val wet=when(id) {
                        "gate"->if(magnitude<0.06f+mix*0.25f) 0f else value
                        "expander"->value*(if(magnitude<0.35f) 0.55f else 1.15f+mix*0.8f)
                        "compressor"->(if(value>=0f) 1f else -1f)*(magnitude/(1f+magnitude*(1.5f+mix*5f)))
                        "limiter"->value/(1f+abs(value)*(2f+mix*12f))
                        "transient","punch"->value*(1f+mix*0.8f)*if((i/channels)<rate/12) 1.4f else 1f
                        else->value
                    }
                    x[i]=original[i]*(1f-mix)+wet*mix
                }
            }
            "widen","mono","phase_invert" -> {
                if(channels>1) for(frame in 0 until frames) {
                    val left=original[frame*channels]; val right=original[frame*channels+1]
                    when(id) {
                        "mono"->{ val mid=(left+right)*0.5f; x[frame*channels]=left*(1f-mix)+mid*mix; x[frame*channels+1]=right*(1f-mix)+mid*mix }
                        "phase_invert"->{ x[frame*channels]=left; x[frame*channels+1]=right*(1f-mix)-left*mix }
                        else->{ val mid=(left+right)*0.5f; val side=(left-right)*0.5f*(1f+mix*1.8f); x[frame*channels]=mid+side; x[frame*channels+1]=mid-side }
                    }
                }
            }
            else -> {
                for(i in x.indices) {
                    val value=original[i]
                    x[i]=when(id) { "vibrato"->value; else->value*(1f-mix*0.15f)+tanh(value*(1f+mix*1.8f))*mix*0.15f }
                }
            }
        }
    }

    private fun addPitchLayer(target:FloatArray,source:FloatArray,channels:Int,amount:Float,ratio:Float) {
        if(amount<=0f) return
        val frames=source.size/channels
        for(frame in 0 until frames) {
            val position=(frame*ratio).coerceIn(0f,(frames-1).toFloat())
            val a=position.toInt(); val b=min(frames-1,a+1); val fraction=position-a
            for(ch in 0 until channels) {
                val shifted=source[a*channels+ch]*(1f-fraction)+source[b*channels+ch]*fraction
                val i=frame*channels+ch; target[i]=(target[i]+shifted*amount).coerceIn(-1f,1f)
            }
        }
    }

    private fun parse(bytes:ByteArray):WavInfo? {
        if(bytes.size<44||ascii(bytes,0,4)!="RIFF"||ascii(bytes,8,4)!="WAVE") return null
        var offset=12; var channels=0; var rate=0; var bits=0; var format=0; var dataOffset=-1; var dataLength=0
        while(offset+8<=bytes.size) {
            val chunk=ascii(bytes,offset,4); val size=readInt(bytes,offset+4).coerceAtLeast(0); val body=offset+8
            if(body.toLong()+size>bytes.size) break
            when(chunk) {
                "fmt "->if(size>=16) { format=readUnsignedShort(bytes,body); channels=readUnsignedShort(bytes,body+2); rate=readInt(bytes,body+4); bits=readUnsignedShort(bytes,body+14) }
                "data"->{ dataOffset=body; dataLength=size }
            }
            offset=body+size+(size and 1)
        }
        if(format!=1||bits!=16||channels !in 1..2||rate !in 8000..192000) return null
        if(dataOffset<0||dataLength<2||dataLength%(2*channels)!=0) return null
        return WavInfo(channels,rate,dataOffset,dataLength)
    }
    private fun ascii(b:ByteArray,o:Int,n:Int)=String(b,o,n,Charsets.US_ASCII)
    private fun readUnsignedShort(b:ByteArray,o:Int)=(b[o].toInt() and 255) or ((b[o+1].toInt() and 255) shl 8)
    private fun readShort(b:ByteArray,o:Int)=readUnsignedShort(b,o).toShort().toInt()
    private fun readInt(b:ByteArray,o:Int)=(b[o].toInt() and 255) or ((b[o+1].toInt() and 255) shl 8) or ((b[o+2].toInt() and 255) shl 16) or (b[o+3].toInt() shl 24)
    private fun writeShort(b:ByteArray,o:Int,v:Int) { b[o]=v.toByte(); b[o+1]=(v shr 8).toByte() }
}
