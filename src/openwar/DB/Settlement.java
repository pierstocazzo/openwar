/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package openwar.DB;

import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import java.util.ArrayList;
import java.util.HashMap;
import openwar.DB.Building.RecruitmentStats;
import openwar.DB.GenericBuilding.GenericRecruitmentStats;
import openwar.Main;
import openwar.world.WorldEntity;
import openwar.world.WorldMap;

/**
 *
 * @author kehl
 */
public class Settlement extends WorldEntity {

    public class Statistics {

        public int population, total_income;
        public float base_growth, total_order, total_growth;
        public float tax_rate = 5f;
        public HashMap<String, Float> growth_modifier;
        public HashMap<String, Float> income_modifier;
        public HashMap<String, Integer> income_adder;
        public HashMap<String, Float> order_modifier;

        public int computeIncome() {
            float factor = tax_rate / 100f;

            for (Float d : income_modifier.values()) {
                factor += d / 100f;
            }

            total_income = (int) (population * factor);

            for (Integer i : income_adder.values()) {
                total_income += i;
            }

            return total_income;
        }

        public float computeGrowth() {
            total_growth = base_growth / 100f;

            for (Float d : growth_modifier.values()) {
                total_growth += d / 100f;
            }

            return total_growth;
        }

        public float computeOrder() {
            total_order = 1f;

            for (Float d : order_modifier.values()) {
                total_order += d / 100f;
            }

            return total_order;
        }

        public Statistics() {
            growth_modifier = new HashMap<String, Float>();
            income_modifier = new HashMap<String, Float>();
            income_adder = new HashMap<String, Integer>();
            order_modifier = new HashMap<String, Float>();

        }
    }

    public class Construction {

        public String refName;
        public int currentTurn, nrTurns, level;

        public Construction() {
        }
    }

    public class Recruitment {

        public String refName;
        public int currentTurn;

        public Recruitment() {
        }
    }

    public class Dock {

        public int builtLevel;
        public Spatial model;
        public int posX, posZ, spawnX, spawnZ;

        public Dock() {
        }
    }
    public Statistics stats;
    public String name;
    public String region;
    public String culture;
    public int level;
    public HashMap<String, Building> buildings;
    public Spatial billBoard;
    public BitmapText label;
    public Dock dock;
    public ArrayList<Construction> constructions;
    public ArrayList<Recruitment> recruitments;
    public HashMap<String, Construction> constructionPool;
    public HashMap<String, Integer> recruitmentPool;

    public Settlement() {
        super();
        buildings = new HashMap<String, Building>();
        constructions = new ArrayList<Construction>();
        recruitments = new ArrayList<Recruitment>();
        constructionPool = new HashMap<String, Construction>();
        recruitmentPool = new HashMap<String, Integer>();
        stats = new Statistics();

    }

