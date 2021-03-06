/*
 * Copyright 2020 Anton Trushkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ksdfv.thelema.phys.ode4j

import org.ksdfv.thelema.ext.traverseSafe
import org.ksdfv.thelema.math.IVec3
import org.ksdfv.thelema.math.Vec3
import org.ksdfv.thelema.phys.*
import org.ksdfv.thelema.utils.Pool
import org.ode4j.math.DVector3
import org.ode4j.ode.*
import kotlin.math.max


/** [ODE manual](http://ode.org/wiki/index.php?title=Manual)
 *
 * @author zeganstyl */
class OdePhysicsWorld: IPhysicsWorld {
    val world = OdeHelper.createWorld().apply {
        // https://ode.org/ode-latest-userguide.html#sec_3_7_0
        erp = 0.2

        // https://ode.org/ode-latest-userguide.html#sec_3_8_2
        cfm = 0.0001
    }

    var space = OdeHelper.createHashSpace(null)
    val contactGroup = OdeHelper.createJointGroup()

    var maxContacts = 40

    /** After contact life time spending, contact will be ended and removed */
    var contactMaxLifeTime = 0.1f
    val aliveContacts = HashMap<BodyContactPair, BodyContactPair>()
    val newContacts = HashSet<BodyContactPair>()
    val oldContacts = HashSet<BodyContactPair>()

    val geomAliveContacts = HashMap<GeomContactPair, GeomContactPair>()
    val geomNewContacts = HashSet<GeomContactPair>()
    val geomOldContacts = HashSet<GeomContactPair>()

    val listeners = ArrayList<IPhysicsWorldListener>()

    var minStep = 0.02

    var nearCallback = DGeom.DNearCallback { _, o1, o2 ->
        val g1 = o1.data as IOdeGeom
        val g2 = o2.data as IOdeGeom
        val b1 = o1.body
        val b2 = o2.body
        val contacts = DContactBuffer(maxContacts) // up to MAX_CONTACTS contacts per box-box
        if (b1 != null && b2 != null && OdeHelper.areConnectedExcluding(b1, b2, DContactJoint::class.java)) return@DNearCallback
        val bm1 = b1?.data as OdeRigidBody?
        val bm2 = b2?.data as OdeRigidBody?
        for (i in 0 until maxContacts) {
            val contact = contacts[i]
            contact.surface.mu = max(bm1?.friction ?: g1.friction, bm2?.friction ?: g2.friction).toDouble()

            contact.surface.mode = OdeConstants.dContactBounce or OdeConstants.dContactSoftCFM
            contact.surface.mu2 = 0.0
            contact.surface.bounce = 0.1
            contact.surface.bounce_vel = 0.1
            contact.surface.soft_cfm = 0.01
        }

        val numContacts = OdeHelper.collide(o1, o2, maxContacts, contacts.geomBuffer)
        if (numContacts != 0) {
            for (i in 0 until numContacts) {
                val contact = contacts[i]
                val c: DJoint = OdeHelper.createContactJoint(world, contactGroup, contact)

                val influence1 = bm1?.influenceOtherBodies ?: g1.influenceOtherBodies
                val influence2 = bm2?.influenceOtherBodies ?: g2.influenceOtherBodies

                // http://ode.org/wiki/index.php?title=Manual#How_do_I_make_.22one_way.22_collision_interaction
                when {
                    !influence1 && !influence2 -> {}
                    else -> {
                        if (influence1 && !influence2) {
                            c.attach(null, b2)
                        } else if (!influence1 && influence2) {
                            c.attach(b1, null)
                        } else {
                            c.attach(b1, b2)
                        }
                    }
                }

                // <Collect collisions> ================================================================================
                val geomPair = geomPairsPool.get()
                geomPair.a = g1
                geomPair.b = g2
                geomPair.depth = contact.contactGeom.depth
                geomPair.lifeTime = contactMaxLifeTime
                val geomAlivePair = geomAliveContacts[geomPair]
                if (geomAlivePair == null) {
                    geomNewContacts.add(geomPair)
                } else {
                    // we need only hash from new pair and we will get current alive pair
                    geomAlivePair.lifeTime = contactMaxLifeTime
                    geomAlivePair.depth = geomPair.depth
                }

                if (bm1 != null && bm2 != null) {
                    val pair = bodyPairsPool.get()
                    pair.a = bm1
                    pair.b = bm2
                    pair.depth = contact.contactGeom.depth
                    pair.lifeTime = contactMaxLifeTime
                    val alivePair = aliveContacts[pair]
                    if (alivePair == null) {
                        newContacts.add(pair)
                    } else {
                        // we need only hash from new pair and we will get current alive pair
                        alivePair.lifeTime = contactMaxLifeTime
                        alivePair.depth = pair.depth
                    }
                }
                // </Collect collisions> ===============================================================================
            }
        }
    }

    override val sourceObject: Any
        get() = world

