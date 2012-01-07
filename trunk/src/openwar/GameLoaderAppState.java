/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package openwar;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import openwar.DB.XMLDataLoader;

/**
 *
 * @author kehl
 */
public class GameLoaderAppState extends AbstractAppState implements ScreenController {

    public enum Status {

        None,
        Init,
        MainMenu,
        Idle,
        LoadingWorldMap
    }
    Main game;
    AppStateManager manager;
    Status status;
    Nifty nifty;
    Screen screen;
    public XMLDataLoader DataLoader;

    public GameLoaderAppState() {
        status = Status.None;
    }

    @Override
    public void bind(Nifty n, Screen s) {
        nifty = n;
        screen = s;
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.initialize(stateManager, (Main) app);
    }

    public void initialize(AppStateManager stateManager, Main main) {
        game = main;
        manager = stateManager;
        status = Status.Init;
        initialized = true;



    }

    public void loadWorldMap() {
        game.nifty.fromXml("ui/loading/ui.xml", "start", this);
        //game.audioState.setMusicMode(AudioAppState.MusicMode.Loading);
        status = Status.LoadingWorldMap;

    }

    public void update(float tpf) {


        switch (status) {

            case Init:
                InitGame();
                status = Status.MainMenu;
                break;

            case MainMenu:
                game.mainMenuState.enterMainMenu();
                game.audioState.setMusicMode(AudioAppState.MusicMode.Menu);
                status = Status.Idle;
                break;


            case LoadingWorldMap:
                manager.attach(game.worldMapState);
                if (Main.devMode) {
                    manager.attach(game.debugState);
                }
                game.audioState.setMusicMode(AudioAppState.MusicMode.WorldMapIdle);
                status = Status.Idle;
                break;
        }



    }

    public void InitGame() {

        manager.attach(game.audioState);
        manager.attach(game.bulletState);
        manager.attach(game.screenshotState);
        manager.attach(game.mainMenuState);

        DataLoader = new XMLDataLoader(game);
        if (!DataLoader.loadAll()) {
            game.wishToQuit = true;
            return;
        }

        game.audioState.setMusicVolume(0.5f);
    }
}
