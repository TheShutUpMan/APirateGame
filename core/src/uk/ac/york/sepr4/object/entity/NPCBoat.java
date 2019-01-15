package uk.ac.york.sepr4.object.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import lombok.Data;
import lombok.Getter;
import uk.ac.york.sepr4.object.building.College;
import uk.ac.york.sepr4.object.projectile.Projectile;
import uk.ac.york.sepr4.object.projectile.ProjectileType;
import uk.ac.york.sepr4.screen.GameScreen;
import uk.ac.york.sepr4.utils.AIUtil;

import java.util.List;
import java.util.Optional;
import java.util.zip.DeflaterOutputStream;

@Data
public class NPCBoat extends LivingEntity {

    //NPCBoat-specific variables
    private float range;
    private float accuracy;

    private float idealDistFromTarget;
    private float gradientForNormalDist;

    private boolean hostile;
    private College allied;
    private Optional<LivingEntity> lastTarget;

    private float targetCheck;

    public NPCBoat(Integer id, Texture texture, float angle, float speed, float maxSpeed, Double health, Double maxHealth, Integer turningSpeed, boolean onFire, List<ProjectileType> projectileTypes, float range, float accuracy, boolean hostile, College allied, float idealDistFromTarget, float gradientForNormalDist) {
        super(id, texture, angle, speed, maxSpeed, health, maxHealth, turningSpeed, onFire, projectileTypes);
        this.range = range;
        this.accuracy = accuracy;

        this.idealDistFromTarget = idealDistFromTarget;
        this.gradientForNormalDist = gradientForNormalDist;

        this.hostile = hostile;
        this.allied = allied;

        this.lastTarget = Optional.empty();
        this.targetCheck = 4f;
    }

    public void act(float deltaTime) {
        setAccelerating(true);
        Array<Float> forces = new Array<Float>();
        Array<Float> angles = new Array<Float>();

        if (!this.isDying()) {
            if (this.hostile) {
                if (targetCheck < 5f) {
                    targetCheck += deltaTime;
                }
                Optional<LivingEntity> optionalTarget = getTarget();
                if (optionalTarget.isPresent()) {
                    LivingEntity target = optionalTarget.get();
                    this.lastTarget = optionalTarget;

                    //Todo: Finish the control logic of enemy to make it better
                    if ((float) this.distanceFrom(target) < this.idealDistFromTarget) {
                        forces.add(1 - AIUtil.normalDistFromMean((float) this.distanceFrom(target), this.gradientForNormalDist, this.idealDistFromTarget));
                        angles.add(AIUtil.convertToRealAngle(this.getAngleTowardsEntity(target)));
                    } else {
                        forces.add(1 - AIUtil.normalDistFromMean((float) this.distanceFrom(target), this.gradientForNormalDist, this.idealDistFromTarget));
                        angles.add(AIUtil.convertToRealAngle(this.getAngleTowardsEntity(target) - (float) Math.PI));
                    }
                    forces.add(AIUtil.normalDistFromMean((float) this.distanceFrom(target), this.gradientForNormalDist, this.idealDistFromTarget));
                    angles.add(AIUtil.convertToRealAngle(target.getAngle() - (float) Math.PI));

                    //TODO: You were here next thing to do is to stop the boats turning to quickly then do the logic for the ai

//                    Array<LivingEntity> livingEntities = getLivingEntitiesInRange();
//
//                    for(LivingEntity livingEntity : livingEntities){
//                        forces.add(AIUtil.straightLineGraphOneIfCloser((float)this.distanceFrom(livingEntity), this.gradientForNormalDist, this.idealDistFromTarget));
//
//                        angles.add((float)(this.getAngleTowardsEntity(livingEntity) + Math.PI));
//                    }

                    float ang = AIUtil.resultantForce(angles, forces).get(1);
                    if(!Float.isNaN(ang)) {
                        setAngle(ang);
                    } else {
                        Gdx.app.error("NPCBoat", "Angle NaN!");
                    }

                }

            } else {
                // move in area?
            }
        }
        super.act(deltaTime);
    }

    public Optional<College> getAllied() {
        if (this.allied == null) {
            return Optional.empty();
        }
        return Optional.of(allied);
    }