    override fun setGravity(x: Float, y: Float, z: Float) {
        world.setGravity(x.toDouble(), y.toDouble(), z.toDouble())
    }

    override fun getGravity(out: IVec3): IVec3 {
        world.getGravity(tmp)
        out.set(tmp.get0().toFloat(), tmp.get1().toFloat(), tmp.get2().toFloat())
        return out
    }

    override fun addPhysicsWorldListener(listener: IPhysicsWorldListener) {
        listeners.add(listener)
    }

    override fun removePhysicsWorldListener(listener: IPhysicsWorldListener) {
        listeners.remove(listener)
    }

    override fun checkCollision(
        shape1: IShape,
        shape2: IShape,
        out: MutableList<IContactInfo>
    ): MutableList<IContactInfo> {
        val contacts = DContactBuffer(maxContacts)
        shape1 as IOdeGeom
        shape2 as IOdeGeom
        val numContacts = OdeHelper.collide(shape1.geom, shape2.geom, maxContacts, contacts.geomBuffer)
        for (i in 0 until numContacts) {
            val contact = contacts[i]
            val pos = contact.contactGeom.pos
            val nor = contact.contactGeom.normal
            out.add(OdeContactInfo(
                Vec3(pos.get0().toFloat(), pos.get1().toFloat(), pos.get2().toFloat()),
                Vec3(nor.get0().toFloat(), nor.get1().toFloat(), nor.get2().toFloat()),
                contact.contactGeom.depth.toFloat()
            ))
        }
        return out
    }

    override fun step(delta: Float) {
        newContacts.clear()
        space.collide(null, nearCallback)
        world.quickStep(minStep)

        contactGroup.empty()

        // remove old contacts
        oldContacts.clear()
        oldContacts.addAll(aliveContacts.values)
        oldContacts.forEach { pair ->
            pair.lifeTime -= delta
            if (pair.lifeTime < 0f) {
                aliveContacts.remove(pair)
                listeners.traverseSafe { it.collisionEnd(pair.a, pair.b) }
                bodyPairsPool.free(pair)
            }
        }

        // add new contacts
        newContacts.removeAll(aliveContacts.values)
        newContacts.forEach { pair ->
            aliveContacts[pair] = pair
            listeners.traverseSafe {
                it.collisionBegin(pair.a, pair.b, pair.depth.toFloat())
            }
        }



        // remove old contacts
        geomOldContacts.clear()
        geomOldContacts.addAll(geomAliveContacts.values)
        geomOldContacts.forEach { pair ->
            pair.lifeTime -= delta
            if (pair.lifeTime < 0f) {
                geomAliveContacts.remove(pair)
                listeners.traverseSafe { it.collisionEnd(pair.a, pair.b) }
                geomPairsPool.free(pair)
            }
        }

        // add new contacts
        geomNewContacts.removeAll(geomAliveContacts.values)
        geomNewContacts.forEach { pair ->
            geomAliveContacts[pair] = pair
            listeners.traverseSafe {
                it.collisionBegin(pair.a, pair.b, pair.depth.toFloat())
            }
        }
    }

    override fun rayShape(length: Float): IRay {
        return OdeRay(this, length)
    }

    override fun heightField(
        width: Float,
        depth: Float,
        widthSamples: Int,
        depthSamples: Int,
        scale: Float,
        offset: Float,
        thickness: Float,
        tiling: Boolean,
        heightProvider: IHeightProvider?
    ): IHeightField {
        return OdeHeightField(this, width, depth, widthSamples, depthSamples, scale, offset, thickness, tiling, heightProvider)
    }

    override fun rigidBody(shape: IShape?, mass: Float): IRigidBody {
        val body = OdeRigidBody(world = this, mass = mass)
        body.shape = shape
        return body
    }

    override fun boxShape(xSize: Float, ySize: Float, zSize: Float): IBoxShape {
        return OdeBoxShape(this, xSize, ySize, zSize)
    }

    override fun sphereShape(radius: Float): ISphereShape {
        return OdeSphereShape(this, radius)
    }
    override fun capsuleShape(radius: Float, length: Float): ICapsuleShape {
        return OdeCapsuleShape(this, radius, length)
    }
    override fun cylinderShape(radius: Float, length: Float): ICylinderShape {
        return OdeCylinderShape(this, radius, length)
    }
    override fun trimeshShape(vertices: FloatArray, indices: IntArray): ITrimeshShape {
        return OdeTrimeshShape(this, vertices, indices)
    }
    override fun planeShape(): IPlaneShape {
        return OdePlaneShape(this)
    }

    override fun isContactExist(body1: IRigidBody, body2: IRigidBody): Boolean = false

    override fun destroy() {
        contactGroup.destroy()
        space.destroy()
        world.destroy()
    }

    companion object {
        val tmp = DVector3()

        val bodyPairsPool = Pool { BodyContactPair() }
        val geomPairsPool = Pool { GeomContactPair() }
    }
}