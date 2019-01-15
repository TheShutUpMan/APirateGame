package uk.ac.york.sepr4.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;
import uk.ac.york.sepr4.screen.GameScreen;

import java.util.*;

public class PirateMap {

    @Getter
    private TiledMap tiledMap;

    private final String objectLayerName = "objects";
    private final String spawnPointObject = "spawn";

    private MapLayer objectLayer;

    @Getter
    private Vector2 spawnPoint;

    @Getter
    private boolean objectsEnabled;

    @Getter
    private List<Polygon> collisionObjects;

    public PirateMap(TiledMap tiledMap) {
        this.tiledMap = tiledMap;
        this.collisionObjects = new ArrayList<>();


        if (checkObjectLayer()) {
            setCollisionObjects();
            this.objectsEnabled = true;
        } else {
            Gdx.app.log("Pirate Map", "Map does NOT contain object layer!");
            this.objectsEnabled = false;
        }

    }

    public Vector2 getSpawnPoint() {
        if (isObjectsEnabled()) {
            return spawnPoint;
        } else {
            return new Vector2(50, 50);
        }
    }

    private void setCollisionObjects() {
        for (MapLayer mapLayer : tiledMap.getLayers()) {

            if (mapLayer instanceof TiledMapTileLayer) {
                TiledMapTileLayer tileLayer = (TiledMapTileLayer) mapLayer;
                Gdx.app.log("PirateMap", "" + mapLayer.getName());

                //scan across
                for (int x = 0; x <= tileLayer.getWidth(); x++) {
                    //scan up
                    for (int y = 0; y <= tileLayer.getHeight(); y++) {
                        TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                        if (cell != null) {
                            TiledMapTile tile = tileLayer.getCell(x, y).getTile();
                            if (tile.getObjects() != null) {
                               // Gdx.app.log("PirateMap", "" + x + ", " + y);

                                Iterator<MapObject> iterator = tile.getObjects().iterator();
                                while (iterator.hasNext()) {
                                    MapObject mapObject = iterator.next();
                                    if (mapObject instanceof PolygonMapObject) {
                                        PolygonMapObject polygonMapObject = (PolygonMapObject) mapObject;
                                        Polygon oldPoly = polygonMapObject.getPolygon();
                                        Polygon polygon = new Polygon();
                                        polygon.setVertices(oldPoly.getVertices());
                                        polygon.setOrigin(oldPoly.getOriginX(), oldPoly.getOriginY());
                                        polygon.setPosition(oldPoly.getX()+x*32f, oldPoly.getY()+y*32f);
                                        polygon.setScale(1/2f, 1/2f);
                                        Gdx.app.log("Polygon", ""+polygonMapObject.getPolygon().getBoundingRectangle().x + ", "+polygonMapObject.getPolygon().getBoundingRectangle().y);
                                        collisionObjects.add(polygon);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        Gdx.app.log("PirateMap", "Loaded " + this.collisionObjects.size() + " collision objects!");
    }

    private boolean checkObjectLayer() {
        this.objectLayer = tiledMap.getLayers().get(objectLayerName);
        if (this.objectLayer != null) {
            return setSpawnObject();
        }
        return false;
    }

    public Optional<RectangleMapObject> getRectObject(String objectName) {
        try {
            MapObject mapObject = objectLayer.getObjects().get(objectName);
            if (mapObject instanceof RectangleMapObject) {
                return Optional.of((RectangleMapObject) mapObject);
            }
        } catch (NullPointerException e) {

        }
        return Optional.empty();
    }

    private boolean setSpawnObject() {
        MapObject mapObject = objectLayer.getObjects().get(spawnPointObject);
        if (mapObject != null && mapObject instanceof RectangleMapObject) {
            RectangleMapObject object = (RectangleMapObject) mapObject;
            Gdx.app.log("pm", object.getRectangle().x+", "+object.getRectangle().y);
            this.spawnPoint = scaleTiledVectorToMap(new Vector2(object.getRectangle().x, object.getRectangle().y));
            return true;
        }
        return false;
    }

    private Vector2 scaleTiledVectorToMap(Vector2 tiledVector) {
        return tiledVector.scl(1 / 2f);
    }

}