    private boolean validTarget(Optional<LivingEntity> optionalLivingEntity) {
        if (optionalLivingEntity.isPresent()) {
            LivingEntity livingEntity = optionalLivingEntity.get();
            if (!(livingEntity.isDying() || livingEntity.isDead())) {
                if (livingEntity.distanceFrom(this) <= range) {
                    //if last target exists, not dead and is still in range
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<LivingEntity> getTarget() {
        if (validTarget(this.lastTarget)) {
            //Gdx.app.log("Target", "Last");
            return this.lastTarget;
        } else {
            this.lastTarget = Optional.empty();
            if (targetCheck > 5f) {
                //Gdx.app.log("Target", "Nearest");
                targetCheck = 0f;
                return getNearestTarget();
            } else {
                return Optional.empty();
            }
        }
    }

    private boolean areAllied(LivingEntity livingEntity) {
        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;
            if (getAllied().isPresent()) {
                return player.getCaptured().contains(getAllied().get());
            }
        } else {
            //must be an NPCBoat
            NPCBoat npcBoat = (NPCBoat) livingEntity;
            if (npcBoat.getAllied().isPresent() && getAllied().isPresent()) {
                return (npcBoat.getAllied().get().equals(getAllied().get()));
            }
        }

        return false;
    }

    private Array<LivingEntity> getLivingEntitiesInRange() {
        Array<LivingEntity> nearby = GameScreen.getInstance().getEntityManager().getLivingEntitiesInArea(getRangeArea());
        if (nearby.contains(this, false)) {
            nearby.removeValue(this, false);
        }
        return nearby;
    }

    private Array<Projectile> getProjectilesInRange() {
        Array<Projectile> nearby = GameScreen.getInstance().getEntityManager().getProjectileManager().getProjectileInArea(getRangeArea());
        return nearby;
    }

    private Array<Entity> getProjectilesToDodge(Array<Projectile> projectiles) {
        Array<Entity> projectilesToDodge = new Array<Entity>();
        for (Projectile projectile : projectiles) {
            float thetaToThisInFuture = AIUtil.perfectAngleToCollide(projectile, this, projectile.getSpeed());
            float thetaActual = AIUtil.convertToRealAngle(projectile.getAngle());
            float dist = (float) projectile.distanceFrom(this);
            boolean isTriangle = true;
            float theta;
            if (thetaToThisInFuture < thetaActual && thetaActual - thetaToThisInFuture < Math.PI / 2) {
                theta = thetaActual - thetaToThisInFuture;
            } else if (thetaActual < thetaToThisInFuture && thetaToThisInFuture - thetaActual < Math.PI / 2) {
                theta = thetaToThisInFuture - thetaActual;
            } else if (thetaActual < thetaToThisInFuture && (2 * Math.PI - thetaToThisInFuture) + thetaActual < Math.PI / 2) {
                theta = (float) (2 * Math.PI - thetaToThisInFuture) + thetaActual;
            } else if (thetaToThisInFuture < thetaActual && (2 * Math.PI - thetaActual) + thetaToThisInFuture < Math.PI / 2) {
                theta = (float) (2 * Math.PI - thetaActual) + thetaToThisInFuture;
            } else {
                theta = 0;
                isTriangle = false;
            }

            if (isTriangle == true) {
                float opp = (float) Math.tan(theta) * dist;
                if (opp < 0) {
                    opp = -opp;
                }
                if (opp < Math.max(3 * this.getRectBounds().height / 4, 3 * this.getRectBounds().width / 4)) {
                    projectilesToDodge.add(projectile);
                }
            }

        }
        return projectilesToDodge;
    }

    private Optional<LivingEntity> getNearestTarget() {
        Player player = GameScreen.getInstance().getEntityManager().getOrCreatePlayer();
        Array<LivingEntity> nearby = getLivingEntitiesInRange();
        if (!areAllied(player)) {
            //not allied - target player
            if (nearby.contains(player, false)) {
                //if player is in range - target
                return Optional.of(player);
            }
        }
        //player has captured this NPC's allied college
        if (nearby.size > 0) {
            Optional<LivingEntity> nearest = Optional.empty();
            for (LivingEntity livingEntity : nearby) {
                if (!areAllied(livingEntity)) {
                    if (nearest.isPresent()) {
                        if (nearest.get().distanceFrom(this) > livingEntity.distanceFrom(this)) {
                            //closest enemy
                            nearest = Optional.of(livingEntity);
                        }
                    } else {
                        nearest = Optional.of(livingEntity);
                    }
                }
            }
            return nearest;
        }

        return Optional.empty();
    }

    private Rectangle getRangeArea() {
        Rectangle radius = getRectBounds();
        radius.set(radius.x - range, radius.y - range, radius.width + 2 * range, radius.height + 2 * range);
        return radius;
    }

}
