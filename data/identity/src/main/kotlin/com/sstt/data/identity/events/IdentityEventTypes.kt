package com.sstt.data.identity.events

/**
 * Event type names emitted and consumed by the identity data module.
 *
 * The event store keeps event payloads generic, so these constants are the
 * stable contract between identity command writers, projections, and tests.
 */
object IdentityEventTypes {
    const val STUDENT_REGISTERED = "StudentRegistered"
    const val STUDENT_PROFILE_CHANGED = "StudentProfileChanged"
}
