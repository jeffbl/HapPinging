package us.mulb.jeff;

import android.util.Log;
import com.neosensory.neosensoryblessed.NeosensoryBlessed;
import com.neosensory.neosensoryblessed.NeoBuzzPsychophysics;

public class NeoVibe {
    private NeosensoryBlessed blessedNeo=null;

    private final String TAG = this.getClass().getSimpleName();
    private final int MIN_MS_BETWEEN_COMMANDS = 100;

    public NeoVibe(NeosensoryBlessed bn) {
        blessedNeo = bn;
    }

    public static int[] getVibes(int v0, int v1, int v2, int v3) {
        return new int[] {v0, v1, v2, v3,};
    }

    public boolean vibe(int[] v){
        //assert(v != null);
        return blessedNeo.vibrateMotors(v);
    }
    public boolean vibe(int v0, int v1, int v2, int v3) {
        return vibe(new int[] {v0,v1,v2,v3});
    }
    public boolean vibeAll(int intensity) {
        return vibe(intensity,intensity,intensity,intensity);
    }
    public boolean vibeOff() {
        return vibe(0,0,0,0);
    }


    //Does a psychometric sweep from one position to the other
    // TODO : this sleeps between sending motor commands, but does not factor in the time to execute each loop, so vibrations will be somewhat too long
    public boolean sweep(float positionStart, float positionEnd, int intensity, int milliseconds) {
        if (positionStart<0.0 || positionStart>1.0 || positionEnd<0.0 || positionEnd>1.0) {
            Log.e(TAG, "Position out of range in sweep() - returning without vibing!");
            return false;
        }

        int steps = milliseconds/MIN_MS_BETWEEN_COMMANDS;
        float positionIncrement = (positionEnd-positionStart)/steps;
        float curPosition = positionStart;
        for(int i=0; i<steps; i++) {
            vibe(NeoBuzzPsychophysics.GetIllusionActivations(intensity, curPosition));
            sleep(MIN_MS_BETWEEN_COMMANDS);
            curPosition += positionIncrement;
        }
        return vibeOff();
    }

    public boolean sweepBounce(float positionStart, float positionEnd, int intensity, int milliseconds) {
        sweep(positionStart, positionEnd, intensity, milliseconds/2);
        return sweep(positionEnd, positionStart, intensity, milliseconds/2);
    }


    ////////////////////// DEPRECATED TEST CODE /////////////////////////

    private boolean sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //This does not work at all
    public boolean stuffBufferTest() {
        //assuming 16ms slots, does this add up to one second?
        int i;
        for(i=0; i<250/16; i++) {
            blessedNeo.vibrateMotors(new int[]{64, 64, 64, 64});
        }
        return vibeOff();
    }

    public boolean fixedSleepTest() {
        //assuming 16ms slots, does this add up to one second?
        for(int i=0; i<250/MIN_MS_BETWEEN_COMMANDS; i++) {
            blessedNeo.vibrateMotors(new int [] {64,64,64,64});
        }
        return vibeOff();
    }

    // DEPRECATED: manually control actuators to get a sweep, without the psychophysics api, single actuator at a time
    public boolean chunkySweep(int pauseDuration, int intensity) {
        try {
            vibe(255, 0, 0, 0);
            Thread.sleep(pauseDuration);
            vibe(0, 255, 0, 0);
            Thread.sleep(pauseDuration);
            vibe(0, 0, 255, 0);
            Thread.sleep(pauseDuration);
            vibe(0, 0, 0, 255);
            Thread.sleep(pauseDuration);
            return vibeOff();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