    @Override
    public void createData(WorldMap m) {
        map = m;

        owner = Main.DB.regions.get(region).owner;


        for (Building b : buildings.values()) {
            GenericBuilding gb = Main.DB.genBuildings.get(b.refName);

            for (GenericRecruitmentStats grs : gb.levels.get(b.level).genRecStats.values()) {
                b.createRecruitmentStats(grs);
            }

            // Special cases for roads and docks
            if (gb.levels.get(b.level).provides.containsKey("dock")) {
                String dockLevel = gb.levels.get(b.level).provides.get("dock").get(0);
                constructDock(Integer.parseInt(dockLevel));
            }
        }

        calculateConstructionPool();
        calculateRecruitmentPool();

        String refname = Main.DB.cultures.get(culture).settlementModels.get(level);
        model = Main.DB.models.get(refname).model.clone();
        model.setLocalTranslation(0.25f, 0f, 0.25f);
        node.attachChild(model);


        banner = (Spatial) new Geometry("", new Quad(1f, 2f));
        banner.setLocalTranslation(-0.5f, 1f, 0f);
        Material mat = new Material(map.game.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mat.setTexture("ColorMap", Main.DB.genFactions.get(owner).banner);
        banner.setQueueBucket(Bucket.Translucent);
        banner.setMaterial(mat);
        node.attachChild(banner);

        createBillBoard();


        node.setLocalTranslation(map.getGLTileCenterAboveSea(posX, posZ));
        map.scene.attachChild(node);

        super.createData(m);

    }

    public void changeOwner(String o) {
        Main.DB.regions.get(region).owner = o;
        owner = o;
        Material mat = new Material(map.game.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mat.setTexture("ColorMap", Main.DB.genFactions.get(owner).banner);
        banner.setMaterial(mat);

    }

    public void createBillBoard() {


        label = new BitmapText(map.game.getAssetManager().loadFont("ui/fonts/palatino.fnt"), false);
        label.setSize(25f);
        label.setText(name);
        float width = label.getLineWidth();
        float height = label.getLineHeight();

        label.setColor(ColorRGBA.Black);

        billBoard = new Geometry(name + "_billboard", new Quad(width, height, false));
        Material mat = new Material(map.game.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        //mat.setTexture("ColorMap",map.game.getAssetManager().loadTexture("ui/fonts/palatino.png"));
        billBoard.setMaterial(mat);


        map.game.guiNode.attachChild(billBoard);
        map.game.guiNode.attachChild(label);



    }

    public boolean requirementMet(String name, String value) {

        if ("dock".equals(name) && dock == null) {
            return false;
        }

        return true;
    }

    public void createDockInfo(int posx, int posz, int spawnx, int spawnz) {
        dock = new Dock();
        dock.posX = posx;
        dock.posZ = posz;
        dock.spawnX = spawnx;
        dock.spawnZ = spawnz;
        dock.builtLevel = -1;

    }

    public void constructDock(int level) {
        if (dock == null) {
            return;
        }

        dock.builtLevel = level;

        String refname = Main.DB.cultures.get(culture).dockModels.get(level);
        dock.model = Main.DB.models.get(refname).model.clone();
        dock.model.setLocalTranslation(map.getGLTileCenterAboveSea(dock.posX, dock.posZ));
        Vector3f dir = map.getGLTileCenterAboveSea(dock.spawnX, dock.spawnZ).subtractLocal(
                map.getGLTileCenterAboveSea(dock.posX, dock.posZ));
        Quaternion q = new Quaternion();
        q.lookAt(dir, Vector3f.UNIT_Y);
        dock.model.setLocalRotation(q);
        map.scene.attachChild(dock.model);

    }

    public void calculateConstructionPool() {
        constructionPool.clear();
        // Run trough all generic buildings
        for (String s : Main.DB.genBuildings.keySet()) {

            boolean processed = false;

            // Check if building is in construction list
            for (Construction c : constructions) {
                if (c.refName.equals(s)) {
                    processed = true;
                    break;
                }
            }

            if (processed) {
                continue;
            }


            // Check if building exists and next level can be built
            for (Building b : buildings.values()) {
                if (b.refName.equals(s)) {
                    if (b.level < Main.DB.genBuildings.get(s).maxLevel) {
                        boolean next_level = true;
                        int l = b.level + 1;
                        for (String n : Main.DB.genBuildings.get(s).levels.get(l).requires.keySet()) {
                            String v = Main.DB.genBuildings.get(s).levels.get(l).requires.get(n);
                            next_level &= requirementMet(n, v);
                        }

                        if (next_level) {
                            Construction cons = new Construction();
                            cons.refName = s;
                            cons.level = l;
                            cons.currentTurn = 0;
                            cons.nrTurns = Main.DB.genBuildings.get(s).levels.get(l).turns;
                            constructionPool.put(s, cons);
                        }
                    }
                    processed = true;
                    break;
                }
            }


            if (processed) {
                continue;
            }


            // Check if first level of building can be constructed
            boolean possible = true;
            for (String n : Main.DB.genBuildings.get(s).requires.keySet()) {
                String v = Main.DB.genBuildings.get(s).requires.get(n);
                possible &= requirementMet(n, v);
            }

            for (String n : Main.DB.genBuildings.get(s).levels.get(0).requires.keySet()) {
                String v = Main.DB.genBuildings.get(s).levels.get(0).requires.get(n);
                possible &= requirementMet(n, v);
            }

            if (possible) {
                Construction cons = new Construction();
                cons.refName = s;
                cons.level = 0;
                cons.currentTurn = 0;
                cons.nrTurns = Main.DB.genBuildings.get(s).levels.get(0).turns;
                constructionPool.put(s, cons);
            }

        }

    }

    public void calculateRecruitmentPool() {

        recruitmentPool.clear();


        for (Building b : buildings.values()) {
            for (RecruitmentStats recStats : b.recStats.values()) {
                int recruiting = 0;

                for (Recruitment r : recruitments) {
                    if (r.refName.equals(recStats.refName)) {
                        recruiting++;
                    }
                }
                int available = recStats.currUnits - recruiting;
                if (available > 0) {
                    recruitmentPool.put(recStats.refName, available);
                }
            }
        }


    }

    public void startConstruction(Construction c) {

        if (constructions.size() >= 6) {
            return;
        }

        int cost = Main.DB.genBuildings.get(c.refName).levels.get(c.level).cost;
        if (Main.DB.factions.get(owner).gold < cost) {
            return;
        }
        Main.DB.factions.get(owner).gold -= cost;

        constructions.add(c);
        constructionPool.remove(c.refName);

    }

    public void startRecruitment(String r) {
        if (recruitments.size() >= 10) {
            return;
        }

        int cost = Main.DB.genUnits.get(r).cost;
        if (Main.DB.factions.get(owner).gold < cost) {
            return;
        }


        Recruitment rec = new Recruitment();
        rec.refName = r;
        rec.currentTurn = 0;

        int n = recruitmentPool.get(r) - 1;
        if (n <= 0) {
            recruitmentPool.remove(r);
        } else {
            recruitmentPool.put(r, n);
        }

        Main.DB.factions.get(owner).gold -= cost;
        recruitments.add(rec);
    }

    public void abortConstruction(Construction c) {
        constructions.remove(c);

        int cost = Main.DB.genBuildings.get(c.refName).levels.get(c.level).cost;
        Main.DB.factions.get(owner).gold += cost;


        constructionPool.put(c.refName, c);

    }

    public void abortRecruitment(Recruitment r) {
        recruitments.remove(r);

        int cost = Main.DB.genUnits.get(r.refName).cost;
        Main.DB.factions.get(owner).gold += cost;

        if (recruitmentPool.containsKey(r.refName)) {
            int l = recruitmentPool.get(r.refName);
            recruitmentPool.put(r.refName, l++);
        } else {
            recruitmentPool.put(r.refName, 1);
        }




    }
    
    
    public int calculateBuildingsUpkeep()
    {
        int total=0;
        
        for (Building b : buildings.values())
            total += Main.DB.genBuildings.get(b.refName).levels.get(b.level).upkeep;
        
        return total;
    }

    @Override
    public void newRound() {

        stats.computeIncome();
        stats.computeOrder();
        stats.computeGrowth();
        stats.population += stats.population * stats.total_growth;

        Main.DB.factions.get(owner).gold += stats.total_income
                - calculateUnitsUpkeep() - calculateBuildingsUpkeep();

        resetMovePoints();


        // Update recruitment stats
        for (Building b : buildings.values()) {
            if (b.recStats.isEmpty()) {
                continue;
            }

            for (String s : b.recStats.keySet()) {
                RecruitmentStats rs = b.recStats.get(s);


                // Skip if maximum recruitable units
                if (rs.currUnits >= rs.grs.maxUnits) {
                    continue;
                }

                // Check if new recruit available
                if (rs.turnsTillNextUnit-- <= 0) {
                    rs.currUnits++;
                    rs.turnsTillNextUnit = rs.grs.turnsTillNextUnit;

                }
            }
        }


        // Update current construction
        if (!constructions.isEmpty()) {
            Construction c = constructions.get(0);
            c.currentTurn++;

            // If building is finished
            if (c.currentTurn == c.nrTurns) {
                constructions.remove(0);
                Building b = new Building(c.refName, c.level);

                // Refresh
                GenericBuilding gb = Main.DB.genBuildings.get(c.refName);
                for (GenericRecruitmentStats grs : gb.levels.get(c.level).genRecStats.values()) {
                    b.createRecruitmentStats(grs);
                }

                if (b.level > 0) {
                    buildings.remove(b.refName);
                }

                buildings.put(b.refName, b);
                Region r = Main.DB.regions.get(region);
                String eval = r.owner + "','" + r.refName + "','" + b.refName + "'," + b.level;
                map.game.doScript("onBuildingBuilt('" + eval + ")");

                // Special cases for roads and docks
                if (gb.levels.get(b.level).provides.containsKey("dock")) {
                    String dockLevel = gb.levels.get(b.level).provides.get("dock").get(0);
                    constructDock(Integer.parseInt(dockLevel));
                }

            }
        }

        // Update current recruitment
        if (!recruitments.isEmpty()) {
            Recruitment r = recruitments.get(0);
            r.currentTurn++;
            if (r.currentTurn == Main.DB.genUnits.get(r.refName).turnsToRecruit) {
                recruitments.remove(0);
                Unit u = new Unit(r.refName);


                // Check if we recruited a naval unit and either add to flotilla or create new one
                if (Main.DB.genUnits.get(u.refName).sails) {

                    Army a = map.getArmy(dock.spawnX, dock.spawnZ);
                    if (a == null) {
                        ArrayList<Unit> un = new ArrayList<Unit>();
                        un.add(u);
                        map.createArmy(dock.spawnX, dock.spawnZ, owner, un);
                    } else {
                        a.addUnit(u);
                    }


                } else {
                    units.add(u);
                }
                Region reg = Main.DB.regions.get(region);
                String eval = reg.owner + "','" + reg.refName + "','" + u.refName;
                map.game.doScript("onUnitRecruited('" + eval + "')");
            }


        }
        calculateConstructionPool();
        calculateRecruitmentPool();



    }

    @Override
    public void update(float tpf) {


        Vector3f pos = map.game.getCamera().getScreenCoordinates(map.getGLTileCenter(posX, posZ));
        float width = label.getLineWidth();
        float height = label.getLineHeight();
        billBoard.setLocalTranslation(pos.x - width / 2, pos.y - 50, -1);
        label.setLocalTranslation(pos.x - width / 2, pos.y - 50 + height, -1);

    }

    public Army dispatchArmy(ArrayList<Unit> split) {
        Army a = new Army();
        Main.DB.factions.get(owner).armies.add(a);
        a.owner = owner;
        a.posX = posX;
        a.posZ = posZ;
        mergeUnitsTo(a, split);
        a.createData(map);
        map.scene.attachChild(a.node);
        return a;
    }
}
