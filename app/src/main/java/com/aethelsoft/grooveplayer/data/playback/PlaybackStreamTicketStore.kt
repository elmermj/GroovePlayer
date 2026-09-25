package com.aethelsoft.grooveplayer.data.playback

import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTicket
import com.aethelsoft.grooveplayer.domain.playback.PlaybackStreamTickets
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStreamTicketStore @Inject constructor() : PlaybackStreamTickets {
    private val tickets = ConcurrentHashMap<String, PlaybackStreamTicket>()

    override fun put(ticket: PlaybackStreamTicket) {
        tickets[ticket.songId] = ticket
    }

    override fun get(songId: String): PlaybackStreamTicket? = tickets[songId]
}
