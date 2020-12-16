package us.mulb.jeff;

import android.os.VibrationEffect;
import android.util.Log;
import com.neosensory.neosensoryblessed.NeosensoryBlessed;
import com.neosensory.neosensoryblessed.NeoBuzzPsychophysics;
import android.os.Vibrator;

import static android.content.Context.VIBRATOR_SERVICE;

public class NeoVibe {
    private NeosensoryBlessed blessedNeo=null;

    private final String TAG = this.getClass().getSimpleName();
    private final int MIN_MS_BETWEEN_COMMANDS = 100;
    private Vibrator vibrator;

    private final boolean VIBE_PHONE=true;  //TODO: could be a checkbox in the UI

    public NeoVibe(NeosensoryBlessed bn, Vibrator v) {
        setBlessedNeo(bn);
        vibrator = v;
    }

    public void setBlessedNeo(NeosensoryBlessed bn) {
        blessedNeo=bn;
    }

    private boolean sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //Takes individual ints and creates the array that the Neosensory API wants
    public static int[] getVibeArray(int v0, int v1, int v2, int v3) {
        return new int[] {v0, v1, v2, v3,};
    }

    public boolean vibe(int v0, int v1, int v2, int v3) {
        return vibe(getVibeArray(v0,v1,v2,v3));
    }

    public boolean vibe(int[] v){
        //assert(v != null);

        Log.d(TAG,"New Amplitudes: " + v[0] + " " + v[1] + " " + v[2] + " " + v[3]);

        if(blessedNeo != null) {
            return(blessedNeo.vibrateMotors(v));
        }

        if(VIBE_PHONE){
            vibrator.cancel(); //get ready for new command by terminating previous vibration.

            if(v[0]!=0 || v[1]!=0 || v[2]!=0 || v[3]!=0) {
                int intensity = (v[0] + v[1] + v[2] + v[3]) / 4;  //set intensity to average of individual intensities...
                vibrator.vibrate(VibrationEffect.createOneShot(5000, intensity));  //vibe a long time to simulate how buzz works - keeps vibing until gets new command
            }
        }

        return true;
    }

    public boolean vibeAll(int intensity) {
        return vibe(intensity,intensity,intensity,intensity);
    }

    public boolean vibeSingleActuator(int actuatorNumber, int intensity) {
        return vibe(actuatorNumber==0?intensity:0, actuatorNumber==1?intensity:0, actuatorNumber==2?intensity:0, actuatorNumber==3?intensity:0);
    }

    public boolean vibeOff() {
        return vibe(0,0,0,0);
    }


    //Does a psychometric sweep from one position to the other, using the Neosensory psychometric API
    // TODO :   this sleeps between sending motor commands, but does not factor in the time to execute each loop, so vibrations will be somewhat too long.
    //          extra time looks like around 3ms on a Pixel 3a
    // not using sleep may help: https://stackoverflow.com/questions/33335906/is-thread-sleepx-accurate-enough-to-use-as-a-clock-in-android
    public boolean sweep(float positionStart, float positionEnd, int intensity, int milliseconds) {
        if (positionStart<0.0 || positionStart>1.0 || positionEnd<0.0 || positionEnd>1.0) {
            Log.e(TAG, "Position out of range in sweep() - returning without vibing!");
            return false;
        }

        int steps = (milliseconds/MIN_MS_BETWEEN_COMMANDS);
        float positionIncrement = (positionEnd-positionStart)/(steps-1);  //steps-1 since need to get all the way to end of the range
        float curPosition = positionStart;
        for(int i=0; i<steps; i++) {
            vibe(NeoBuzzPsychophysics.GetIllusionActivations(intensity, curPosition));
            sleep(MIN_MS_BETWEEN_COMMANDS);
            curPosition += positionIncrement;
        }
        return vibeOff();
    }

    // The bounce plays the furthest (midpoint of time) location for double the time of the other positions
    public boolean sweepBounce(float positionStart, float positionEnd, int intensity, int milliseconds) {
        sweep(positionStart, positionEnd, intensity, milliseconds/2);
        return sweep(positionEnd, positionStart, intensity, milliseconds/2);
    }



    //just vibe actuators individually, not trying for psychometric sweep
    public boolean sweepDiscreteNOTFINISHED(int actuatorStart, int actuatorEnd, int intensity, int milliseconds) {
        boolean ret=false;
        int vibeMS = milliseconds / Math.abs(actuatorEnd-actuatorStart)+1;

        if(vibeMS<MIN_MS_BETWEEN_COMMANDS) {
            Log.w(TAG, "vibeMS may be too low to maintain correct timing...");
        }


        int actuatorStep = (int)Math.signum(actuatorEnd-actuatorStart);
        for(int i=actuatorStart; i<=actuatorEnd; i+=actuatorStep){
            vibeSingleActuator(i, intensity);
            sleep(vibeMS);
        }
        vibeOff();
        return ret;
    }


    public boolean randomVibes(int msAtEachPosition, int intensity, int milliseconds) {
        boolean ret=false;

        if(msAtEachPosition<MIN_MS_BETWEEN_COMMANDS) {
            Log.w(TAG, "msAtEachPosition may be too low to maintain correct timing...");
        }

        for (int i=0; i<milliseconds; i+=msAtEachPosition) {
            ret=vibe((int)(Math.round(255*Math.random())), (int)(Math.round(255*Math.random())), (int)(Math.round(255*Math.random())), (int)(Math.round(255*Math.random())));
            sleep(msAtEachPosition);
        }
        vibeOff();
        return ret;
    }





    ////////////////////// DEPRECATED TEST CODE /////////////////////////

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
