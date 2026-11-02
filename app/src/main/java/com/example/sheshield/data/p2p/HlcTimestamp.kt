package com.example.sheshield.data.p2p

import org.json.JSONObject

data class HlcTimestamp(
    val wallTime: Long,
    val counter: Int,
    val senderId: String
) : Comparable<HlcTimestamp> {
    override fun compareTo(other: HlcTimestamp): Int {
        if (this.wallTime != other.wallTime) {
            return this.wallTime.compareTo(other.wallTime)
        }
        if (this.counter != other.counter) {
            return this.counter.compareTo(other.counter)
        }
        return this.senderId.compareTo(other.senderId)
    }

    fun toJson(): JSONObject = JSONObject()
        .put("wallTime", wallTime)
        .put("counter", counter)
        .put("senderId", senderId)

    companion object {
        fun fromJson(json: JSONObject): HlcTimestamp = HlcTimestamp(
            wallTime = json.getLong("wallTime"),
            counter = json.getInt("counter"),
            senderId = json.getString("senderId")
        )

        private var latestWallTime = 0L
        private var counter = 0

        @Synchronized
        fun generate(nodeId: String): HlcTimestamp {
            val physicalTime = System.currentTimeMillis()
            if (physicalTime > latestWallTime) {
                latestWallTime = physicalTime
                counter = 0
            } else {
                counter++
            }
            return HlcTimestamp(latestWallTime, counter, nodeId)
        }

        @Synchronized
        fun update(remote: HlcTimestamp, nodeId: String): HlcTimestamp {
            val physicalTime = System.currentTimeMillis()
            val maxWall = maxOf(physicalTime, latestWallTime, remote.wallTime)
            
            if (maxWall == latestWallTime && maxWall == remote.wallTime) {
                counter = maxOf(counter, remote.counter) + 1
                latestWallTime = maxWall
            } else if (maxWall == latestWallTime) {
                counter++
            } else if (maxWall == remote.wallTime) {
                counter = remote.counter + 1
                latestWallTime = maxWall
            } else {
                latestWallTime = physicalTime
                counter = 0
            }
            return HlcTimestamp(latestWallTime, counter, nodeId)
        }
    }
}
