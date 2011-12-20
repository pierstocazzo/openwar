/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package openwar;

import com.jme3.app.state.AbstractAppState;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioNode.Status;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author kehl
 */
public class AudioAppState extends AbstractAppState {

    public Main game;

    public enum MusicMode {

        None, Menu, Loading, WorldMapIdle;
    }
    public String currentMusic;
    public MusicMode mode = MusicMode.None;
    public ArrayList<String> menu, loading, worldMapIdle;
    public float soundVolume = 1f, musicVolume = 1f;
    public float secsBetweenSongs = 5.0f;
    public float currentSecsWait = 0f;
    public AudioNode fadeOutNode = null;

    public AudioAppState(Main g) {
        game = g;
        menu = new ArrayList<String>();
        loading = new ArrayList<String>();

        worldMapIdle = new ArrayList<String>();
    }

    public void setMusicVolume(float v) {
        musicVolume = v;
        for (AudioNode n : Main.DB.musicNodes.values()) {
            n.setVolume(v);
        }
    }

    public void setSoundVolume(float v) {
        soundVolume = v;
        for (AudioNode n : Main.DB.soundNodes.values()) {
            n.setVolume(v);
        }
    }

    public void setMusicMode(MusicMode m) {

        if (m == mode) {
            return;
        }

        if (currentMusic != null) {
            fadeOutNode = Main.DB.musicNodes.get(currentMusic);
        }

        mode = m;
        selectNextSong();



    }

    public void selectNextSong() {
        ArrayList<String> list = null;
        switch (mode) {
            case WorldMapIdle:
                list = worldMapIdle;
                break;

            case Loading:
                list = loading;
                break;
        }

        Random r = new Random();
        currentMusic = list.get(r.nextInt(list.size()));

        Main.DB.musicNodes.get(currentMusic).play();

        if (Main.devMode) {
            System.err.println("Jukebox now playing: " + currentMusic);
        }
    }

    @Override
    public void update(float tpf) {

        if (fadeOutNode != null) {

            fadeOutNode.setVolume(fadeOutNode.getVolume() - 0.01f);

            if (fadeOutNode.getVolume() <= 0.01f) {
                fadeOutNode.stop();
                fadeOutNode.setVolume(soundVolume);
                fadeOutNode = null;
            }

        }
        

        if (mode == MusicMode.None || currentMusic == null) {
            return;
        }

        AudioNode n = Main.DB.musicNodes.get(currentMusic);

        switch (n.getStatus()) {

            case Stopped:


                if (currentSecsWait >= secsBetweenSongs) {
                    selectNextSong();
                    Main.DB.musicNodes.get(currentMusic).play();
                    currentSecsWait = 0f;
                } else {
                    currentSecsWait += tpf;
                }
                break;
        }




    }
}
