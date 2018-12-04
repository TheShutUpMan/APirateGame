package uk.ac.york.sepr4.object.projectile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import lombok.Getter;
import uk.ac.york.sepr4.object.entity.EntityManager;
import uk.ac.york.sepr4.object.entity.LivingEntity;
import uk.ac.york.sepr4.screen.GameScreen;

public class ProjectileManager {

    //ToDo: Add a function that takes a square or circle and returns them
    @Getter
    private Array<ProjectileType> projectileTypeList;

    @Getter
    private Array<Projectile> projectileList;

    private GameScreen gameScreen;
    private EntityManager entityManager;

    public ProjectileManager(GameScreen gameScreen, EntityManager entityManager) {
        this.gameScreen = gameScreen;
        this.entityManager = entityManager;
        this.projectileList = new Array<Projectile>();

        Json json = new Json();
        projectileTypeList = json.fromJson(Array.class, ProjectileType.class, Gdx.files.internal("projectiles.json"));
    }

    public ProjectileType getDefaultWeaponType() {
        return projectileTypeList.peek();
    }

    private Integer getNextProjectileID(){
        return projectileList.size;
    }

    public void spawnProjectile(ProjectileType projectileType, LivingEntity livingEntity, float speed, float angle) {
        Projectile projectile = new Projectile(getNextProjectileID(), projectileType, livingEntity, speed, angle);
        projectileList.add(projectile);
    }

    /**
     * Adds and removes projectiles as actors from the stage.
     */
    public void handleProjectiles(Stage stage) {
        stage.getActors().removeAll(removeNonActiveProjectiles(), true);

        for (Projectile projectile : getProjectileList()) {
            if (!stage.getActors().contains(projectile, true)) {
                Gdx.app.log("Test Log", "Adding new projectile to actors list.");
                stage.addActor(projectile);
            }
        }
    }

    public Array<Projectile> removeNonActiveProjectiles() {
        Array<Projectile> toRemove = new Array<Projectile>();
        for(Projectile projectile : projectileList) {
            if(!projectile.isActive()){
                toRemove.add(projectile);
            }
        }
        projectileList.removeAll(toRemove, true);
        return toRemove;
    }

}
