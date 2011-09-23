/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package openwar.DB;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.UpdateControl;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.Callable;
import openwar.Main;
import openwar.world.Tile;
import openwar.world.WorldEntity;
import openwar.world.WorldMap;

/**
 *
 * @author kehl
 */
public class Army extends WorldEntity {

    public int currMovePoints;
    public Stack<Tile> route;
    public boolean onRoute = false;

    public Army() {

        super();

    }

    @Override
    public void createData(WorldMap m) {

        this.map = m;

        model = (Spatial) new Geometry("", new Sphere(10, 10, 0.5f));
        model.setMaterial(new Material(map.game.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md"));
        model.setShadowMode(ShadowMode.CastAndReceive);
        node.setLocalTranslation(map.getGLTileCenter(posX, posZ));
        model.setLocalTranslation(0f, 0.5f, 0f);
        node.attachChild(model);

        banner = (Spatial) new Geometry("", new Quad(1f, 2f));
        banner.setLocalTranslation(-0.5f, 2f, 0);
        Material mat = new Material(map.game.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mat.setTexture("ColorMap", Main.DB.genFactions.get(owner).banner);
        banner.setQueueBucket(Bucket.Translucent);
        banner.setMaterial(mat);
        node.attachChild(banner);

        map.scene.attachChild(node);
        calculateMovePoints();

    }

    public void addUnit(Unit u) {
        units.add(u);
        calculateMovePoints();
    }

    @Override
    public int calculateMovePoints() {

        return currMovePoints = super.calculateMovePoints();

    }
    
    @Override
    public void update(float tpf) {

        if (!onRoute) {
            return;
        }
        if (route == null || route.isEmpty()) {
            route = null;
            onRoute = false;

        } else {

            Tile t = route.peek();

            if (currMovePoints < map.getTileCosts(t)) {
                onRoute = false;
                return;
            }

            Vector3f checkpoint = map.getGLTileCenter(t);

            // next checkpoint reached
            if (checkpoint.distanceSquared(node.getLocalTranslation()) < 0.0025f) {
                route.pop();
                reduceMovePoints(map.getTileCosts(t));


                if (route.isEmpty()) {
                    Army a = map.getArmy(t);
                    if (a != null) {

                        if (a.owner.equals(owner)) {
                            mergeWith(a);
                        } else {
                            ArrayList<Army> a1 = new ArrayList<Army>();
                            ArrayList<Army> a2 = new ArrayList<Army>();
                            a1.add(this);
                            a2.add(a);
                            int result = map.game.worldMapState.battle(a1, a2);


                        }
                    }
                }

                posX = t.x;
                posZ = t.z;
                node.setLocalTranslation(map.getGLTileCenter(t));


                if (map.selectedArmy == this) {
                    map.drawReachableArea(this);
                }

                if (route.isEmpty()) {

                    Settlement s = map.getSettlement(t);
                    if (s != null) {
                        garrisonArmy(s);
                    }
                    return;
                }

            }

            t = route.peek();
            checkpoint = map.getGLTileCenter(t);
            Vector3f dir = checkpoint.subtract(node.getLocalTranslation()).normalizeLocal();
            node.setLocalTranslation(node.getLocalTranslation().addLocal(dir.multLocal(tpf)));



        }

    }

    public void setRoute(Stack<Tile> r) {

        r.pop();
        route = r;
        onRoute = true;


    }

    public void mergeWith(final Army a) {
        final Army l = this;
        for (Unit u : l.units) {
            a.units.add(u);
        }

        map.scene.addControl(new UpdateControl());
        map.scene.getControl(UpdateControl.class).enqueue(new Callable() {

            @Override
            public Object call() throws Exception {


                map.removeArmy(l);
                if (map.selectedArmy == l || map.selectedArmy == a) {
                    map.selectArmy(a);
                }

                return null;
            }
        });

    }

    public Army cloneSimple() {
        Army a = new Army();
        Main.DB.hashedFactions.get(owner).armies.add(a);
        a.owner = owner;
        a.posX = posX;
        a.posZ = posZ;

        return a;
    }

    public Army splitThisArmy(ArrayList<Unit> split) {

        Army a = cloneSimple();
        mergeUnitsTo(a, split);
        a.createData(map);
        map.scene.attachChild(a.node);


        return a;
    }

    public void splitOtherArmy(Army from, ArrayList<Unit> split) {

        mergeUnitsFrom(from, split);

    }

    public void garrisonArmy(final Settlement s) {

        final Army a = this;
        for (Unit u : a.units) {
            s.units.add(u);
        }

        map.scene.addControl(new UpdateControl());
        map.scene.getControl(UpdateControl.class).enqueue(new Callable() {

            @Override
            public Object call() throws Exception {


                map.removeArmy(a);
                if (map.selectedArmy == a) {
                    map.selectSettlement(s);
                }
                return null;
            }
        });
    }
}
